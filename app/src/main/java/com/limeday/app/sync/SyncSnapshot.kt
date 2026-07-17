package com.limeday.app.sync

import com.limeday.app.data.DailyReview
import com.limeday.app.data.DailySummary
import com.limeday.app.data.SyncEntity
import com.limeday.app.data.TodoItem
import com.limeday.app.data.TodoPriority
import org.json.JSONArray
import org.json.JSONObject

data class SyncSnapshot(
    val formatVersion: Int = FORMAT_VERSION,
    val generatedAt: Long,
    val deviceId: String,
    val todos: List<TodoItem>,
    val reviews: List<DailyReview>,
    val summaries: List<DailySummary>
) {
    fun toJson(): String = JSONObject()
        .put("formatVersion", formatVersion)
        .put("generatedAt", generatedAt)
        .put("deviceId", deviceId)
        .put("todos", JSONArray().apply { todos.forEach { put(it.toJson()) } })
        .put("reviews", JSONArray().apply { reviews.forEach { put(it.toJson()) } })
        .put("summaries", JSONArray().apply { summaries.forEach { put(it.toJson()) } })
        .toString()

    companion object {
        const val FORMAT_VERSION = 1

        fun fromJson(value: String): SyncSnapshot {
            val json = JSONObject(value)
            val version = json.getInt("formatVersion")
            require(version == FORMAT_VERSION) { "不支持的同步文件版本：$version" }
            return SyncSnapshot(
                formatVersion = version,
                generatedAt = json.optLong("generatedAt"),
                deviceId = json.optString("deviceId"),
                todos = json.getJSONArray("todos").mapObjects(::todoFromJson),
                reviews = json.getJSONArray("reviews").mapObjects(::reviewFromJson),
                summaries = json.getJSONArray("summaries").mapObjects(::summaryFromJson)
            )
        }

        fun merge(local: SyncSnapshot, remote: SyncSnapshot): SyncSnapshot = SyncSnapshot(
            generatedAt = maxOf(local.generatedAt, remote.generatedAt),
            deviceId = local.deviceId,
            todos = mergeBy(local.todos, remote.todos) { it.id },
            reviews = mergeBy(local.reviews, remote.reviews) { it.date },
            summaries = mergeBy(local.summaries, remote.summaries) { it.date }
        )

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
            sortOrder = json.getString("sortOrder"),
            createdAt = json.getLong("createdAt"),
            updatedAt = json.getLong("updatedAt"),
            deletedAt = json.nullableLong("deletedAt"),
            deviceId = json.getString("deviceId"),
            revision = json.getLong("revision")
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
    }
}

private fun TodoItem.toJson() = JSONObject()
    .put("id", id).put("date", date).put("title", title).put("note", note)
    .put("isCompleted", isCompleted).put("priority", TodoPriority.normalize(priority)).put("sortOrder", sortOrder)
    .put("createdAt", createdAt).put("updatedAt", updatedAt).putNullable("deletedAt", deletedAt)
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

private fun JSONObject.putNullable(key: String, value: Long?): JSONObject = put(key, value ?: JSONObject.NULL)

private fun JSONObject.nullableLong(key: String): Long? = if (isNull(key)) null else getLong(key)

private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
    (0 until length()).map { transform(getJSONObject(it)) }
