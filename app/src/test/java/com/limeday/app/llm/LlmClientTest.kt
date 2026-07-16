package com.limeday.app.llm

import org.junit.Assert.assertEquals
import org.junit.Test

class LlmClientTest {
    private val client = LlmClient()

    @Test
    fun `parses OpenAI compatible response`() {
        val body = """{"choices":[{"message":{"content":"完成概览：完成两项"}}]}"""

        assertEquals("完成概览：完成两项", client.parseContent(LlmProvider.OPENAI_COMPATIBLE, body))
    }

    @Test
    fun `joins Anthropic text blocks`() {
        val body = """{"content":[{"type":"text","text":"今日亮点"},{"type":"text","text":"明日建议"}]}"""

        assertEquals("今日亮点\n明日建议", client.parseContent(LlmProvider.ANTHROPIC, body))
    }

    @Test
    fun `joins Gemini response parts`() {
        val body = """{"candidates":[{"content":{"parts":[{"text":"值得调整"},{"text":"早点休息"}]}}]}"""

        assertEquals("值得调整\n早点休息", client.parseContent(LlmProvider.GEMINI, body))
    }
}
