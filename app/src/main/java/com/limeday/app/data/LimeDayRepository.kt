package com.limeday.app.data

import kotlinx.coroutines.flow.Flow

class LimeDayRepository(private val dao: LimeDayDao) {
    fun observeTodos(date: String): Flow<List<TodoItem>> = dao.observeTodos(date)
    fun observeReview(date: String): Flow<DailyReview?> = dao.observeReview(date)
    fun observeSummary(date: String): Flow<DailySummary?> = dao.observeSummary(date)

    suspend fun addTodo(date: String, title: String, note: String = "") {
        dao.insertTodo(TodoItem(date = date, title = title.trim(), note = note.trim()))
    }

    suspend fun updateTodo(todo: TodoItem, title: String, note: String) {
        dao.updateTodo(todo.copy(title = title.trim(), note = note.trim()))
    }

    suspend fun setTodoCompleted(todo: TodoItem, completed: Boolean) {
        dao.updateTodo(todo.copy(isCompleted = completed))
    }

    suspend fun deleteTodo(todo: TodoItem) = dao.deleteTodo(todo)

    suspend fun saveReview(review: DailyReview) {
        dao.upsertReview(review.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun saveSummary(summary: DailySummary) = dao.upsertSummary(summary)
}
