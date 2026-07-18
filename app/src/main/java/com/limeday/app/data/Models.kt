package com.limeday.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalDate
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

object TodoDefaults {
    const val INBOX_GROUP_ID = "00000000-0000-4000-8000-000000000001"
}

object TodoRecurrence {
    const val NONE = "none"
    const val DAILY = "daily"
    const val WEEKDAYS = "weekdays"
    const val WEEKLY = "weekly"
    const val MONTHLY = "monthly"

    fun isSupported(value: String): Boolean = value in setOf(NONE, DAILY, WEEKDAYS, WEEKLY, MONTHLY) ||
        INTERVAL.matches(value)

    fun normalize(value: String): String = value.takeIf(::isSupported) ?: NONE

    fun nextDate(value: String, from: LocalDate): LocalDate? = when (val rule = normalize(value)) {
        NONE -> null
        DAILY -> from.plusDays(1)
        WEEKDAYS -> generateSequence(from.plusDays(1)) { it.plusDays(1) }
            .first { it.dayOfWeek !in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) }
        WEEKLY -> from.plusWeeks(1)
        MONTHLY -> from.plusMonths(1)
        else -> {
            val match = INTERVAL.matchEntire(rule) ?: return null
            val amount = match.groupValues[1].toLong().coerceIn(1, 365)
            when (match.groupValues[2]) {
                "DAYS" -> from.plusDays(amount)
                "WEEKS" -> from.plusWeeks(amount)
                "MONTHS" -> from.plusMonths(amount.coerceAtMost(120))
                else -> null
            }
        }
    }

    private val INTERVAL = Regex("interval:([1-9][0-9]{0,2}):(DAYS|WEEKS|MONTHS)")
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
    @ColumnInfo(name = "group_id", defaultValue = "'${TodoDefaults.INBOX_GROUP_ID}'") val groupId: String = TodoDefaults.INBOX_GROUP_ID,
    @ColumnInfo(name = "due_date") val dueDate: String? = null,
    @ColumnInfo(name = "due_time") val dueTime: String? = null,
    @ColumnInfo(name = "due_at") val dueAt: Long? = null,
    @ColumnInfo(name = "due_zone_id") val dueZoneId: String? = null,
    @ColumnInfo(name = "reminder_at") val reminderAt: Long? = null,
    @ColumnInfo(defaultValue = "'none'") val recurrence: String = TodoRecurrence.NONE,
    @ColumnInfo(name = "recurrence_source_id") val recurrenceSourceId: String? = null,
    @ColumnInfo(name = "sort_order") val sortOrder: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") override val updatedAt: Long = createdAt,
    @ColumnInfo(name = "deleted_at") override val deletedAt: Long? = null,
    @ColumnInfo(name = "device_id") override val deviceId: String,
    @ColumnInfo(defaultValue = "1") override val revision: Long = 1
) : SyncEntity

@Entity(
    tableName = "todo_groups",
    indices = [Index(value = ["sort_order"], name = "todo_groups_sort_idx")]
)
data class TodoGroup(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    @ColumnInfo(name = "icon_key", defaultValue = "'leaf'") val iconKey: String = "leaf",
    @ColumnInfo(name = "color_key", defaultValue = "'mint'") val colorKey: String = "mint",
    @ColumnInfo(name = "sort_order") val sortOrder: String,
    @ColumnInfo(name = "is_inbox", defaultValue = "0") val isInbox: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") override val updatedAt: Long = createdAt,
    @ColumnInfo(name = "deleted_at") override val deletedAt: Long? = null,
    @ColumnInfo(name = "device_id") override val deviceId: String,
    @ColumnInfo(defaultValue = "1") override val revision: Long = 1
) : SyncEntity

@Entity(
    tableName = "todo_steps",
    indices = [Index(value = ["todo_id"], name = "todo_steps_todo_idx")]
)
data class TodoStep(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "todo_id") val todoId: String,
    val title: String,
    @ColumnInfo(name = "is_completed", defaultValue = "0") val isCompleted: Boolean = false,
    @ColumnInfo(name = "sort_order") val sortOrder: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") override val updatedAt: Long = createdAt,
    @ColumnInfo(name = "deleted_at") override val deletedAt: Long? = null,
    @ColumnInfo(name = "device_id") override val deviceId: String,
    @ColumnInfo(defaultValue = "1") override val revision: Long = 1
) : SyncEntity

@Entity(tableName = "todo_tombstones")
data class TodoTombstone(
    @PrimaryKey @ColumnInfo(name = "todo_id") val todoId: String,
    @ColumnInfo(name = "updated_at") override val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val tombstonedAt: Long,
    @ColumnInfo(name = "device_id") override val deviceId: String,
    @ColumnInfo(defaultValue = "1") override val revision: Long = 1
) : SyncEntity {
    @get:Ignore
    override val deletedAt: Long? get() = tombstonedAt
}

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

@Entity(
    tableName = "range_summaries",
    indices = [Index(value = ["range_start"], name = "range_summaries_start_idx")]
)
data class RangeSummary(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "range_start") val rangeStart: String,
    @ColumnInfo(name = "range_end") val rangeEnd: String,
    @ColumnInfo(name = "period_type") val periodType: String,
    val prompt: String,
    val content: String,
    @ColumnInfo(name = "provider_id") val providerId: String,
    @ColumnInfo(name = "provider_name") val providerName: String,
    val model: String,
    @ColumnInfo(name = "include_existing_summaries", defaultValue = "0") val includeExistingSummaries: Boolean = false,
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
    @ColumnInfo(name = "schema_version_value") val schemaVersionValue: Int = 6,
    @ColumnInfo(name = "legacy_migration_version", defaultValue = "0") val legacyMigrationVersion: Int = 0,
    @ColumnInfo(name = "last_sync_at") val lastSyncAt: Long? = null,
    @ColumnInfo(name = "last_sync_status", defaultValue = "''") val lastSyncStatus: String = ""
)
