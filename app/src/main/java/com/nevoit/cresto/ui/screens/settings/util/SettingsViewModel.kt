package com.nevoit.cresto.ui.screens.settings.util

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class SettingsViewModel : ViewModel() {

    val isCustomPrimaryColorEnabled = mutableStateOf(SettingsManager.isCustomPrimaryColorEnabled)
    val isUseDynamicColor = mutableStateOf(SettingsManager.isUseDynamicColor)
    val isLiteMode = mutableStateOf(SettingsManager.isLiteMode)
    val isLiquidGlass = mutableStateOf(SettingsManager.isLiquidGlass)
    val colorMode = mutableIntStateOf(SettingsManager.colorMode)

    fun onCustomPrimaryColorChanged(isEnabled: Boolean) {
        SettingsManager.isCustomPrimaryColorEnabled = isEnabled
        isCustomPrimaryColorEnabled.value = isEnabled
    }

    fun onUseDynamicColorChanged(isEnabled: Boolean) {
        SettingsManager.isUseDynamicColor = isEnabled
        isUseDynamicColor.value = isEnabled
    }

    fun onLiteModeChanged(isEnabled: Boolean) {
        SettingsManager.isLiteMode = isEnabled
        isLiteMode.value = isEnabled
    }

    fun onLiquidGlassChanged(isEnabled: Boolean) {
        SettingsManager.isLiquidGlass = isEnabled
        isLiquidGlass.value = isEnabled
    }

    fun colorMode(mode: Int) {
        SettingsManager.colorMode = mode
        colorMode.value = mode
    }
}