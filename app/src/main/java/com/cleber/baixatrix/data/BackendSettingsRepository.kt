package com.cleber.baixatrix.data

import android.content.Context
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

data class BackendSettings(
    val baseUrl: String = "",
    val apiKey: String = ""
)

/** Private app storage for the user's self-hosted YouTube resolver settings. */
class BackendSettingsRepository(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun load(): BackendSettings = BackendSettings(
        baseUrl = preferences.getString(KEY_BASE_URL, "").orEmpty(),
        apiKey = preferences.getString(KEY_API_KEY, "").orEmpty()
    )

    fun save(baseUrl: String, apiKey: String) {
        val normalizedUrl = normalizeBaseUrl(baseUrl).orEmpty()
        preferences.edit()
            .putString(KEY_BASE_URL, normalizedUrl)
            .putString(KEY_API_KEY, apiKey.trim())
            .apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "youtube_backend"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_API_KEY = "api_key"

        fun normalizeBaseUrl(value: String): String? {
            val url = value.trim().toHttpUrlOrNull() ?: return null
            if (
                url.scheme != "https" ||
                url.username.isNotEmpty() ||
                url.password.isNotEmpty() ||
                url.query != null ||
                url.fragment != null
            ) {
                return null
            }

            val path = url.encodedPath.trimEnd('/').ifEmpty { "/" }
            return url.newBuilder().encodedPath(path).build().toString().trimEnd('/')
        }
    }
}
