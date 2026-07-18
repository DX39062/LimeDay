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

    @Query("SELECT * FROM range_summaries WHERE deleted_at IS NULL ORDER BY generated_at DESC")
    fun observeRangeSummaries(): Flow<List<RangeSummary>>

    @Query("SELECT * FROM todo_groups WHERE deleted_at IS NULL ORDER BY is_inbox DESC, sort_order ASC, created_at ASC")
    fun observeGroups(): Flow<List<TodoGroup>>

    @Query("SELECT * FROM todo_steps WHERE deleted_at IS NULL ORDER BY todo_id ASC, sort_order ASC, created_at ASC")
    fun observeSteps(): Flow<List<TodoStep>>

    @Query("SELECT * FROM todos")
    suspend fun allTodos(): List<TodoItem>

    @Query("SELECT * FROM daily_reviews")
    suspend fun allReviews(): List<DailyReview>

    @Query("SELECT * FROM daily_summaries")
    suspend fun allSummaries(): List<DailySummary>

    @Query("SELECT * FROM range_summaries")
    suspend fun allRangeSummaries(): List<RangeSummary>

    @Query("SELECT * FROM todo_groups")
    suspend fun allGroups(): List<TodoGroup>

    @Query("SELECT * FROM todo_groups WHERE id = :id LIMIT 1")
    suspend fun groupById(id: String): TodoGroup?

    @Query("SELECT * FROM todo_steps")
    suspend fun allSteps(): List<TodoStep>

    @Query("SELECT * FROM todo_tombstones")
    suspend fun allTodoTombstones(): List<TodoTombstone>

    @Query("SELECT * FROM todos WHERE deleted_at IS NULL AND is_completed = 0 AND reminder_at IS NOT NULL")
    suspend fun activeReminderTodos(): List<TodoItem>

    @Query("SELECT * FROM todos WHERE id = :id LIMIT 1")
    suspend fun todoById(id: String): TodoItem?

    @Query("SELECT * FROM todo_steps WHERE todo_id = :todoId AND deleted_at IS NULL ORDER BY sort_order ASC, created_at ASC")
    suspend fun stepsForTodo(todoId: String): List<TodoStep>

    @Query("SELECT * FROM todos WHERE recurrence_source_id = :sourceId AND date = :date AND deleted_at IS NULL LIMIT 1")
    suspend fun recurrenceInstance(sourceId: String, date: String): TodoItem?

    @Query("""SELECT * FROM todos
        WHERE deleted_at IS NULL AND (
            title LIKE '%' || :query || '%' ESCAPE '\\' OR
            note LIKE '%' || :query || '%' ESCAPE '\\' OR
            EXISTS (SELECT 1 FROM todo_steps s WHERE s.todo_id = todos.id AND s.deleted_at IS NULL AND s.title LIKE '%' || :query || '%' ESCAPE '\\')
        )
        ORDER BY is_completed ASC, date ASC, sort_order ASC""")
    fun searchTodos(query: String): Flow<List<TodoItem>>

    @Query("""SELECT * FROM todos WHERE deleted_at IS NULL AND is_completed = 0 AND
        ((due_at IS NOT NULL AND due_at < :now) OR (due_at IS NULL AND due_date IS NOT NULL AND due_date < :today))
        ORDER BY due_date ASC, due_at ASC, sort_order ASC""")
    fun overdueTodos(today: String, now: Long): Flow<List<TodoItem>>

    @Query("""SELECT * FROM todos WHERE deleted_at IS NULL AND due_date IS NOT NULL
        ORDER BY is_completed ASC, due_date ASC, due_at ASC, sort_order ASC""")
    fun plannedTodos(): Flow<List<TodoItem>>

    @Query("SELECT * FROM todos WHERE date >= :start AND date <= :end AND deleted_at IS NULL ORDER BY date ASC, is_completed ASC, sort_order ASC")
    suspend fun todosBetween(start: String, end: String): List<TodoItem>

    @Query("SELECT * FROM daily_reviews WHERE date >= :start AND date <= :end AND deleted_at IS NULL ORDER BY date ASC")
    suspend fun reviewsBetween(start: String, end: String): List<DailyReview>

    @Query("SELECT * FROM daily_summaries WHERE date >= :start AND date <= :end AND deleted_at IS NULL ORDER BY date ASC")
    suspend fun summariesBetween(start: String, end: String): List<DailySummary>

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
    suspend fun upsertRangeSummary(summary: RangeSummary)

    @Upsert
    suspend fun upsertRangeSummaries(summaries: List<RangeSummary>)

    @Upsert
    suspend fun upsertGroup(group: TodoGroup)

    @Upsert
    suspend fun upsertGroups(groups: List<TodoGroup>)

    @Upsert
    suspend fun upsertStep(step: TodoStep)

    @Upsert
    suspend fun upsertSteps(steps: List<TodoStep>)

    @Upsert
    suspend fun upsertTodoTombstone(tombstone: TodoTombstone)

    @Upsert
    suspend fun upsertTodoTombstones(tombstones: List<TodoTombstone>)

    @Query("DELETE FROM todo_steps WHERE todo_id IN (:todoIds)")
    suspend fun deleteStepsForTodos(todoIds: List<String>)

    @Query("DELETE FROM todos WHERE id IN (:todoIds)")
    suspend fun deleteTodosById(todoIds: List<String>)

    @Upsert
    suspend fun upsertMetadata(metadata: AppMetadata)
}
