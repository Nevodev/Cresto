package com.nevoit.cresto.feature.settings.util

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class AISettingsViewModel : ViewModel() {

    val apiUrl = mutableStateOf(SettingsManager.aiApiUrl)
    val apiKey = mutableStateOf(SettingsManager.aiApiKey)
    val textModel = mutableStateOf(SettingsManager.aiTextModel)
    val multimodalModel = mutableStateOf(SettingsManager.aiMultimodalModel)
    val lastSavedAt = mutableStateOf(0L)

    fun onApiUrlChanged(value: String) {
        apiUrl.value = value
        saveSettingsIfChanged()
    }

    fun onApiKeyChanged(value: String) {
        apiKey.value = value
        saveSettingsIfChanged()
    }

    fun onTextModelChanged(value: String) {
        textModel.value = value
        saveSettingsIfChanged()
    }

    fun onMultimodalModelChanged(value: String) {
        multimodalModel.value = value
        saveSettingsIfChanged()
    }

    fun restoreDefaults() {
        SettingsManager.resetAiSettingsToDefaults()
        apiUrl.value = SettingsManager.aiApiUrl
        apiKey.value = SettingsManager.aiApiKey
        textModel.value = SettingsManager.aiTextModel
        multimodalModel.value = SettingsManager.aiMultimodalModel
        lastSavedAt.value = System.currentTimeMillis()
    }

    fun saveSettings() {
        val newApiUrl = apiUrl.value.trim()
        val newApiKey = apiKey.value.trim()
        val newTextModel = textModel.value.trim()
        val newMultimodalModel = multimodalModel.value.trim()

        SettingsManager.aiApiUrl = newApiUrl
        SettingsManager.aiApiKey = newApiKey
        SettingsManager.aiTextModel = newTextModel
        SettingsManager.aiMultimodalModel = newMultimodalModel
        lastSavedAt.value = System.currentTimeMillis()
    }

    private fun saveSettingsIfChanged() {
        val newApiUrl = apiUrl.value.trim()
        val newApiKey = apiKey.value.trim()
        val newTextModel = textModel.value.trim()
        val newMultimodalModel = multimodalModel.value.trim()

        if (
            newApiUrl == SettingsManager.aiApiUrl &&
            newApiKey == SettingsManager.aiApiKey &&
            newTextModel == SettingsManager.aiTextModel &&
            newMultimodalModel == SettingsManager.aiMultimodalModel
        ) {
            return
        }

        saveSettings()
    }
}

