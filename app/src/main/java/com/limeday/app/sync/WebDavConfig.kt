package com.limeday.app.sync

data class WebDavConfig(
    val baseUrl: String = "",
    val username: String = "",
    val password: String = "",
    val directory: String = "LimeDay"
) {
    val normalized: WebDavConfig
        get() = copy(
            baseUrl = baseUrl.trim().trimEnd('/'),
            username = username.trim(),
            password = password.trim(),
            directory = directory.trim().trim('/').ifBlank { "LimeDay" }
        )

    val isConfigured: Boolean
        get() = normalized.run {
            baseUrl.startsWith("https://") && username.isNotBlank() && password.isNotBlank()
        }
}
