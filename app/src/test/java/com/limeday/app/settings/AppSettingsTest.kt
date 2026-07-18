package com.limeday.app.settings

import com.limeday.app.ui.shouldEnableLlmForUpgrade
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
        assertFalse(settings.llmEnabled)
    }

    @Test
    fun `llm upgrade only enables legacy users with providers and no explicit choice`() {
        assertEquals(true, shouldEnableLlmForUpgrade(hasExplicitSetting = false, savedProviderCount = 1))
        assertFalse(shouldEnableLlmForUpgrade(hasExplicitSetting = false, savedProviderCount = 0))
        assertFalse(shouldEnableLlmForUpgrade(hasExplicitSetting = true, savedProviderCount = 3))
    }
}
