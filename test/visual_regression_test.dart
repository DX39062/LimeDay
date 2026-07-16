import 'package:drift/native.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:lime_day/app/lime_day_app.dart';
import 'package:lime_day/data/local/app_database.dart';
import 'package:lime_day/data/repositories/lime_day_repository.dart';
import 'package:lime_day/providers.dart';
import 'package:lime_day/security/llm_config_store.dart';

void main() {
  Future<ProviderContainer> createContainer() async {
    SharedPreferences.setMockInitialValues({});
    final preferences = await SharedPreferences.getInstance();
    final database = AppDatabase(NativeDatabase.memory());
    await database.customSelect('SELECT 1').get();
    final repository = LimeDayRepository(database);
    final date = DateTime(2026, 7, 16);
    await repository.addTodo(date, '完成 Flutter 数据层', note: '迁移旧版 Room 数据');
    await repository.addTodo(date, '检查 Android 自适应布局');
    final first = (await database.select(database.todos).get()).first;
    await repository.toggleTodo(first);
    await repository.saveReview(
      date: date,
      highlight: '核心功能已经连通',
      challenge: '首次构建依赖下载较慢',
      learning: '先固定数据协议，再扩展多端',
      tomorrowFocus: '完成 APK 验收',
      mood: 4,
    );
    await repository.saveSummary(
      date: date,
      content: '完成概览\n今日完成了一项关键工作。\n\n明日建议\n继续完成真机验证。',
      provider: 'openai_compatible',
      model: 'gpt-4.1-mini',
    );
    final container = ProviderContainer(
      overrides: [
        databaseProvider.overrideWithValue(database),
        sharedPreferencesProvider.overrideWithValue(preferences),
        llmConfigStoreProvider.overrideWithValue(LlmConfigStore()),
      ],
    );
    container.read(selectedDateProvider.notifier).select(date);
    addTearDown(() async {
      container.dispose();
      await database.close();
    });
    return container;
  }

  Future<void> renderAtSize(
    WidgetTester tester,
    ProviderContainer container,
    Size size,
  ) async {
    tester.view.devicePixelRatio = 1;
    tester.view.physicalSize = size;
    addTearDown(tester.view.resetDevicePixelRatio);
    addTearDown(tester.view.resetPhysicalSize);
    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: container,
        child: const LimeDayApp(),
      ),
    );
    await tester.pump(const Duration(milliseconds: 800));
  }

  testWidgets('phone day layout', (tester) async {
    final container = await createContainer();
    await renderAtSize(tester, container, const Size(412, 915));
    expect(
      find.byType(MaterialApp),
      matchesGoldenFile('goldens/day_phone.png'),
    );
    await tester.pumpWidget(const SizedBox.shrink());
    await tester.pump(const Duration(milliseconds: 1));
  });

  testWidgets('tablet day layout', (tester) async {
    final container = await createContainer();
    await renderAtSize(tester, container, const Size(1000, 800));
    expect(
      find.byType(MaterialApp),
      matchesGoldenFile('goldens/day_tablet.png'),
    );
    await tester.pumpWidget(const SizedBox.shrink());
    await tester.pump(const Duration(milliseconds: 1));
  });
}
