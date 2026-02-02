package com.nevoit.cresto.ui.theme.glasense

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
    val scrimNormal: Color,
    val scrimBold: Color,

    // for hierarchical cards
    val pageBackground: Color,
    val cardBackground: Color,

    val primary: Color,
    val onPrimary: Color,

    val content: Color,
    val contentVariant: Color,

    val error: Color,
    val onError: Color
)

val GlasenseLightPalette = GlasenseColors(
    activeTrack = Green500,
    inactiveTrack = Color(0xFF787880).copy(.25f),
    activeThumb = Color.White,
    inactiveThumb = Color.White,
    pageBackground = Color(0xFFF3F4F6),
    cardBackground = Color.White,
    scrimNormal = Color.Black.copy(alpha = 0.05f),
    scrimBold = Color.Black.copy(alpha = 0.2f),
    primary = Blue500,
    onPrimary = Color.White,
    content = Color.Black,
    contentVariant = Color.Black.copy(.5f),
    error = Red500,
    onError = Color.White
)

val GlasenseDarkPalette = GlasenseColors(
    activeTrack = Green500,
    inactiveTrack = Color(0xFF787880).copy(.25f),
    activeThumb = Color.White,
    inactiveThumb = Color.White,
    pageBackground = Color.Black,
    cardBackground = Color(0xFF1B1C1D),
    scrimNormal = Color.White.copy(alpha = 0.1f),
    scrimBold = Color.White.copy(alpha = 0.4f),
    primary = Blue500,
    onPrimary = Color.White,
    content = Color.White,
    contentVariant = Color.White.copy(.5f),
    error = Red500,
    onError = Color.White
)

fun glasenseColorsFromScheme(scheme: ColorScheme, isDark: Boolean): GlasenseColors {
    val pageBackground = if (isDark) Color.Black else scheme.surfaceContainer
    val cardBackground = if (isDark) scheme.surfaceContainer else scheme.surface

    val contentColor = if (isDark) Color.White else Color.Black

    val scrimNormal = contentColor.copy(alpha = if (isDark) 0.1f else 0.05f)
    val scrimBold = contentColor.copy(alpha = if (isDark) 0.4f else 0.2f)

    return GlasenseColors(
        activeTrack = scheme.primary,
        inactiveTrack = scheme.surfaceContainerHighest,
        activeThumb = scheme.onPrimary,
        inactiveThumb = scheme.outline,
        pageBackground = pageBackground,
        cardBackground = cardBackground,
        scrimNormal = scrimNormal,
        scrimBold = scrimBold,
        primary = scheme.primary,
        onPrimary = scheme.onPrimary,
        content = contentColor,
        contentVariant = contentColor.copy(.5f),
        error = scheme.error,
        onError = scheme.onError
    )
}

val LocalGlasenseColors = staticCompositionLocalOf { GlasenseLightPalette }