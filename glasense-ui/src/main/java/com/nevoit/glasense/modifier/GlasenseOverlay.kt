package com.nevoit.glasense.modifier

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
    Heavy,
    Float
}

fun Modifier.glasenseOverlay(
    visible: Boolean = true,
    dark: Boolean = false,
    weight: OverlayWeight = OverlayWeight.Float
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
    val heavy: List<LayerConfig>,
    val float: List<LayerConfig>
) {
    operator fun get(weight: OverlayWeight): List<LayerConfig> = when (weight) {
        OverlayWeight.Light -> light
        OverlayWeight.Normal -> normal
        OverlayWeight.Medium -> medium
        OverlayWeight.Bold -> bold
        OverlayWeight.Heavy -> heavy
        OverlayWeight.Float -> float
    }
}

private val LightLayers = LayerPalette(
    light = listOf(
        LayerConfig(Color(0xFF333333), 0.35f, BlendMode.Luminosity),
        LayerConfig(Color(0xFF666666), 0.60f, BlendMode.ColorDodge),
        LayerConfig(Color(0xFFFFFFFF), 0.20f, BlendMode.SrcOver)
    ),
    normal = listOf(
        LayerConfig(Color(0xFF6C6C6C), 0.70f, BlendMode.Luminosity),
        LayerConfig(Color(0xFF252525), 1.00f, BlendMode.Plus),
        LayerConfig(Color(0xFF555555), 0.50f, BlendMode.ColorDodge),
        LayerConfig(Color(0xFFFFFFFF), 0.30f, BlendMode.SrcOver)
    ),
    medium = listOf(
        LayerConfig(Color(0xFF888888), 0.70f, BlendMode.Luminosity),
        LayerConfig(Color(0xFF5F5F5F), 1.00f, BlendMode.ColorDodge),
        LayerConfig(Color(0xFF555555), 0.50f, BlendMode.ColorDodge),
        LayerConfig(Color(0xFFFFFFFF), 0.10f, BlendMode.SrcOver)
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
    ),
    float = listOf(
        LayerConfig(Color(0xFF888888), 0.70f, BlendMode.Luminosity),
        LayerConfig(Color(0xFF5F5F5F), 1.00f, BlendMode.ColorDodge),
        LayerConfig(Color(0xFF555555), 0.50f, BlendMode.ColorDodge),
        LayerConfig(Color(0xFFFFFFFF), 0.10f, BlendMode.SrcOver)
    )
)

private val DarkLayers = LayerPalette(
    light = listOf(
        LayerConfig(color = Color(0xFF333333), 0.35f, BlendMode.Luminosity),
        LayerConfig(color = Color(0xFF666666), 0.60f, BlendMode.ColorDodge),
        LayerConfig(color = Color(0xFF000000), 0.10f, BlendMode.Luminosity),
        LayerConfig(color = Color(0xFF000000), 0.20f, BlendMode.Overlay),
        LayerConfig(color = Color(0xFF000000), 0.10f, BlendMode.SrcOver)
    ),
    normal = listOf(
        LayerConfig(color = Color(0xFF1D1D1D), 0.60f, BlendMode.Luminosity),
        LayerConfig(color = Color(0xFF5F5F5F), 0.82f, BlendMode.ColorDodge),
        LayerConfig(color = Color(0xFF555555), 0.50f, BlendMode.ColorDodge),
        LayerConfig(color = Color(0xFFFFFFFF), 0.15f, BlendMode.Plus),
        LayerConfig(color = Color(0xFF000000), 0.45f, BlendMode.SrcOver)
    ),
    medium = listOf(
        LayerConfig(Color(0xFFFFFFFF), 0.10f, BlendMode.SrcOver),
        LayerConfig(Color(0xFF555555), 0.50f, BlendMode.ColorDodge),
        LayerConfig(Color(0xFF000000), 0.20f, BlendMode.Luminosity),
        LayerConfig(Color(0xFF000000), 0.20f, BlendMode.Overlay),
        LayerConfig(Color(0xFF595959), 0.40f, BlendMode.Luminosity)
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
    ),
    float = listOf(
        LayerConfig(Color(0xFFFFFFFF), 0.1f, BlendMode.SrcOver),
        LayerConfig(Color(0xFF555555), 0.50f, BlendMode.ColorDodge),
        LayerConfig(Color(0xFF000000), 0.50f, BlendMode.Luminosity),
        LayerConfig(Color(0xFF000000), 0.20f, BlendMode.Overlay),
        LayerConfig(Color(0xFF595959), 0.40f, BlendMode.Luminosity)
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