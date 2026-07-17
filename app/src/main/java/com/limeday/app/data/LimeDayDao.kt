package com.limeday.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface LimeDayDao {
    @Query("SELECT * FROM todos WHERE date = :date AND deleted_at IS NULL ORDER BY is_completed ASC, sort_order ASC, created_at ASC")
    fun observeTodos(date: String): Flow<List<TodoItem>>

    @Query("SELECT * FROM todos WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC, updated_at DESC")
    fun observeDeletedTodos(): Flow<List<TodoItem>>

    @Query("SELECT * FROM daily_reviews WHERE date = :date AND deleted_at IS NULL LIMIT 1")
    fun observeReview(date: String): Flow<DailyReview?>

    @Query("SELECT * FROM daily_summaries WHERE date = :date AND deleted_at IS NULL LIMIT 1")
    fun observeSummary(date: String): Flow<DailySummary?>

    @Query("SELECT * FROM todos")
    suspend fun allTodos(): List<TodoItem>

    @Query("SELECT * FROM daily_reviews")
    suspend fun allReviews(): List<DailyReview>

    @Query("SELECT * FROM daily_summaries")
    suspend fun allSummaries(): List<DailySummary>

    @Query("SELECT * FROM app_metadata WHERE id = 1 LIMIT 1")
    suspend fun metadata(): AppMetadata?

    @Upsert
    suspend fun upsertTodo(todo: TodoItem)

    @Upsert
    suspend fun upsertTodos(todos: List<TodoItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReview(review: DailyReview)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReviews(reviews: List<DailyReview>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSummary(summary: DailySummary)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSummaries(summaries: List<DailySummary>)

    @Upsert
    suspend fun upsertMetadata(metadata: AppMetadata)
}
