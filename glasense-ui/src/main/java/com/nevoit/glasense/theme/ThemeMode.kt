package com.nevoit.glasense.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

object GlasenseThemeMode {
    const val LIGHT = 0
    const val DARK = 1
    const val SYSTEM = 2
}

val LocalGlasenseIsDarkTheme = compositionLocalOf { false }

fun resolveDarkTheme(mode: Int, systemInDarkTheme: Boolean): Boolean {
    return when (mode) {
        GlasenseThemeMode.LIGHT -> false
        GlasenseThemeMode.DARK -> true
        else -> systemInDarkTheme
    }
}

@Composable
fun resolveDarkTheme(mode: Int): Boolean {
    return resolveDarkTheme(mode = mode, systemInDarkTheme = isSystemInDarkTheme())
}

