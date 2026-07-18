package com.limeday.app.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.limeday.app.LimeDayApplication
import com.limeday.app.settings.TodoReminderWorker
import java.util.concurrent.TimeUnit

class WebDavSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val application = applicationContext as LimeDayApplication
        val config = application.webDavConfigStore.load()
        if (!config.isConfigured) return Result.success()
        return runCatching {
            application.syncCoordinator.sync(config)
            application.repository.activeReminderTodos().forEach { TodoReminderWorker.schedule(application, it) }
        }
            .fold(
                onSuccess = { Result.success() },
                onFailure = { error ->
                    application.repository.recordSync(error.message ?: "后台同步失败")
                    if ((error as? WebDavException)?.retryable == true) Result.retry() else Result.failure()
                }
            )
    }

    companion object {
        private const val WORK_NAME = "limeday-webdav-sync"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WebDavSyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancel(context: Context) = WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
