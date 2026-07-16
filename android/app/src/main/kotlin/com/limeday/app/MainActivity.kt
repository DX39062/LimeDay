package com.limeday.app

import android.util.Base64
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONObject

class MainActivity : FlutterActivity() {
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            "com.limeday.app/legacy_migration"
        ).setMethodCallHandler { call, result ->
            when (call.method) {
                "databasePath" -> result.success(getDatabasePath("lime_day.db").absolutePath)
                "legacyLlmConfig" -> result.success(readLegacyLlmConfig())
                else -> result.notImplemented()
            }
        }
    }

    private fun readLegacyLlmConfig(): Map<String, String>? = runCatching {
        val encrypted = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getString(KEY_CONFIG, null) ?: return null
        val json = JSONObject(decrypt(encrypted))
        val provider = when (json.optString("provider")) {
            "ANTHROPIC" -> "anthropic"
            "GEMINI" -> "gemini"
            else -> "openai_compatible"
        }
        mapOf(
            "provider" to provider,
            "baseUrl" to json.optString("baseUrl"),
            "model" to json.optString("model"),
            "apiKey" to json.optString("apiKey")
        )
    }.getOrNull()

    private fun decrypt(value: String): String {
        val parts = value.split(':', limit = 2)
        require(parts.size == 2)
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val key = keyStore.getKey(KEY_ALIAS, null) as SecretKey
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        return cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP))
            .toString(Charsets.UTF_8)
    }

    companion object {
        private const val PREFS_NAME = "llm_secure_config"
        private const val KEY_CONFIG = "encrypted_config"
        private const val KEY_ALIAS = "lime_day_llm_key_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
