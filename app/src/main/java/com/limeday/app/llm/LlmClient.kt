package com.limeday.app.llm

import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
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
    suspend fun generate(config: LlmServiceConfig, prompt: String): String {
        require(config.isConfigured) { "请先完成模型配置" }
        val request = when (config.protocol) {
            LlmProtocol.OPENAI_CHAT -> openAiChatRequest(config, prompt)
            LlmProtocol.OPENAI_RESPONSES -> openAiResponsesRequest(config, prompt)
            LlmProtocol.ANTHROPIC_MESSAGES -> anthropicRequest(config, prompt)
            LlmProtocol.GEMINI_NATIVE -> geminiRequest(config, prompt)
        }
        return execute(config, request) { body -> parseContent(config.protocol, body) }
            .ifBlank { throw LlmException("模型返回了空内容") }
    }

    suspend fun fetchModels(config: LlmServiceConfig): List<String> {
        val normalized = config.normalized
        require(normalized.endpointAllowed) { "接口地址必须使用 HTTPS，或明确允许本地 HTTP" }
        if (normalized.apiKey.isBlank() && normalized.presetId != "ollama") throw LlmException("请先填写 API Key")
        var lastError: LlmException? = null
        for (url in resolveModelsUrls(normalized)) {
            val request = Request.Builder().url(url).get().header("Accept", "application/json").apply {
                when (normalized.protocol) {
                    LlmProtocol.OPENAI_CHAT, LlmProtocol.OPENAI_RESPONSES ->
                        if (normalized.apiKey.isNotBlank()) header("Authorization", "Bearer ${normalized.apiKey}")
                    LlmProtocol.ANTHROPIC_MESSAGES -> {
                        header("x-api-key", normalized.apiKey)
                        header("anthropic-version", "2023-06-01")
                    }
                    LlmProtocol.GEMINI_NATIVE -> header("x-goog-api-key", normalized.apiKey)
                }
            }.build()
            try {
                return execute(normalized, request) { parseModels(normalized.protocol, it) }
                    .distinct().sorted().ifEmpty { throw LlmException("服务商没有返回可用模型") }
            } catch (error: LlmException) {
                lastError = error
                if (!error.message.orEmpty().contains("HTTP 404") && !error.message.orEmpty().contains("HTTP 405")) throw error
            }
        }
        throw lastError ?: LlmException("无法推导模型列表地址")
    }

    internal fun resolveModelsUrl(config: LlmServiceConfig): String = resolveModelsUrls(config).first()

    internal fun resolveModelsUrls(config: LlmServiceConfig): List<String> {
        config.modelsUrl.trim().takeIf(String::isNotBlank)?.let { return listOf(requireValidUrl(it, config)) }
        val base = config.baseUrl.trim().trimEnd('/')
        requireValidUrl(base, config)
        val lastSegment = base.substringAfterLast('/')
        val versioned = lastSegment.matches(Regex("v\\d+(?:beta)?"))
        val candidates = mutableListOf<String>()
        if (versioned) {
            candidates += "$base/models"
            if (lastSegment !in setOf("v1", "v1beta")) candidates += "$base/v1/models"
        } else {
            candidates += "$base/v1/models"
            candidates += "$base/models"
        }
        val suffix = COMPAT_SUFFIXES.firstOrNull(base::endsWith)
        if (suffix != null) {
            val root = base.removeSuffix(suffix).trimEnd('/')
            candidates += "$root/v1/models"
            candidates += "$root/models"
        }
        return candidates.distinct().map { requireValidUrl(it, config) }
    }

    private fun openAiChatRequest(config: LlmServiceConfig, prompt: String): Request {
        val body = JSONObject()
            .put("model", config.model)
            .put("temperature", 0.4)
            .put("messages", JSONArray()
                .put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT))
                .put(JSONObject().put("role", "user").put("content", prompt)))
        return jsonRequest("${config.baseUrl}/chat/completions", body)
            .header("Authorization", "Bearer ${config.apiKey}").build()
    }

    private fun openAiResponsesRequest(config: LlmServiceConfig, prompt: String): Request {
        val input = JSONArray()
            .put(JSONObject().put("role", "system").put("content", JSONArray().put(
                JSONObject().put("type", "input_text").put("text", SYSTEM_PROMPT)
            )))
            .put(JSONObject().put("role", "user").put("content", JSONArray().put(
                JSONObject().put("type", "input_text").put("text", prompt)
            )))
        val body = JSONObject().put("model", config.model).put("input", input).put("temperature", 0.4)
        return jsonRequest("${config.baseUrl}/responses", body)
            .header("Authorization", "Bearer ${config.apiKey}").build()
    }

    private fun anthropicRequest(config: LlmServiceConfig, prompt: String): Request {
        val body = JSONObject()
            .put("model", config.model).put("max_tokens", 1200).put("system", SYSTEM_PROMPT)
            .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt)))
        return jsonRequest("${config.baseUrl}/messages", body)
            .header("x-api-key", config.apiKey).header("anthropic-version", "2023-06-01").build()
    }

    private fun geminiRequest(config: LlmServiceConfig, prompt: String): Request {
        val encodedModel = URLEncoder.encode(config.model.removePrefix("models/"), Charsets.UTF_8.name())
        val body = JSONObject()
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", SYSTEM_PROMPT))))
            .put("contents", JSONArray().put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
            .put("generationConfig", JSONObject().put("temperature", 0.4).put("maxOutputTokens", 1200))
        return jsonRequest("${config.baseUrl}/models/$encodedModel:generateContent", body)
            .header("x-goog-api-key", config.apiKey).build()
    }

    private fun jsonRequest(url: String, body: JSONObject): Request.Builder = Request.Builder()
        .url(url).post(body.toString().toRequestBody(JSON_MEDIA_TYPE)).header("Accept", "application/json")

    private suspend fun <T> execute(config: LlmServiceConfig, request: Request, parse: (String) -> T): T =
        suspendCancellableCoroutine { continuation ->
            val call = httpClient.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (!continuation.isActive) return
                    continuation.resumeWithException(LlmException("网络请求失败，请检查网络和接口地址"))
                }

                override fun onResponse(call: Call, response: Response) {
                    if (!continuation.isActive) {
                        response.close()
                        return
                    }
                    runCatching {
                        response.use {
                            val body = it.body.string()
                            if (!it.isSuccessful) {
                                throw LlmException(parseError(it.code, body).redact(config))
                            }
                            parse(body)
                        }
                    }.fold(
                        onSuccess = { continuation.resumeWith(Result.success(it)) },
                        onFailure = { error ->
                            val safe = when (error) {
                                is LlmException -> error
                                else -> LlmException("模型响应格式无法解析")
                            }
                            continuation.resumeWith(Result.failure(safe))
                        }
                    )
                }
            })
        }

    internal fun parseContent(protocol: LlmProtocol, body: String): String {
        val json = JSONObject(body)
        return when (protocol) {
            LlmProtocol.OPENAI_CHAT -> json.getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").getString("content").trim()
            LlmProtocol.OPENAI_RESPONSES -> parseResponsesContent(json)
            LlmProtocol.ANTHROPIC_MESSAGES -> json.getJSONArray("content").objects()
                .filter { it.optString("type") == "text" }.joinToString("\n") { it.optString("text") }.trim()
            LlmProtocol.GEMINI_NATIVE -> json.getJSONArray("candidates").getJSONObject(0)
                .getJSONObject("content").getJSONArray("parts").objects()
                .joinToString("\n") { it.optString("text") }.trim()
        }
    }

    internal fun parseModels(protocol: LlmProtocol, body: String): List<String> {
        val json = JSONObject(body)
        return when (protocol) {
            LlmProtocol.GEMINI_NATIVE -> json.optJSONArray("models").objects()
                .map { it.optString("name").removePrefix("models/") }.filter(String::isNotBlank)
            else -> json.optJSONArray("data").objects().map { it.optString("id") }.filter(String::isNotBlank)
        }
    }

    private fun parseResponsesContent(json: JSONObject): String {
        json.optString("output_text").takeIf(String::isNotBlank)?.let { return it.trim() }
        return json.optJSONArray("output").objects().flatMap { output ->
            output.optJSONArray("content").objects()
        }.filter { it.optString("type") in setOf("output_text", "text") }
            .joinToString("\n") { it.optString("text") }.trim()
    }

    private fun requireValidUrl(value: String, config: LlmServiceConfig): String {
        val url = value.toHttpUrlOrNull() ?: throw LlmException("模型列表地址无效")
        if (url.scheme == "http" && !config.allowInsecureHttp) throw LlmException("HTTP 地址需要开启本地不安全连接")
        if (url.scheme !in setOf("http", "https")) throw LlmException("模型列表地址协议不受支持")
        return url.toString().trimEnd('/')
    }

    private fun parseError(code: Int, body: String): String {
        val detail = runCatching {
            when (val error = JSONObject(body).opt("error")) {
                is JSONObject -> error.optString("message")
                is String -> error
                else -> ""
            }
        }.getOrDefault("").take(180)
        return if (detail.isBlank()) "接口请求失败（HTTP $code）" else "接口请求失败（HTTP $code）：$detail"
    }

    private fun String.redact(config: LlmServiceConfig): String =
        if (config.apiKey.isBlank()) this else replace(config.apiKey, "[已隐藏]", ignoreCase = false)

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private const val SYSTEM_PROMPT =
            "你是一个克制、清晰的中文复盘助手。只依据用户提供的记录回答，不编造事实。" +
                "遵循用户本次指令，使用易读的纯文本输出，不使用Emoji。"
        private val COMPAT_SUFFIXES = listOf("/api/claudecode", "/api/anthropic", "/anthropic", "/claude")
    }
}

private fun JSONArray?.objects(): List<JSONObject> = this?.let { array ->
    (0 until array.length()).mapNotNull { array.optJSONObject(it) }
}.orEmpty()
