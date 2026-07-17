package com.limeday.app.llm

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertEquals
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

    @Test
    fun `same vendor can be saved as multiple independent services`() {
        val preset = LlmProviderPresets.find("deepseek")!!
        val personal = preset.createProvider().copy(name = "DeepSeek 个人")
        val work = preset.createProvider().copy(name = "DeepSeek 工作")
        val settings = LlmSettings(providers = listOf(personal, work), activeProviderId = work.id)

        assertNotEquals(personal.id, work.id)
        assertEquals(2, settings.providers.size)
        assertEquals("DeepSeek 工作", settings.activeProvider?.name)
    }
}
