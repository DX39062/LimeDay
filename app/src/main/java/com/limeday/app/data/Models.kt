package com.limeday.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

interface SyncEntity {
    val updatedAt: Long
    val deletedAt: Long?
    val deviceId: String
    val revision: Long
}

object TodoPriority {
    const val LOW = 0
    const val NORMAL = 1
    const val HIGH = 2

    fun normalize(value: Int): Int = value.coerceIn(LOW, HIGH)
}

@Entity(
    tableName = "todos",
    indices = [Index(value = ["date"], name = "todos_date_idx")]
)
data class TodoItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val date: String,
    val title: String,
    @ColumnInfo(defaultValue = "''") val note: String = "",
    @ColumnInfo(name = "is_completed", defaultValue = "0") val isCompleted: Boolean = false,
    @ColumnInfo(defaultValue = "1") val priority: Int = TodoPriority.NORMAL,
    @ColumnInfo(name = "sort_order") val sortOrder: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") override val updatedAt: Long = createdAt,
    @ColumnInfo(name = "deleted_at") override val deletedAt: Long? = null,
    @ColumnInfo(name = "device_id") override val deviceId: String,
    @ColumnInfo(defaultValue = "1") override val revision: Long = 1
) : SyncEntity

@Entity(
    tableName = "daily_reviews",
    indices = [Index(value = ["date"], name = "reviews_date_idx", unique = true)]
)
data class DailyReview(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val date: String,
    @ColumnInfo(defaultValue = "''") val highlight: String = "",
    @ColumnInfo(defaultValue = "''") val challenge: String = "",
    @ColumnInfo(defaultValue = "''") val learning: String = "",
    @ColumnInfo(name = "tomorrow_focus", defaultValue = "''") val tomorrowFocus: String = "",
    @ColumnInfo(defaultValue = "0") val mood: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") override val updatedAt: Long = createdAt,
    @ColumnInfo(name = "deleted_at") override val deletedAt: Long? = null,
    @ColumnInfo(name = "device_id") override val deviceId: String,
    @ColumnInfo(defaultValue = "1") override val revision: Long = 1
) : SyncEntity {
    fun hasContent(): Boolean = highlight.isNotBlank() || challenge.isNotBlank() ||
        learning.isNotBlank() || tomorrowFocus.isNotBlank() || mood != 0
}

@Entity(
    tableName = "daily_summaries",
    indices = [Index(value = ["date"], name = "summaries_date_idx", unique = true)]
)
data class DailySummary(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val date: String,
    val content: String,
    val provider: String,
    val model: String,
    @ColumnInfo(name = "generated_at") val generatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") override val updatedAt: Long = generatedAt,
    @ColumnInfo(name = "deleted_at") override val deletedAt: Long? = null,
    @ColumnInfo(name = "device_id") override val deviceId: String,
    @ColumnInfo(defaultValue = "1") override val revision: Long = 1
) : SyncEntity

@Entity(tableName = "app_metadata")
data class AppMetadata(
    @PrimaryKey @ColumnInfo(defaultValue = "1") val id: Int = 1,
    @ColumnInfo(name = "device_id") val deviceId: String,
    @ColumnInfo(name = "schema_version_value") val schemaVersionValue: Int = 5,
    @ColumnInfo(name = "legacy_migration_version", defaultValue = "0") val legacyMigrationVersion: Int = 0,
    @ColumnInfo(name = "last_sync_at") val lastSyncAt: Long? = null,
    @ColumnInfo(name = "last_sync_status", defaultValue = "''") val lastSyncStatus: String = ""
)
