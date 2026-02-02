package com.nevoit.cresto.ui.theme.glasense

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nevoit.cresto.ui.screens.settings.util.SettingsViewModel

private val DarkColorScheme = darkColorScheme(
    onBackground = Color.White,
    onSurface = Color.White,
    primary = Blue500,
    background = Color.Black,
    surface = Color(0xFF1B1C1D),
)

private val LightColorScheme = lightColorScheme(
    onBackground = Color.Black,
    onSurface = Color.Black,
    primary = Blue500,
    background = Color.White,
    surface = Color(0xFFF3F4F6)
)

val AppColors: GlasenseColors
    @Composable
    get() = LocalGlasenseColors.current

@Composable
fun GlasenseTheme(
    settingsViewModel: SettingsViewModel = viewModel(),
    content: @Composable () -> Unit
) {
    val colorMode = settingsViewModel.colorMode.intValue
    val dynamicColor = settingsViewModel.isUseDynamicColor.value

    val systemInDark = isSystemInDarkTheme()

    val useDarkTheme = when (colorMode) {
        0 -> false
        1 -> true
        else -> systemInDark
    }

    val colorScheme = when {
        dynamicColor -> {
            val context = LocalContext.current
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        useDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val glasenseColors = if (dynamicColor) {
        glasenseColorsFromScheme(colorScheme, useDarkTheme)
    } else {
        if (useDarkTheme) GlasenseDarkPalette else GlasenseLightPalette
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            val windowInsetsController = WindowCompat.getInsetsController(window, view)

            windowInsetsController.isAppearanceLightStatusBars = !useDarkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = {
            CompositionLocalProvider(
                LocalGlasenseColors provides glasenseColors
            ) {
                content()
            }
        }
    )
}