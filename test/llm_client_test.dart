import 'package:flutter_test/flutter_test.dart';

import 'package:lime_day/domain/llm_config.dart';
import 'package:lime_day/llm/llm_client.dart';

void main() {
  test('parses OpenAI compatible content', () {
    final value = LlmClient.parseContent(LlmProvider.openAiCompatible, {
      'choices': [
        {
          'message': {'content': '完成概览'},
        },
      ],
    });
    expect(value, '完成概览');
  });

  test('joins Anthropic text blocks', () {
    final value = LlmClient.parseContent(LlmProvider.anthropic, {
      'content': [
        {'type': 'text', 'text': '第一段'},
        {'type': 'tool_use', 'name': 'ignored'},
        {'type': 'text', 'text': '第二段'},
      ],
    });
    expect(value, '第一段\n第二段');
  });

  test('joins Gemini parts', () {
    final value = LlmClient.parseContent(LlmProvider.gemini, {
      'candidates': [
        {
          'content': {
            'parts': [
              {'text': '今日亮点'},
              {'text': '明日建议'},
            ],
          },
        },
      ],
    });
    expect(value, '今日亮点\n明日建议');
  });
}
