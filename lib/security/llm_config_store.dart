import 'dart:convert';

import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import '../domain/llm_config.dart';
import '../platform/legacy_migration_bridge.dart';

class LlmConfigStore {
  LlmConfigStore({FlutterSecureStorage? storage})
    : _storage =
          storage ??
          const FlutterSecureStorage(
            aOptions: AndroidOptions(
              storageNamespace: 'lime_day_llm_v2',
              migrateWithBackup: true,
            ),
          );

  static const _configKey = 'llm_config';
  static const _migrationKey = 'legacy_migration_complete';
  final FlutterSecureStorage _storage;

  Future<LlmConfig> load() async {
    final encoded = await _storage.read(key: _configKey);
    if (encoded == null || encoded.isEmpty) return const LlmConfig();
    try {
      return LlmConfig.fromJson(
        Map<String, Object?>.from(jsonDecode(encoded) as Map),
      );
    } on Object {
      return const LlmConfig();
    }
  }

  Future<void> save(LlmConfig config) async {
    final normalized = config.copyWith(
      baseUrl: config.baseUrl.trim().replaceFirst(RegExp(r'/+$'), ''),
      model: config.model.trim(),
      apiKey: config.apiKey.trim(),
    );
    await _storage.write(
      key: _configKey,
      value: jsonEncode(normalized.toJson()),
    );
  }

  Future<void> clear() => _storage.delete(key: _configKey);

  Future<void> migrateLegacy(LegacyMigrationBridge bridge) async {
    if (await _storage.read(key: _migrationKey) == 'true') return;
    final current = await _storage.read(key: _configKey);
    if (current == null) {
      final legacy = await bridge.legacyLlmConfig();
      if (legacy != null && (legacy['apiKey'] as String? ?? '').isNotEmpty) {
        await save(LlmConfig.fromJson(legacy));
      }
    }
    await _storage.write(key: _migrationKey, value: 'true');
  }
}
