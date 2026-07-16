import 'dart:io';

import 'package:drift/drift.dart';
import 'package:drift/native.dart';
import 'package:sqlite3/sqlite3.dart' as sqlite;
import 'package:uuid/uuid.dart';

part 'app_database.g.dart';

@DataClassName('TodoItem')
class Todos extends Table {
  TextColumn get id => text()();
  TextColumn get date => text()();
  TextColumn get title => text().withLength(min: 1, max: 80)();
  TextColumn get note =>
      text().withLength(max: 300).withDefault(const Constant(''))();
  BoolColumn get isCompleted => boolean().withDefault(const Constant(false))();
  TextColumn get sortOrder => text()();
  IntColumn get createdAt => integer()();
  IntColumn get updatedAt => integer()();
  IntColumn get deletedAt => integer().nullable()();
  TextColumn get deviceId => text()();
  IntColumn get revision => integer().withDefault(const Constant(1))();

  @override
  Set<Column<Object>> get primaryKey => {id};
}

class DailyReviews extends Table {
  TextColumn get id => text()();
  TextColumn get date => text().unique()();
  TextColumn get highlight =>
      text().withLength(max: 1000).withDefault(const Constant(''))();
  TextColumn get challenge =>
      text().withLength(max: 1000).withDefault(const Constant(''))();
  TextColumn get learning =>
      text().withLength(max: 1000).withDefault(const Constant(''))();
  TextColumn get tomorrowFocus =>
      text().withLength(max: 1000).withDefault(const Constant(''))();
  IntColumn get mood => integer().withDefault(const Constant(0))();
  IntColumn get createdAt => integer()();
  IntColumn get updatedAt => integer()();
  IntColumn get deletedAt => integer().nullable()();
  TextColumn get deviceId => text()();
  IntColumn get revision => integer().withDefault(const Constant(1))();

  @override
  Set<Column<Object>> get primaryKey => {id};
}

class DailySummaries extends Table {
  TextColumn get id => text()();
  TextColumn get date => text().unique()();
  TextColumn get content => text()();
  TextColumn get provider => text()();
  TextColumn get model => text()();
  IntColumn get generatedAt => integer()();
  IntColumn get updatedAt => integer()();
  IntColumn get deletedAt => integer().nullable()();
  TextColumn get deviceId => text()();
  IntColumn get revision => integer().withDefault(const Constant(1))();

  @override
  Set<Column<Object>> get primaryKey => {id};
}

class AppMetadata extends Table {
  IntColumn get id => integer().withDefault(const Constant(1))();
  TextColumn get deviceId => text()();
  IntColumn get schemaVersionValue => integer()();
  IntColumn get legacyMigrationVersion =>
      integer().withDefault(const Constant(0))();

  @override
  Set<Column<Object>> get primaryKey => {id};
}

@DriftDatabase(tables: [Todos, DailyReviews, DailySummaries, AppMetadata])
class AppDatabase extends _$AppDatabase {
  AppDatabase(super.executor);

  static const _uuid = Uuid();

  static Future<AppDatabase> open(String path) async {
    final file = File(path);
    if (await file.exists()) {
      final database = sqlite.sqlite3.open(path);
      final version = database.userVersion;
      if (version == 1 || version == 2) {
        database.execute('PRAGMA wal_checkpoint(FULL)');
        database.close();
        final backup = File('$path.room-v$version.backup');
        if (!await backup.exists()) {
          await file.copy(backup.path);
        }
      } else {
        database.close();
      }
    } else {
      await file.parent.create(recursive: true);
    }
    return AppDatabase(NativeDatabase.createInBackground(file));
  }

  @override
  int get schemaVersion => 3;

  @override
  MigrationStrategy get migration => MigrationStrategy(
    onCreate: (migrator) async {
      await migrator.createAll();
      await _createIndexes();
      await _ensureMetadata();
    },
    onUpgrade: (migrator, from, to) async {
      if (from < 3) {
        await _migrateLegacyRoom(migrator, from);
      }
    },
    beforeOpen: (details) async {
      await customStatement('PRAGMA foreign_keys = ON');
      await _createIndexes();
      await _ensureMetadata();
    },
  );

  Future<void> _createIndexes() async {
    await customStatement(
      'CREATE INDEX IF NOT EXISTS todos_date_idx ON todos(date)',
    );
    await customStatement(
      'CREATE INDEX IF NOT EXISTS reviews_date_idx ON daily_reviews(date)',
    );
    await customStatement(
      'CREATE INDEX IF NOT EXISTS summaries_date_idx '
      'ON daily_summaries(date)',
    );
  }

  Future<void> _ensureMetadata() async {
    final current = await select(appMetadata).getSingleOrNull();
    if (current == null) {
      await into(appMetadata).insert(
        AppMetadataCompanion.insert(
          deviceId: _uuid.v4(),
          schemaVersionValue: schemaVersion,
          legacyMigrationVersion: const Value(0),
        ),
      );
    }
  }

  Future<bool> _tableExists(String table) async {
    final row = await customSelect(
      "SELECT name FROM sqlite_master WHERE type='table' AND name = ? LIMIT 1",
      variables: [Variable.withString(table)],
    ).getSingleOrNull();
    return row != null;
  }

  Future<void> _migrateLegacyRoom(Migrator migrator, int from) async {
    final hasTodos = await _tableExists('todos');
    final hasReviews = await _tableExists('daily_reviews');
    final hasSummaries = await _tableExists('daily_summaries');

    if (hasTodos) {
      await customStatement('ALTER TABLE todos RENAME TO legacy_todos');
    }
    if (hasReviews) {
      await customStatement(
        'ALTER TABLE daily_reviews RENAME TO legacy_daily_reviews',
      );
    }
    if (hasSummaries) {
      await customStatement(
        'ALTER TABLE daily_summaries RENAME TO legacy_daily_summaries',
      );
    }

    await migrator.createAll();
    final deviceId = _uuid.v4();

    if (hasTodos) {
      final rows = await customSelect('SELECT * FROM legacy_todos').get();
      for (final row in rows) {
        final createdAt = row.read<int>('createdAt');
        await into(todos).insert(
          TodosCompanion.insert(
            id: _uuid.v4(),
            date: row.read<String>('date'),
            title: row.read<String>('title'),
            note: Value(row.read<String>('note')),
            isCompleted: Value(row.read<int>('isCompleted') != 0),
            sortOrder: '${createdAt.toString().padLeft(20, '0')}-legacy',
            createdAt: createdAt,
            updatedAt: createdAt,
            deviceId: deviceId,
          ),
        );
      }
      await customStatement('DROP TABLE legacy_todos');
    }

    if (hasReviews) {
      final rows = await customSelect(
        'SELECT * FROM legacy_daily_reviews',
      ).get();
      for (final row in rows) {
        final updatedAt = row.read<int>('updatedAt');
        await into(dailyReviews).insert(
          DailyReviewsCompanion.insert(
            id: _uuid.v4(),
            date: row.read<String>('date'),
            highlight: Value(row.read<String>('highlight')),
            challenge: Value(row.read<String>('challenge')),
            learning: Value(row.read<String>('learning')),
            tomorrowFocus: Value(row.read<String>('tomorrowFocus')),
            mood: Value(row.read<int>('mood')),
            createdAt: updatedAt,
            updatedAt: updatedAt,
            deviceId: deviceId,
          ),
        );
      }
      await customStatement('DROP TABLE legacy_daily_reviews');
    }

    if (hasSummaries) {
      final rows = await customSelect(
        'SELECT * FROM legacy_daily_summaries',
      ).get();
      for (final row in rows) {
        final generatedAt = row.read<int>('generatedAt');
        await into(dailySummaries).insert(
          DailySummariesCompanion.insert(
            id: _uuid.v4(),
            date: row.read<String>('date'),
            content: row.read<String>('content'),
            provider: row.read<String>('provider'),
            model: row.read<String>('model'),
            generatedAt: generatedAt,
            updatedAt: generatedAt,
            deviceId: deviceId,
          ),
        );
      }
      await customStatement('DROP TABLE legacy_daily_summaries');
    }

    await into(appMetadata).insert(
      AppMetadataCompanion.insert(
        deviceId: deviceId,
        schemaVersionValue: schemaVersion,
        legacyMigrationVersion: Value(from),
      ),
    );
    await _createIndexes();
  }
}
