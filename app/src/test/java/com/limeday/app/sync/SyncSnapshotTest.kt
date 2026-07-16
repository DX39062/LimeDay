package com.limeday.app.sync

import com.limeday.app.data.DailyReview
import com.limeday.app.data.TodoItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncSnapshotTest {
    @Test
    fun `snapshot json round trip keeps records and tombstones`() {
        val source = snapshot(
            todos = listOf(todo(id = "todo-1", updatedAt = 20, deletedAt = 20)),
            reviews = listOf(review(updatedAt = 30))
        )

        val decoded = SyncSnapshot.fromJson(source.toJson())

        assertEquals(source.deviceId, decoded.deviceId)
        assertEquals("todo-1", decoded.todos.single().id)
        assertNotNull(decoded.todos.single().deletedAt)
        assertEquals("亮点", decoded.reviews.single().highlight)
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
        deviceId: String = "device-a"
    ) = TodoItem(
        id = id,
        date = "2026-07-16",
        title = title,
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
