package com.nevoit.cresto.ui.components.glasense

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

enum class OverlayWeight {
    Light,
    Normal,
    Medium,
    Bold,
    Heavy
}

fun Modifier.glasenseOverlay(
    visible: Boolean = true,
    dark: Boolean = false,
    weight: OverlayWeight = OverlayWeight.Medium
): Modifier = drawWithContent {
    if (visible) {
        drawOverlay(dark, weight)
    }

    drawContent()
}

private data class LayerConfig(
    val color: Color,
    val alpha: Float,
    val blendMode: BlendMode = BlendMode.SrcOver
)

private data class LayerPalette(
    val light: List<LayerConfig>,
    val normal: List<LayerConfig>,
    val medium: List<LayerConfig>,
    val bold: List<LayerConfig>,
    val heavy: List<LayerConfig>
) {
    operator fun get(weight: OverlayWeight): List<LayerConfig> = when (weight) {
        OverlayWeight.Light -> light
        OverlayWeight.Normal -> normal
        OverlayWeight.Medium -> medium
        OverlayWeight.Bold -> bold
        OverlayWeight.Heavy -> heavy
    }
}

private val LightLayers = LayerPalette(
    light = listOf(
        LayerConfig(Color(0xFF333333), 0.35f, BlendMode.Luminosity),
        LayerConfig(Color(0xFF666666), 0.6f, BlendMode.ColorDodge),
        LayerConfig(Color(0xFFFFFFFF), 0.2f, BlendMode.SrcOver)
    ),
    normal = listOf(
        LayerConfig(Color(0xFF6C6C6C), 0.7f, BlendMode.Luminosity),
        LayerConfig(Color(0xFF252525), 1.0f, BlendMode.Plus),
        LayerConfig(Color(0xFF555555), 0.5f, BlendMode.ColorDodge),
        LayerConfig(Color(0xFFFFFFFF), 0.3f, BlendMode.SrcOver)
    ),
    medium = listOf(
        LayerConfig(Color(0xFF888888), 0.7f, BlendMode.Luminosity),
        LayerConfig(Color(0xFF5F5F5F), 1.0f, BlendMode.ColorDodge),
        LayerConfig(Color(0xFF555555), 0.5f, BlendMode.ColorDodge),
        LayerConfig(Color(0xFFFFFFFF), 0.1f, BlendMode.SrcOver)
    ),
    bold = listOf(
        LayerConfig(Color(0xFF333333), 0.35f, BlendMode.Luminosity),
        LayerConfig(Color(0xFF666666), 0.60f, BlendMode.ColorDodge),
        LayerConfig(Color(0xFFFFFFFF), 0.50f, BlendMode.SrcOver),
        LayerConfig(Color(0xFFFFFFFF), 0.25f, BlendMode.SrcOver),
        LayerConfig(Color(0xFF000000), 0.05f, BlendMode.Overlay)
    ),
    heavy = listOf(
        LayerConfig(Color(0xFF333333), 0.35f, BlendMode.Luminosity),
        LayerConfig(Color(0xFF666666), 0.60f, BlendMode.ColorDodge),
        LayerConfig(Color(0xFFFFFFFF), 0.50f, BlendMode.SrcOver),
        LayerConfig(Color(0xFFFFFFFF), 0.50f, BlendMode.SrcOver),
        LayerConfig(Color(0xFF000000), 0.02f, BlendMode.SrcOver)
    )
)

private val DarkLayers = LayerPalette(
    light = listOf(
        LayerConfig(color = Color(0xFF333333), alpha = 0.35f, blendMode = BlendMode.Luminosity),
        LayerConfig(color = Color(0xFF666666), alpha = 0.60f, blendMode = BlendMode.ColorDodge),
        LayerConfig(color = Color(0xFF000000), alpha = 0.10f, blendMode = BlendMode.Luminosity),
        LayerConfig(color = Color(0xFF000000), alpha = 0.20f, blendMode = BlendMode.Overlay),
        LayerConfig(color = Color(0xFF000000), alpha = 0.10f, blendMode = BlendMode.SrcOver)
    ),
    normal = listOf(
        LayerConfig(color = Color(0xFF1D1D1D), alpha = 0.60f, blendMode = BlendMode.Luminosity),
        LayerConfig(color = Color(0xFF5F5F5F), alpha = 0.82f, blendMode = BlendMode.ColorDodge),
        LayerConfig(color = Color(0xFF555555), alpha = 0.50f, blendMode = BlendMode.ColorDodge),
        LayerConfig(color = Color(0xFFFFFFFF), alpha = 0.15f, blendMode = BlendMode.Plus),
        LayerConfig(color = Color(0xFF000000), alpha = 0.45f, blendMode = BlendMode.SrcOver)
    ),
    medium = listOf(
        LayerConfig(Color(0xFFFFFFFF), 0.1f, BlendMode.SrcOver),
        LayerConfig(Color(0xFF555555), 0.5f, BlendMode.ColorDodge),
        LayerConfig(Color(0xFF000000), 0.2f, BlendMode.Luminosity),
        LayerConfig(Color(0xFF000000), 0.2f, BlendMode.Overlay),
        LayerConfig(Color(0xFF595959), 0.4f, BlendMode.Luminosity)
    ),
    bold = listOf(
        LayerConfig(Color(0xFF000000), 0.35f, BlendMode.Luminosity),
        LayerConfig(Color(0xFF666666), 0.60f, BlendMode.ColorDodge),
        LayerConfig(Color(0xFF000000), 0.50f, BlendMode.SrcOver),
        LayerConfig(Color(0xFF000000), 0.25f, BlendMode.SrcOver),
        LayerConfig(Color(0xFFFFFFFF), 0.10f, BlendMode.Plus)
    ),
    heavy = listOf(
        LayerConfig(Color(0xFF000000), 0.35f, BlendMode.Luminosity),
        LayerConfig(Color(0xFF666666), 0.60f, BlendMode.ColorDodge),
        LayerConfig(Color(0xFF000000), 0.50f, BlendMode.SrcOver),
        LayerConfig(Color(0xFF000000), 0.50f, BlendMode.SrcOver),
        LayerConfig(Color(0xFFFFFFFF), 0.05f, BlendMode.Plus)
    )
)

private fun DrawScope.drawOverlay(
    isDark: Boolean,
    weight: OverlayWeight
) {
    val palette = if (isDark) DarkLayers else LightLayers

    val layers = palette[weight]

    layers.forEach { config ->
        drawRect(
            color = config.color.copy(alpha = config.alpha),
            blendMode = config.blendMode
        )
    }
}