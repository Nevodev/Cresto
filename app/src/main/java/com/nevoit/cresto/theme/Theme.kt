package com.nevoit.cresto.theme

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
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nevoit.cresto.feature.settings.util.SettingsManager
import com.nevoit.cresto.feature.settings.util.SettingsViewModel
import com.nevoit.glasense.theme.Blue500
import com.nevoit.glasense.theme.GlasenseColors
import com.nevoit.glasense.theme.GlasenseDarkPalette
import com.nevoit.glasense.theme.GlasenseLightPalette
import com.nevoit.glasense.theme.GlasenseSpecs
import com.nevoit.glasense.theme.GlasenseSpecsStandard
import com.nevoit.glasense.theme.GlasenseSpecsVariant
import com.nevoit.glasense.theme.LocalGlasenseColors
import com.nevoit.glasense.theme.LocalGlasenseIsDarkTheme
import com.nevoit.glasense.theme.LocalGlasenseSpecs
import com.nevoit.glasense.theme.glasenseColorsFromScheme

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

val AppSpecs: GlasenseSpecs
    @Composable
    get() = LocalGlasenseSpecs.current

@Composable
fun GlasenseTheme(
    settingsViewModel: SettingsViewModel = viewModel(),
    content: @Composable () -> Unit
) {
    val colorMode = settingsViewModel.colorMode.intValue
    val dynamicColor = settingsViewModel.isUseDynamicColor.value
    val customPrimaryEnabled = settingsViewModel.isCustomPrimaryColorEnabled.value
    val themePrimaryColorArgb = settingsViewModel.themePrimaryColor.intValue
    val liquidGlass = settingsViewModel.isLiquidGlass.value

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

    val baseGlasenseColors = if (dynamicColor) {
        glasenseColorsFromScheme(colorScheme, useDarkTheme)
    } else {
        if (useDarkTheme) GlasenseDarkPalette else GlasenseLightPalette
    }

    val resolvedPrimary = when {
        dynamicColor -> baseGlasenseColors.primary
        customPrimaryEnabled -> Color(themePrimaryColorArgb)
        else -> Blue500
    }

    val glasenseColors = baseGlasenseColors.copy(
        primary = resolvedPrimary,
        activeTrack = if (customPrimaryEnabled) resolvedPrimary else baseGlasenseColors.activeTrack
    )

    val glasenseSpecs = if (liquidGlass || dynamicColor) {
        GlasenseSpecsVariant
    } else {
        GlasenseSpecsStandard
    }

    val liteMode = SettingsManager.isLiteModeState.value

    val glasenseSettings = remember(liquidGlass, liteMode) {
        GlasenseSettings(
            liquidGlass = liquidGlass,
            liteMode = liteMode
        )
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
                LocalGlasenseColors provides glasenseColors,
                LocalGlasenseSpecs provides glasenseSpecs,
                LocalGlasenseSettings provides glasenseSettings,
                LocalGlasenseIsDarkTheme provides useDarkTheme
            ) {
                content()
            }
        }
    )
}