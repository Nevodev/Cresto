package com.nevoit.cresto.feature.settings.update

import com.nevoit.cresto.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {
    private const val TIMEOUT_MS = 5_000

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun check(): UpdateCheckResult = withContext(Dispatchers.IO) {
        val manifestUrl = BuildConfig.UPDATE_MANIFEST_URL.trim()
        if (manifestUrl.isBlank()) return@withContext UpdateCheckResult.NotConfigured

        runCatching {
            val manifest = fetchManifest(manifestUrl)
            val currentVersionCode = BuildConfig.VERSION_CODE
            val hasUpdate = manifest.versionCode > currentVersionCode
            if (!hasUpdate) {
                UpdateCheckResult.NoUpdate
            } else {
                val isUnsupported = manifest.minSupportedVersionCode?.let {
                    currentVersionCode < it
                } ?: false
                UpdateCheckResult.HasUpdate(
                    UpdateInfo(
                        manifest = manifest,
                        isRequired = manifest.forceUpdate || isUnsupported
                    )
                )
            }
        }.getOrElse { throwable ->
            UpdateCheckResult.Failed(throwable)
        }
    }

    private fun fetchManifest(manifestUrl: String): UpdateManifest {
        val connection = (URL(manifestUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "Cresto/${BuildConfig.VERSION_NAME}")
        }

        return try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                error("Update manifest request failed: HTTP $responseCode")
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            json.decodeFromString<UpdateManifest>(body)
        } finally {
            connection.disconnect()
        }
    }
}
