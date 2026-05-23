package com.nevoit.glasense.theme

import android.util.Log
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

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

    // for bottom sheets
    val elevatedPageBackground: Color,
    val elevatedCardBackground: Color,

    val primary: Color,
    val onPrimary: Color,

    // main font colors
    val content: Color,
    val contentVariant: Color,

    val highlightText: Color,

    // error color
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
    elevatedPageBackground = Color(0xFFF3F4F6),
    elevatedCardBackground = Color.White,
    scrimLight = Color.Black.copy(alpha = 0.025f),
    scrimNormal = Color.Black.copy(alpha = 0.05f),
    scrimMedium = Color.Black.copy(alpha = 0.1f),
    scrimBold = Color.Black.copy(alpha = 0.2f),
    primary = Blue500,
    onPrimary = Color.White,
    content = Color.Black,
    contentVariant = Color.Black.copy(.5f),
    highlightText = Yellow500,
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
    elevatedPageBackground = Color(0xFF1C1C1E),
    elevatedCardBackground = Color(0xFF2C2C2E),
    scrimLight = Color.White.copy(alpha = 0.05f),
    scrimNormal = Color.White.copy(alpha = 0.1f),
    scrimMedium = Color.White.copy(alpha = 0.2f),
    scrimBold = Color.White.copy(alpha = 0.4f),
    primary = Blue500,
    onPrimary = Color.White,
    content = Color.White,
    contentVariant = Color.White.copy(.5f),
    highlightText = Yellow500,
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
        error = scheme.error.umamify(1.5f),
        onError = scheme.onError.umamify(1.5f),
        segmentedControlBackground = scheme.secondaryContainer,
        onSegmentedControlBackground = scheme.onSecondaryContainer,
        segmentedControlIndicator = scheme.secondary,
        onSegmentedControlIndicator = scheme.onSecondary
    )
}

val LocalGlasenseColors = staticCompositionLocalOf { GlasenseLightPalette }

data class OklchColor(
    val l: Float,
    val c: Float,
    val h: Float,
    val alpha: Float = 1f
) {
    fun toColor(): Color {
        val hRad = h * PI / 180.0

        val a = (c * cos(hRad)).toFloat()
        val b = (c * sin(hRad)).toFloat()

        return Color(
            colorSpace = ColorSpaces.Oklab,
            red = l,
            green = a,
            blue = b,
            alpha = alpha
        ).convert(ColorSpaces.Srgb)
    }
}

fun Color.toOklch(): OklchColor {
    val oklab = this.convert(ColorSpaces.Oklab)

    val l = oklab.component1()
    val a = oklab.component2()
    val b = oklab.component3()
    val alpha = oklab.alpha

    val c = hypot(a.toDouble(), b.toDouble()).toFloat()

    var h = (atan2(b.toDouble(), a.toDouble()) * 180.0 / PI).toFloat()
    if (h < 0f) {
        h += 360f
    }

    return OklchColor(l, c, h, alpha)
}

fun Color.purify(factor: Float = 1f): Color {
    val oklch = this.toOklch()

    val purifiedL = if (oklch.l == 1f) 1f else (oklch.l - 0.75f) * (1f - factor) + 0.75f
    val purifiedC = (oklch.c - 0.164f) * (1f - factor) + 0.164f

    return OklchColor(purifiedL, purifiedC, oklch.h, oklch.alpha).toColor()
}

fun Color.printOklch() {
    val oklch = this.toOklch()
    Log.d(
        "GlasenseColors",
        "Color: $this, Oklch: L=${oklch.l}, C=${oklch.c}, H=${oklch.h}, alpha=${oklch.alpha}"
    )
}

fun Color.umamify(factor: Float = 1f): Color {
    val oklch = this.toOklch()

    return OklchColor(oklch.l, oklch.c * factor, oklch.h, oklch.alpha).toColor()
}

fun Color.lumify(factor: Float = 1f): Color {
    val oklch = this.toOklch()

    return OklchColor((oklch.l * factor).coerceIn(0f, 1f), oklch.c, oklch.h, oklch.alpha).toColor()
}
