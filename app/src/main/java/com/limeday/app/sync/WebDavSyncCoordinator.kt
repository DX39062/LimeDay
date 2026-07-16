package com.limeday.app.sync

import com.limeday.app.data.LimeDayRepository

data class SyncResult(val message: String, val syncedAt: Long)

class WebDavSyncCoordinator(
    private val repository: LimeDayRepository,
    private val client: WebDavClient
) {
    suspend fun sync(config: WebDavConfig): SyncResult {
        val remote = client.download(config)
        val merged = if (remote == null) repository.snapshot() else repository.merge(remote)
        client.upload(config, merged)
        val time = System.currentTimeMillis()
        val message = "同步完成：${merged.todos.size} 条待办，${merged.reviews.size} 条复盘"
        repository.recordSync(message, time)
        return SyncResult(message, time)
    }
}
