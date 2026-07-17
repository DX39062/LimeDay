package com.limeday.app.data

import androidx.room.withTransaction
import com.limeday.app.sync.SyncSnapshot
import java.util.UUID
import kotlinx.coroutines.flow.Flow

class LimeDayRepository(private val database: AppDatabase) {
    private val dao = database.limeDayDao()

    fun observeTodos(date: String): Flow<List<TodoItem>> = dao.observeTodos(date)
    fun observeDeletedTodos(): Flow<List<TodoItem>> = dao.observeDeletedTodos()
    fun observeReview(date: String): Flow<DailyReview?> = dao.observeReview(date)
    fun observeSummary(date: String): Flow<DailySummary?> = dao.observeSummary(date)
    fun observeRangeSummaries(): Flow<List<RangeSummary>> = dao.observeRangeSummaries()

    suspend fun todosBetween(start: String, end: String): List<TodoItem> = dao.todosBetween(start, end)

    suspend fun deviceId(): String = ensureMetadata().deviceId

    suspend fun addTodo(date: String, title: String, note: String = "") {
        val now = System.currentTimeMillis()
        dao.upsertTodo(
            TodoItem(
                date = date,
                title = title.trim(),
                note = note.trim(),
                sortOrder = "%020d-%s".format(now, UUID.randomUUID()),
                createdAt = now,
                updatedAt = now,
                deviceId = deviceId()
            )
        )
    }

    suspend fun updateTodo(todo: TodoItem, title: String, note: String) {
        dao.upsertTodo(todo.changed().copy(title = title.trim(), note = note.trim()))
    }

    suspend fun setTodoPriority(todo: TodoItem, priority: Int) {
        dao.upsertTodo(todo.changed().copy(priority = TodoPriority.normalize(priority)))
    }

    suspend fun moveTodo(todo: TodoItem, date: String) {
        dao.upsertTodo(todo.changed().copy(date = date))
    }

    suspend fun duplicateTodo(todo: TodoItem) {
        val now = System.currentTimeMillis()
        dao.upsertTodo(
            TodoItem(
                date = todo.date,
                title = todo.title,
                note = todo.note,
                priority = TodoPriority.normalize(todo.priority),
                sortOrder = "%020d-%s".format(now, UUID.randomUUID()),
                createdAt = now,
                updatedAt = now,
                deviceId = deviceId()
            )
        )
    }

    suspend fun setTodoCompleted(todo: TodoItem, completed: Boolean) {
        dao.upsertTodo(todo.changed().copy(isCompleted = completed))
    }

    suspend fun deleteTodo(todo: TodoItem) {
        val now = System.currentTimeMillis()
        dao.upsertTodo(
            todo.copy(
                updatedAt = now,
                deletedAt = now,
                deviceId = deviceId(),
                revision = todo.revision + 1
            )
        )
    }

    suspend fun restoreTodo(todo: TodoItem) {
        val now = System.currentTimeMillis()
        dao.upsertTodo(
            todo.copy(
                updatedAt = now,
                deletedAt = null,
                deviceId = deviceId(),
                revision = todo.revision + 1
            )
        )
    }

    suspend fun newReview(date: String): DailyReview = DailyReview(date = date, deviceId = deviceId())

    suspend fun saveReview(review: DailyReview) {
        val now = System.currentTimeMillis()
        dao.upsertReview(
            review.copy(
                updatedAt = now,
                deviceId = deviceId(),
                revision = review.revision + 1
            )
        )
    }

    suspend fun saveSummary(date: String, content: String, provider: String, model: String, current: DailySummary?) {
        val now = System.currentTimeMillis()
        dao.upsertSummary(
            (current ?: DailySummary(
                date = date,
                content = content,
                provider = provider,
                model = model,
                generatedAt = now,
                deviceId = deviceId()
            )).copy(
                content = content,
                provider = provider,
                model = model,
                generatedAt = now,
                updatedAt = now,
                deletedAt = null,
                deviceId = deviceId(),
                revision = (current?.revision ?: 0) + 1
            )
        )
    }

    suspend fun rangeData(start: String, end: String, includeExistingSummaries: Boolean): RangeSourceData =
        database.withTransaction {
            RangeSourceData(
                todos = dao.todosBetween(start, end),
                reviews = dao.reviewsBetween(start, end),
                summaries = if (includeExistingSummaries) dao.summariesBetween(start, end) else emptyList()
            )
        }

    suspend fun saveRangeSummary(
        start: String,
        end: String,
        periodType: String,
        prompt: String,
        content: String,
        providerId: String,
        providerName: String,
        model: String,
        includeExistingSummaries: Boolean
    ) {
        val now = System.currentTimeMillis()
        dao.upsertRangeSummary(
            RangeSummary(
                rangeStart = start,
                rangeEnd = end,
                periodType = periodType,
                prompt = prompt,
                content = content,
                providerId = providerId,
                providerName = providerName,
                model = model,
                includeExistingSummaries = includeExistingSummaries,
                generatedAt = now,
                updatedAt = now,
                deviceId = deviceId()
            )
        )
    }

    suspend fun deleteRangeSummary(summary: RangeSummary) {
        val now = System.currentTimeMillis()
        dao.upsertRangeSummary(
            summary.copy(
                updatedAt = now,
                deletedAt = now,
                deviceId = deviceId(),
                revision = summary.revision + 1
            )
        )
    }

    suspend fun snapshot(): SyncSnapshot = database.withTransaction {
        SyncSnapshot(
            generatedAt = System.currentTimeMillis(),
            deviceId = deviceId(),
            todos = dao.allTodos(),
            reviews = dao.allReviews(),
            summaries = dao.allSummaries(),
            rangeSummaries = dao.allRangeSummaries()
        )
    }

    suspend fun merge(remote: SyncSnapshot): SyncSnapshot = database.withTransaction {
        val local = SyncSnapshot(
            generatedAt = System.currentTimeMillis(),
            deviceId = deviceId(),
            todos = dao.allTodos(),
            reviews = dao.allReviews(),
            summaries = dao.allSummaries(),
            rangeSummaries = dao.allRangeSummaries()
        )
        val merged = SyncSnapshot.merge(local, remote)
        dao.upsertTodos(merged.todos)
        dao.upsertReviews(merged.reviews)
        dao.upsertSummaries(merged.summaries)
        dao.upsertRangeSummaries(merged.rangeSummaries)
        merged.copy(generatedAt = System.currentTimeMillis(), deviceId = deviceId())
    }

    suspend fun exportJson(): String = snapshot().toJson()

    suspend fun importJson(value: String): SyncSnapshot = merge(SyncSnapshot.fromJson(value))

    suspend fun recordSync(status: String, time: Long = System.currentTimeMillis()) {
        val metadata = ensureMetadata()
        dao.upsertMetadata(metadata.copy(lastSyncAt = time, lastSyncStatus = status.take(240)))
    }

    suspend fun metadata(): AppMetadata = ensureMetadata()

    private suspend fun ensureMetadata(): AppMetadata {
        dao.metadata()?.let { return it }
        val metadata = AppMetadata(deviceId = UUID.randomUUID().toString())
        dao.upsertMetadata(metadata)
        return metadata
    }

    private suspend fun TodoItem.changed(): TodoItem = copy(
        updatedAt = System.currentTimeMillis(),
        deviceId = deviceId(),
        revision = revision + 1
    )
}

data class RangeSourceData(
    val todos: List<TodoItem>,
    val reviews: List<DailyReview>,
    val summaries: List<DailySummary>
) {
    val isEmpty: Boolean get() = todos.isEmpty() && reviews.none(DailyReview::hasContent) && summaries.isEmpty()
}
