package com.nevoit.cresto.ui.components.glasense

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.effect
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.shapes.Capsule
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.NavigationButtonActiveColors
import com.nevoit.cresto.theme.NavigationButtonNormalColors
import com.nevoit.cresto.theme.isAppInDarkTheme

const val AGSL_CODE = """
uniform shader image;
uniform vec4 curvePoints;
uniform float intensity;
uniform float saturation;
uniform float brightness;

vec3 rgb2hsl(vec3 color) {
    float fmin = min(min(color.r, color.g), color.b);
    float fmax = max(max(color.r, color.g), color.b);
    float delta = fmax - fmin;

    float h = 0.0;
    float s = 0.0;
    float l = (fmax + fmin) / 2.0;

    if (delta > 0.004) {
        s = l < 0.5 ? delta / (fmax + fmin) : delta / (2.0 - fmax - fmin);

        float deltaR = fmax - color.r;
        float deltaG = fmax - color.g;

        if (deltaR < 0.001) {
            h = (color.g - color.b) / delta + (color.g < color.b ? 6.0 : 0.0);
        } else if (deltaG < 0.001) {
            h = (color.b - color.r) / delta + 2.0;
        } else {
            h = (color.r - color.g) / delta + 4.0;
        }
        h /= 6.0;
    }

    return vec3(h, s, l);
}


vec3 hsl2rgb(vec3 hsl) {
    vec3 rgb = clamp(abs(mod(hsl.x * 6.0 + vec3(0.0, 4.0, 2.0), 6.0) - 3.0) - 1.0, 0.0, 1.0);
    return hsl.z + hsl.y * (rgb - 0.5) * (1.0 - abs(2.0 * hsl.z - 1.0));
}

float bezierBrightness(float t, vec4 p) {
    float u = 1.0 - t; float tt = t * t; float uu = u * u;
    return (uu * u * p.x) + (3.0 * uu * t * p.y) + (3.0 * u * tt * p.z) + (tt * t * p.w);
}

half4 main(float2 fragCoord) {
    half4 color = image.eval(fragCoord);

    vec3 hsl = rgb2hsl(color.rgb);

    hsl.z = clamp(bezierBrightness(hsl.z, curvePoints), 0.0, 1.0);
    hsl.y = clamp(hsl.y * saturation, 0.0, 1.0);
    hsl.z = clamp(hsl.z * (1 + brightness), 0.0, 1.0);

    half3 mappedColor = half3(hsl2rgb(hsl));

    color.rgb = mix(color.rgb, mappedColor, intensity);
    
    return color;
}
"""

@Immutable
data class MaterialRecipe(
    val p0: Float,
    val p1: Float,
    val p2: Float,
    val p3: Float,
    val intensity: Float,
    val saturation: Float,
    val brightness: Float
)

object Recipes {
    val RegularLight =
        MaterialRecipe(0.9f, 0.83f, 0.925f, 0.815f, 0.75f, 1.5f, 0.2f) // ORIGINALLY it was 0.1f
    val RegularDark = MaterialRecipe(0.16f, 0.26f, 0.1f, 0.1f, 0.75f, 1.5f, 0f)
    val BarLight =
        MaterialRecipe(0.8f, 0.9f, 1.1f, 0.825f, 0.75f, 1.1f, 0.15f) // ORIGINALLY it was 0.1f
    val BarDark = MaterialRecipe(0.23f, 0.52f, 0.27f, 0.255f, 0.75f, 2.0f, -0.1f)
}

/**
 * A custom navigation button with active and inactive states.
 *
 * @param modifier The modifier to be applied to the button.
 * @param isActive Whether the button is currently active.
 * @param onClick The callback to be invoked when the button is clicked.
 * @param backdrop The backdrop layer for the glassmorphism effect.
 * @param content The content to be displayed inside the button.
 */
@Composable
fun GlasenseNavigationButton(
    modifier: Modifier = Modifier,
    isActive: Boolean,
    onClick: () -> Unit,
    backdrop: LayerBackdrop,
    liquidGlass: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = isAppInDarkTheme()
    val tint = AppColors.primary
    // Modifier for drawing the button's background based on its state.

    val curve = if (darkTheme) Recipes.BarDark else Recipes.BarLight
    val shader = RuntimeShader(AGSL_CODE)

    shader.setFloatUniform("curvePoints", curve.p0, curve.p1, curve.p2, curve.p3)
    shader.setFloatUniform("intensity", curve.intensity)
    shader.setFloatUniform("saturation", curve.saturation)
    shader.setFloatUniform("brightness", curve.brightness)

    val renderEffect = RenderEffect.createRuntimeShaderEffect(
        shader,
        "image"
    ).asComposeRenderEffect()

    val finalModifier = if (isActive && !liquidGlass) {
        Modifier
            .fillMaxSize()
            .glasenseHighlight(100.dp)
    } else {
        Modifier
            .fillMaxSize()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { Capsule() },
                shadow = null,
                innerShadow = null,
                highlight = { if (liquidGlass) Highlight.Default else null },
                effects = {
                    padding = if (liquidGlass) 8f.dp.toPx() * 2 else 32f.dp.toPx() * 2
                    if (!isActive) effect(renderEffect)
                    blur(if (liquidGlass) 8f.dp.toPx() else 32f.dp.toPx(), TileMode.Clamp)
                    if (liquidGlass) lens(16f.dp.toPx(), 48f.dp.toPx())
                },
                onDrawSurface = {
                    if (liquidGlass && isActive) {
                        drawRect(tint, blendMode = BlendMode.Hue, alpha = .8f)
                        drawRect(tint.copy(alpha = 0.7f))
                    }
                }
            )
            .then(if (!liquidGlass) Modifier.glasenseHighlight(100.dp) else Modifier)
    }

    // The base button with shape, click handling, shadow, and colors.
    GlasenseButton(
        shape = Capsule(),
        onClick = onClick,
        modifier = modifier
            .fillMaxHeight()
            .dropShadow(
                Capsule(),
                Shadow(
                    radius = 24.dp,
                    color = Color.Black.copy(alpha = 0.08f),
                    spread = 0.dp,
                    offset = DpOffset(0.dp, 8.dp)
                )
            ),
        colors = if (isActive) NavigationButtonActiveColors.primary() else NavigationButtonNormalColors.primary(),
        animated = false
    ) {
        // Box to apply the background modifier and center the content.
        Box(modifier = finalModifier, contentAlignment = Alignment.Center) {
            content()
        }
    }
}
