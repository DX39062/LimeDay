package com.limeday.app.llm

import java.util.UUID

enum class LlmProtocol(val displayName: String) {
    OPENAI_CHAT("OpenAI Chat"),
    OPENAI_RESPONSES("OpenAI Responses"),
    ANTHROPIC_MESSAGES("Anthropic Messages"),
    GEMINI_NATIVE("Gemini Native")
}

data class LlmProviderPreset(
    val id: String,
    val displayName: String,
    val protocol: LlmProtocol,
    val baseUrl: String,
    val defaultModel: String,
    val modelsUrl: String = ""
) {
    fun createProvider(now: Long = System.currentTimeMillis()): LlmServiceConfig = LlmServiceConfig(
        name = displayName,
        presetId = id,
        protocol = protocol,
        baseUrl = baseUrl,
        model = defaultModel,
        modelsUrl = modelsUrl,
        createdAt = now,
        updatedAt = now
    )
}

object LlmProviderPresets {
    val all: List<LlmProviderPreset> = listOf(
        LlmProviderPreset("openai", "OpenAI", LlmProtocol.OPENAI_RESPONSES, "https://api.openai.com/v1", "gpt-4.1-mini"),
        LlmProviderPreset("anthropic", "Anthropic", LlmProtocol.ANTHROPIC_MESSAGES, "https://api.anthropic.com/v1", "claude-sonnet-4-5"),
        LlmProviderPreset("gemini", "Google Gemini", LlmProtocol.GEMINI_NATIVE, "https://generativelanguage.googleapis.com/v1beta", "gemini-2.5-flash"),
        LlmProviderPreset("openrouter", "OpenRouter", LlmProtocol.OPENAI_CHAT, "https://openrouter.ai/api/v1", "openai/gpt-4.1-mini"),
        LlmProviderPreset("deepseek", "DeepSeek", LlmProtocol.OPENAI_CHAT, "https://api.deepseek.com/v1", "deepseek-chat"),
        LlmProviderPreset("kimi", "Moonshot Kimi", LlmProtocol.OPENAI_CHAT, "https://api.moonshot.cn/v1", "moonshot-v1-8k"),
        LlmProviderPreset("qwen", "通义千问", LlmProtocol.OPENAI_CHAT, "https://dashscope.aliyuncs.com/compatible-mode/v1", "qwen-plus"),
        LlmProviderPreset("zhipu", "智谱 GLM", LlmProtocol.OPENAI_CHAT, "https://open.bigmodel.cn/api/paas/v4", "glm-4.5-flash"),
        LlmProviderPreset("siliconflow", "SiliconFlow", LlmProtocol.OPENAI_CHAT, "https://api.siliconflow.cn/v1", "Qwen/Qwen3-8B"),
        LlmProviderPreset("minimax", "MiniMax", LlmProtocol.OPENAI_CHAT, "https://api.minimax.chat/v1", "MiniMax-M2.1"),
        LlmProviderPreset("doubao", "豆包", LlmProtocol.OPENAI_CHAT, "https://ark.cn-beijing.volces.com/api/v3", "doubao-seed-1-6-flash-250715"),
        LlmProviderPreset("xai", "xAI", LlmProtocol.OPENAI_CHAT, "https://api.x.ai/v1", "grok-3-mini"),
        LlmProviderPreset("mistral", "Mistral", LlmProtocol.OPENAI_CHAT, "https://api.mistral.ai/v1", "mistral-small-latest"),
        LlmProviderPreset("groq", "Groq", LlmProtocol.OPENAI_CHAT, "https://api.groq.com/openai/v1", "llama-3.3-70b-versatile"),
        LlmProviderPreset("ollama", "Ollama", LlmProtocol.OPENAI_CHAT, "http://127.0.0.1:11434/v1", "qwen3:8b"),
        LlmProviderPreset("custom", "自定义 OpenAI 兼容", LlmProtocol.OPENAI_CHAT, "", "")
    )

    fun find(id: String): LlmProviderPreset? = all.firstOrNull { it.id == id }
}

data class LlmServiceConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val presetId: String = "custom",
    val protocol: LlmProtocol = LlmProtocol.OPENAI_CHAT,
    val baseUrl: String = "",
    val model: String = "",
    val apiKey: String = "",
    val modelsUrl: String = "",
    val allowInsecureHttp: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
) {
    val normalized: LlmServiceConfig
        get() = copy(
            name = name.trim(),
            baseUrl = baseUrl.trim().trimEnd('/'),
            model = model.trim(),
            apiKey = apiKey.trim(),
            modelsUrl = modelsUrl.trim()
        )

    val endpointAllowed: Boolean
        get() = baseUrl.startsWith("https://") || (allowInsecureHttp && baseUrl.startsWith("http://"))

    val isConfigured: Boolean
        get() = name.isNotBlank() && endpointAllowed && model.isNotBlank() &&
            (apiKey.isNotBlank() || presetId == "ollama")
}

data class LlmModelCache(
    val providerId: String,
    val models: List<String>,
    val fetchedAt: Long
) {
    fun isFresh(now: Long = System.currentTimeMillis()): Boolean =
        models.isNotEmpty() && now - fetchedAt in 0 until CACHE_TTL_MS

    companion object {
        const val CACHE_TTL_MS = 24L * 60L * 60L * 1000L
    }
}

data class LlmSettings(
    val providers: List<LlmServiceConfig> = emptyList(),
    val activeProviderId: String? = null,
    val favoritePrompts: List<String> = emptyList(),
    val recentPrompts: List<String> = emptyList(),
    val modelCaches: List<LlmModelCache> = emptyList()
) {
    val activeProvider: LlmServiceConfig?
        get() = providers.firstOrNull { it.id == activeProviderId } ?: providers.firstOrNull()

    val isConfigured: Boolean
        get() = activeProvider?.isConfigured == true

    fun cacheFor(providerId: String): LlmModelCache? = modelCaches.firstOrNull { it.providerId == providerId }

    companion object {
        val builtInPrompts = listOf("总结今日进展", "给出明日建议", "分析未完成事项", "压缩成三句话")
    }
}
