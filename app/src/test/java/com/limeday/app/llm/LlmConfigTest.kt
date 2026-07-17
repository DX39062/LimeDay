package com.limeday.app.llm

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LlmConfigTest {
    @Test
    fun `preset catalog covers recommended providers and protocols`() {
        val ids = LlmProviderPresets.all.map(LlmProviderPreset::id).toSet()

        assertTrue(ids.containsAll(setOf("openai", "anthropic", "gemini", "openrouter", "deepseek", "kimi", "qwen", "zhipu", "siliconflow", "minimax", "doubao", "xai", "mistral", "groq", "ollama", "custom")))
        assertTrue(LlmProviderPresets.all.map(LlmProviderPreset::protocol).toSet().containsAll(LlmProtocol.entries))
    }

    @Test
    fun `plain http requires explicit opt in`() {
        val provider = LlmProviderPresets.find("ollama")!!.createProvider()

        assertFalse(provider.isConfigured)
        assertTrue(provider.copy(allowInsecureHttp = true).isConfigured)
    }

    @Test
    fun `model cache expires after twenty four hours`() {
        val now = 1_000_000_000L
        val cache = LlmModelCache("provider", listOf("model"), now)

        assertTrue(cache.isFresh(now + LlmModelCache.CACHE_TTL_MS - 1))
        assertFalse(cache.isFresh(now + LlmModelCache.CACHE_TTL_MS))
    }
}
