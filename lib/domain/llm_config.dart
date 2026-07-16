enum LlmProvider {
  openAiCompatible(
    id: 'openai_compatible',
    displayName: 'OpenAI 兼容',
    defaultBaseUrl: 'https://api.openai.com/v1',
    defaultModel: 'gpt-4.1-mini',
  ),
  anthropic(
    id: 'anthropic',
    displayName: 'Anthropic',
    defaultBaseUrl: 'https://api.anthropic.com/v1',
    defaultModel: 'claude-sonnet-4-5',
  ),
  gemini(
    id: 'gemini',
    displayName: 'Google Gemini',
    defaultBaseUrl: 'https://generativelanguage.googleapis.com/v1beta',
    defaultModel: 'gemini-2.5-flash',
  );

  const LlmProvider({
    required this.id,
    required this.displayName,
    required this.defaultBaseUrl,
    required this.defaultModel,
  });

  final String id;
  final String displayName;
  final String defaultBaseUrl;
  final String defaultModel;

  static LlmProvider fromId(String? value) {
    final normalized = value?.toLowerCase();
    return LlmProvider.values.firstWhere(
      (provider) =>
          provider.id == normalized ||
          provider.name.toLowerCase() == normalized ||
          provider.displayName.toLowerCase() == normalized,
      orElse: () => LlmProvider.openAiCompatible,
    );
  }
}

class LlmConfig {
  const LlmConfig({
    this.provider = LlmProvider.openAiCompatible,
    this.baseUrl = 'https://api.openai.com/v1',
    this.model = 'gpt-4.1-mini',
    this.apiKey = '',
  });

  final LlmProvider provider;
  final String baseUrl;
  final String model;
  final String apiKey;

  bool get isConfigured =>
      baseUrl.startsWith('https://') &&
      model.trim().isNotEmpty &&
      apiKey.trim().isNotEmpty;

  LlmConfig copyWith({
    LlmProvider? provider,
    String? baseUrl,
    String? model,
    String? apiKey,
  }) => LlmConfig(
    provider: provider ?? this.provider,
    baseUrl: baseUrl ?? this.baseUrl,
    model: model ?? this.model,
    apiKey: apiKey ?? this.apiKey,
  );

  Map<String, Object?> toJson() => {
    'provider': provider.id,
    'baseUrl': baseUrl,
    'model': model,
    'apiKey': apiKey,
  };

  factory LlmConfig.fromJson(Map<String, Object?> json) {
    final provider = LlmProvider.fromId(json['provider'] as String?);
    return LlmConfig(
      provider: provider,
      baseUrl: (json['baseUrl'] as String?) ?? provider.defaultBaseUrl,
      model: (json['model'] as String?) ?? provider.defaultModel,
      apiKey: (json['apiKey'] as String?) ?? '',
    );
  }
}
