import 'package:drift/drift.dart';
import 'package:uuid/uuid.dart';

import '../../core/date_utils.dart';
import '../local/app_database.dart';

class LimeDayRepository {
  LimeDayRepository(this._database);

  final AppDatabase _database;
  static const _uuid = Uuid();

  Future<String> get _deviceId async =>
      (await _database.select(_database.appMetadata).getSingle()).deviceId;

  Stream<List<TodoItem>> watchTodos(DateTime date) {
    final query = _database.select(_database.todos)
      ..where(
        (table) => table.date.equals(dateKey(date)) & table.deletedAt.isNull(),
      )
      ..orderBy([
        (table) => OrderingTerm(expression: table.isCompleted),
        (table) => OrderingTerm(expression: table.sortOrder),
        (table) => OrderingTerm(expression: table.createdAt),
      ]);
    return query.watch();
  }

  Stream<DailyReview?> watchReview(DateTime date) {
    final query = _database.select(_database.dailyReviews)
      ..where(
        (table) => table.date.equals(dateKey(date)) & table.deletedAt.isNull(),
      );
    return query.watchSingleOrNull();
  }

  Stream<DailySummary?> watchSummary(DateTime date) {
    final query = _database.select(_database.dailySummaries)
      ..where(
        (table) => table.date.equals(dateKey(date)) & table.deletedAt.isNull(),
      );
    return query.watchSingleOrNull();
  }

  Stream<Set<String>> watchActiveDates() {
    return _database
        .customSelect(
          '''
      SELECT date FROM todos WHERE deleted_at IS NULL
      UNION SELECT date FROM daily_reviews WHERE deleted_at IS NULL
      UNION SELECT date FROM daily_summaries WHERE deleted_at IS NULL
      ''',
          readsFrom: {
            _database.todos,
            _database.dailyReviews,
            _database.dailySummaries,
          },
        )
        .watch()
        .map((rows) => rows.map((row) => row.read<String>('date')).toSet());
  }

  Future<void> addTodo(DateTime date, String title, {String note = ''}) async {
    final cleanTitle = _limit(title.trim(), 80);
    if (cleanTitle.isEmpty) return;
    final now = DateTime.now().toUtc().millisecondsSinceEpoch;
    await _database
        .into(_database.todos)
        .insert(
          TodosCompanion.insert(
            id: _uuid.v4(),
            date: dateKey(date),
            title: cleanTitle,
            note: Value(_limit(note.trim(), 300)),
            sortOrder: '${now.toString().padLeft(20, '0')}-${_uuid.v4()}',
            createdAt: now,
            updatedAt: now,
            deviceId: await _deviceId,
          ),
        );
  }

  Future<void> updateTodo(TodoItem todo, String title, String note) async {
    final cleanTitle = _limit(title.trim(), 80);
    if (cleanTitle.isEmpty) return;
    await (_database.update(
      _database.todos,
    )..where((table) => table.id.equals(todo.id))).write(
      TodosCompanion(
        title: Value(cleanTitle),
        note: Value(_limit(note.trim(), 300)),
        updatedAt: Value(DateTime.now().toUtc().millisecondsSinceEpoch),
        deviceId: Value(await _deviceId),
        revision: Value(todo.revision + 1),
      ),
    );
  }

  Future<void> toggleTodo(TodoItem todo) async {
    await (_database.update(
      _database.todos,
    )..where((table) => table.id.equals(todo.id))).write(
      TodosCompanion(
        isCompleted: Value(!todo.isCompleted),
        updatedAt: Value(DateTime.now().toUtc().millisecondsSinceEpoch),
        deviceId: Value(await _deviceId),
        revision: Value(todo.revision + 1),
      ),
    );
  }

  Future<void> deleteTodo(TodoItem todo) async {
    final now = DateTime.now().toUtc().millisecondsSinceEpoch;
    await (_database.update(
      _database.todos,
    )..where((table) => table.id.equals(todo.id))).write(
      TodosCompanion(
        deletedAt: Value(now),
        updatedAt: Value(now),
        deviceId: Value(await _deviceId),
        revision: Value(todo.revision + 1),
      ),
    );
  }

  Future<void> saveReview({
    required DateTime date,
    required String highlight,
    required String challenge,
    required String learning,
    required String tomorrowFocus,
    required int mood,
  }) async {
    final key = dateKey(date);
    final existing = await (_database.select(
      _database.dailyReviews,
    )..where((table) => table.date.equals(key))).getSingleOrNull();
    final now = DateTime.now().toUtc().millisecondsSinceEpoch;
    final deviceId = await _deviceId;
    final values = DailyReviewsCompanion(
      highlight: Value(_limit(highlight, 1000)),
      challenge: Value(_limit(challenge, 1000)),
      learning: Value(_limit(learning, 1000)),
      tomorrowFocus: Value(_limit(tomorrowFocus, 1000)),
      mood: Value(mood < 0 ? 0 : (mood > 5 ? 5 : mood)),
      updatedAt: Value(now),
      deletedAt: const Value(null),
      deviceId: Value(deviceId),
      revision: Value((existing?.revision ?? 0) + 1),
    );
    if (existing == null) {
      await _database
          .into(_database.dailyReviews)
          .insert(
            values.copyWith(
              id: Value(_uuid.v4()),
              date: Value(key),
              createdAt: Value(now),
            ),
          );
    } else {
      await (_database.update(
        _database.dailyReviews,
      )..where((table) => table.id.equals(existing.id))).write(values);
    }
  }

  Future<void> saveSummary({
    required DateTime date,
    required String content,
    required String provider,
    required String model,
  }) async {
    final key = dateKey(date);
    final existing = await (_database.select(
      _database.dailySummaries,
    )..where((table) => table.date.equals(key))).getSingleOrNull();
    final now = DateTime.now().toUtc().millisecondsSinceEpoch;
    final deviceId = await _deviceId;
    final values = DailySummariesCompanion(
      content: Value(content),
      provider: Value(provider),
      model: Value(model),
      generatedAt: Value(now),
      updatedAt: Value(now),
      deletedAt: const Value(null),
      deviceId: Value(deviceId),
      revision: Value((existing?.revision ?? 0) + 1),
    );
    if (existing == null) {
      await _database
          .into(_database.dailySummaries)
          .insert(values.copyWith(id: Value(_uuid.v4()), date: Value(key)));
    } else {
      await (_database.update(
        _database.dailySummaries,
      )..where((table) => table.id.equals(existing.id))).write(values);
    }
  }

  static String _limit(String value, int maxLength) =>
      value.length <= maxLength ? value : value.substring(0, maxLength);
}
