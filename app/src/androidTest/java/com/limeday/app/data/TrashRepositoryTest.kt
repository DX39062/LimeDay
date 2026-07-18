package com.limeday.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrashRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: LimeDayRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = LimeDayRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun deletedTodoMovesToTrashAndRestoresToOriginalDate() = runBlocking {
        val date = "2026-07-17"
        repository.addTodo(date, "可恢复待办")
        val todo = repository.observeTodos(date).first().single()

        repository.deleteTodo(todo)

        assertTrue(repository.observeTodos(date).first().isEmpty())
        val deleted = repository.observeDeletedTodos().first().single()
        assertEquals(date, deleted.date)

        repository.restoreTodo(deleted)

        assertTrue(repository.observeDeletedTodos().first().isEmpty())
        assertEquals("可恢复待办", repository.observeTodos(date).first().single().title)
    }

    @Test
    fun priorityMoveAndDuplicatePreserveExpectedFields() = runBlocking {
        val sourceDate = "2026-07-17"
        val targetDate = "2026-07-20"
        repository.addTodo(sourceDate, "安排发布", "保留备注")
        val source = repository.observeTodos(sourceDate).first().single()

        repository.setTodoPriority(source, TodoPriority.HIGH)
        val prioritized = repository.observeTodos(sourceDate).first().single()
        repository.duplicateTodo(prioritized)

        val copied = repository.observeTodos(sourceDate).first().first { it.id != prioritized.id }
        assertEquals(TodoPriority.HIGH, copied.priority)
        assertEquals("保留备注", copied.note)
        assertTrue(!copied.isCompleted)

        repository.moveTodo(prioritized, targetDate)

        val moved = repository.observeTodos(targetDate).first().single()
        assertEquals(prioritized.id, moved.id)
        assertEquals(TodoPriority.HIGH, moved.priority)
        assertTrue(moved.revision > source.revision)
    }

    @Test
    fun expandedTodoGroupStepsAndRecurrenceCreateOneNextInstance() = runBlocking {
        repository.addTodo("2026-07-18", "循环任务")
        val source = repository.observeTodos("2026-07-18").first().single()
        val group = repository.addGroup("工作", "folder", "yellow")
        val updated = repository.updateTodo(
            source,
            TodoEdit(
                title = source.title,
                note = "包含检索词",
                groupId = group.id,
                dueDate = LocalDate.of(2026, 7, 18),
                dueTime = LocalTime.of(18, 30),
                dueZoneId = ZoneId.of("Asia/Shanghai"),
                reminderAt = LocalDate.of(2026, 7, 18).atTime(17, 30).atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli(),
                recurrence = TodoRecurrence.DAILY
            )
        )
        repository.addStep(updated.id, "第一步")

        repository.setTodoCompleted(updated, true)
        repository.setTodoCompleted(updated, true)

        val next = repository.observeTodos("2026-07-19").first().single()
        assertEquals(group.id, next.groupId)
        assertEquals("2026-07-19", next.dueDate)
        assertEquals("第一步", repository.snapshot().steps.single { it.todoId == next.id }.title)
        assertTrue(!repository.snapshot().steps.single { it.todoId == next.id }.isCompleted)
    }

    @Test
    fun permanentDeleteRemovesBodyAndStepsButKeepsMinimalTombstone() = runBlocking {
        repository.addTodo("2026-07-18", "彻底删除")
        val todo = repository.observeTodos("2026-07-18").first().single()
        repository.addStep(todo.id, "敏感正文")
        repository.deleteTodo(todo)
        val deleted = repository.observeDeletedTodos().first().single()

        repository.permanentlyDeleteTodos(listOf(deleted))

        val snapshot = repository.snapshot()
        assertTrue(snapshot.todos.none { it.id == todo.id })
        assertTrue(snapshot.steps.none { it.todoId == todo.id })
        assertEquals(todo.id, snapshot.todoTombstones.single().todoId)
    }

    @Test
    fun searchFindsStepTextAndOverdueUsesDueDateNotPlanDate() = runBlocking {
        repository.addTodo("2026-07-01", "旧计划但未截止")
        val oldPlan = repository.observeTodos("2026-07-01").first().single()
        repository.updateTodo(
            oldPlan,
            TodoEdit(
                title = oldPlan.title,
                note = "",
                dueDate = LocalDate.of(2026, 7, 20)
            )
        )
        repository.addTodo("2026-07-18", "按步骤搜索")
        val searchable = repository.observeTodos("2026-07-18").first().single()
        repository.addStep(searchable.id, "联系供应商")
        repository.updateTodo(
            searchable,
            TodoEdit(
                title = searchable.title,
                note = "",
                dueDate = LocalDate.of(2026, 7, 17)
            )
        )

        assertEquals(searchable.id, repository.searchTodos("供应商").first().single().id)
        val overdue = repository.overdueTodos(today = LocalDate.of(2026, 7, 18), now = Long.MAX_VALUE).first()
        assertEquals(listOf(searchable.id), overdue.map(TodoItem::id))
    }
}
