package com.limeday.app.llm

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LlmClientTest {
    private val client = LlmClient()

    @Test
    fun `parses OpenAI chat response`() {
        val body = """{"choices":[{"message":{"content":"完成概览：完成两项"}}]}"""

        assertEquals("完成概览：完成两项", client.parseContent(LlmProtocol.OPENAI_CHAT, body))
    }

    @Test
    fun `parses OpenAI responses output blocks`() {
        val body = """{"output":[{"content":[{"type":"output_text","text":"阶段进展"},{"type":"output_text","text":"下一步"}]}]}"""

        assertEquals("阶段进展\n下一步", client.parseContent(LlmProtocol.OPENAI_RESPONSES, body))
    }

    @Test
    fun `joins Anthropic text blocks`() {
        val body = """{"content":[{"type":"text","text":"今日亮点"},{"type":"text","text":"明日建议"}]}"""

        assertEquals("今日亮点\n明日建议", client.parseContent(LlmProtocol.ANTHROPIC_MESSAGES, body))
    }

    @Test
    fun `joins Gemini response parts`() {
        val body = """{"candidates":[{"content":{"parts":[{"text":"值得调整"},{"text":"早点休息"}]}}]}"""

        assertEquals("值得调整\n早点休息", client.parseContent(LlmProtocol.GEMINI_NATIVE, body))
    }

    @Test
    fun `parses OpenAI compatible model list`() {
        val body = """{"data":[{"id":"model-b"},{"id":"model-a"}]}"""

        assertEquals(listOf("model-b", "model-a"), client.parseModels(LlmProtocol.OPENAI_CHAT, body))
    }

    @Test
    fun `parses Gemini model list without resource prefix`() {
        val body = """{"models":[{"name":"models/gemini-2.5-flash"},{"name":"models/gemini-pro"}]}"""

        assertEquals(listOf("gemini-2.5-flash", "gemini-pro"), client.parseModels(LlmProtocol.GEMINI_NATIVE, body))
    }

    @Test
    fun `derives models endpoint from versioned base url`() {
        val config = LlmProviderPresets.find("deepseek")!!.createProvider().copy(apiKey = "secret")

        assertEquals("https://api.deepseek.com/v1/models", client.resolveModelsUrl(config))
    }

    @Test
    fun `uses explicit models endpoint override`() {
        val config = LlmProviderPresets.find("custom")!!.createProvider().copy(
            baseUrl = "https://example.com/v1",
            modelsUrl = "https://models.example.com/catalog",
            model = "custom",
            apiKey = "secret"
        )

        assertEquals("https://models.example.com/catalog", client.resolveModelsUrl(config))
    }

    @Test
    fun `unversioned base url gets compatible model candidates`() {
        val config = LlmServiceConfig(
            name = "Custom",
            baseUrl = "https://api.example.com",
            model = "model",
            apiKey = "secret"
        )

        assertEquals(
            listOf("https://api.example.com/v1/models", "https://api.example.com/models"),
            client.resolveModelsUrls(config)
        )
    }

    @Test
    fun `fetches and sorts OpenAI compatible models with bearer auth`() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"data":[{"id":"z-model"},{"id":"a-model"}]}""").setHeader("Content-Type", "application/json"))
            val config = LlmServiceConfig(
                name = "Local",
                protocol = LlmProtocol.OPENAI_CHAT,
                baseUrl = server.url("/v1").toString().trimEnd('/'),
                model = "a-model",
                apiKey = "secret-key",
                allowInsecureHttp = true
            )

            val models = client.fetchModels(config)
            val request = server.takeRequest()

            assertEquals(listOf("a-model", "z-model"), models)
            assertEquals("/v1/models", request.path)
            assertEquals("Bearer secret-key", request.getHeader("Authorization"))
        }
    }

    @Test
    fun `fetch errors never expose api key`() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":{"message":"bad key secret-key"}}"""))
            val config = LlmServiceConfig(
                name = "Local",
                protocol = LlmProtocol.OPENAI_CHAT,
                baseUrl = server.url("/v1").toString().trimEnd('/'),
                model = "model",
                apiKey = "secret-key",
                allowInsecureHttp = true
            )

            val error = runCatching { client.fetchModels(config) }.exceptionOrNull()

            assertTrue(error is LlmException)
            assertFalse(error?.message.orEmpty().contains("secret-key"))
        }
    }
}
