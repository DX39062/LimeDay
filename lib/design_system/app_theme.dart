import 'package:flutter/material.dart';

abstract final class AppTheme {
  static const Color brandGreen = Color(0xFF176B5B);
  static const Color brandAmber = Color(0xFFB86512);

  static ThemeData light([ColorScheme? dynamicScheme]) {
    final scheme =
        dynamicScheme ??
        ColorScheme.fromSeed(
          seedColor: brandGreen,
          brightness: Brightness.light,
        ).copyWith(
          primary: brandGreen,
          onPrimary: Colors.white,
          secondary: const Color(0xFF596661),
          tertiary: brandAmber,
          surface: const Color(0xFFFCFDFC),
          surfaceContainerLowest: Colors.white,
          surfaceContainerLow: const Color(0xFFF4F7F5),
          surfaceContainer: const Color(0xFFEDF2EF),
          outline: const Color(0xFF74807B),
          outlineVariant: const Color(0xFFC7D0CC),
        );
    return _base(scheme);
  }

  static ThemeData dark([ColorScheme? dynamicScheme]) {
    final scheme =
        dynamicScheme ??
        ColorScheme.fromSeed(
          seedColor: const Color(0xFF70D7C0),
          brightness: Brightness.dark,
        ).copyWith(
          primary: const Color(0xFF70D7C0),
          onPrimary: const Color(0xFF00382E),
          secondary: const Color(0xFFBEC9C4),
          tertiary: const Color(0xFFFFB86E),
          surface: const Color(0xFF111412),
          surfaceContainerLowest: const Color(0xFF0B0E0C),
          surfaceContainerLow: const Color(0xFF181C1A),
          surfaceContainer: const Color(0xFF202522),
          outline: const Color(0xFF89948F),
          outlineVariant: const Color(0xFF3D4742),
        );
    return _base(scheme);
  }

  static ThemeData _base(ColorScheme scheme) {
    final base = ThemeData(
      useMaterial3: true,
      colorScheme: scheme,
      scaffoldBackgroundColor: scheme.surface,
      splashFactory: InkSparkle.splashFactory,
    );
    return base.copyWith(
      textTheme: base.textTheme.copyWith(
        displaySmall: base.textTheme.displaySmall?.copyWith(
          fontWeight: FontWeight.w700,
          height: 1.12,
          letterSpacing: 0,
        ),
        headlineMedium: base.textTheme.headlineMedium?.copyWith(
          fontWeight: FontWeight.w700,
          height: 1.2,
          letterSpacing: 0,
        ),
        headlineSmall: base.textTheme.headlineSmall?.copyWith(
          fontWeight: FontWeight.w700,
          height: 1.2,
          letterSpacing: 0,
        ),
        titleLarge: base.textTheme.titleLarge?.copyWith(
          fontWeight: FontWeight.w700,
          letterSpacing: 0,
        ),
        titleMedium: base.textTheme.titleMedium?.copyWith(
          fontWeight: FontWeight.w600,
          letterSpacing: 0,
        ),
        bodyLarge: base.textTheme.bodyLarge?.copyWith(
          height: 1.5,
          letterSpacing: 0,
        ),
        bodyMedium: base.textTheme.bodyMedium?.copyWith(
          height: 1.45,
          letterSpacing: 0,
        ),
        labelLarge: base.textTheme.labelLarge?.copyWith(letterSpacing: 0),
      ),
      cardTheme: CardThemeData(
        elevation: 0,
        margin: EdgeInsets.zero,
        color: scheme.surfaceContainerLow,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: scheme.surfaceContainerLowest,
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide(color: scheme.outlineVariant),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide(color: scheme.outlineVariant),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide(color: scheme.primary, width: 2),
        ),
        contentPadding: const EdgeInsets.symmetric(
          horizontal: 16,
          vertical: 14,
        ),
      ),
      bottomSheetTheme: BottomSheetThemeData(
        showDragHandle: true,
        backgroundColor: scheme.surface,
        shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(16)),
        ),
      ),
      navigationBarTheme: NavigationBarThemeData(
        height: 72,
        backgroundColor: scheme.surfaceContainerLowest,
        indicatorColor: scheme.primaryContainer,
        labelTextStyle: WidgetStatePropertyAll(
          base.textTheme.labelMedium?.copyWith(
            fontWeight: FontWeight.w600,
            letterSpacing: 0,
          ),
        ),
      ),
      navigationRailTheme: NavigationRailThemeData(
        backgroundColor: scheme.surfaceContainerLowest,
        indicatorColor: scheme.primaryContainer,
        useIndicator: true,
        minWidth: 80,
      ),
      dividerTheme: DividerThemeData(
        color: scheme.outlineVariant.withValues(alpha: 0.7),
        space: 1,
      ),
    );
  }
}
