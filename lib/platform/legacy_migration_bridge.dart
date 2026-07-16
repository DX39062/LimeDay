import 'package:flutter/services.dart';

class LegacyMigrationBridge {
  const LegacyMigrationBridge();

  static const MethodChannel _channel = MethodChannel(
    'com.limeday.app/legacy_migration',
  );

  Future<String> databasePath() async {
    final path = await _channel.invokeMethod<String>('databasePath');
    if (path == null || path.isEmpty) {
      throw StateError('无法获取应用数据库路径');
    }
    return path;
  }

  Future<Map<String, Object?>?> legacyLlmConfig() async {
    final value = await _channel.invokeMapMethod<String, Object?>(
      'legacyLlmConfig',
    );
    return value == null ? null : Map<String, Object?>.from(value);
  }
}
