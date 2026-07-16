package com.limeday.app.llm

enum class LlmProvider(
    val displayName: String,
    val defaultBaseUrl: String,
    val defaultModel: String
) {
    OPENAI_COMPATIBLE("OpenAI 兼容", "https://api.openai.com/v1", "gpt-4.1-mini"),
    ANTHROPIC("Anthropic", "https://api.anthropic.com/v1", "claude-sonnet-4-5"),
    GEMINI("Google Gemini", "https://generativelanguage.googleapis.com/v1beta", "gemini-2.5-flash")
}

data class LlmConfig(
    val provider: LlmProvider = LlmProvider.OPENAI_COMPATIBLE,
    val baseUrl: String = provider.defaultBaseUrl,
    val model: String = provider.defaultModel,
    val apiKey: String = ""
) {
    val isConfigured: Boolean
        get() = baseUrl.startsWith("https://") && model.isNotBlank() && apiKey.isNotBlank()
}

