package com.limeday.app.sync

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class WebDavException(message: String, val retryable: Boolean = false) : Exception(message)

class WebDavClient(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
) {
    suspend fun test(config: WebDavConfig) = withContext(Dispatchers.IO) {
        val value = requireConfig(config)
        execute(
            Request.Builder().url(value.baseUrl.toHttpUrl())
                .method("PROPFIND", PROPFIND_BODY.toRequestBody(XML))
                .header("Depth", "0")
                .authorized(value)
                .build(),
            allowed = setOf(200, 207)
        )
    }

    suspend fun download(config: WebDavConfig): SyncSnapshot? = withContext(Dispatchers.IO) {
        val value = requireConfig(config)
        try {
            downloadFile(value, FILE_NAME_V2) ?: downloadFile(value, FILE_NAME_V1)
        } catch (error: WebDavException) {
            throw error
        } catch (_: IOException) {
            throw WebDavException("WebDAV 网络连接失败", retryable = true)
        }
    }

    suspend fun upload(config: WebDavConfig, snapshot: SyncSnapshot) = withContext(Dispatchers.IO) {
        val value = requireConfig(config)
        ensureDirectories(value)
        val request = Request.Builder().url(fileUrl(value, FILE_NAME_V2))
            .put(snapshot.toJson().toRequestBody(JSON))
            .authorized(value)
            .build()
        execute(request, allowed = 200..299)
    }

    private fun ensureDirectories(config: WebDavConfig) {
        var current = config.baseUrl.toHttpUrl()
        config.directory.split('/').filter(String::isNotBlank).forEach { segment ->
            current = current.newBuilder().addPathSegment(segment).addPathSegment("").build()
            val request = Request.Builder().url(current)
                .method("MKCOL", ByteArray(0).toRequestBody(null))
                .authorized(config)
                .build()
            execute(request, allowed = setOf(200, 201, 204, 301, 302, 405))
        }
    }

    private fun downloadFile(config: WebDavConfig, fileName: String): SyncSnapshot? {
        val request = Request.Builder().url(fileUrl(config, fileName)).get().authorized(config).build()
        httpClient.newCall(request).execute().use { response ->
            if (response.code == 404) return null
            if (!response.isSuccessful) throw httpError(response.code)
            val body = response.body.string()
            if (body.length > MAX_SNAPSHOT_CHARS) throw WebDavException("远端同步文件过大")
            return runCatching { SyncSnapshot.fromJson(body) }
                .getOrElse { throw WebDavException(it.message ?: "远端同步文件无法解析") }
        }
    }

    private fun fileUrl(config: WebDavConfig, fileName: String): HttpUrl {
        val builder = config.baseUrl.toHttpUrl().newBuilder()
        config.directory.split('/').filter(String::isNotBlank).forEach(builder::addPathSegment)
        return builder.addPathSegment(fileName).build()
    }

    private fun execute(request: Request, allowed: Iterable<Int>) {
        try {
            httpClient.newCall(request).execute().use { response ->
                if (response.code !in allowed) throw httpError(response.code)
            }
        } catch (error: WebDavException) {
            throw error
        } catch (_: IOException) {
            throw WebDavException("WebDAV 网络连接失败", retryable = true)
        }
    }

    private fun requireConfig(config: WebDavConfig): WebDavConfig {
        val value = config.normalized
        if (!value.isConfigured) throw WebDavException("请填写 HTTPS 地址、用户名和密码")
        return value
    }

    private fun httpError(code: Int): WebDavException = when (code) {
        401, 403 -> WebDavException("WebDAV 认证失败，请检查账号和密码")
        404 -> WebDavException("WebDAV 地址不存在")
        409 -> WebDavException("WebDAV 远端目录无法创建")
        429 -> WebDavException("WebDAV 请求过于频繁，请稍后重试", retryable = true)
        in 500..599 -> WebDavException("WebDAV 服务暂时不可用（HTTP $code）", retryable = true)
        else -> WebDavException("WebDAV 请求失败（HTTP $code）")
    }

    private fun Request.Builder.authorized(config: WebDavConfig): Request.Builder =
        header("Authorization", Credentials.basic(config.username, config.password))

    companion object {
        private const val FILE_NAME_V2 = "limeday-sync-v2.json"
        private const val FILE_NAME_V1 = "limeday-sync-v1.json"
        private const val MAX_SNAPSHOT_CHARS = 10_000_000
        private val JSON = "application/json; charset=utf-8".toMediaType()
        private val XML = "application/xml; charset=utf-8".toMediaType()
        private const val PROPFIND_BODY = """<?xml version="1.0"?><propfind xmlns="DAV:"><prop><resourcetype/></prop></propfind>"""
    }
}
