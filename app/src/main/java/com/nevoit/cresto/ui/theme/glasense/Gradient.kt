package com.nevoit.cresto.ui.theme.glasense

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces

val easingBlackGradientMask = Brush.verticalGradient(
    colorStops = arrayOf(
        0.0f to Color.Black.copy(alpha = 0.00f),

        0.097f to Color.Black.copy(alpha = 0.01f),
        0.2021f to Color.Black.copy(alpha = 0.02f),
        0.3034f to Color.Black.copy(alpha = 0.03f),
        0.4005f to Color.Black.copy(alpha = 0.04f),
        0.4928f to Color.Black.copy(alpha = 0.05f),
        0.5799f to Color.Black.copy(alpha = 0.07f),
        0.6613f to Color.Black.copy(alpha = 0.08f),
        0.7363f to Color.Black.copy(alpha = 0.10f),
        0.8047f to Color.Black.copy(alpha = 0.12f),
        0.8657f to Color.Black.copy(alpha = 0.14f),
        0.9189f to Color.Black.copy(alpha = 0.16f),
        0.9639f to Color.Black.copy(alpha = 0.18f),
        1.0f to Color.Black.copy(alpha = 0.20f)
    )
)
val easingBlackGradientMaskInvert = Brush.verticalGradient(
    colorStops = arrayOf(
        0.0f to Color.Black.copy(alpha = 1f),

        0.0361f to Color.Black.copy(alpha = 0.9f),
        0.0811f to Color.Black.copy(alpha = 0.8f),
        0.1343f to Color.Black.copy(alpha = 0.7f),
        0.1953f to Color.Black.copy(alpha = 0.6f),
        0.2637f to Color.Black.copy(alpha = 0.5f),
        0.3387f to Color.Black.copy(alpha = 0.42f),
        0.4201f to Color.Black.copy(alpha = 0.33f),
        0.5072f to Color.Black.copy(alpha = 0.25f),
        0.5995f to Color.Black.copy(alpha = 0.2f),
        0.6966f to Color.Black.copy(alpha = 0.15f),
        0.7979f to Color.Black.copy(alpha = 0.1f),
        0.903f to Color.Black.copy(alpha = 0.05f),
        1.0f to Color.Black.copy(alpha = 0.00f)
    )
)

val linearGradientMaskT2B = Brush.verticalGradient(
    colorStops = arrayOf(
        0f to Color.Black,
        1f to Color.Black.copy(alpha = 0f)
    )
)

val linearGradientMaskT2B70 = Brush.verticalGradient(
    colorStops = arrayOf(
        0.7f to Color.Black,
        1f to Color.Black.copy(alpha = 0f)
    )
)

val linearGradientMaskB2T70 = Brush.verticalGradient(
    colorStops = arrayOf(
        0f to Color.Black.copy(alpha = 0f),
        0.3f to Color.Black
    )
)
val linearGradientMaskB2T90 = Brush.verticalGradient(
    colorStops = arrayOf(
        0f to Color.Black.copy(alpha = 0f),
        0.2f to Color.Black
    )
)

val gradientColorsDark =
    listOf(
        Blue500.adjustSaturationInOklab(1.2f).convert(ColorSpaces.Oklab)
            .copy(red = 0.65f, green = 0f),
        Blue500.adjustSaturationInOklab(1.2f).convert(ColorSpaces.Oklab)
            .copy(red = 0.65f, green = 0f),
        Violet500.adjustSaturationInOklab(1.5f).convert(ColorSpaces.Oklab)
            .copy(red = 0.9f, blue = -0.35f),
        Orange500.adjustSaturationInOklab(1.5f).convert(ColorSpaces.Oklab)
            .copy(red = 0.9f),
        Orange500.adjustSaturationInOklab(1.5f).convert(ColorSpaces.Oklab)
            .copy(red = 0.9f),
        Violet500.adjustSaturationInOklab(1.5f).convert(ColorSpaces.Oklab)
            .copy(red = 0.9f, blue = -0.35f),
        Blue500.adjustSaturationInOklab(1.2f).convert(ColorSpaces.Oklab)
            .copy(red = 0.65f, green = 0f),
        Blue500.adjustSaturationInOklab(1.2f).convert(ColorSpaces.Oklab)
            .copy(red = 0.65f, green = 0f),
    )

val gradientColorsLight =
    listOf(
        Blue500.adjustSaturationInOklab(1.2f).convert(ColorSpaces.Oklab)
            .copy(red = 0.8f, green = 0f),
        Blue500.adjustSaturationInOklab(1.2f).convert(ColorSpaces.Oklab)
            .copy(red = 0.8f, green = 0f),
        Violet500.adjustSaturationInOklab(1.5f).convert(ColorSpaces.Oklab)
            .copy(red = 0.9f, blue = -0.35f),
        Orange500.adjustSaturationInOklab(1.0f).convert(ColorSpaces.Oklab)
            .copy(red = 0.9f),
        Orange500.adjustSaturationInOklab(1.0f).convert(ColorSpaces.Oklab)
            .copy(red = 0.9f),
        Violet500.adjustSaturationInOklab(1.5f).convert(ColorSpaces.Oklab)
            .copy(red = 0.9f, blue = -0.35f),
        Blue500.adjustSaturationInOklab(1.2f).convert(ColorSpaces.Oklab)
            .copy(red = 0.8f, green = 0f),
        Blue500.adjustSaturationInOklab(1.2f).convert(ColorSpaces.Oklab)
            .copy(red = 0.8f, green = 0f),
    )


val highlightColorsDark =
    listOf(
        Color(0xFFA4A4A4).copy(alpha = 1f),
        Color(0xFFA4A4A4).copy(alpha = 0f),
        Color(0xFFA4A4A4).copy(alpha = 1f),
    )

val highlightColorsLight =
    listOf(
        Color(0xFFA4A4A4).copy(alpha = 0.7f),
        Color(0xFFA4A4A4).copy(alpha = 0f),
        Color(0xFFA4A4A4).copy(alpha = 0.7f),
    )