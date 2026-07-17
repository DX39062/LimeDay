package com.limeday.app.settings

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val label: String) {
    SYSTEM("跟随系统"),
    LIGHT("浅色"),
    DARK("深色")
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val todoReminderEnabled: Boolean = false,
    val todoReminderHour: Int = 9,
    val todoReminderMinute: Int = 0,
    val reviewReminderEnabled: Boolean = false,
    val reviewReminderHour: Int = 21,
    val reviewReminderMinute: Int = 0
)

class AppSettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val mutableSettings = MutableStateFlow(load())

    val settings: StateFlow<AppSettings> = mutableSettings.asStateFlow()

    fun setThemeMode(mode: ThemeMode) = update { copy(themeMode = mode) }

    fun setTodoReminder(enabled: Boolean, hour: Int, minute: Int) = update {
        copy(
            todoReminderEnabled = enabled,
            todoReminderHour = hour.coerceIn(0, 23),
            todoReminderMinute = minute.coerceIn(0, 59)
        )
    }

    fun setReviewReminder(enabled: Boolean, hour: Int, minute: Int) = update {
        copy(
            reviewReminderEnabled = enabled,
            reviewReminderHour = hour.coerceIn(0, 23),
            reviewReminderMinute = minute.coerceIn(0, 59)
        )
    }

    private fun update(transform: AppSettings.() -> AppSettings) {
        val value = mutableSettings.value.transform()
        preferences.edit {
            putString(KEY_THEME, value.themeMode.name)
            putBoolean(KEY_TODO_ENABLED, value.todoReminderEnabled)
            putInt(KEY_TODO_HOUR, value.todoReminderHour)
            putInt(KEY_TODO_MINUTE, value.todoReminderMinute)
            putBoolean(KEY_REVIEW_ENABLED, value.reviewReminderEnabled)
            putInt(KEY_REVIEW_HOUR, value.reviewReminderHour)
            putInt(KEY_REVIEW_MINUTE, value.reviewReminderMinute)
        }
        mutableSettings.value = value
    }

    private fun load(): AppSettings = AppSettings(
        themeMode = runCatching {
            ThemeMode.valueOf(preferences.getString(KEY_THEME, ThemeMode.SYSTEM.name).orEmpty())
        }.getOrDefault(ThemeMode.SYSTEM),
        todoReminderEnabled = preferences.getBoolean(KEY_TODO_ENABLED, false),
        todoReminderHour = preferences.getInt(KEY_TODO_HOUR, 9).coerceIn(0, 23),
        todoReminderMinute = preferences.getInt(KEY_TODO_MINUTE, 0).coerceIn(0, 59),
        reviewReminderEnabled = preferences.getBoolean(KEY_REVIEW_ENABLED, false),
        reviewReminderHour = preferences.getInt(KEY_REVIEW_HOUR, 21).coerceIn(0, 23),
        reviewReminderMinute = preferences.getInt(KEY_REVIEW_MINUTE, 0).coerceIn(0, 59)
    )

    companion object {
        private const val PREFERENCES_NAME = "limeday-app-settings"
        private const val KEY_THEME = "theme"
        private const val KEY_TODO_ENABLED = "todo-reminder-enabled"
        private const val KEY_TODO_HOUR = "todo-reminder-hour"
        private const val KEY_TODO_MINUTE = "todo-reminder-minute"
        private const val KEY_REVIEW_ENABLED = "review-reminder-enabled"
        private const val KEY_REVIEW_HOUR = "review-reminder-hour"
        private const val KEY_REVIEW_MINUTE = "review-reminder-minute"
    }
}
