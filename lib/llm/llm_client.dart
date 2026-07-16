import 'package:dio/dio.dart';

import '../domain/llm_config.dart';

class LlmException implements Exception {
  const LlmException(this.message);
  final String message;

  @override
  String toString() => message;
}

class LlmClient {
  LlmClient({Dio? dio})
    : _dio =
          dio ??
          Dio(
            BaseOptions(
              connectTimeout: const Duration(seconds: 20),
              receiveTimeout: const Duration(seconds: 60),
              sendTimeout: const Duration(seconds: 30),
              responseType: ResponseType.json,
            ),
          );

  final Dio _dio;

  Future<String> summarize(
    LlmConfig config,
    String prompt, {
    CancelToken? cancelToken,
  }) async {
    if (!config.isConfigured) {
      throw const LlmException('请先完成模型配置');
    }

    try {
      final Response<Object?> response = switch (config.provider) {
        LlmProvider.openAiCompatible => await _dio.post<Object?>(
          '${config.baseUrl}/chat/completions',
          cancelToken: cancelToken,
          options: Options(
            headers: {'Authorization': 'Bearer ${config.apiKey}'},
          ),
          data: {
            'model': config.model,
            'temperature': 0.4,
            'messages': [
              {'role': 'system', 'content': _systemPrompt},
              {'role': 'user', 'content': prompt},
            ],
          },
        ),
        LlmProvider.anthropic => await _dio.post<Object?>(
          '${config.baseUrl}/messages',
          cancelToken: cancelToken,
          options: Options(
            headers: {
              'x-api-key': config.apiKey,
              'anthropic-version': '2023-06-01',
            },
          ),
          data: {
            'model': config.model,
            'max_tokens': 900,
            'system': _systemPrompt,
            'messages': [
              {'role': 'user', 'content': prompt},
            ],
          },
        ),
        LlmProvider.gemini => await _dio.post<Object?>(
          '${config.baseUrl}/models/'
          '${Uri.encodeComponent(config.model)}:generateContent',
          cancelToken: cancelToken,
          options: Options(headers: {'x-goog-api-key': config.apiKey}),
          data: {
            'systemInstruction': {
              'parts': [
                {'text': _systemPrompt},
              ],
            },
            'contents': [
              {
                'role': 'user',
                'parts': [
                  {'text': prompt},
                ],
              },
            ],
            'generationConfig': {'temperature': 0.4, 'maxOutputTokens': 900},
          },
        ),
      };
      final content = parseContent(config.provider, response.data).trim();
      if (content.isEmpty) throw const LlmException('模型返回了空内容');
      return content;
    } on DioException catch (error) {
      if (CancelToken.isCancel(error)) {
        throw const LlmException('已取消生成');
      }
      final status = error.response?.statusCode;
      final detail = _safeErrorDetail(error.response?.data, config.apiKey);
      if (status != null) {
        throw LlmException(
          detail.isEmpty
              ? '接口请求失败（HTTP $status）'
              : '接口请求失败（HTTP $status）：$detail',
        );
      }
      throw const LlmException('网络请求失败，请检查网络和接口地址');
    } on LlmException {
      rethrow;
    } on Object {
      throw const LlmException('模型响应格式无法解析');
    }
  }

  static String parseContent(LlmProvider provider, Object? body) {
    final json = Map<String, Object?>.from(body! as Map);
    return switch (provider) {
      LlmProvider.openAiCompatible =>
        (((json['choices'] as List).first as Map)['message'] as Map)['content']
            as String,
      LlmProvider.anthropic =>
        (json['content'] as List)
            .where((item) => (item as Map)['type'] == 'text')
            .map((item) => (item as Map)['text'] as String? ?? '')
            .join('\n'),
      LlmProvider.gemini =>
        ((((json['candidates'] as List).first as Map)['content']
                    as Map)['parts']
                as List)
            .map((item) => (item as Map)['text'] as String? ?? '')
            .join('\n'),
    };
  }

  static String _safeErrorDetail(Object? body, String apiKey) {
    try {
      final json = body is Map ? body : const <String, Object?>{};
      final error = json['error'];
      final message = error is Map ? error['message'] : error;
      final clean = (message?.toString() ?? '').replaceAll(apiKey, '[已隐藏]');
      return clean.length <= 180 ? clean : clean.substring(0, 180);
    } on Object {
      return '';
    }
  }

  static const _systemPrompt =
      '你是一个克制、温和的中文日复盘助手。只依据用户提供的记录总结，不编造事实。'
      '使用简洁纯文本输出四段：完成概览、今日亮点、值得调整、明日建议。不要使用Emoji。';
}
