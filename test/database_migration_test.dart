import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:sqlite3/sqlite3.dart' as sqlite;

import 'package:lime_day/data/local/app_database.dart';

void main() {
  for (final version in [1, 2]) {
    test('migrates Room v$version without losing records', () async {
      final directory = await Directory.systemTemp.createTemp(
        'limeday_v$version',
      );
      addTearDown(() => directory.delete(recursive: true));
      final path = '${directory.path}/lime_day.db';
      _createLegacyDatabase(path, version);

      final database = await AppDatabase.open(path);
      addTearDown(database.close);
      await database.customSelect('SELECT 1').get();

      final todos = await database.select(database.todos).get();
      final reviews = await database.select(database.dailyReviews).get();
      final summaries = await database.select(database.dailySummaries).get();
      final metadata = await database.select(database.appMetadata).getSingle();

      expect(todos, hasLength(1));
      expect(todos.single.id, isNot('1'));
      expect(todos.single.title, '迁移旧待办');
      expect(todos.single.isCompleted, isTrue);
      expect(reviews, hasLength(1));
      expect(reviews.single.highlight, '旧版亮点');
      expect(summaries, hasLength(version == 2 ? 1 : 0));
      expect(metadata.legacyMigrationVersion, version);
      expect(File('$path.room-v$version.backup').existsSync(), isTrue);
    });
  }
}

void _createLegacyDatabase(String path, int version) {
  final database = sqlite.sqlite3.open(path);
  database.execute('''
    CREATE TABLE todos (
      id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
      date TEXT NOT NULL,
      title TEXT NOT NULL,
      note TEXT NOT NULL,
      isCompleted INTEGER NOT NULL,
      createdAt INTEGER NOT NULL
    )
  ''');
  database.execute('''
    CREATE TABLE daily_reviews (
      date TEXT NOT NULL PRIMARY KEY,
      highlight TEXT NOT NULL,
      challenge TEXT NOT NULL,
      learning TEXT NOT NULL,
      tomorrowFocus TEXT NOT NULL,
      mood INTEGER NOT NULL,
      updatedAt INTEGER NOT NULL
    )
  ''');
  if (version == 2) {
    database.execute('''
      CREATE TABLE daily_summaries (
        date TEXT NOT NULL PRIMARY KEY,
        content TEXT NOT NULL,
        provider TEXT NOT NULL,
        model TEXT NOT NULL,
        generatedAt INTEGER NOT NULL
      )
    ''');
  }
  database.execute(
    "INSERT INTO todos VALUES (1, '2026-07-16', '迁移旧待办', '旧备注', 1, 1000)",
  );
  database.execute(
    "INSERT INTO daily_reviews VALUES "
    "('2026-07-16', '旧版亮点', '', '旧版收获', '明日重点', 4, 2000)",
  );
  if (version == 2) {
    database.execute(
      "INSERT INTO daily_summaries VALUES "
      "('2026-07-16', '旧版总结', 'OpenAI 兼容', 'test-model', 3000)",
    );
  }
  database.userVersion = version;
  database.close();
}
