package com.nevoit.cresto.ui.screens.settings.util

import androidx.lifecycle.ViewModel

class SettingsViewModel : ViewModel() {

    val colorMode = SettingsManager.colorModeState
    val isCustomPrimaryColorEnabled = SettingsManager.isCustomPrimaryColorEnabledState
    val isUseDynamicColor = SettingsManager.isUseDynamicColorState
    val isLiteMode = SettingsManager.isLiteModeState
    val isLiquidGlass = SettingsManager.isLiquidGlassState
    val themePrimaryColor = SettingsManager.themePrimaryColorState

    fun onCustomPrimaryColorChanged(isEnabled: Boolean) {
        SettingsManager.isCustomPrimaryColorEnabled = isEnabled
    }

    fun onUseDynamicColorChanged(isEnabled: Boolean) {
        SettingsManager.isUseDynamicColor = isEnabled
        if (isEnabled) {
            SettingsManager.isCustomPrimaryColorEnabled = false
        }
    }

    fun onLiteModeChanged(isEnabled: Boolean) {
        SettingsManager.isLiteMode = isEnabled
    }

    fun onLiquidGlassChanged(isEnabled: Boolean) {
        SettingsManager.isLiquidGlass = isEnabled
    }

    fun colorMode(mode: Int) {
        SettingsManager.colorMode = mode
    }

    fun onThemePrimaryColorChanged(colorArgb: Int) {
        SettingsManager.themePrimaryColor = colorArgb
    }
}