import 'package:drift/native.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:lime_day/data/local/app_database.dart';
import 'package:lime_day/data/repositories/lime_day_repository.dart';

void main() {
  late AppDatabase database;
  late LimeDayRepository repository;

  setUp(() async {
    database = AppDatabase(NativeDatabase.memory());
    await database.customSelect('SELECT 1').get();
    repository = LimeDayRepository(database);
  });

  tearDown(() => database.close());

  test(
    'todo lifecycle is date-scoped and deletion keeps a tombstone',
    () async {
      final date = DateTime(2026, 7, 16);
      await repository.addTodo(date, '完成 Flutter 重构', note: '生成 APK');
      var todos = await repository.watchTodos(date).first;
      expect(todos, hasLength(1));
      expect(todos.single.id, isNotEmpty);
      expect(todos.single.revision, 1);

      await repository.toggleTodo(todos.single);
      todos = await repository.watchTodos(date).first;
      expect(todos.single.isCompleted, isTrue);
      expect(todos.single.revision, 2);

      await repository.deleteTodo(todos.single);
      expect(await repository.watchTodos(date).first, isEmpty);
      final tombstones = await database.select(database.todos).get();
      expect(tombstones.single.deletedAt, isNotNull);
      expect(await repository.watchTodos(DateTime(2026, 7, 17)).first, isEmpty);
    },
  );

  test('review upserts one active record per date', () async {
    final date = DateTime(2026, 7, 16);
    await repository.saveReview(
      date: date,
      highlight: '完成数据层',
      challenge: '',
      learning: '',
      tomorrowFocus: '完善界面',
      mood: 4,
    );
    await repository.saveReview(
      date: date,
      highlight: '完成数据层和测试',
      challenge: '',
      learning: '迁移可验证',
      tomorrowFocus: '构建 APK',
      mood: 5,
    );

    final rows = await database.select(database.dailyReviews).get();
    expect(rows, hasLength(1));
    expect(rows.single.highlight, '完成数据层和测试');
    expect(rows.single.revision, 2);
  });
}
