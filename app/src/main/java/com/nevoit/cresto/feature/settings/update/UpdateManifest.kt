package com.nevoit.cresto.feature.settings.update

import kotlinx.serialization.Serializable

@Serializable
data class UpdateManifest(
    val schemaVersion: Int = 1,
    val platform: String = "android",
    val channel: String = "stable",
    val versionCode: Int,
    val versionName: String,
    val minSupportedVersionCode: Int? = null,
    val forceUpdate: Boolean = false,
    val publishedAt: String? = null,
    val heroImageUrl: String? = null,
    val downloadUrl: String,
    val fallbackDownloadUrl: String? = null,
    val description: Map<String, String> = emptyMap(),
    val releaseNotes: Map<String, List<ReleaseNoteSection>> = emptyMap()
)

@Serializable
data class ReleaseNoteSection(
    val type: String? = null,
    val title: String? = null,
    val items: List<String> = emptyList()
)

data class UpdateInfo(
    val manifest: UpdateManifest,
    val isRequired: Boolean
)

sealed interface UpdateCheckResult {
    data class HasUpdate(val updateInfo: UpdateInfo) : UpdateCheckResult
    data object NoUpdate : UpdateCheckResult
    data object NotConfigured : UpdateCheckResult
    data class Failed(val throwable: Throwable) : UpdateCheckResult
}

fun UpdateManifest.localizedDescription(languageTag: String): String? {
    return localizedValue(description, languageTag)
}

fun UpdateManifest.localizedReleaseNotes(languageTag: String): List<ReleaseNoteSection> {
    return localizedValue(releaseNotes, languageTag) ?: emptyList()
}

private fun <T> localizedValue(values: Map<String, T>, languageTag: String): T? {
    val language = languageTag.substringBefore("-")
    return values[languageTag]
        ?: values[language]
        ?: values["en"]
        ?: values.values.firstOrNull()
}
