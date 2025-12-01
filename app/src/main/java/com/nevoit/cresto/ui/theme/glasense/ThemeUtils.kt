package com.nevoit.cresto.ui.theme.glasense

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.nevoit.cresto.ui.screens.settings.util.SettingsManager

@Composable
fun isAppInDarkTheme(): Boolean {
    val mode = SettingsManager.colorModeState.intValue

    val systemDark = isSystemInDarkTheme()

    return when (mode) {
        0 -> false
        1 -> true
        else -> systemDark
    }
}