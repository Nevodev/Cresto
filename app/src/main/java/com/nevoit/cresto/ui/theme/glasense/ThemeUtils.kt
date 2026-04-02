package com.nevoit.cresto.ui.theme.glasense

import androidx.compose.runtime.Composable
import com.nevoit.cresto.ui.screens.settings.util.SettingsManager
import com.nevoit.glasense.theme.resolveDarkTheme

@Composable
fun isAppInDarkTheme(): Boolean {
    val mode = SettingsManager.colorModeState.intValue
    return resolveDarkTheme(mode)
}