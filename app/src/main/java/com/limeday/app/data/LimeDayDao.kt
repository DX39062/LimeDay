package com.limeday.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface LimeDayDao {
    @Query("SELECT * FROM todos WHERE date = :date ORDER BY isCompleted ASC, createdAt ASC")
    fun observeTodos(date: String): Flow<List<TodoItem>>

    @Insert
    suspend fun insertTodo(todo: TodoItem)

    @Update
    suspend fun updateTodo(todo: TodoItem)

    @Delete
    suspend fun deleteTodo(todo: TodoItem)

    @Query("SELECT * FROM daily_reviews WHERE date = :date LIMIT 1")
    fun observeReview(date: String): Flow<DailyReview?>

    @Upsert
    suspend fun upsertReview(review: DailyReview)

    @Query("SELECT * FROM daily_summaries WHERE date = :date LIMIT 1")
    fun observeSummary(date: String): Flow<DailySummary?>

    @Upsert
    suspend fun upsertSummary(summary: DailySummary)
}
