package com.nevoit.cresto.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import com.nevoit.glasense.theme.tokens.Blue500
import com.nevoit.glasense.theme.tokens.Orange500
import com.nevoit.glasense.theme.tokens.Violet500

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