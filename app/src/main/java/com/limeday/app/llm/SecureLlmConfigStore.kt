package com.limeday.app.llm

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONArray
import org.json.JSONObject

class SecureLlmConfigStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): LlmSettings {
        val encrypted = preferences.getString(KEY_CONFIG, null) ?: return LlmSettings()
        return runCatching {
            val json = JSONObject(decrypt(encrypted))
            if (json.has("providers")) parseSettings(json) else migrateLegacy(json)
        }.getOrElse {
            preferences.edit { remove(KEY_CONFIG) }
            LlmSettings()
        }
    }

    fun save(settings: LlmSettings) {
        val providers = settings.providers.map(LlmServiceConfig::normalized)
        val validIds = providers.mapTo(mutableSetOf(), LlmServiceConfig::id)
        val normalized = settings.copy(
            providers = providers,
            activeProviderId = settings.activeProviderId?.takeIf(validIds::contains) ?: providers.firstOrNull()?.id,
            favoritePrompts = settings.favoritePrompts.map(String::trim).filter(String::isNotBlank).distinct(),
            recentPrompts = settings.recentPrompts.map(String::trim).filter(String::isNotBlank).distinct().take(10),
            modelCaches = settings.modelCaches.filter { it.providerId in validIds }.distinctBy(LlmModelCache::providerId)
        )
        preferences.edit { putString(KEY_CONFIG, encrypt(normalized.toJson().toString())) }
    }

    fun clear() = preferences.edit { remove(KEY_CONFIG) }

    private fun parseSettings(json: JSONObject): LlmSettings = LlmSettings(
        providers = json.optJSONArray("providers").mapObjects(::providerFromJson),
        activeProviderId = json.optString("activeProviderId").takeIf(String::isNotBlank),
        favoritePrompts = json.optJSONArray("favoritePrompts").mapStrings(),
        recentPrompts = json.optJSONArray("recentPrompts").mapStrings().take(10),
        modelCaches = json.optJSONArray("modelCaches").mapObjects(::cacheFromJson)
    )

    private fun migrateLegacy(json: JSONObject): LlmSettings {
        val legacyProvider = json.optString("provider", "OPENAI_COMPATIBLE")
        val presetId = when (legacyProvider) {
            "ANTHROPIC" -> "anthropic"
            "GEMINI" -> "gemini"
            else -> "custom"
        }
        val preset = LlmProviderPresets.find(presetId) ?: LlmProviderPresets.all.first()
        val provider = preset.createProvider().copy(
            id = UUID.randomUUID().toString(),
            name = when (legacyProvider) {
                "ANTHROPIC" -> "Anthropic"
                "GEMINI" -> "Google Gemini"
                else -> "OpenAI 兼容"
            },
            protocol = when (legacyProvider) {
                "ANTHROPIC" -> LlmProtocol.ANTHROPIC_MESSAGES
                "GEMINI" -> LlmProtocol.GEMINI_NATIVE
                else -> LlmProtocol.OPENAI_CHAT
            },
            baseUrl = json.optString("baseUrl", preset.baseUrl),
            model = json.optString("model").ifBlank {
                when (legacyProvider) {
                    "ANTHROPIC" -> LlmProviderPresets.find("anthropic")!!.defaultModel
                    "GEMINI" -> LlmProviderPresets.find("gemini")!!.defaultModel
                    else -> "gpt-4.1-mini"
                }
            },
            apiKey = json.optString("apiKey")
        )
        return LlmSettings(listOf(provider), provider.id).also(::save)
    }

    private fun LlmSettings.toJson(): JSONObject = JSONObject()
        .put("version", 2)
        .put("activeProviderId", activeProviderId ?: "")
        .put("providers", JSONArray().apply { providers.forEach { put(it.toJson()) } })
        .put("favoritePrompts", JSONArray(favoritePrompts))
        .put("recentPrompts", JSONArray(recentPrompts))
        .put("modelCaches", JSONArray().apply { modelCaches.forEach { put(it.toJson()) } })

    private fun LlmServiceConfig.toJson(): JSONObject = JSONObject()
        .put("id", id).put("name", name).put("presetId", presetId).put("protocol", protocol.name)
        .put("baseUrl", baseUrl).put("model", model).put("apiKey", apiKey).put("modelsUrl", modelsUrl)
        .put("allowInsecureHttp", allowInsecureHttp).put("createdAt", createdAt).put("updatedAt", updatedAt)

    private fun providerFromJson(json: JSONObject): LlmServiceConfig {
        val protocol = runCatching { LlmProtocol.valueOf(json.getString("protocol")) }
            .getOrDefault(LlmProtocol.OPENAI_CHAT)
        return LlmServiceConfig(
            id = json.optString("id").ifBlank { UUID.randomUUID().toString() },
            name = json.optString("name"),
            presetId = json.optString("presetId", "custom"),
            protocol = protocol,
            baseUrl = json.optString("baseUrl"),
            model = json.optString("model"),
            apiKey = json.optString("apiKey"),
            modelsUrl = json.optString("modelsUrl"),
            allowInsecureHttp = json.optBoolean("allowInsecureHttp"),
            createdAt = json.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = json.optLong("updatedAt", System.currentTimeMillis())
        )
    }

    private fun LlmModelCache.toJson(): JSONObject = JSONObject()
        .put("providerId", providerId).put("models", JSONArray(models)).put("fetchedAt", fetchedAt)

    private fun cacheFromJson(json: JSONObject): LlmModelCache = LlmModelCache(
        providerId = json.optString("providerId"),
        models = json.optJSONArray("models").mapStrings(),
        fetchedAt = json.optLong("fetchedAt")
    )

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        val ciphertext = Base64.encodeToString(cipher.doFinal(value.toByteArray()), Base64.NO_WRAP)
        return "$iv:$ciphertext"
    }

    private fun decrypt(value: String): String {
        val parts = value.split(':', limit = 2)
        require(parts.size == 2)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)))
        return cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)).toString(Charsets.UTF_8)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
            generateKey()
        }
    }

    companion object {
        private const val PREFS_NAME = "lime_day_llm"
        private const val KEY_CONFIG = "encrypted_config"
        private const val KEY_ALIAS = "lime_day_llm_key_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}

private fun JSONArray?.mapStrings(): List<String> = this?.let { array ->
    (0 until array.length()).map { array.optString(it) }.filter(String::isNotBlank)
}.orEmpty()

private fun <T> JSONArray?.mapObjects(transform: (JSONObject) -> T): List<T> = this?.let { array ->
    (0 until array.length()).map { transform(array.getJSONObject(it)) }
}.orEmpty()
