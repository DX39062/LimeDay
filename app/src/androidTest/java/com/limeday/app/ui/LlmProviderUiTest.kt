package com.limeday.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.limeday.app.llm.LlmProviderPresets
import com.limeday.app.ui.theme.LimeDayTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LlmProviderUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun savedServiceHasFixedEditActionAndClickableCard() {
        var editCount by mutableStateOf(0)
        val provider = LlmProviderPresets.find("deepseek")!!.createProvider()
        composeRule.setContent {
            LimeDayTheme {
                LlmProviderCard(
                    provider = provider,
                    active = true,
                    canMoveUp = false,
                    canMoveDown = false,
                    testing = false,
                    onActivate = {},
                    onEdit = { editCount += 1 },
                    onTest = {},
                    onDuplicate = {},
                    onMoveUp = {},
                    onMoveDown = {},
                    onDelete = {}
                )
            }
        }

        composeRule.onNodeWithContentDescription("编辑模型服务：${provider.name}").assertExists().performClick()
        composeRule.runOnIdle { assertTrue(editCount >= 1) }
    }

    @Test
    fun firstAddShowsExplicitManualModelFetch() {
        val provider = LlmProviderPresets.find("openai")!!.createProvider().copy(apiKey = "")
        composeRule.setContent {
            LimeDayTheme {
                LlmProviderEditorDialog(
                    initial = provider,
                    isNew = true,
                    models = emptyList(),
                    isFetching = false,
                    message = null,
                    onDismiss = {},
                    onFetchModels = {},
                    onSave = {}
                )
            }
        }

        composeRule.onNodeWithText("手动获取模型").assertExists().assertIsNotEnabled()
    }
}
