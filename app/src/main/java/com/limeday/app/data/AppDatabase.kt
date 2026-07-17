package com.limeday.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID

@Database(
    entities = [TodoItem::class, DailyReview::class, DailySummary::class, AppMetadata::class],
    version = 5,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun limeDayDao(): LimeDayDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "lime_day.db"
            ).addMigrations(MIGRATION_1_4, MIGRATION_2_4, MIGRATION_3_4, MIGRATION_4_5)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        insertMetadata(db, legacyVersion = 0)
                    }
                })
                .build()
                .also { instance = it }
        }

        internal val MIGRATION_1_4 = object : Migration(1, 4) {
            override fun migrate(db: SupportSQLiteDatabase) = migrateRoomLegacy(db, 1)
        }

        internal val MIGRATION_2_4 = object : Migration(2, 4) {
            override fun migrate(db: SupportSQLiteDatabase) = migrateRoomLegacy(db, 2)
        }

        internal val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) = migrateDrift(db)
        }

        internal val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE todos ADD COLUMN priority INTEGER NOT NULL DEFAULT 1")
                db.execSQL("UPDATE app_metadata SET schema_version_value = 5 WHERE id = 1")
            }
        }

        private fun migrateRoomLegacy(db: SupportSQLiteDatabase, version: Int) {
            db.execSQL("ALTER TABLE todos RENAME TO legacy_todos")
            db.execSQL("ALTER TABLE daily_reviews RENAME TO legacy_daily_reviews")
            if (version >= 2) db.execSQL("ALTER TABLE daily_summaries RENAME TO legacy_daily_summaries")
            createV4Schema(db)
            val deviceId = UUID.randomUUID().toString()
            db.execSQL(
                """INSERT INTO todos (id, date, title, note, is_completed, sort_order, created_at, updated_at, deleted_at, device_id, revision)
                    SELECT lower(hex(randomblob(4))) || '-' || lower(hex(randomblob(2))) || '-4' || substr(lower(hex(randomblob(2))),2) || '-' || substr('89ab',abs(random()) % 4 + 1,1) || substr(lower(hex(randomblob(2))),2) || '-' || lower(hex(randomblob(6))),
                    date, title, note, isCompleted, printf('%020d-legacy', createdAt), createdAt, createdAt, NULL, ?, 1
                    FROM legacy_todos""".trimIndent(),
                arrayOf(deviceId)
            )
            db.execSQL(
                """INSERT INTO daily_reviews (id, date, highlight, challenge, learning, tomorrow_focus, mood, created_at, updated_at, deleted_at, device_id, revision)
                    SELECT lower(hex(randomblob(4))) || '-' || lower(hex(randomblob(2))) || '-4' || substr(lower(hex(randomblob(2))),2) || '-' || substr('89ab',abs(random()) % 4 + 1,1) || substr(lower(hex(randomblob(2))),2) || '-' || lower(hex(randomblob(6))),
                    date, highlight, challenge, learning, tomorrowFocus, mood, updatedAt, updatedAt, NULL, ?, 1
                    FROM legacy_daily_reviews""".trimIndent(),
                arrayOf(deviceId)
            )
            if (version >= 2) {
                db.execSQL(
                    """INSERT INTO daily_summaries (id, date, content, provider, model, generated_at, updated_at, deleted_at, device_id, revision)
                        SELECT lower(hex(randomblob(4))) || '-' || lower(hex(randomblob(2))) || '-4' || substr(lower(hex(randomblob(2))),2) || '-' || substr('89ab',abs(random()) % 4 + 1,1) || substr(lower(hex(randomblob(2))),2) || '-' || lower(hex(randomblob(6))),
                        date, content, provider, model, generatedAt, generatedAt, NULL, ?, 1
                        FROM legacy_daily_summaries""".trimIndent(),
                    arrayOf(deviceId)
                )
            }
            db.execSQL("DROP TABLE legacy_todos")
            db.execSQL("DROP TABLE legacy_daily_reviews")
            if (version >= 2) db.execSQL("DROP TABLE legacy_daily_summaries")
            insertMetadata(db, deviceId, version, schemaVersion = 4)
        }

        private fun migrateDrift(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE todos RENAME TO drift_todos")
            db.execSQL("ALTER TABLE daily_reviews RENAME TO drift_daily_reviews")
            db.execSQL("ALTER TABLE daily_summaries RENAME TO drift_daily_summaries")
            db.execSQL("ALTER TABLE app_metadata RENAME TO drift_app_metadata")
            db.execSQL("DROP INDEX IF EXISTS todos_date_idx")
            db.execSQL("DROP INDEX IF EXISTS reviews_date_idx")
            db.execSQL("DROP INDEX IF EXISTS summaries_date_idx")
            createV4Schema(db)
            db.execSQL("""INSERT INTO todos SELECT id, date, title, note, is_completed, sort_order, created_at, updated_at, deleted_at, device_id, revision FROM drift_todos""")
            db.execSQL("""INSERT INTO daily_reviews SELECT id, date, highlight, challenge, learning, tomorrow_focus, mood, created_at, updated_at, deleted_at, device_id, revision FROM drift_daily_reviews""")
            db.execSQL("""INSERT INTO daily_summaries SELECT id, date, content, provider, model, generated_at, updated_at, deleted_at, device_id, revision FROM drift_daily_summaries""")
            db.execSQL(
                """INSERT INTO app_metadata (id, device_id, schema_version_value, legacy_migration_version, last_sync_at, last_sync_status)
                    SELECT id, device_id, 4, legacy_migration_version, NULL, '' FROM drift_app_metadata""".trimIndent()
            )
            db.execSQL("DROP TABLE drift_todos")
            db.execSQL("DROP TABLE drift_daily_reviews")
            db.execSQL("DROP TABLE drift_daily_summaries")
            db.execSQL("DROP TABLE drift_app_metadata")
        }

        private fun createV4Schema(db: SupportSQLiteDatabase) {
            db.execSQL("""CREATE TABLE IF NOT EXISTS todos (id TEXT NOT NULL, date TEXT NOT NULL, title TEXT NOT NULL, note TEXT NOT NULL DEFAULT '', is_completed INTEGER NOT NULL DEFAULT 0, sort_order TEXT NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, deleted_at INTEGER, device_id TEXT NOT NULL, revision INTEGER NOT NULL DEFAULT 1, PRIMARY KEY(id))""")
            db.execSQL("CREATE INDEX IF NOT EXISTS todos_date_idx ON todos(date)")
            db.execSQL("""CREATE TABLE IF NOT EXISTS daily_reviews (id TEXT NOT NULL, date TEXT NOT NULL, highlight TEXT NOT NULL DEFAULT '', challenge TEXT NOT NULL DEFAULT '', learning TEXT NOT NULL DEFAULT '', tomorrow_focus TEXT NOT NULL DEFAULT '', mood INTEGER NOT NULL DEFAULT 0, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, deleted_at INTEGER, device_id TEXT NOT NULL, revision INTEGER NOT NULL DEFAULT 1, PRIMARY KEY(id))""")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS reviews_date_idx ON daily_reviews(date)")
            db.execSQL("""CREATE TABLE IF NOT EXISTS daily_summaries (id TEXT NOT NULL, date TEXT NOT NULL, content TEXT NOT NULL, provider TEXT NOT NULL, model TEXT NOT NULL, generated_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, deleted_at INTEGER, device_id TEXT NOT NULL, revision INTEGER NOT NULL DEFAULT 1, PRIMARY KEY(id))""")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS summaries_date_idx ON daily_summaries(date)")
            db.execSQL("""CREATE TABLE IF NOT EXISTS app_metadata (id INTEGER NOT NULL DEFAULT 1, device_id TEXT NOT NULL, schema_version_value INTEGER NOT NULL, legacy_migration_version INTEGER NOT NULL DEFAULT 0, last_sync_at INTEGER, last_sync_status TEXT NOT NULL DEFAULT '', PRIMARY KEY(id))""")
        }

        private fun insertMetadata(
            db: SupportSQLiteDatabase,
            deviceId: String = UUID.randomUUID().toString(),
            legacyVersion: Int,
            schemaVersion: Int = 5
        ) {
            db.execSQL(
                "INSERT OR IGNORE INTO app_metadata (id, device_id, schema_version_value, legacy_migration_version, last_sync_status) VALUES (1, ?, ?, ?, '')",
                arrayOf<Any>(deviceId, schemaVersion, legacyVersion)
            )
        }
    }
}
