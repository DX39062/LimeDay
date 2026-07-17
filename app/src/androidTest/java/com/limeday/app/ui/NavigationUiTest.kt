package com.limeday.app.ui

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
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
        composeRule.onNodeWithTag("review_screen").performScrollToNode(hasTestTag("summary_panel"))
        composeRule.onNodeWithTag("summary_panel", useUnmergedTree = true).assertExists()
    }

    @Test
    fun settingsEntryOpensCommonSettingsScreen() {
        composeRule.onNodeWithContentDescription("设置").performClick()

        composeRule.onNodeWithTag("settings_screen").assertExists()
    }
}
