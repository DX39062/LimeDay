import 'package:dynamic_color/dynamic_color.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../design_system/app_theme.dart';
import '../features/home/home_shell.dart';
import '../providers.dart';

class LimeDayApp extends ConsumerWidget {
  const LimeDayApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final preferences = ref.watch(appPreferencesProvider);
    return DynamicColorBuilder(
      builder: (lightDynamic, darkDynamic) {
        final useDynamic = preferences.useDynamicColor;
        return MaterialApp(
          title: '青柠日记',
          debugShowCheckedModeBanner: false,
          theme: AppTheme.light(useDynamic ? lightDynamic : null),
          darkTheme: AppTheme.dark(useDynamic ? darkDynamic : null),
          themeMode: preferences.themeMode,
          home: const HomeShell(),
        );
      },
    );
  }
}
