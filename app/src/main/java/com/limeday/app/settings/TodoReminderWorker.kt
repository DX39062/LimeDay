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
import com.limeday.app.data.TodoItem
import java.util.concurrent.TimeUnit

class TodoReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val todoId = inputData.getString(KEY_TODO_ID) ?: return Result.failure()
        val expectedAt = inputData.getLong(KEY_REMINDER_AT, -1L)
        val application = applicationContext as LimeDayApplication
        val todo = application.repository.todoById(todoId) ?: return Result.success()
        if (todo.deletedAt != null || todo.isCompleted || todo.reminderAt != expectedAt) return Result.success()
        showNotification(todo)
        return Result.success()
    }

    private fun showNotification(todo: TodoItem) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        createChannel(applicationContext)
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_SELECTED_DATE, todo.date)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            todo.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("待办提醒")
            .setContentText(todo.title)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(todo.id.hashCode(), notification)
    }

    companion object {
        private const val KEY_TODO_ID = "todo-id"
        private const val KEY_REMINDER_AT = "reminder-at"
        private const val CHANNEL_ID = "todo-item-reminders"

        fun schedule(context: Context, todo: TodoItem) {
            val reminderAt = todo.reminderAt
            if (reminderAt == null || reminderAt <= System.currentTimeMillis() || todo.isCompleted || todo.deletedAt != null) {
                cancel(context, todo.id)
                return
            }
            val request = OneTimeWorkRequestBuilder<TodoReminderWorker>()
                .setInitialDelay((reminderAt - System.currentTimeMillis()).coerceAtLeast(0), TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(KEY_TODO_ID to todo.id, KEY_REMINDER_AT to reminderAt))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(workName(todo.id), ExistingWorkPolicy.REPLACE, request)
        }

        fun cancel(context: Context, todoId: String) {
            WorkManager.getInstance(context).cancelUniqueWork(workName(todoId))
            NotificationManagerCompat.from(context).cancel(todoId.hashCode())
        }

        private fun workName(todoId: String) = "todo-reminder-$todoId"

        private fun createChannel(context: Context) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "待办事项提醒", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "单条待办的自定义提醒"
                }
            )
        }
    }
}
