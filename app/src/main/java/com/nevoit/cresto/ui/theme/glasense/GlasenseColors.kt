package com.nevoit.cresto.ui.theme.glasense

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class GlasenseColors(
    val activeTrack: Color,
    val inactiveTrack: Color,
    val activeThumb: Color,
    val inactiveThumb: Color
)

val GlasenseLightPalette = GlasenseColors(
    activeTrack = Green500,
    inactiveTrack = Color(0xFF787880).copy(.25f),
    activeThumb = Color.White,
    inactiveThumb = Color.White,
)

val GlasenseDarkPalette = GlasenseColors(
    activeTrack = Green500,
    inactiveTrack = Color(0xFF787880).copy(.25f),
    activeThumb = Color.White,
    inactiveThumb = Color.White,
)

fun glasenseColorsFromScheme(scheme: ColorScheme): GlasenseColors {
    return GlasenseColors(
        activeTrack = scheme.primary,
        inactiveTrack = scheme.surfaceContainerHighest,
        activeThumb = scheme.onPrimary,
        inactiveThumb = scheme.outline,
    )
}

val LocalGlasenseColors = staticCompositionLocalOf { GlasenseLightPalette }