import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../domain/llm_config.dart';
import '../../llm/llm_client.dart';
import '../../providers.dart';

class SettingsPage extends ConsumerWidget {
  const SettingsPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final preferences = ref.watch(appPreferencesProvider);
    final config = ref.watch(llmConfigProvider);
    return SafeArea(
      bottom: false,
      child: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 760),
          child: ListView(
            padding: const EdgeInsets.fromLTRB(20, 26, 20, 48),
            children: [
              Text('设置', style: Theme.of(context).textTheme.headlineMedium),
              const SizedBox(height: 28),
              const _SettingsHeading(title: '外观', icon: Icons.palette_outlined),
              const SizedBox(height: 12),
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      Text(
                        '主题',
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                      const SizedBox(height: 12),
                      SegmentedButton<ThemeMode>(
                        segments: const [
                          ButtonSegment(
                            value: ThemeMode.system,
                            icon: Icon(Icons.brightness_auto_outlined),
                            label: Text('跟随系统'),
                          ),
                          ButtonSegment(
                            value: ThemeMode.light,
                            icon: Icon(Icons.light_mode_outlined),
                            label: Text('浅色'),
                          ),
                          ButtonSegment(
                            value: ThemeMode.dark,
                            icon: Icon(Icons.dark_mode_outlined),
                            label: Text('深色'),
                          ),
                        ],
                        selected: {preferences.themeMode},
                        onSelectionChanged: (selection) => ref
                            .read(appPreferencesProvider.notifier)
                            .setThemeMode(selection.first),
                      ),
                      const Divider(height: 28),
                      SwitchListTile(
                        contentPadding: EdgeInsets.zero,
                        title: const Text('系统动态配色'),
                        secondary: const Icon(Icons.color_lens_outlined),
                        value: preferences.useDynamicColor,
                        onChanged: (value) => ref
                            .read(appPreferencesProvider.notifier)
                            .setDynamicColor(value),
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 30),
              const _SettingsHeading(
                title: '智能总结',
                icon: Icons.auto_awesome_outlined,
              ),
              const SizedBox(height: 12),
              Card(
                child: config.when(
                  data: (value) => ListTile(
                    contentPadding: const EdgeInsets.symmetric(
                      horizontal: 16,
                      vertical: 8,
                    ),
                    leading: Icon(
                      value.isConfigured
                          ? Icons.verified_user_outlined
                          : Icons.key_outlined,
                      color: value.isConfigured
                          ? Theme.of(context).colorScheme.primary
                          : null,
                    ),
                    title: Text(
                      value.isConfigured ? value.provider.displayName : '尚未配置',
                    ),
                    subtitle: value.isConfigured
                        ? Text(
                            value.model,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          )
                        : null,
                    trailing: const Icon(Icons.chevron_right_rounded),
                    onTap: () => _openConfigEditor(context, ref, value),
                  ),
                  loading: () => const Padding(
                    padding: EdgeInsets.all(28),
                    child: Center(child: CircularProgressIndicator()),
                  ),
                  error: (error, stack) => const ListTile(
                    leading: Icon(Icons.error_outline),
                    title: Text('模型配置读取失败'),
                  ),
                ),
              ),
              const SizedBox(height: 30),
              const _SettingsHeading(
                title: '数据与隐私',
                icon: Icons.shield_outlined,
              ),
              const SizedBox(height: 12),
              Card(
                child: Column(
                  children: const [
                    ListTile(
                      leading: Icon(Icons.phone_android_outlined),
                      title: Text('本地数据'),
                      subtitle: Text('待办、复盘和总结保存在此设备'),
                    ),
                    Divider(indent: 56),
                    ListTile(
                      leading: Icon(Icons.cloud_off_outlined),
                      title: Text('云同步'),
                      subtitle: Text('当前版本未启用'),
                    ),
                    Divider(indent: 56),
                    ListTile(
                      leading: Icon(Icons.info_outline_rounded),
                      title: Text('版本'),
                      subtitle: Text('2.0.0 (3)'),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _openConfigEditor(
    BuildContext context,
    WidgetRef ref,
    LlmConfig config,
  ) async {
    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      builder: (context) => _LlmConfigEditor(initial: config),
    );
    ref.invalidate(llmConfigProvider);
  }
}

class _SettingsHeading extends StatelessWidget {
  const _SettingsHeading({required this.title, required this.icon});
  final String title;
  final IconData icon;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Icon(icon, size: 22, color: Theme.of(context).colorScheme.primary),
        const SizedBox(width: 9),
        Text(title, style: Theme.of(context).textTheme.titleLarge),
      ],
    );
  }
}

class _LlmConfigEditor extends ConsumerStatefulWidget {
  const _LlmConfigEditor({required this.initial});
  final LlmConfig initial;

  @override
  ConsumerState<_LlmConfigEditor> createState() => _LlmConfigEditorState();
}

class _LlmConfigEditorState extends ConsumerState<_LlmConfigEditor> {
  late LlmProvider _provider;
  late final TextEditingController _baseUrl;
  late final TextEditingController _model;
  late final TextEditingController _apiKey;
  bool _obscure = true;
  bool _testing = false;
  String? _message;
  bool _messageIsError = false;

  @override
  void initState() {
    super.initState();
    _provider = widget.initial.provider;
    _baseUrl = TextEditingController(text: widget.initial.baseUrl);
    _model = TextEditingController(text: widget.initial.model);
    _apiKey = TextEditingController(text: widget.initial.apiKey);
  }

  LlmConfig? _currentConfig() {
    final config = LlmConfig(
      provider: _provider,
      baseUrl: _baseUrl.text.trim().replaceFirst(RegExp(r'/+$'), ''),
      model: _model.text.trim(),
      apiKey: _apiKey.text.trim(),
    );
    if (!config.baseUrl.startsWith('https://')) {
      setState(() {
        _message = '接口地址必须使用 HTTPS';
        _messageIsError = true;
      });
      return null;
    }
    if (config.model.isEmpty || config.apiKey.isEmpty) {
      setState(() {
        _message = '请填写模型名称和 API Key';
        _messageIsError = true;
      });
      return null;
    }
    return config;
  }

  Future<void> _save() async {
    final config = _currentConfig();
    if (config == null) return;
    await ref.read(llmConfigStoreProvider).save(config);
    if (mounted) Navigator.pop(context);
  }

  Future<void> _testConnection() async {
    final config = _currentConfig();
    if (config == null) return;
    setState(() {
      _testing = true;
      _message = null;
    });
    try {
      await ref.read(llmClientProvider).summarize(config, '这是连接测试。请只回复“连接成功”。');
      if (mounted) {
        setState(() {
          _message = '连接成功';
          _messageIsError = false;
        });
      }
    } on LlmException catch (error) {
      if (mounted) {
        setState(() {
          _message = error.message;
          _messageIsError = true;
        });
      }
    } finally {
      if (mounted) setState(() => _testing = false);
    }
  }

  Future<void> _clear() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('清除模型配置？'),
        content: const Text('已保存的 API Key 将从安全存储中删除。'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('取消'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('清除'),
          ),
        ],
      ),
    );
    if (confirmed != true) return;
    await ref.read(llmConfigStoreProvider).clear();
    if (mounted) Navigator.pop(context);
  }

  @override
  void dispose() {
    _baseUrl.dispose();
    _model.dispose();
    _apiKey.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      padding: EdgeInsets.fromLTRB(
        20,
        4,
        20,
        MediaQuery.viewInsetsOf(context).bottom + 24,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text('模型配置', style: Theme.of(context).textTheme.headlineSmall),
          const SizedBox(height: 18),
          DropdownButtonFormField<LlmProvider>(
            initialValue: _provider,
            decoration: const InputDecoration(labelText: '服务商'),
            items: [
              for (final provider in LlmProvider.values)
                DropdownMenuItem(
                  value: provider,
                  child: Text(provider.displayName),
                ),
            ],
            onChanged: (provider) {
              if (provider == null) return;
              setState(() {
                _provider = provider;
                _baseUrl.text = provider.defaultBaseUrl;
                _model.text = provider.defaultModel;
              });
            },
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _baseUrl,
            keyboardType: TextInputType.url,
            decoration: const InputDecoration(labelText: 'Base URL'),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _model,
            decoration: const InputDecoration(labelText: '模型'),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _apiKey,
            obscureText: _obscure,
            enableSuggestions: false,
            autocorrect: false,
            decoration: InputDecoration(
              labelText: 'API Key',
              suffixIcon: IconButton(
                tooltip: _obscure ? '显示 API Key' : '隐藏 API Key',
                onPressed: () => setState(() => _obscure = !_obscure),
                icon: Icon(
                  _obscure
                      ? Icons.visibility_outlined
                      : Icons.visibility_off_outlined,
                ),
              ),
            ),
          ),
          if (_message != null) ...[
            const SizedBox(height: 12),
            Text(
              _message!,
              style: TextStyle(
                color: _messageIsError
                    ? Theme.of(context).colorScheme.error
                    : Theme.of(context).colorScheme.primary,
              ),
            ),
          ],
          const SizedBox(height: 18),
          Row(
            children: [
              OutlinedButton.icon(
                onPressed: _testing ? null : _testConnection,
                icon: _testing
                    ? const SizedBox.square(
                        dimension: 16,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Icon(Icons.network_check_rounded),
                label: const Text('测试连接'),
              ),
              const Spacer(),
              if (widget.initial.isConfigured)
                IconButton(
                  tooltip: '清除配置',
                  onPressed: _clear,
                  icon: const Icon(Icons.delete_outline_rounded),
                ),
              const SizedBox(width: 8),
              FilledButton(onPressed: _save, child: const Text('保存')),
            ],
          ),
        ],
      ),
    );
  }
}
