package com.limeday.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
}
