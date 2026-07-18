package com.limeday.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val databaseNames = mutableListOf<String>()

    @After
    fun cleanUp() {
        databaseNames.forEach(context::deleteDatabase)
    }

    @Test
    fun roomV1MigratesWithoutLosingTodoOrReview() = runBlocking {
        val database = openMigratedDatabase(version = 1)

        assertEquals("迁移待办", database.limeDayDao().allTodos().single().title)
        assertTrue(database.limeDayDao().allTodos().single().isCompleted)
        assertEquals(TodoPriority.NORMAL, database.limeDayDao().allTodos().single().priority)
        assertEquals("旧版亮点", database.limeDayDao().allReviews().single().highlight)
        assertTrue(database.limeDayDao().allSummaries().isEmpty())
        assertEquals(1, database.limeDayDao().metadata()?.legacyMigrationVersion)
        database.close()
    }

    @Test
    fun roomV2MigratesSummaryAndCreatesSyncMetadata() = runBlocking {
        val database = openMigratedDatabase(version = 2)

        assertEquals("旧版总结", database.limeDayDao().allSummaries().single().content)
        val todo = database.limeDayDao().allTodos().single()
        assertFalse(todo.id == "1")
        assertTrue(todo.deviceId.isNotBlank())
        assertEquals(2, database.limeDayDao().metadata()?.legacyMigrationVersion)
        database.close()
    }

    @Test
    fun driftV3MigratesToNativeRoomV5() = runBlocking {
        val database = openMigratedDatabase(version = 3)

        assertEquals("flutter-id", database.limeDayDao().allTodos().single().id)
        assertEquals(7, database.limeDayDao().allTodos().single().revision)
        assertEquals("flutter-device", database.limeDayDao().metadata()?.deviceId)
        assertEquals(7, database.limeDayDao().metadata()?.schemaVersionValue)
        assertEquals(TodoPriority.NORMAL, database.limeDayDao().allTodos().single().priority)
        database.close()
    }

    @Test
    fun roomV4AddsNormalPriority() = runBlocking {
        val database = openMigratedDatabase(version = 4)

        assertEquals("v4-id", database.limeDayDao().allTodos().single().id)
        assertEquals(TodoPriority.NORMAL, database.limeDayDao().allTodos().single().priority)
        assertEquals(7, database.limeDayDao().metadata()?.schemaVersionValue)
        database.close()
    }

    @Test
    fun roomV5AddsRangeSummaryTableWithoutChangingExistingData() = runBlocking {
        val database = openMigratedDatabase(version = 5)

        assertEquals("v5-id", database.limeDayDao().allTodos().single().id)
        assertTrue(database.limeDayDao().allRangeSummaries().isEmpty())
        assertEquals(7, database.limeDayDao().metadata()?.schemaVersionValue)
        assertEquals(TodoDefaults.INBOX_GROUP_ID, database.limeDayDao().allTodos().single().groupId)
        assertTrue(database.limeDayDao().allGroups().single().isInbox)
        database.close()
    }

    @Test
    fun roomV6AddsExpandedTodoDataAndPermanentDeletionTables() = runBlocking {
        val database = openMigratedDatabase(version = 6)

        val todo = database.limeDayDao().allTodos().single()
        assertEquals("v5-id", todo.id)
        assertEquals(TodoDefaults.INBOX_GROUP_ID, todo.groupId)
        assertEquals(TodoRecurrence.NONE, todo.recurrence)
        assertTrue(database.limeDayDao().allGroups().single().isInbox)
        assertTrue(database.limeDayDao().allSteps().isEmpty())
        assertTrue(database.limeDayDao().allTodoTombstones().isEmpty())
        assertEquals(7, database.limeDayDao().metadata()?.schemaVersionValue)
        database.close()
    }

    private fun openMigratedDatabase(version: Int): AppDatabase {
        val name = "migration-$version-${System.nanoTime()}.db"
        databaseNames += name
        val file = context.getDatabasePath(name)
        file.parentFile?.mkdirs()
        when (version) {
            1, 2 -> createRoomLegacy(file, version)
            3 -> createDriftV3(file)
            4 -> createRoomV4(file)
            5 -> createRoomV5(file)
            6 -> createRoomV6(file)
        }
        return Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(
                AppDatabase.MIGRATION_1_4,
                AppDatabase.MIGRATION_2_4,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7
            )
            .build()
            .also { it.openHelper.writableDatabase }
    }

    private fun createRoomLegacy(file: File, version: Int) {
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL("CREATE TABLE todos (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, date TEXT NOT NULL, title TEXT NOT NULL, note TEXT NOT NULL, isCompleted INTEGER NOT NULL, createdAt INTEGER NOT NULL)")
            db.execSQL("CREATE INDEX index_todos_date ON todos(date)")
            db.execSQL("CREATE TABLE daily_reviews (date TEXT NOT NULL PRIMARY KEY, highlight TEXT NOT NULL, challenge TEXT NOT NULL, learning TEXT NOT NULL, tomorrowFocus TEXT NOT NULL, mood INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
            if (version == 2) {
                db.execSQL("CREATE TABLE daily_summaries (date TEXT NOT NULL PRIMARY KEY, content TEXT NOT NULL, provider TEXT NOT NULL, model TEXT NOT NULL, generatedAt INTEGER NOT NULL)")
            }
            db.execSQL("INSERT INTO todos VALUES (1, '2026-07-16', '迁移待办', '备注', 1, 1000)")
            db.execSQL("INSERT INTO daily_reviews VALUES ('2026-07-16', '旧版亮点', '', '旧版收获', '明日重点', 4, 2000)")
            if (version == 2) db.execSQL("INSERT INTO daily_summaries VALUES ('2026-07-16', '旧版总结', 'OpenAI 兼容', 'test-model', 3000)")
            db.version = version
        }
    }

    private fun createDriftV3(file: File) {
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL("CREATE TABLE todos (id TEXT NOT NULL PRIMARY KEY, date TEXT NOT NULL, title TEXT NOT NULL, note TEXT NOT NULL DEFAULT '', is_completed INTEGER NOT NULL DEFAULT 0, sort_order TEXT NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, deleted_at INTEGER, device_id TEXT NOT NULL, revision INTEGER NOT NULL DEFAULT 1)")
            db.execSQL("CREATE INDEX todos_date_idx ON todos(date)")
            db.execSQL("CREATE TABLE daily_reviews (id TEXT NOT NULL PRIMARY KEY, date TEXT NOT NULL UNIQUE, highlight TEXT NOT NULL DEFAULT '', challenge TEXT NOT NULL DEFAULT '', learning TEXT NOT NULL DEFAULT '', tomorrow_focus TEXT NOT NULL DEFAULT '', mood INTEGER NOT NULL DEFAULT 0, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, deleted_at INTEGER, device_id TEXT NOT NULL, revision INTEGER NOT NULL DEFAULT 1)")
            db.execSQL("CREATE INDEX reviews_date_idx ON daily_reviews(date)")
            db.execSQL("CREATE TABLE daily_summaries (id TEXT NOT NULL PRIMARY KEY, date TEXT NOT NULL UNIQUE, content TEXT NOT NULL, provider TEXT NOT NULL, model TEXT NOT NULL, generated_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, deleted_at INTEGER, device_id TEXT NOT NULL, revision INTEGER NOT NULL DEFAULT 1)")
            db.execSQL("CREATE INDEX summaries_date_idx ON daily_summaries(date)")
            db.execSQL("CREATE TABLE app_metadata (id INTEGER NOT NULL PRIMARY KEY DEFAULT 1, device_id TEXT NOT NULL, schema_version_value INTEGER NOT NULL, legacy_migration_version INTEGER NOT NULL DEFAULT 0)")
            db.execSQL("INSERT INTO todos VALUES ('flutter-id', '2026-07-16', 'Flutter 待办', '', 0, '1', 1000, 2000, NULL, 'flutter-device', 7)")
            db.execSQL("INSERT INTO daily_reviews VALUES ('review-id', '2026-07-16', '亮点', '', '', '', 3, 1000, 2000, NULL, 'flutter-device', 2)")
            db.execSQL("INSERT INTO daily_summaries VALUES ('summary-id', '2026-07-16', '总结', 'provider', 'model', 2000, 2000, NULL, 'flutter-device', 2)")
            db.execSQL("INSERT INTO app_metadata VALUES (1, 'flutter-device', 3, 2)")
            db.version = 3
        }
    }

    private fun createRoomV4(file: File) {
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL("CREATE TABLE todos (id TEXT NOT NULL PRIMARY KEY, date TEXT NOT NULL, title TEXT NOT NULL, note TEXT NOT NULL DEFAULT '', is_completed INTEGER NOT NULL DEFAULT 0, sort_order TEXT NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, deleted_at INTEGER, device_id TEXT NOT NULL, revision INTEGER NOT NULL DEFAULT 1)")
            db.execSQL("CREATE INDEX todos_date_idx ON todos(date)")
            db.execSQL("CREATE TABLE daily_reviews (id TEXT NOT NULL PRIMARY KEY, date TEXT NOT NULL, highlight TEXT NOT NULL DEFAULT '', challenge TEXT NOT NULL DEFAULT '', learning TEXT NOT NULL DEFAULT '', tomorrow_focus TEXT NOT NULL DEFAULT '', mood INTEGER NOT NULL DEFAULT 0, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, deleted_at INTEGER, device_id TEXT NOT NULL, revision INTEGER NOT NULL DEFAULT 1)")
            db.execSQL("CREATE UNIQUE INDEX reviews_date_idx ON daily_reviews(date)")
            db.execSQL("CREATE TABLE daily_summaries (id TEXT NOT NULL PRIMARY KEY, date TEXT NOT NULL, content TEXT NOT NULL, provider TEXT NOT NULL, model TEXT NOT NULL, generated_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, deleted_at INTEGER, device_id TEXT NOT NULL, revision INTEGER NOT NULL DEFAULT 1)")
            db.execSQL("CREATE UNIQUE INDEX summaries_date_idx ON daily_summaries(date)")
            db.execSQL("CREATE TABLE app_metadata (id INTEGER NOT NULL PRIMARY KEY DEFAULT 1, device_id TEXT NOT NULL, schema_version_value INTEGER NOT NULL, legacy_migration_version INTEGER NOT NULL DEFAULT 0, last_sync_at INTEGER, last_sync_status TEXT NOT NULL DEFAULT '')")
            db.execSQL("INSERT INTO todos VALUES ('v4-id', '2026-07-17', 'V4 待办', '', 0, '1', 1000, 1000, NULL, 'v4-device', 1)")
            db.execSQL("INSERT INTO app_metadata VALUES (1, 'v4-device', 4, 0, NULL, '')")
            db.version = 4
        }
    }

    private fun createRoomV5(file: File) {
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL("CREATE TABLE todos (id TEXT NOT NULL PRIMARY KEY, date TEXT NOT NULL, title TEXT NOT NULL, note TEXT NOT NULL DEFAULT '', is_completed INTEGER NOT NULL DEFAULT 0, priority INTEGER NOT NULL DEFAULT 1, sort_order TEXT NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, deleted_at INTEGER, device_id TEXT NOT NULL, revision INTEGER NOT NULL DEFAULT 1)")
            db.execSQL("CREATE INDEX todos_date_idx ON todos(date)")
            db.execSQL("CREATE TABLE daily_reviews (id TEXT NOT NULL PRIMARY KEY, date TEXT NOT NULL, highlight TEXT NOT NULL DEFAULT '', challenge TEXT NOT NULL DEFAULT '', learning TEXT NOT NULL DEFAULT '', tomorrow_focus TEXT NOT NULL DEFAULT '', mood INTEGER NOT NULL DEFAULT 0, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, deleted_at INTEGER, device_id TEXT NOT NULL, revision INTEGER NOT NULL DEFAULT 1)")
            db.execSQL("CREATE UNIQUE INDEX reviews_date_idx ON daily_reviews(date)")
            db.execSQL("CREATE TABLE daily_summaries (id TEXT NOT NULL PRIMARY KEY, date TEXT NOT NULL, content TEXT NOT NULL, provider TEXT NOT NULL, model TEXT NOT NULL, generated_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, deleted_at INTEGER, device_id TEXT NOT NULL, revision INTEGER NOT NULL DEFAULT 1)")
            db.execSQL("CREATE UNIQUE INDEX summaries_date_idx ON daily_summaries(date)")
            db.execSQL("CREATE TABLE app_metadata (id INTEGER NOT NULL PRIMARY KEY DEFAULT 1, device_id TEXT NOT NULL, schema_version_value INTEGER NOT NULL, legacy_migration_version INTEGER NOT NULL DEFAULT 0, last_sync_at INTEGER, last_sync_status TEXT NOT NULL DEFAULT '')")
            db.execSQL("INSERT INTO todos VALUES ('v5-id', '2026-07-17', 'V5 待办', '', 0, 1, '1', 1000, 1000, NULL, 'v5-device', 1)")
            db.execSQL("INSERT INTO app_metadata VALUES (1, 'v5-device', 5, 0, NULL, '')")
            db.version = 5
        }
    }

    private fun createRoomV6(file: File) {
        createRoomV5(file)
        SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
            db.execSQL("CREATE TABLE range_summaries (id TEXT NOT NULL PRIMARY KEY, range_start TEXT NOT NULL, range_end TEXT NOT NULL, period_type TEXT NOT NULL, prompt TEXT NOT NULL, content TEXT NOT NULL, provider_id TEXT NOT NULL, provider_name TEXT NOT NULL, model TEXT NOT NULL, include_existing_summaries INTEGER NOT NULL DEFAULT 0, generated_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, deleted_at INTEGER, device_id TEXT NOT NULL, revision INTEGER NOT NULL DEFAULT 1)")
            db.execSQL("CREATE INDEX range_summaries_start_idx ON range_summaries(range_start)")
            db.execSQL("UPDATE app_metadata SET schema_version_value = 6 WHERE id = 1")
            db.version = 6
        }
    }
}
