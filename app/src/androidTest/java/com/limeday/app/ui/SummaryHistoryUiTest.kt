package com.limeday.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.limeday.app.data.RangeSummary
import com.limeday.app.ui.theme.LimeDayTheme
import org.junit.Rule
import org.junit.Test

class SummaryHistoryUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun historyCardStartsCollapsedAndCanExpand() {
        var expanded by mutableStateOf(false)
        val summary = RangeSummary(
            id = "summary-test",
            rangeStart = "2026-07-01",
            rangeEnd = "2026-07-07",
            periodType = "week",
            prompt = "只在展开态出现的指令",
            content = "第一行摘要\n第二行完整内容",
            providerId = "provider-test",
            providerName = "测试服务",
            model = "test-model",
            deviceId = "device-test"
        )
        composeRule.setContent {
            LimeDayTheme {
                RangeSummaryCard(
                    summary = summary,
                    expanded = expanded,
                    onToggle = { expanded = !expanded },
                    onDelete = {}
                )
            }
        }

        composeRule.onNodeWithText(summary.prompt).assertDoesNotExist()
        composeRule.onNodeWithText("${summary.rangeStart} 至 ${summary.rangeEnd}").performClick()
        composeRule.onNodeWithText(summary.prompt).assertExists()
    }
}
