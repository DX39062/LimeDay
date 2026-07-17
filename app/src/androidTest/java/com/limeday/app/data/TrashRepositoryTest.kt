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
}
