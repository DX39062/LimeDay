package com.limeday.app.llm

import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class LlmException(message: String) : Exception(message)

class LlmClient(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    suspend fun summarize(config: LlmConfig, prompt: String): String = withContext(Dispatchers.IO) {
        require(config.isConfigured) { "请先完成模型配置" }
        val request = when (config.provider) {
            LlmProvider.OPENAI_COMPATIBLE -> openAiRequest(config, prompt)
            LlmProvider.ANTHROPIC -> anthropicRequest(config, prompt)
            LlmProvider.GEMINI -> geminiRequest(config, prompt)
        }
        try {
            httpClient.newCall(request).execute().use { response ->
                val body = response.body.string()
                if (!response.isSuccessful) {
                    val safeMessage = parseError(response.code, body)
                        .replace(config.apiKey, "[已隐藏]", ignoreCase = false)
                    throw LlmException(safeMessage)
                }
                parseContent(config.provider, body).ifBlank {
                    throw LlmException("模型返回了空内容")
                }
            }
        } catch (error: LlmException) {
            throw error
        } catch (error: IOException) {
            throw LlmException("网络请求失败，请检查网络和接口地址")
        } catch (error: Exception) {
            throw LlmException("模型响应格式无法解析")
        }
    }

    private fun openAiRequest(config: LlmConfig, prompt: String): Request {
        val body = JSONObject()
            .put("model", config.model)
            .put("temperature", 0.4)
            .put("messages", JSONArray()
                .put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT))
                .put(JSONObject().put("role", "user").put("content", prompt)))
        return requestBuilder("${config.baseUrl}/chat/completions", body)
            .header("Authorization", "Bearer ${config.apiKey}")
            .build()
    }

    private fun anthropicRequest(config: LlmConfig, prompt: String): Request {
        val body = JSONObject()
            .put("model", config.model)
            .put("max_tokens", 900)
            .put("system", SYSTEM_PROMPT)
            .put("messages", JSONArray().put(
                JSONObject().put("role", "user").put("content", prompt)
            ))
        return requestBuilder("${config.baseUrl}/messages", body)
            .header("x-api-key", config.apiKey)
            .header("anthropic-version", "2023-06-01")
            .build()
    }

    private fun geminiRequest(config: LlmConfig, prompt: String): Request {
        val encodedModel = URLEncoder.encode(config.model, Charsets.UTF_8.name())
        val body = JSONObject()
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(
                JSONObject().put("text", SYSTEM_PROMPT)
            )))
            .put("contents", JSONArray().put(
                JSONObject().put("role", "user").put("parts", JSONArray().put(
                    JSONObject().put("text", prompt)
                ))
            ))
            .put("generationConfig", JSONObject().put("temperature", 0.4).put("maxOutputTokens", 900))
        return requestBuilder("${config.baseUrl}/models/$encodedModel:generateContent", body)
            .header("x-goog-api-key", config.apiKey)
            .build()
    }

    private fun requestBuilder(url: String, body: JSONObject): Request.Builder = Request.Builder()
        .url(url)
        .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
        .header("Accept", "application/json")

    internal fun parseContent(provider: LlmProvider, body: String): String {
        val json = JSONObject(body)
        return when (provider) {
            LlmProvider.OPENAI_COMPATIBLE -> json.getJSONArray("choices")
                .getJSONObject(0).getJSONObject("message").getString("content").trim()
            LlmProvider.ANTHROPIC -> {
                val content = json.getJSONArray("content")
                (0 until content.length()).asSequence()
                    .map { content.getJSONObject(it) }
                    .filter { it.optString("type") == "text" }
                    .joinToString("\n") { it.optString("text") }.trim()
            }
            LlmProvider.GEMINI -> {
                val parts = json.getJSONArray("candidates").getJSONObject(0)
                    .getJSONObject("content").getJSONArray("parts")
                (0 until parts.length()).joinToString("\n") {
                    parts.getJSONObject(it).optString("text")
                }.trim()
            }
        }
    }

    private fun parseError(code: Int, body: String): String {
        val detail = runCatching {
            val error = JSONObject(body).opt("error")
            when (error) {
                is JSONObject -> error.optString("message")
                is String -> error
                else -> ""
            }
        }.getOrDefault("").take(180)
        return if (detail.isBlank()) "接口请求失败（HTTP $code）" else "接口请求失败（HTTP $code）：$detail"
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private const val SYSTEM_PROMPT =
            "你是一个克制、温和的中文日复盘助手。只依据用户提供的记录总结，不编造事实。" +
                "使用简洁纯文本输出四段：完成概览、今日亮点、值得调整、明日建议。不要使用Emoji。"
    }
}
