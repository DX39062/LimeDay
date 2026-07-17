package com.limeday.app.sync

import com.limeday.app.data.DailyReview
import com.limeday.app.data.TodoItem
import com.limeday.app.data.TodoPriority
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncSnapshotTest {
    @Test
    fun `snapshot json round trip keeps records and tombstones`() {
        val source = snapshot(
            todos = listOf(todo(id = "todo-1", updatedAt = 20, deletedAt = 20, priority = TodoPriority.HIGH)),
            reviews = listOf(review(updatedAt = 30))
        )

        val decoded = SyncSnapshot.fromJson(source.toJson())

        assertEquals(source.deviceId, decoded.deviceId)
        assertEquals("todo-1", decoded.todos.single().id)
        assertNotNull(decoded.todos.single().deletedAt)
        assertEquals(TodoPriority.HIGH, decoded.todos.single().priority)
        assertEquals("亮点", decoded.reviews.single().highlight)
    }

    @Test
    fun `legacy snapshot without priority defaults to normal`() {
        val json = JSONObject(snapshot(todos = listOf(todo(updatedAt = 10))).toJson())
        json.getJSONArray("todos").getJSONObject(0).remove("priority")

        val decoded = SyncSnapshot.fromJson(json.toString())

        assertEquals(TodoPriority.NORMAL, decoded.todos.single().priority)
    }

    @Test
    fun `out of range priority is normalized`() {
        val json = JSONObject(snapshot(todos = listOf(todo(updatedAt = 10))).toJson())
        json.getJSONArray("todos").getJSONObject(0).put("priority", 99)

        val decoded = SyncSnapshot.fromJson(json.toString())

        assertEquals(TodoPriority.HIGH, decoded.todos.single().priority)
    }

    @Test
    fun `newer remote todo wins by updated time`() {
        val local = snapshot(todos = listOf(todo(title = "本地", updatedAt = 100)))
        val remote = snapshot(deviceId = "device-b", todos = listOf(todo(title = "远端", updatedAt = 200, deviceId = "device-b")))

        val merged = SyncSnapshot.merge(local, remote)

        assertEquals("远端", merged.todos.single().title)
    }

    @Test
    fun `tombstone prevents older live record from returning`() {
        val local = snapshot(todos = listOf(todo(title = "旧内容", updatedAt = 100)))
        val remote = snapshot(
            deviceId = "device-b",
            todos = listOf(todo(title = "旧内容", updatedAt = 200, deletedAt = 200, deviceId = "device-b"))
        )

        val merged = SyncSnapshot.merge(local, remote)

        assertTrue(merged.todos.single().deletedAt != null)
    }

    @Test
    fun `reviews merge by date instead of physical id`() {
        val local = snapshot(reviews = listOf(review(id = "review-a", highlight = "本地", updatedAt = 100)))
        val remote = snapshot(
            deviceId = "device-b",
            reviews = listOf(review(id = "review-b", highlight = "远端", updatedAt = 200, deviceId = "device-b"))
        )

        val merged = SyncSnapshot.merge(local, remote)

        assertEquals(1, merged.reviews.size)
        assertEquals("远端", merged.reviews.single().highlight)
    }

    @Test
    fun `backup json contains business data but no credential fields`() {
        val json = snapshot(todos = listOf(todo(updatedAt = 10))).toJson()

        assertTrue(json.contains("todo-1"))
        assertFalse(json.contains("password", ignoreCase = true))
        assertFalse(json.contains("apiKey", ignoreCase = true))
    }

    @Test
    fun `unsupported backup format is rejected before merge`() {
        val invalid = snapshot().toJson().replace("\"formatVersion\":1", "\"formatVersion\":99")

        assertThrows(IllegalArgumentException::class.java) { SyncSnapshot.fromJson(invalid) }
    }

    private fun snapshot(
        deviceId: String = "device-a",
        todos: List<TodoItem> = emptyList(),
        reviews: List<DailyReview> = emptyList()
    ) = SyncSnapshot(
        generatedAt = 100,
        deviceId = deviceId,
        todos = todos,
        reviews = reviews,
        summaries = emptyList()
    )

    private fun todo(
        id: String = "todo-1",
        title: String = "待办",
        updatedAt: Long,
        deletedAt: Long? = null,
        deviceId: String = "device-a",
        priority: Int = TodoPriority.NORMAL
    ) = TodoItem(
        id = id,
        date = "2026-07-16",
        title = title,
        priority = priority,
        sortOrder = "1",
        createdAt = 1,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        deviceId = deviceId
    )

    private fun review(
        id: String = "review-1",
        highlight: String = "亮点",
        updatedAt: Long,
        deviceId: String = "device-a"
    ) = DailyReview(
        id = id,
        date = "2026-07-16",
        highlight = highlight,
        createdAt = 1,
        updatedAt = updatedAt,
        deviceId = deviceId
    )
}
