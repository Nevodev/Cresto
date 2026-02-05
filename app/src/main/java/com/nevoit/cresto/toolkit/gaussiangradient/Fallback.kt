package com.nevoit.cresto.toolkit.gaussiangradient

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush.Companion.verticalGradient
import androidx.compose.ui.graphics.Color

fun Modifier.smoothGradientMaskFallbackInvert(color: Color, alpha: Float): Modifier {
    return this.background(
        verticalGradient(
            colorStops = arrayOf(
                0.0f to color.copy(alpha = 1f),

                0.0361f to color.copy(alpha = 0.9f),
                0.0811f to color.copy(alpha = 0.8f),
                0.1343f to color.copy(alpha = 0.7f),
                0.1953f to color.copy(alpha = 0.6f),
                0.2637f to color.copy(alpha = 0.5f),
                0.3387f to color.copy(alpha = 0.42f),
                0.4201f to color.copy(alpha = 0.33f),
                0.5072f to color.copy(alpha = 0.25f),
                0.5995f to color.copy(alpha = 0.2f),
                0.6966f to color.copy(alpha = 0.15f),
                0.7979f to color.copy(alpha = 0.1f),
                0.903f to color.copy(alpha = 0.05f),
                1.0f to color.copy(alpha = 0.00f)
            )
        ), alpha = alpha
    )
}

fun Modifier.smoothGradientMaskFallback(color: Color, alpha: Float): Modifier {
    return this.background(
        verticalGradient(
            colorStops = arrayOf(
                0.0f to color.copy(alpha = 0.00f),
                0.097f to color.copy(alpha = 0.01f),
                0.2021f to color.copy(alpha = 0.02f),
                0.3034f to color.copy(alpha = 0.03f),
                0.4005f to color.copy(alpha = 0.04f),
                0.4928f to color.copy(alpha = 0.05f),
                0.5799f to color.copy(alpha = 0.07f),
                0.6613f to color.copy(alpha = 0.08f),
                0.7363f to color.copy(alpha = 0.10f),
                0.8047f to color.copy(alpha = 0.12f),
                0.8657f to color.copy(alpha = 0.14f),
                0.9189f to color.copy(alpha = 0.16f),
                0.9639f to color.copy(alpha = 0.18f),
                1.0f to color.copy(alpha = 0.20f)
            )
        ), alpha = alpha
    )
}