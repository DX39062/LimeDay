package com.limeday.app.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.limeday.app.data.TodoItem
import com.limeday.app.data.TodoPriority
import com.limeday.app.ui.theme.LimeDayTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TodoComponentsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun swipeLeftOnlyRevealsDeleteAction() {
        var deleted = false
        composeRule.setContent {
            LimeDayTheme {
                SwipeTodoRow(
                    todo = todo(),
                    expanded = false,
                    anyRowExpanded = false,
                    onExpandedChange = {},
                    onToggle = {},
                    onEdit = {},
                    onSetPriority = {},
                    onMove = {},
                    onDuplicate = {},
                    onDelete = { deleted = true }
                )
            }
        }

        composeRule.onNodeWithText("滑动删除测试").performTouchInput { swipeLeft() }

        composeRule.runOnIdle { assertTrue(!deleted) }
        composeRule.onNodeWithText("移到回收站").performClick()
        composeRule.runOnIdle { assertTrue(deleted) }
    }

    @Test
    fun swipeActionCanBeClosedBySwipingRight() {
        var expanded = false
        composeRule.setContent {
            LimeDayTheme {
                SwipeTodoRow(
                    todo = todo(),
                    expanded = expanded,
                    anyRowExpanded = expanded,
                    onExpandedChange = { expanded = it },
                    onToggle = {},
                    onEdit = {},
                    onSetPriority = {},
                    onMove = {},
                    onDuplicate = {},
                    onDelete = {}
                )
            }
        }

        composeRule.onNodeWithText("滑动删除测试").performTouchInput { swipeLeft() }
        composeRule.onNodeWithText("移到回收站").assertExists()
        composeRule.onNodeWithText("滑动删除测试").performTouchInput { swipeRight() }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("移到回收站").assertDoesNotExist()
    }

    @Test
    fun tappingTodoBodyClosesOpenSwipeAction() {
        var expanded by mutableStateOf(false)
        composeRule.setContent {
            LimeDayTheme {
                SwipeTodoRow(
                    todo = todo(),
                    expanded = expanded,
                    anyRowExpanded = expanded,
                    onExpandedChange = { expanded = it },
                    onToggle = {},
                    onEdit = {},
                    onSetPriority = {},
                    onMove = {},
                    onDuplicate = {},
                    onDelete = {}
                )
            }
        }

        composeRule.onNodeWithText("滑动删除测试").performTouchInput { swipeLeft() }
        composeRule.onNodeWithText("移到回收站").assertExists()
        composeRule.onNodeWithContentDescription("编辑待办：滑动删除测试").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("移到回收站").assertDoesNotExist()
    }

    @Test
    fun moreOptionsCanSetPriority() {
        var selectedPriority = TodoPriority.NORMAL
        composeRule.setContent {
            LimeDayTheme {
                SwipeTodoRow(
                    todo = todo(),
                    expanded = false,
                    anyRowExpanded = false,
                    onExpandedChange = {},
                    onToggle = {},
                    onEdit = {},
                    onSetPriority = { selectedPriority = it },
                    onMove = {},
                    onDuplicate = {},
                    onDelete = {}
                )
            }
        }

        composeRule.onNodeWithContentDescription("滑动删除测试的更多选项").performClick()
        composeRule.onNodeWithText("高").performClick()

        composeRule.runOnIdle { assertTrue(selectedPriority == TodoPriority.HIGH) }
    }

    @Test
    fun editorDeletionRequiresConfirmation() {
        var deleted = false
        composeRule.setContent {
            LimeDayTheme {
                TodoEditor(
                    todo = todo(),
                    onDismiss = {},
                    onSave = { _, _ -> },
                    onDelete = { deleted = true }
                )
            }
        }

        composeRule.onNodeWithText("移到回收站").performClick()
        composeRule.runOnIdle { assertTrue(!deleted) }
        composeRule.onNodeWithText("确认移入").performClick()
        composeRule.runOnIdle { assertTrue(deleted) }
    }

    private fun todo() = TodoItem(
        id = "swipe-test",
        date = "2026-07-17",
        title = "滑动删除测试",
        sortOrder = "1",
        deviceId = "device-test"
    )
}
