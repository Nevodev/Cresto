package com.nevoit.cresto.theme

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
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
import com.nevoit.glasense.theme.GlasenseColors
import com.nevoit.glasense.theme.GlasenseDarkPalette
import com.nevoit.glasense.theme.GlasenseLightPalette
import com.nevoit.glasense.theme.GlasenseSpecs
import com.nevoit.glasense.theme.GlasenseSpecsStandard
import com.nevoit.glasense.theme.GlasenseSpecsVariant
import com.nevoit.glasense.theme.GlasenseTheme
import com.nevoit.glasense.theme.purify
import com.nevoit.glasense.theme.tokens.Blue500
import com.nevoit.glasense.theme.umamify

val AppColors: GlasenseColors
    @Composable
    get() = GlasenseTheme.colors

val AppSpecs: GlasenseSpecs
    @Composable
    get() = GlasenseTheme.specs

private fun systemDynamicSeedColor(context: Context): Color =
    Color(context.getColor(android.R.color.system_accent1_600))

private fun glasenseColorsFromScheme(scheme: ColorScheme, isDark: Boolean): GlasenseColors {
    val pageBackground = if (isDark) Color.Black else scheme.surfaceContainer
    val cardBackground = if (isDark) scheme.surfaceContainer else scheme.surface

    val pageBackgroundElevated = if (isDark) scheme.surfaceContainer else pageBackground
    val cardBackgroundElevated = if (isDark) scheme.surfaceContainerHigh else cardBackground

    val contentColor = if (isDark) Color.White else Color.Black

    val scrimLight = contentColor.copy(alpha = if (isDark) 0.05f else 0.025f)
    val scrimNormal = contentColor.copy(alpha = if (isDark) 0.1f else 0.05f)
    val scrimMedium = contentColor.copy(alpha = if (isDark) 0.2f else 0.1f)
    val scrimBold = contentColor.copy(alpha = if (isDark) 0.4f else 0.2f)

    return GlasenseColors(
        activeTrack = scheme.primary,
        inactiveTrack = scheme.surfaceContainerHighest,
        activeThumb = scheme.onPrimary,
        inactiveThumb = scheme.outline,
        pageBackground = pageBackground,
        cardBackground = cardBackground,
        elevatedPageBackground = pageBackgroundElevated,
        elevatedCardBackground = cardBackgroundElevated,
        scrimLight = scrimLight,
        scrimNormal = scrimNormal,
        scrimMedium = scrimMedium,
        scrimBold = scrimBold,
        primary = scheme.primary,
        onPrimary = scheme.onPrimary,
        content = contentColor,
        contentVariant = contentColor.copy(.5f),
        highlightText = scheme.tertiary.purify(0.8f),
        error = scheme.error.umamify(1.2f),
        onError = scheme.onError.umamify(1.2f),
        segmentedControlBackground = scheme.secondaryContainer,
        onSegmentedControlBackground = scheme.onSecondaryContainer,
        segmentedControlIndicator = scheme.secondary,
        onSegmentedControlIndicator = scheme.onSecondary
    )
}

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

    val context = LocalContext.current
    val baseGlasenseColors = if (dynamicColor) {
        val colorScheme = remember(context, useDarkTheme) {
            dynamicColorScheme(
                seedColor = systemDynamicSeedColor(context),
                isDark = useDarkTheme
            )
        }
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
            liteMode = liteMode,
            dynamicColor = dynamicColor
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

    GlasenseTheme(
        darkTheme = useDarkTheme,
        colors = glasenseColors,
        specs = glasenseSpecs
    ) {
        CompositionLocalProvider(
            LocalGlasenseSettings provides glasenseSettings
        ) {
            content()
        }
    }
}