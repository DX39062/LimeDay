import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'app/lime_day_app.dart';
import 'data/local/app_database.dart';
import 'platform/legacy_migration_bridge.dart';
import 'providers.dart';
import 'security/llm_config_store.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);
  SystemChrome.setSystemUIOverlayStyle(
    const SystemUiOverlayStyle(
      statusBarColor: Colors.transparent,
      systemNavigationBarColor: Colors.transparent,
      systemNavigationBarDividerColor: Colors.transparent,
    ),
  );
  runApp(const _BootstrapApp());
}

class _BootstrapData {
  const _BootstrapData({
    required this.database,
    required this.preferences,
    required this.configStore,
  });

  final AppDatabase database;
  final SharedPreferences preferences;
  final LlmConfigStore configStore;
}

class _BootstrapApp extends StatefulWidget {
  const _BootstrapApp();

  @override
  State<_BootstrapApp> createState() => _BootstrapAppState();
}

class _BootstrapAppState extends State<_BootstrapApp> {
  late Future<_BootstrapData> _initialization = _initialize();

  Future<_BootstrapData> _initialize() async {
    const bridge = LegacyMigrationBridge();
    final path = await bridge.databasePath();
    final database = await AppDatabase.open(path);
    try {
      await database.customSelect('SELECT 1').get();
    } on Object {
      await database.close();
      rethrow;
    }
    final preferences = await SharedPreferences.getInstance();
    final configStore = LlmConfigStore();
    await configStore.migrateLegacy(bridge);
    return _BootstrapData(
      database: database,
      preferences: preferences,
      configStore: configStore,
    );
  }

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<_BootstrapData>(
      future: _initialization,
      builder: (context, snapshot) {
        if (snapshot.hasData) {
          final data = snapshot.requireData;
          return ProviderScope(
            overrides: [
              databaseProvider.overrideWithValue(data.database),
              sharedPreferencesProvider.overrideWithValue(data.preferences),
              llmConfigStoreProvider.overrideWithValue(data.configStore),
            ],
            child: const LimeDayApp(),
          );
        }
        if (snapshot.hasError) {
          return MaterialApp(
            debugShowCheckedModeBanner: false,
            theme: ThemeData(useMaterial3: true),
            home: Scaffold(
              body: SafeArea(
                child: Center(
                  child: ConstrainedBox(
                    constraints: const BoxConstraints(maxWidth: 420),
                    child: Padding(
                      padding: const EdgeInsets.all(24),
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          const Icon(Icons.storage_rounded, size: 44),
                          const SizedBox(height: 16),
                          Text(
                            '数据初始化失败',
                            style: Theme.of(context).textTheme.headlineSmall,
                          ),
                          const SizedBox(height: 10),
                          const Text(
                            '旧数据仍保留在设备中。请重试，或重新启动应用。',
                            textAlign: TextAlign.center,
                          ),
                          const SizedBox(height: 20),
                          FilledButton.icon(
                            onPressed: () =>
                                setState(() => _initialization = _initialize()),
                            icon: const Icon(Icons.refresh_rounded),
                            label: const Text('重试'),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              ),
            ),
          );
        }
        return MaterialApp(
          debugShowCheckedModeBanner: false,
          theme: ThemeData(useMaterial3: true),
          home: const Scaffold(
            body: Center(child: CircularProgressIndicator()),
          ),
        );
      },
    );
  }
}
