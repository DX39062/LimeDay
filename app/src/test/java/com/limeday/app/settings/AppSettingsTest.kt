package com.limeday.app.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AppSettingsTest {
    @Test
    fun `defaults follow system with both reminders disabled`() {
        val settings = AppSettings()

        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertFalse(settings.todoReminderEnabled)
        assertEquals(9, settings.todoReminderHour)
        assertFalse(settings.reviewReminderEnabled)
        assertEquals(21, settings.reviewReminderHour)
    }
}
