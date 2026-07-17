package com.limeday.app.ui

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.limeday.app.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun reviewIsASecondLevelScreenAndContainsSummaryPanel() {
        composeRule.onNodeWithTag("day_screen").assertExists()
        composeRule.onNodeWithTag("review_screen").assertDoesNotExist()

        composeRule.onNodeWithTag("review_entry").performClick()

        composeRule.onNodeWithTag("review_screen").assertExists()
        composeRule.onNodeWithTag("review_todos").assertExists()
        composeRule.onNodeWithText("解决了什么问题？").assertExists()
        composeRule.onNodeWithText("随便写写").assertExists()
        composeRule.onNodeWithText("今天有什么收获？").assertDoesNotExist()
        composeRule.onNodeWithTag("review_screen").performScrollToNode(hasTestTag("summary_panel"))
        composeRule.onNodeWithTag("summary_panel", useUnmergedTree = true).assertExists()
    }

    @Test
    fun settingsEntryOpensCommonSettingsScreen() {
        composeRule.onNodeWithContentDescription("设置").performClick()

        composeRule.onNodeWithTag("settings_screen").assertExists()
        composeRule.onNodeWithContentDescription("返回").assertDoesNotExist()
        composeRule.onNodeWithText("WebDAV 根地址").assertDoesNotExist()
    }

    @Test
    fun summaryIsASeparatePrimaryScreen() {
        composeRule.onNodeWithText("总结").performClick()

        composeRule.onNodeWithTag("summary_screen").assertExists()
        composeRule.onNodeWithText("本周").assertExists()
        composeRule.onNodeWithText("总结历史").assertExists()
        composeRule.onNodeWithContentDescription("模型服务").assertDoesNotExist()
    }

    @Test
    fun dateCardOpensMonthJumpDialog() {
        composeRule.onNodeWithContentDescription("选择日期").performClick()

        composeRule.onNodeWithContentDescription("上个月").assertExists()
        composeRule.onNodeWithContentDescription("下个月").assertExists()
        composeRule.onNodeWithText("取消").assertExists()
    }

    @Test
    fun modelProvidersAreManagedOnASecondLevelScreen() {
        composeRule.onNodeWithContentDescription("设置").performClick()
        composeRule.onNodeWithTag("settings_screen").performScrollToNode(hasText("模型服务"))
        composeRule.onNodeWithText("模型服务").performClick()

        composeRule.onNodeWithTag("llm_provider_screen").assertExists()
        composeRule.onNodeWithText("API Key", substring = true).assertExists()
    }

    @Test
    fun webDavConfigurationIsASecondLevelScreen() {
        composeRule.onNodeWithContentDescription("设置").performClick()
        composeRule.onNodeWithTag("settings_screen").performScrollToNode(hasText("WebDAV 同步"))
        composeRule.onNodeWithText("WebDAV 同步").performClick()

        composeRule.onNodeWithTag("webdav_settings_screen").assertExists()
        composeRule.onNodeWithText("WebDAV 根地址").assertExists()
    }

    @Test
    fun trashIsASecondLevelScreen() {
        composeRule.onNodeWithContentDescription("设置").performClick()
        composeRule.onNodeWithTag("settings_screen").performScrollToNode(hasText("回收站"))
        composeRule.onNodeWithText("回收站").performClick()

        composeRule.onNodeWithTag("trash_screen").assertExists()
    }
}
