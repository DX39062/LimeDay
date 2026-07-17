package com.limeday.app.settings

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.limeday.app.LimeDayApplication
import com.limeday.app.MainActivity
import com.limeday.app.R
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class DailyReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val type = ReminderType.fromValue(inputData.getString(KEY_TYPE)) ?: return Result.failure()
        val application = applicationContext as LimeDayApplication
        val settings = application.appSettingsStore.settings.value
        if (type.isEnabled(settings)) showNotification(type)
        schedule(applicationContext, type, settings, ExistingWorkPolicy.APPEND_OR_REPLACE)
        return Result.success()
    }

    private fun showNotification(type: ReminderType) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        createChannel(applicationContext)
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            type.notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(type.title)
            .setContentText(type.message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(type.notificationId, notification)
    }

    enum class ReminderType(
        val value: String,
        val workName: String,
        val notificationId: Int,
        val title: String,
        val message: String
    ) {
        TODO("todo", "limeday-todo-reminder", 2101, "今天要做什么？", "打开青柠日记，安排今天最重要的事。"),
        REVIEW("review", "limeday-review-reminder", 2102, "该复盘今天了", "回顾完成的事情，为明天留下一点方向。");

        fun isEnabled(settings: AppSettings): Boolean = when (this) {
            TODO -> settings.todoReminderEnabled
            REVIEW -> settings.reviewReminderEnabled
        }

        fun hour(settings: AppSettings): Int = when (this) {
            TODO -> settings.todoReminderHour
            REVIEW -> settings.reviewReminderHour
        }

        fun minute(settings: AppSettings): Int = when (this) {
            TODO -> settings.todoReminderMinute
            REVIEW -> settings.reviewReminderMinute
        }

        companion object {
            fun fromValue(value: String?): ReminderType? = entries.firstOrNull { it.value == value }
        }
    }

    companion object {
        private const val KEY_TYPE = "reminder-type"
        private const val CHANNEL_ID = "daily-reminders"

        fun scheduleAll(context: Context, settings: AppSettings) {
            ReminderType.entries.forEach { schedule(context, it, settings) }
        }

        fun schedule(
            context: Context,
            type: ReminderType,
            settings: AppSettings,
            policy: ExistingWorkPolicy = ExistingWorkPolicy.REPLACE
        ) {
            val workManager = WorkManager.getInstance(context)
            if (!type.isEnabled(settings)) {
                workManager.cancelUniqueWork(type.workName)
                return
            }
            val now = ZonedDateTime.now()
            var next = now.withHour(type.hour(settings)).withMinute(type.minute(settings)).withSecond(0).withNano(0)
            if (!next.isAfter(now)) next = next.plusDays(1)
            val delay = Duration.between(now, next).toMillis().coerceAtLeast(0)
            val request = OneTimeWorkRequestBuilder<DailyReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(KEY_TYPE to type.value))
                .build()
            workManager.enqueueUniqueWork(type.workName, policy, request)
        }

        fun createChannel(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "每日提醒", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "每日待办和复盘提醒"
                }
            )
        }
    }
}
