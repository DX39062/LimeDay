package com.limeday.app.sync

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONObject

class SecureWebDavConfigStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): WebDavConfig {
        val encrypted = preferences.getString(KEY_CONFIG, null) ?: return WebDavConfig()
        return runCatching {
            val json = JSONObject(decrypt(encrypted))
            WebDavConfig(
                baseUrl = json.optString("baseUrl"),
                username = json.optString("username"),
                password = json.optString("password"),
                directory = json.optString("directory", "LimeDay")
            ).normalized
        }.getOrElse { WebDavConfig() }
    }

    fun save(config: WebDavConfig) {
        val value = config.normalized
        val json = JSONObject()
            .put("baseUrl", value.baseUrl)
            .put("username", value.username)
            .put("password", value.password)
            .put("directory", value.directory)
        preferences.edit { putString(KEY_CONFIG, encrypt(json.toString())) }
    }

    fun clear() = preferences.edit { remove(KEY_CONFIG) }

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
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP))
        )
        return cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)).toString(Charsets.UTF_8)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    companion object {
        private const val PREFS_NAME = "webdav_secure_config"
        private const val KEY_CONFIG = "encrypted_config"
        private const val KEY_ALIAS = "lime_day_webdav_key_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
