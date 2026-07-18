package com.limeday.app.data

import androidx.room.withTransaction
import com.limeday.app.sync.SyncSnapshot
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.flow.Flow

class LimeDayRepository(private val database: AppDatabase) {
    private val dao = database.limeDayDao()

    fun observeTodos(date: String): Flow<List<TodoItem>> = dao.observeTodos(date)
    fun observeDeletedTodos(): Flow<List<TodoItem>> = dao.observeDeletedTodos()
    fun observeReview(date: String): Flow<DailyReview?> = dao.observeReview(date)
    fun observeSummary(date: String): Flow<DailySummary?> = dao.observeSummary(date)
    fun observeRangeSummaries(): Flow<List<RangeSummary>> = dao.observeRangeSummaries()
    fun observeGroups(): Flow<List<TodoGroup>> = dao.observeGroups()
    fun observeSteps(): Flow<List<TodoStep>> = dao.observeSteps()

    suspend fun todosBetween(start: String, end: String): List<TodoItem> = dao.todosBetween(start, end)
    suspend fun todoById(id: String): TodoItem? = dao.todoById(id)
    suspend fun activeReminderTodos(): List<TodoItem> = dao.activeReminderTodos()
    fun searchTodos(query: String): Flow<List<TodoItem>> = dao.searchTodos(escapeLike(query.trim()))
    fun overdueTodos(today: LocalDate = LocalDate.now(), now: Long = System.currentTimeMillis()): Flow<List<TodoItem>> =
        dao.overdueTodos(today.toString(), now)
    fun plannedTodos(): Flow<List<TodoItem>> = dao.plannedTodos()

    suspend fun ensureDefaultGroupName(): Boolean = database.withTransaction {
        val group = dao.groupById(TodoDefaults.INBOX_GROUP_ID) ?: return@withTransaction false
        if (!group.isInbox || group.deletedAt != null || group.name != "收件箱") return@withTransaction false
        dao.upsertGroup(
            group.copy(
                name = "日常",
                updatedAt = System.currentTimeMillis(),
                deviceId = deviceId(),
                revision = group.revision + 1
            )
        )
        true
    }

    suspend fun deviceId(): String = ensureMetadata().deviceId

    suspend fun addTodo(date: String, title: String, note: String = ""): TodoItem {
        val now = System.currentTimeMillis()
        val todo = TodoItem(
                date = date,
                title = title.trim(),
                note = note.trim(),
                sortOrder = "%020d-%s".format(now, UUID.randomUUID()),
                createdAt = now,
                updatedAt = now,
                deviceId = deviceId()
            )
        dao.upsertTodo(todo)
        return todo
    }

    suspend fun updateTodo(todo: TodoItem, title: String, note: String) {
        dao.upsertTodo(todo.changed().copy(title = title.trim(), note = note.trim()))
    }

    suspend fun updateTodo(todo: TodoItem, edit: TodoEdit): TodoItem {
        val dueDate = edit.dueDate?.toString()
        val dueTime = edit.dueTime?.withSecond(0)?.withNano(0)?.toString()
        val zoneId = edit.dueZoneId ?: ZoneId.systemDefault()
        val dueAt = if (edit.dueDate != null && edit.dueTime != null) {
            edit.dueDate.atTime(edit.dueTime).atZone(zoneId).toInstant().toEpochMilli()
        } else null
        val updated = todo.changed().copy(
            title = edit.title.trim().take(80),
            note = edit.note.trim().take(300),
            groupId = edit.groupId,
            dueDate = dueDate,
            dueTime = dueTime,
            dueAt = dueAt,
            dueZoneId = dueAt?.let { zoneId.id },
            reminderAt = edit.reminderAt,
            recurrence = TodoRecurrence.normalize(edit.recurrence)
        )
        dao.upsertTodo(updated)
        return updated
    }

    suspend fun setTodoPriority(todo: TodoItem, priority: Int) {
        dao.upsertTodo(todo.changed().copy(priority = TodoPriority.normalize(priority)))
    }

    suspend fun moveTodo(todo: TodoItem, date: String) {
        dao.upsertTodo(todo.changed().copy(date = date))
    }

    suspend fun duplicateTodo(todo: TodoItem): TodoItem {
        val now = System.currentTimeMillis()
        val duplicate = TodoItem(
                date = todo.date,
                title = todo.title,
                note = todo.note,
                priority = TodoPriority.normalize(todo.priority),
                groupId = todo.groupId,
                dueDate = todo.dueDate,
                dueTime = todo.dueTime,
                dueAt = todo.dueAt,
                dueZoneId = todo.dueZoneId,
                reminderAt = todo.reminderAt,
                recurrence = TodoRecurrence.normalize(todo.recurrence),
                sortOrder = "%020d-%s".format(now, UUID.randomUUID()),
                createdAt = now,
                updatedAt = now,
                deviceId = deviceId()
            )
        dao.upsertTodo(duplicate)
        dao.stepsForTodo(todo.id).forEachIndexed { index, step ->
            dao.upsertStep(
                step.copy(
                    id = UUID.randomUUID().toString(),
                    todoId = duplicate.id,
                    isCompleted = false,
                    sortOrder = "%020d-%04d".format(now, index),
                    createdAt = now,
                    updatedAt = now,
                    deletedAt = null,
                    deviceId = duplicate.deviceId,
                    revision = 1
                )
            )
        }
        return duplicate
    }

    suspend fun setTodoCompleted(todo: TodoItem, completed: Boolean): TodoItem? = database.withTransaction {
        dao.upsertTodo(todo.changed().copy(isCompleted = completed))
        if (!completed) return@withTransaction null
        createNextRecurrence(todo)
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

    suspend fun addGroup(name: String, iconKey: String = "leaf", colorKey: String = "mint"): TodoGroup {
        val now = System.currentTimeMillis()
        val group = TodoGroup(
            name = name.trim().take(40),
            iconKey = iconKey,
            colorKey = colorKey,
            sortOrder = "%020d-%s".format(now, UUID.randomUUID()),
            createdAt = now,
            updatedAt = now,
            deviceId = deviceId()
        )
        dao.upsertGroup(group)
        return group
    }

    suspend fun updateGroup(group: TodoGroup, name: String, iconKey: String, colorKey: String) {
        val now = System.currentTimeMillis()
        dao.upsertGroup(
            group.copy(
                name = name.trim().take(40),
                iconKey = iconKey,
                colorKey = colorKey,
                updatedAt = now,
                deviceId = deviceId(),
                revision = group.revision + 1
            )
        )
    }

    suspend fun deleteGroup(group: TodoGroup) {
        if (group.isInbox) return
        val now = System.currentTimeMillis()
        dao.upsertGroup(
            group.copy(updatedAt = now, deletedAt = now, deviceId = deviceId(), revision = group.revision + 1)
        )
    }

    suspend fun moveGroup(group: TodoGroup, offset: Int) = database.withTransaction {
        val ordered = dao.allGroups().filter { it.deletedAt == null }.sortedWith(compareByDescending<TodoGroup> { it.isInbox }.thenBy { it.sortOrder })
        val from = ordered.indexOfFirst { it.id == group.id }
        val to = (from + offset).coerceIn(0, ordered.lastIndex)
        if (from < 0 || from == to || group.isInbox || ordered[to].isInbox) return@withTransaction
        val other = ordered[to]
        val now = System.currentTimeMillis()
        val currentDevice = deviceId()
        dao.upsertGroup(group.copy(sortOrder = other.sortOrder, updatedAt = now, deviceId = currentDevice, revision = group.revision + 1))
        dao.upsertGroup(other.copy(sortOrder = group.sortOrder, updatedAt = now, deviceId = currentDevice, revision = other.revision + 1))
    }

    suspend fun addStep(todoId: String, title: String): TodoStep {
        val now = System.currentTimeMillis()
        val step = TodoStep(
            todoId = todoId,
            title = title.trim().take(120),
            sortOrder = "%020d-%s".format(now, UUID.randomUUID()),
            createdAt = now,
            updatedAt = now,
            deviceId = deviceId()
        )
        dao.upsertStep(step)
        return step
    }

    suspend fun updateStep(step: TodoStep, title: String = step.title, completed: Boolean = step.isCompleted) {
        dao.upsertStep(
            step.copy(
                title = title.trim().take(120),
                isCompleted = completed,
                updatedAt = System.currentTimeMillis(),
                deviceId = deviceId(),
                revision = step.revision + 1
            )
        )
    }

    suspend fun deleteStep(step: TodoStep) {
        val now = System.currentTimeMillis()
        dao.upsertStep(
            step.copy(updatedAt = now, deletedAt = now, deviceId = deviceId(), revision = step.revision + 1)
        )
    }

    suspend fun moveStep(step: TodoStep, offset: Int) = database.withTransaction {
        val ordered = dao.stepsForTodo(step.todoId)
        val from = ordered.indexOfFirst { it.id == step.id }
        val to = (from + offset).coerceIn(0, ordered.lastIndex)
        if (from < 0 || from == to) return@withTransaction
        val other = ordered[to]
        val now = System.currentTimeMillis()
        val currentDevice = deviceId()
        dao.upsertStep(step.copy(sortOrder = other.sortOrder, updatedAt = now, deviceId = currentDevice, revision = step.revision + 1))
        dao.upsertStep(other.copy(sortOrder = step.sortOrder, updatedAt = now, deviceId = currentDevice, revision = other.revision + 1))
    }

    suspend fun permanentlyDeleteTodos(todos: Collection<TodoItem>) = database.withTransaction {
        if (todos.isEmpty()) return@withTransaction
        val now = System.currentTimeMillis()
        val currentDeviceId = deviceId()
        todos.forEach { todo ->
            dao.upsertTodoTombstone(
                TodoTombstone(
                    todoId = todo.id,
                    updatedAt = now,
                    tombstonedAt = now,
                    deviceId = currentDeviceId,
                    revision = todo.revision + 1
                )
            )
        }
        val ids = todos.map(TodoItem::id)
        dao.deleteStepsForTodos(ids)
        dao.deleteTodosById(ids)
    }

    private suspend fun createNextRecurrence(todo: TodoItem): TodoItem? {
        val currentDate = LocalDate.parse(todo.date)
        val nextDate = TodoRecurrence.nextDate(todo.recurrence, currentDate) ?: return null
        val sourceId = todo.recurrenceSourceId ?: todo.id
        dao.recurrenceInstance(sourceId, nextDate.toString())?.let { return it }
        val now = System.currentTimeMillis()
        val currentDeviceId = deviceId()
        val nextDueDate = todo.dueDate?.let { due ->
            TodoRecurrence.nextDate(todo.recurrence, LocalDate.parse(due))?.toString()
        }
        val zone = todo.dueZoneId?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()
        val nextDueAt = if (nextDueDate != null && todo.dueTime != null) {
            LocalDate.parse(nextDueDate).atTime(LocalTime.parse(todo.dueTime)).atZone(zone).toInstant().toEpochMilli()
        } else null
        val reminderOffset = if (todo.dueAt != null && todo.reminderAt != null) todo.dueAt - todo.reminderAt else null
        val next = todo.copy(
            id = UUID.randomUUID().toString(),
            date = nextDate.toString(),
            isCompleted = false,
            dueDate = nextDueDate,
            dueAt = nextDueAt,
            reminderAt = if (nextDueAt != null && reminderOffset != null) nextDueAt - reminderOffset else null,
            recurrenceSourceId = sourceId,
            sortOrder = "%020d-%s".format(now, UUID.randomUUID()),
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
            deviceId = currentDeviceId,
            revision = 1
        )
        dao.upsertTodo(next)
        dao.stepsForTodo(todo.id).forEachIndexed { index, step ->
            dao.upsertStep(
                step.copy(
                    id = UUID.randomUUID().toString(),
                    todoId = next.id,
                    isCompleted = false,
                    sortOrder = "%020d-%04d".format(now, index),
                    createdAt = now,
                    updatedAt = now,
                    deletedAt = null,
                    deviceId = currentDeviceId,
                    revision = 1
                )
            )
        }
        return next
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
            groups = dao.allGroups(),
            steps = dao.allSteps(),
            todoTombstones = dao.allTodoTombstones(),
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
            groups = dao.allGroups(),
            steps = dao.allSteps(),
            todoTombstones = dao.allTodoTombstones(),
            reviews = dao.allReviews(),
            summaries = dao.allSummaries(),
            rangeSummaries = dao.allRangeSummaries()
        )
        val merged = SyncSnapshot.merge(local, remote)
        val mergedTodoIds = merged.todos.mapTo(hashSetOf(), TodoItem::id)
        val suppressedIds = merged.todoTombstones.map(TodoTombstone::todoId).filterNot(mergedTodoIds::contains)
        if (suppressedIds.isNotEmpty()) {
            dao.deleteStepsForTodos(suppressedIds)
            dao.deleteTodosById(suppressedIds)
        }
        dao.upsertTodos(merged.todos)
        val normalizedGroups = merged.groups.map { group ->
            if (group.id == TodoDefaults.INBOX_GROUP_ID && group.isInbox && group.deletedAt == null && group.name == "收件箱") {
                group.copy(
                    name = "日常",
                    updatedAt = System.currentTimeMillis(),
                    deviceId = deviceId(),
                    revision = group.revision + 1
                )
            } else group
        }
        dao.upsertGroups(normalizedGroups)
        dao.upsertSteps(merged.steps)
        dao.upsertTodoTombstones(merged.todoTombstones)
        dao.upsertReviews(merged.reviews)
        dao.upsertSummaries(merged.summaries)
        dao.upsertRangeSummaries(merged.rangeSummaries)
        merged.copy(generatedAt = System.currentTimeMillis(), deviceId = deviceId(), groups = normalizedGroups)
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

    private fun escapeLike(value: String): String = value
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")
}

data class TodoEdit(
    val title: String,
    val note: String,
    val groupId: String = TodoDefaults.INBOX_GROUP_ID,
    val dueDate: LocalDate? = null,
    val dueTime: LocalTime? = null,
    val dueZoneId: ZoneId? = null,
    val reminderAt: Long? = null,
    val recurrence: String = TodoRecurrence.NONE
)

data class RangeSourceData(
    val todos: List<TodoItem>,
    val reviews: List<DailyReview>,
    val summaries: List<DailySummary>
) {
    val isEmpty: Boolean get() = todos.isEmpty() && reviews.none(DailyReview::hasContent) && summaries.isEmpty()
}
