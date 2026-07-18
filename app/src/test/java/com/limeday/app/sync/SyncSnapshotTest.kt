package com.limeday.app.sync

import com.limeday.app.data.DailyReview
import com.limeday.app.data.TodoItem
import com.limeday.app.data.TodoPriority
import com.limeday.app.data.RangeSummary
import com.limeday.app.data.TodoDefaults
import com.limeday.app.data.TodoGroup
import com.limeday.app.data.TodoStep
import com.limeday.app.data.TodoTombstone
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
            groups = listOf(group()),
            steps = listOf(step()),
            tombstones = listOf(tombstone(updatedAt = 40)),
            reviews = listOf(review(updatedAt = 30))
        )

        val decoded = SyncSnapshot.fromJson(source.toJson())

        assertEquals(source.deviceId, decoded.deviceId)
        assertEquals("todo-1", decoded.todos.single().id)
        assertNotNull(decoded.todos.single().deletedAt)
        assertEquals(TodoPriority.HIGH, decoded.todos.single().priority)
        assertEquals("工作", decoded.groups.first { !it.isInbox }.name)
        assertEquals("拆成一步", decoded.steps.single().title)
        assertEquals("todo-1", decoded.todoTombstones.single().todoId)
        assertEquals("亮点", decoded.reviews.single().highlight)
    }

    @Test
    fun `v1 snapshot migrates to v2 with inbox and new todo defaults`() {
        val json = JSONObject(snapshot(todos = listOf(todo(updatedAt = 10))).toJson()).apply {
            put("formatVersion", 1)
            remove("groups")
            remove("steps")
            remove("todoTombstones")
            getJSONArray("todos").getJSONObject(0).apply {
                remove("groupId")
                remove("recurrence")
            }
        }

        val decoded = SyncSnapshot.fromJson(json.toString())

        assertEquals(2, decoded.formatVersion)
        assertEquals(TodoDefaults.INBOX_GROUP_ID, decoded.todos.single().groupId)
        assertTrue(decoded.groups.single().isInbox)
        assertTrue(decoded.steps.isEmpty())
    }

    @Test
    fun `legacy snapshot without priority defaults to normal`() {
        val json = JSONObject(snapshot(todos = listOf(todo(updatedAt = 10))).toJson())
        json.getJSONArray("todos").getJSONObject(0).remove("priority")

        val decoded = SyncSnapshot.fromJson(json.toString())

        assertEquals(TodoPriority.NORMAL, decoded.todos.single().priority)
    }

    @Test
    fun `legacy snapshot without range summaries defaults to empty`() {
        val json = JSONObject(snapshot().toJson()).apply { remove("rangeSummaries") }

        val decoded = SyncSnapshot.fromJson(json.toString())

        assertTrue(decoded.rangeSummaries.isEmpty())
    }

    @Test
    fun `range summary round trip and merge use stable id`() {
        val local = snapshot(rangeSummaries = listOf(rangeSummary(content = "本地", updatedAt = 100)))
        val remote = snapshot(
            deviceId = "device-b",
            rangeSummaries = listOf(rangeSummary(content = "远端", updatedAt = 200, deviceId = "device-b"))
        )

        val decoded = SyncSnapshot.fromJson(local.toJson())
        val merged = SyncSnapshot.merge(local, remote)

        assertEquals("本地", decoded.rangeSummaries.single().content)
        assertEquals("远端", merged.rangeSummaries.single().content)
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
        val invalid = JSONObject(snapshot().toJson()).put("formatVersion", 99).toString()

        assertThrows(IllegalArgumentException::class.java) { SyncSnapshot.fromJson(invalid) }
    }

    @Test
    fun `permanent tombstone suppresses older todo and its steps`() {
        val local = snapshot(
            todos = listOf(todo(updatedAt = 100)),
            steps = listOf(step(updatedAt = 100))
        )
        val remote = snapshot(
            deviceId = "device-b",
            tombstones = listOf(tombstone(updatedAt = 200, deviceId = "device-b"))
        )

        val merged = SyncSnapshot.merge(local, remote)

        assertTrue(merged.todos.isEmpty())
        assertTrue(merged.steps.isEmpty())
        assertEquals("todo-1", merged.todoTombstones.single().todoId)
    }

    @Test
    fun `newer todo can explicitly recover past an older permanent tombstone`() {
        val local = snapshot(tombstones = listOf(tombstone(updatedAt = 100)))
        val remote = snapshot(deviceId = "device-b", todos = listOf(todo(updatedAt = 200, deviceId = "device-b")))

        val merged = SyncSnapshot.merge(local, remote)

        assertEquals("todo-1", merged.todos.single().id)
    }

    private fun snapshot(
        deviceId: String = "device-a",
        todos: List<TodoItem> = emptyList(),
        groups: List<TodoGroup> = emptyList(),
        steps: List<TodoStep> = emptyList(),
        tombstones: List<TodoTombstone> = emptyList(),
        reviews: List<DailyReview> = emptyList(),
        rangeSummaries: List<RangeSummary> = emptyList()
    ) = SyncSnapshot(
        generatedAt = 100,
        deviceId = deviceId,
        todos = todos,
        groups = groups,
        steps = steps,
        todoTombstones = tombstones,
        reviews = reviews,
        summaries = emptyList(),
        rangeSummaries = rangeSummaries
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

    private fun group() = TodoGroup(
        id = "group-1",
        name = "工作",
        sortOrder = "1",
        deviceId = "device-a"
    )

    private fun step(updatedAt: Long = 30) = TodoStep(
        id = "step-1",
        todoId = "todo-1",
        title = "拆成一步",
        sortOrder = "1",
        createdAt = 1,
        updatedAt = updatedAt,
        deviceId = "device-a"
    )

    private fun tombstone(updatedAt: Long, deviceId: String = "device-a") = TodoTombstone(
        todoId = "todo-1",
        updatedAt = updatedAt,
        tombstonedAt = updatedAt,
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

    private fun rangeSummary(
        content: String,
        updatedAt: Long,
        deviceId: String = "device-a"
    ) = RangeSummary(
        id = "range-1",
        rangeStart = "2026-07-01",
        rangeEnd = "2026-07-07",
        periodType = "week",
        prompt = "总结本周",
        content = content,
        providerId = "provider-1",
        providerName = "供应商",
        model = "model",
        generatedAt = updatedAt,
        updatedAt = updatedAt,
        deviceId = deviceId
    )
}
