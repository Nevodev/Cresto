package com.nevoit.glasense.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class GlasenseColors(
    // for switches
    val activeTrack: Color,
    val inactiveTrack: Color,
    val activeThumb: Color,
    val inactiveThumb: Color,

    // scrim
    val scrimLight: Color,
    val scrimNormal: Color,
    val scrimMedium: Color,
    val scrimBold: Color,

    // for hierarchical cards
    val pageBackground: Color,
    val cardBackground: Color,

    val pageBackgroundElevated: Color,
    val cardBackgroundElevated: Color,

    val primary: Color,
    val onPrimary: Color,

    val content: Color,
    val contentVariant: Color,

    val error: Color,
    val onError: Color,

    // for segmented control
    val segmentedControlBackground: Color = scrimNormal,
    val onSegmentedControlBackground: Color = contentVariant,
    val segmentedControlIndicator: Color,
    val onSegmentedControlIndicator: Color = content
)

val GlasenseLightPalette = GlasenseColors(
    activeTrack = Green500,
    inactiveTrack = Color(0xFF787880).copy(.25f),
    activeThumb = Color.White,
    inactiveThumb = Color.White,
    pageBackground = Color(0xFFF3F4F6),
    cardBackground = Color.White,
    pageBackgroundElevated = Color(0xFFF3F4F6),
    cardBackgroundElevated = Color.White,
    scrimLight = Color.Black.copy(alpha = 0.025f),
    scrimNormal = Color.Black.copy(alpha = 0.05f),
    scrimMedium = Color.Black.copy(alpha = 0.1f),
    scrimBold = Color.Black.copy(alpha = 0.2f),
    primary = Blue500,
    onPrimary = Color.White,
    content = Color.Black,
    contentVariant = Color.Black.copy(.5f),
    error = Red500,
    onError = Color.White,
    segmentedControlIndicator = Color.White
)

val GlasenseDarkPalette = GlasenseColors(
    activeTrack = Green500,
    inactiveTrack = Color(0xFF787880).copy(.25f),
    activeThumb = Color.White,
    inactiveThumb = Color.White,
    pageBackground = Color.Black,
    cardBackground = Color(0xFF1B1C1D),
    pageBackgroundElevated = Color(0xFF1C1C1E),
    cardBackgroundElevated = Color(0xFF2C2C2E),
    scrimLight = Color.White.copy(alpha = 0.05f),
    scrimNormal = Color.White.copy(alpha = 0.1f),
    scrimMedium = Color.White.copy(alpha = 0.2f),
    scrimBold = Color.White.copy(alpha = 0.4f),
    primary = Blue500,
    onPrimary = Color.White,
    content = Color.White,
    contentVariant = Color.White.copy(.5f),
    error = Red500,
    onError = Color.White,
    segmentedControlIndicator = Color(0xFF636366)
)

fun glasenseColorsFromScheme(scheme: ColorScheme, isDark: Boolean): GlasenseColors {
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
        pageBackgroundElevated = pageBackgroundElevated,
        cardBackgroundElevated = cardBackgroundElevated,
        scrimLight = scrimLight,
        scrimNormal = scrimNormal,
        scrimMedium = scrimMedium,
        scrimBold = scrimBold,
        primary = scheme.primary,
        onPrimary = scheme.onPrimary,
        content = contentColor,
        contentVariant = contentColor.copy(.5f),
        error = scheme.error,
        onError = scheme.onError,
        segmentedControlBackground = scheme.secondaryContainer,
        onSegmentedControlBackground = scheme.onSecondaryContainer,
        segmentedControlIndicator = scheme.secondary,
        onSegmentedControlIndicator = scheme.onSecondary
    )
}

val LocalGlasenseColors = staticCompositionLocalOf { GlasenseLightPalette }

