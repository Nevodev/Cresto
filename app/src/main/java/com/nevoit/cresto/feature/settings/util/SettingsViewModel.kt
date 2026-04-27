package com.nevoit.cresto.feature.settings.util

import androidx.lifecycle.ViewModel

class SettingsViewModel : ViewModel() {

    val colorMode = SettingsManager.colorModeState
    val isCustomPrimaryColorEnabled = SettingsManager.isCustomPrimaryColorEnabledState
    val isUseDynamicColor = SettingsManager.isUseDynamicColorState
    val isLiteMode = SettingsManager.isLiteModeState
    val isLiquidGlass = SettingsManager.isLiquidGlassState
    val themePrimaryColor = SettingsManager.themePrimaryColorState
    val isDueTodayMarker = SettingsManager.isDueTodayMarkerState
    val isOverdueMarker = SettingsManager.isOverdueMarkerState
    val isCompletionSoundEnabled = SettingsManager.isCompletionSoundEnabledState
    val isEasterEggEnabled = SettingsManager.isEasterEggState
    val isSuperGraphicUltraModernGirlEnabled = SettingsManager.isSuperGraphicUltraModernGirlState

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

    fun onDueTodayMarkerChanged(isEnabled: Boolean) {
        SettingsManager.isDueTodayMarker = isEnabled
    }

    fun onOverdueMarkerChanged(isEnabled: Boolean) {
        SettingsManager.isOverdueMarker = isEnabled
    }

    fun onCompletionSoundChanged(isEnabled: Boolean) {
        SettingsManager.isCompletionSoundEnabled = isEnabled
    }

    fun unlockEasterEgg() {
        SettingsManager.isEasterEggEnabled = true
    }

    fun onSuperGraphicUltraModernGirlChanged(isEnabled: Boolean) {
        SettingsManager.isSuperGraphicUltraModernGirlEnabled = isEnabled
    }
}