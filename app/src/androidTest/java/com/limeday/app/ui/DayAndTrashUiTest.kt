package com.limeday.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.limeday.app.data.TodoDefaults
import com.limeday.app.data.TodoGroup
import com.limeday.app.data.TodoItem
import com.limeday.app.ui.theme.LimeDayTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DayAndTrashUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun smartViewSearchAndGroupCollapseAreReachable() {
        val group = TodoGroup(
            id = TodoDefaults.INBOX_GROUP_ID,
            name = "日常",
            sortOrder = "0",
            isInbox = true,
            deviceId = "device"
        )
        val todo = todo("one", "需要检索的待办")
        var selectedMode = TodoViewMode.DAY
        var query = ""
        composeRule.setContent {
            var state by remember { mutableStateOf(
                DayUiState(
                    todos = listOf(todo),
                    displayedTodos = listOf(todo),
                    todoGroups = listOf(group),
                    isLoading = false
                )
            ) }
            LimeDayTheme {
                DayScreen(
                    state = state,
                    onPreviousDay = {}, onNextDay = {}, onToday = {}, onAddTodo = { _, _ -> }, onToggleTodo = {},
                    onUpdateTodo = { _, _ -> }, onAddStep = { _, _ -> }, onToggleStep = {}, onUpdateStep = { _, _ -> },
                    onMoveStep = { _, _ -> }, onDeleteStep = {}, onSetTodoPriority = { _, _ -> }, onMoveTodo = { _, _ -> },
                    onDuplicateTodo = {}, onDeleteTodo = {}, onRestoreTodo = {}, onOpenReview = {}, onSelectDate = {},
                    onLoadMonth = {}, onSetViewMode = { selectedMode = it; state = state.copy(todoViewMode = it) },
                    onSearch = { query = it; state = state.copy(todoSearchQuery = it) }, onAddGroup = { _, _, _ -> },
                    onUpdateGroup = { _, _, _, _ -> }, onMoveGroup = { _, _ -> }, onDeleteGroup = {}
                )
            }
        }

        composeRule.onNodeWithText("逾期").performClick()
        composeRule.runOnIdle { assertEquals(TodoViewMode.OVERDUE, selectedMode) }
        composeRule.onNodeWithTag("daily_progress_compact").assertDoesNotExist()
        composeRule.onNodeWithTag("todo_search").assertDoesNotExist()
        composeRule.onNodeWithTag("todo_search_button").performClick()
        composeRule.onNodeWithTag("todo_search").assertExists()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("todo_search").assertExists()
        composeRule.onNodeWithTag("todo_search").performTextInput("检索")
        composeRule.runOnIdle { assertEquals("检索", query) }
        composeRule.onNodeWithTag("group_header_${TodoDefaults.INBOX_GROUP_ID}").performClick()
        composeRule.onNodeWithTag("todo_search").assertExists()
        composeRule.onNodeWithText("需要检索的待办").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("清除搜索").performClick()
        composeRule.onNodeWithText("逾期").performClick()
        composeRule.onNodeWithTag("todo_search").assertDoesNotExist()

        composeRule.onNodeWithTag("todo_search_button").performClick()
        composeRule.onNodeWithTag("todo_search").performTextInput("再查")
        composeRule.onNodeWithContentDescription("关闭搜索").performClick()
        composeRule.onNodeWithTag("todo_search").assertDoesNotExist()
        composeRule.runOnIdle {
            assertEquals("", query)
            assertEquals(TodoViewMode.OVERDUE, selectedMode)
        }
    }

    @Test
    fun quickAddKeepsChosenGroupAndPassesItToNewTodo() {
        val daily = TodoGroup(
            id = TodoDefaults.INBOX_GROUP_ID,
            name = "日常",
            iconKey = "daily",
            sortOrder = "0",
            isInbox = true,
            deviceId = "device"
        )
        val work = TodoGroup(id = "work", name = "工作", iconKey = "work", sortOrder = "1", deviceId = "device")
        var added = "" to ""
        composeRule.setContent {
            LimeDayTheme {
                DayScreen(
                    state = DayUiState(todoGroups = listOf(daily, work), isLoading = false),
                    onPreviousDay = {}, onNextDay = {}, onToday = {}, onAddTodo = { title, groupId -> added = title to groupId },
                    onToggleTodo = {}, onUpdateTodo = { _, _ -> }, onAddStep = { _, _ -> }, onToggleStep = {},
                    onUpdateStep = { _, _ -> }, onMoveStep = { _, _ -> }, onDeleteStep = {},
                    onSetTodoPriority = { _, _ -> }, onMoveTodo = { _, _ -> }, onDuplicateTodo = {}, onDeleteTodo = {},
                    onRestoreTodo = {}, onOpenReview = {}, onSelectDate = {}, onLoadMonth = {}, onSetViewMode = {},
                    onSearch = {}, onAddGroup = { _, _, _ -> }, onUpdateGroup = { _, _, _, _ -> },
                    onMoveGroup = { _, _ -> }, onDeleteGroup = {}
                )
            }
        }

        composeRule.onNodeWithTag("quick_add_group").performClick()
        composeRule.onNodeWithTag("quick_add_group_work").performClick()
        composeRule.onNodeWithText("添加一件要做的事").performTextInput("写周报")
        composeRule.onNodeWithContentDescription("添加待办").performClick()
        composeRule.runOnIdle { assertEquals("写周报" to "work", added) }
    }

    @Test
    fun trashSelectAllRequiresConfirmationBeforePermanentDelete() {
        val deleted = listOf(todo("one", "一", 10), todo("two", "二", 20))
        var permanentlyDeleted = emptyList<TodoItem>()
        composeRule.setContent {
            LimeDayTheme {
                TrashScreen(
                    state = DayUiState(deletedTodos = deleted, isLoading = false),
                    onBack = {},
                    onRestore = {},
                    onPermanentDelete = { permanentlyDeleted = it.toList() }
                )
            }
        }

        composeRule.onNodeWithTag("trash_select_all").performClick()
        composeRule.onNodeWithTag("trash_delete_selected").performClick()
        composeRule.onNodeWithText("永久删除 2 项？").assertExists()
        composeRule.runOnIdle { assertEquals(0, permanentlyDeleted.size) }
        composeRule.onNodeWithTag("confirm_permanent_delete").performClick()
        composeRule.runOnIdle { assertEquals(setOf("one", "two"), permanentlyDeleted.mapTo(mutableSetOf(), TodoItem::id)) }
    }

    private fun todo(id: String, title: String, deletedAt: Long? = null) = TodoItem(
        id = id,
        date = "2026-07-18",
        title = title,
        sortOrder = id,
        deletedAt = deletedAt,
        deviceId = "device"
    )
}
