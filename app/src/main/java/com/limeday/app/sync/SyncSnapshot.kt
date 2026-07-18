package com.limeday.app.sync

import com.limeday.app.data.DailyReview
import com.limeday.app.data.DailySummary
import com.limeday.app.data.RangeSummary
import com.limeday.app.data.SyncEntity
import com.limeday.app.data.TodoItem
import com.limeday.app.data.TodoDefaults
import com.limeday.app.data.TodoGroup
import com.limeday.app.data.TodoPriority
import com.limeday.app.data.TodoRecurrence
import com.limeday.app.data.TodoStep
import com.limeday.app.data.TodoTombstone
import org.json.JSONArray
import org.json.JSONObject

data class SyncSnapshot(
    val formatVersion: Int = FORMAT_VERSION,
    val generatedAt: Long,
    val deviceId: String,
    val todos: List<TodoItem>,
    val groups: List<TodoGroup> = emptyList(),
    val steps: List<TodoStep> = emptyList(),
    val todoTombstones: List<TodoTombstone> = emptyList(),
    val reviews: List<DailyReview>,
    val summaries: List<DailySummary>,
    val rangeSummaries: List<RangeSummary> = emptyList()
) {
    fun toJson(): String = JSONObject()
        .put("formatVersion", formatVersion)
        .put("generatedAt", generatedAt)
        .put("deviceId", deviceId)
        .put("todos", JSONArray().apply { todos.forEach { put(it.toJson()) } })
        .put("groups", JSONArray().apply { groups.forEach { put(it.toJson()) } })
        .put("steps", JSONArray().apply { steps.forEach { put(it.toJson()) } })
        .put("todoTombstones", JSONArray().apply { todoTombstones.forEach { put(it.toJson()) } })
        .put("reviews", JSONArray().apply { reviews.forEach { put(it.toJson()) } })
        .put("summaries", JSONArray().apply { summaries.forEach { put(it.toJson()) } })
        .put("rangeSummaries", JSONArray().apply { rangeSummaries.forEach { put(it.toJson()) } })
        .toString()

    companion object {
        const val FORMAT_VERSION = 2

        fun fromJson(value: String): SyncSnapshot {
            val json = JSONObject(value)
            val version = json.getInt("formatVersion")
            require(version in setOf(1, FORMAT_VERSION)) { "不支持的同步文件版本：$version" }
            val deviceId = json.optString("deviceId")
            val generatedAt = json.optLong("generatedAt")
            val groups = if (version >= 2) {
                json.optJSONArray("groups")?.mapObjects(::groupFromJson).orEmpty()
            } else {
                listOf(inboxGroup(deviceId, generatedAt))
            }
            return SyncSnapshot(
                formatVersion = FORMAT_VERSION,
                generatedAt = json.optLong("generatedAt"),
                deviceId = deviceId,
                todos = json.getJSONArray("todos").mapObjects(::todoFromJson),
                groups = ensureInbox(groups, deviceId, generatedAt),
                steps = if (version >= 2) json.optJSONArray("steps")?.mapObjects(::stepFromJson).orEmpty() else emptyList(),
                todoTombstones = if (version >= 2) json.optJSONArray("todoTombstones")?.mapObjects(::tombstoneFromJson).orEmpty() else emptyList(),
                reviews = json.getJSONArray("reviews").mapObjects(::reviewFromJson),
                summaries = json.getJSONArray("summaries").mapObjects(::summaryFromJson),
                rangeSummaries = json.optJSONArray("rangeSummaries")?.mapObjects(::rangeSummaryFromJson).orEmpty()
            )
        }

        fun merge(local: SyncSnapshot, remote: SyncSnapshot): SyncSnapshot {
            val tombstones = mergeBy(local.todoTombstones, remote.todoTombstones) { it.todoId }
            val tombstonesById = tombstones.associateBy(TodoTombstone::todoId)
            val todos = mergeBy(local.todos, remote.todos) { it.id }.filter { todo ->
                tombstonesById[todo.id]?.let { compareVersion(todo, it) > 0 } ?: true
            }
            return SyncSnapshot(
                generatedAt = maxOf(local.generatedAt, remote.generatedAt),
                deviceId = local.deviceId,
                todos = todos,
                groups = ensureInbox(mergeBy(local.groups, remote.groups) { it.id }, local.deviceId, local.generatedAt),
                steps = mergeBy(local.steps, remote.steps) { it.id }.filter { step -> todos.any { it.id == step.todoId } },
                todoTombstones = tombstones,
                reviews = mergeBy(local.reviews, remote.reviews) { it.date },
                summaries = mergeBy(local.summaries, remote.summaries) { it.date },
                rangeSummaries = mergeBy(local.rangeSummaries, remote.rangeSummaries) { it.id }
            )
        }

        private fun <T : SyncEntity> mergeBy(
            local: List<T>,
            remote: List<T>,
            key: (T) -> String
        ): List<T> {
            val result = linkedMapOf<String, T>()
            (local + remote).forEach { candidate ->
                val identity = key(candidate)
                val current = result[identity]
                if (current == null || compareVersion(candidate, current) > 0) result[identity] = candidate
            }
            return result.values.sortedBy(key)
        }

        private fun compareVersion(left: SyncEntity, right: SyncEntity): Int =
            compareValuesBy(left, right, SyncEntity::updatedAt, SyncEntity::revision, SyncEntity::deviceId)

        private fun todoFromJson(json: JSONObject) = TodoItem(
            id = json.getString("id"),
            date = json.getString("date"),
            title = json.getString("title"),
            note = json.optString("note"),
            isCompleted = json.optBoolean("isCompleted"),
            priority = if (json.has("priority")) {
                TodoPriority.normalize(json.optInt("priority", TodoPriority.NORMAL))
            } else {
                TodoPriority.NORMAL
            },
            groupId = json.optString("groupId", TodoDefaults.INBOX_GROUP_ID).ifBlank { TodoDefaults.INBOX_GROUP_ID },
            dueDate = json.nullableString("dueDate"),
            dueTime = json.nullableString("dueTime"),
            dueAt = json.nullableLong("dueAt"),
            dueZoneId = json.nullableString("dueZoneId"),
            reminderAt = json.nullableLong("reminderAt"),
            recurrence = TodoRecurrence.normalize(json.optString("recurrence", TodoRecurrence.NONE)),
            recurrenceSourceId = json.nullableString("recurrenceSourceId"),
            sortOrder = json.getString("sortOrder"),
            createdAt = json.getLong("createdAt"),
            updatedAt = json.getLong("updatedAt"),
            deletedAt = json.nullableLong("deletedAt"),
            deviceId = json.getString("deviceId"),
            revision = json.getLong("revision")
        )

        private fun groupFromJson(json: JSONObject) = TodoGroup(
            id = json.getString("id"),
            name = json.getString("name"),
            iconKey = json.optString("iconKey", "leaf"),
            colorKey = json.optString("colorKey", "mint"),
            sortOrder = json.getString("sortOrder"),
            isInbox = json.optBoolean("isInbox"),
            createdAt = json.getLong("createdAt"),
            updatedAt = json.getLong("updatedAt"),
            deletedAt = json.nullableLong("deletedAt"),
            deviceId = json.getString("deviceId"),
            revision = json.getLong("revision")
        )

        private fun stepFromJson(json: JSONObject) = TodoStep(
            id = json.getString("id"),
            todoId = json.getString("todoId"),
            title = json.getString("title"),
            isCompleted = json.optBoolean("isCompleted"),
            sortOrder = json.getString("sortOrder"),
            createdAt = json.getLong("createdAt"),
            updatedAt = json.getLong("updatedAt"),
            deletedAt = json.nullableLong("deletedAt"),
            deviceId = json.getString("deviceId"),
            revision = json.getLong("revision")
        )

        private fun tombstoneFromJson(json: JSONObject) = TodoTombstone(
            todoId = json.getString("todoId"),
            updatedAt = json.getLong("updatedAt"),
            tombstonedAt = json.getLong("deletedAt"),
            deviceId = json.getString("deviceId"),
            revision = json.getLong("revision")
        )

        private fun ensureInbox(groups: List<TodoGroup>, deviceId: String, timestamp: Long): List<TodoGroup> =
            if (groups.any { it.id == TodoDefaults.INBOX_GROUP_ID }) groups
            else listOf(inboxGroup(deviceId, timestamp)) + groups

        private fun inboxGroup(deviceId: String, timestamp: Long) = TodoGroup(
            id = TodoDefaults.INBOX_GROUP_ID,
            name = "收件箱",
            iconKey = "inbox",
            colorKey = "mint",
            sortOrder = "00000000000000000000-inbox",
            isInbox = true,
            createdAt = timestamp,
            updatedAt = timestamp,
            deviceId = deviceId
        )

        private fun reviewFromJson(json: JSONObject) = DailyReview(
            id = json.getString("id"),
            date = json.getString("date"),
            highlight = json.optString("highlight"),
            challenge = json.optString("challenge"),
            learning = json.optString("learning"),
            tomorrowFocus = json.optString("tomorrowFocus"),
            mood = json.optInt("mood"),
            createdAt = json.getLong("createdAt"),
            updatedAt = json.getLong("updatedAt"),
            deletedAt = json.nullableLong("deletedAt"),
            deviceId = json.getString("deviceId"),
            revision = json.getLong("revision")
        )

        private fun summaryFromJson(json: JSONObject) = DailySummary(
            id = json.getString("id"),
            date = json.getString("date"),
            content = json.getString("content"),
            provider = json.getString("provider"),
            model = json.getString("model"),
            generatedAt = json.getLong("generatedAt"),
            updatedAt = json.getLong("updatedAt"),
            deletedAt = json.nullableLong("deletedAt"),
            deviceId = json.getString("deviceId"),
            revision = json.getLong("revision")
        )

        private fun rangeSummaryFromJson(json: JSONObject) = RangeSummary(
            id = json.getString("id"),
            rangeStart = json.getString("rangeStart"),
            rangeEnd = json.getString("rangeEnd"),
            periodType = json.getString("periodType"),
            prompt = json.getString("prompt"),
            content = json.getString("content"),
            providerId = json.optString("providerId"),
            providerName = json.getString("providerName"),
            model = json.getString("model"),
            includeExistingSummaries = json.optBoolean("includeExistingSummaries"),
            generatedAt = json.getLong("generatedAt"),
            updatedAt = json.getLong("updatedAt"),
            deletedAt = json.nullableLong("deletedAt"),
            deviceId = json.getString("deviceId"),
            revision = json.getLong("revision")
        )
    }
}

private fun TodoItem.toJson() = JSONObject()
    .put("id", id).put("date", date).put("title", title).put("note", note)
    .put("isCompleted", isCompleted).put("priority", TodoPriority.normalize(priority)).put("sortOrder", sortOrder)
    .put("groupId", groupId).putNullable("dueDate", dueDate).putNullable("dueTime", dueTime)
    .putNullable("dueAt", dueAt).putNullable("dueZoneId", dueZoneId).putNullable("reminderAt", reminderAt)
    .put("recurrence", TodoRecurrence.normalize(recurrence)).putNullable("recurrenceSourceId", recurrenceSourceId)
    .put("createdAt", createdAt).put("updatedAt", updatedAt).putNullable("deletedAt", deletedAt)
    .put("deviceId", deviceId).put("revision", revision)

private fun TodoGroup.toJson() = JSONObject()
    .put("id", id).put("name", name).put("iconKey", iconKey).put("colorKey", colorKey)
    .put("sortOrder", sortOrder).put("isInbox", isInbox).put("createdAt", createdAt)
    .put("updatedAt", updatedAt).putNullable("deletedAt", deletedAt).put("deviceId", deviceId).put("revision", revision)

private fun TodoStep.toJson() = JSONObject()
    .put("id", id).put("todoId", todoId).put("title", title).put("isCompleted", isCompleted)
    .put("sortOrder", sortOrder).put("createdAt", createdAt).put("updatedAt", updatedAt)
    .putNullable("deletedAt", deletedAt).put("deviceId", deviceId).put("revision", revision)

private fun TodoTombstone.toJson() = JSONObject()
    .put("todoId", todoId).put("updatedAt", updatedAt).put("deletedAt", tombstonedAt)
    .put("deviceId", deviceId).put("revision", revision)

private fun DailyReview.toJson() = JSONObject()
    .put("id", id).put("date", date).put("highlight", highlight).put("challenge", challenge)
    .put("learning", learning).put("tomorrowFocus", tomorrowFocus).put("mood", mood)
    .put("createdAt", createdAt).put("updatedAt", updatedAt).putNullable("deletedAt", deletedAt)
    .put("deviceId", deviceId).put("revision", revision)

private fun DailySummary.toJson() = JSONObject()
    .put("id", id).put("date", date).put("content", content).put("provider", provider)
    .put("model", model).put("generatedAt", generatedAt).put("updatedAt", updatedAt)
    .putNullable("deletedAt", deletedAt).put("deviceId", deviceId).put("revision", revision)

private fun RangeSummary.toJson() = JSONObject()
    .put("id", id).put("rangeStart", rangeStart).put("rangeEnd", rangeEnd).put("periodType", periodType)
    .put("prompt", prompt).put("content", content).put("providerId", providerId).put("providerName", providerName)
    .put("model", model).put("includeExistingSummaries", includeExistingSummaries).put("generatedAt", generatedAt)
    .put("updatedAt", updatedAt).putNullable("deletedAt", deletedAt).put("deviceId", deviceId).put("revision", revision)

private fun JSONObject.putNullable(key: String, value: Long?): JSONObject = put(key, value ?: JSONObject.NULL)

private fun JSONObject.putNullable(key: String, value: String?): JSONObject = put(key, value ?: JSONObject.NULL)

private fun JSONObject.nullableLong(key: String): Long? = if (isNull(key)) null else getLong(key)

private fun JSONObject.nullableString(key: String): String? = if (!has(key) || isNull(key)) null else getString(key)

private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
    (0 until length()).map { transform(getJSONObject(it)) }
