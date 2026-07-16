import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'core/date_utils.dart';
import 'data/local/app_database.dart';
import 'data/repositories/lime_day_repository.dart';
import 'domain/llm_config.dart';
import 'llm/llm_client.dart';
import 'security/llm_config_store.dart';

final databaseProvider = Provider<AppDatabase>(
  (ref) => throw StateError('databaseProvider must be overridden'),
);
final sharedPreferencesProvider = Provider<SharedPreferences>(
  (ref) => throw StateError('sharedPreferencesProvider must be overridden'),
);
final llmConfigStoreProvider = Provider<LlmConfigStore>(
  (ref) => throw StateError('llmConfigStoreProvider must be overridden'),
);
final repositoryProvider = Provider<LimeDayRepository>(
  (ref) => LimeDayRepository(ref.watch(databaseProvider)),
);
final llmClientProvider = Provider<LlmClient>((ref) => LlmClient());

class SelectedDateController extends Notifier<DateTime> {
  @override
  DateTime build() => dateOnly(DateTime.now());

  void select(DateTime date) => state = dateOnly(date);
  void previous() => state = state.subtract(const Duration(days: 1));
  void next() => state = state.add(const Duration(days: 1));
  void today() => state = dateOnly(DateTime.now());
}

final selectedDateProvider = NotifierProvider<SelectedDateController, DateTime>(
  SelectedDateController.new,
);

final todosProvider = StreamProvider.family<List<TodoItem>, String>((ref, key) {
  return ref.watch(repositoryProvider).watchTodos(dateFromKey(key));
});
final reviewProvider = StreamProvider.family<DailyReview?, String>((ref, key) {
  return ref.watch(repositoryProvider).watchReview(dateFromKey(key));
});
final summaryProvider = StreamProvider.family<DailySummary?, String>((
  ref,
  key,
) {
  return ref.watch(repositoryProvider).watchSummary(dateFromKey(key));
});
final activeDatesProvider = StreamProvider<Set<String>>((ref) {
  return ref.watch(repositoryProvider).watchActiveDates();
});
final llmConfigProvider = FutureProvider<LlmConfig>((ref) {
  return ref.watch(llmConfigStoreProvider).load();
});

class AppPreferences {
  const AppPreferences({
    this.themeMode = ThemeMode.system,
    this.useDynamicColor = false,
  });

  final ThemeMode themeMode;
  final bool useDynamicColor;

  AppPreferences copyWith({ThemeMode? themeMode, bool? useDynamicColor}) =>
      AppPreferences(
        themeMode: themeMode ?? this.themeMode,
        useDynamicColor: useDynamicColor ?? this.useDynamicColor,
      );
}

class AppPreferencesController extends Notifier<AppPreferences> {
  static const _themeKey = 'theme_mode';
  static const _dynamicColorKey = 'dynamic_color';

  SharedPreferences get _preferences => ref.read(sharedPreferencesProvider);

  @override
  AppPreferences build() {
    final theme = switch (_preferences.getString(_themeKey)) {
      'light' => ThemeMode.light,
      'dark' => ThemeMode.dark,
      _ => ThemeMode.system,
    };
    return AppPreferences(
      themeMode: theme,
      useDynamicColor: _preferences.getBool(_dynamicColorKey) ?? false,
    );
  }

  Future<void> setThemeMode(ThemeMode mode) async {
    state = state.copyWith(themeMode: mode);
    await _preferences.setString(_themeKey, mode.name);
  }

  Future<void> setDynamicColor(bool enabled) async {
    state = state.copyWith(useDynamicColor: enabled);
    await _preferences.setBool(_dynamicColorKey, enabled);
  }
}

final appPreferencesProvider =
    NotifierProvider<AppPreferencesController, AppPreferences>(
      AppPreferencesController.new,
    );
