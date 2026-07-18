package com.limeday.app.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.limeday.app.llm.LlmProviderPresets
import com.limeday.app.llm.LlmSettings
import com.limeday.app.llm.LlmServiceConfig
import com.limeday.app.ui.theme.LimeDayTheme
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

class LlmProviderSettingsUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun existingProviderStillShowsBothAddEntriesAndCreatesNewId() {
        val existing = LlmProviderPresets.all.first().createProvider().copy(id = "existing-provider")
        var saved: LlmServiceConfig? = null
        composeRule.setContent {
            LimeDayTheme {
                LlmProviderSettingsScreen(
                    state = DayUiState(
                        llmSettings = LlmSettings(listOf(existing), existing.id),
                        isLoading = false
                    ),
                    onBack = {},
                    onSave = { saved = it; true },
                    onActivate = {},
                    onDuplicate = {},
                    onDelete = {},
                    onMove = { _, _ -> },
                    onFetchModels = { _, _ -> },
                    onLoadCachedModels = {},
                    onClearMessage = {}
                )
            }
        }

        composeRule.onNodeWithContentDescription("添加模型服务").assertExists()
        composeRule.onNodeWithTag("add_llm_provider_bottom").assertExists()
        composeRule.onNodeWithContentDescription("添加模型服务").performClick()
        composeRule.onNodeWithText("选择服务预设").assertExists()
        composeRule.onNodeWithText("DeepSeek").performClick()
        composeRule.onNodeWithText("手动获取模型").assertExists()
        composeRule.onNodeWithText("保存").performClick()
        composeRule.runOnIdle { assertNotEquals(existing.id, saved?.id) }
    }
}
