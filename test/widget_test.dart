import 'package:drift/native.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:lime_day/app/lime_day_app.dart';
import 'package:lime_day/data/local/app_database.dart';
import 'package:lime_day/providers.dart';
import 'package:lime_day/security/llm_config_store.dart';

void main() {
  testWidgets('renders the day experience and primary navigation', (
    tester,
  ) async {
    SharedPreferences.setMockInitialValues({});
    final preferences = await SharedPreferences.getInstance();
    final database = AppDatabase(NativeDatabase.memory());
    await database.customSelect('SELECT 1').get();
    addTearDown(database.close);

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          databaseProvider.overrideWithValue(database),
          sharedPreferencesProvider.overrideWithValue(preferences),
          llmConfigStoreProvider.overrideWithValue(LlmConfigStore()),
        ],
        child: const LimeDayApp(),
      ),
    );
    await tester.pump(const Duration(milliseconds: 600));

    expect(find.text('青柠日记'), findsOneWidget);
    expect(find.text('每日待办'), findsOneWidget);
    expect(find.text('今日'), findsOneWidget);
    expect(find.text('日历'), findsOneWidget);
    expect(find.text('设置'), findsOneWidget);

    await tester.tap(find.widgetWithText(NavigationDestination, '日历'));
    await tester.pump(const Duration(milliseconds: 300));
    expect(find.text('选择一天'), findsOneWidget);

    await tester.pumpWidget(const SizedBox.shrink());
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 1));
  });
}
