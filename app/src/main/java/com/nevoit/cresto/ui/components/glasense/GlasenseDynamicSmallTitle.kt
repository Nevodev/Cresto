package com.nevoit.cresto.ui.components.glasense

import android.graphics.RenderEffect
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.effect
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.LocalGlasenseSettings
import com.nevoit.glasense.core.component.Text
import com.nevoit.glasense.theme.GlasenseTheme
import com.nevoit.glasense.theme.tokens.Springs

/**
 * A dynamic small title bar that appears with an animation.
 * It includes a background with a haze effect and a gradient mask.
 *
 * @param modifier The modifier to be applied to the container.
 * @param title The text to display as the title.
 * @param statusBarHeight The height of the system status bar.
 * @param isVisible Whether the title should be visible.
 * @param backdrop For blur effect.
 * @param surfaceColor The color of the surface behind the title.
 * @param content The main content to be displayed below the title bar.
 */
@Composable
fun GlasenseDynamicSmallTitle(
    modifier: Modifier,
    title: String,
    textStyle: TextStyle = TextStyle(),
    statusBarHeight: Dp,
    isVisible: Boolean,
    backdrop: Backdrop,
    surfaceColor: Color = AppColors.pageBackground,
    titleHorizontalPadding: Dp = 80.dp,
    content: @Composable () -> Unit
) {
    val blur = !LocalGlasenseSettings.current.liteMode

    val alpha =
        animateFloatAsState(targetValue = if (isVisible) 1f else 0f, animationSpec = tween(300))

    val textAlpha = animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(if (isVisible) 300 else 200)
    )
    val scale = animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.8f,
        animationSpec = Springs.smooth(if (isVisible) 300 else 400)
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                this.alpha = alpha.value
            }
            .height(48.dp + statusBarHeight + 48.dp)
            .fillMaxWidth()
            .drawPlainBackdrop(
                backdrop = backdrop,
                shape = { RectangleShape },
                effects = {
                    if (blur) blur(3f.dp.toPx())
                    effect(
                        RenderEffect.createRuntimeShaderEffect(
                            obtainRuntimeShader(
                                "AlphaMask",
                                """
uniform shader content;

uniform float2 size;
layout(color) uniform half4 tint;
uniform float tintIntensity;

half4 main(float2 coord) {
float blurAlpha = smoothstep(size.y, size.y * 0.7, coord.y);
float tintAlpha = smoothstep(size.y, size.y * 0.6, coord.y);
return mix(content.eval(coord) * blurAlpha, tint * tintAlpha, tintIntensity);
}"""
                            ).apply {
                                setFloatUniform("size", size.width, size.height)
                                setColorUniform("tint", surfaceColor.toArgb())
                                setFloatUniform("tintIntensity", 0.7f)
                            },
                            "content"
                        )
                    )
                }
            )
    ) {}
    Box(
        modifier = modifier
            .statusBarsPadding()
            .height(48.dp)
            .fillMaxWidth()
    ) {
        content()

        Text(
            title,
            style = GlasenseTheme.type.smallTitle.merge(textStyle),
            maxLines = 1,
            modifier = Modifier
                .padding(horizontal = titleHorizontalPadding)
                .align(Alignment.Center)
                .graphicsLayer {
                    this.scaleX = scale.value
                    this.scaleY = scale.value
                    this.alpha = textAlpha.value
                },
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun LazyListState.isScrolledPast(threshold: Dp): androidx.compose.runtime.State<Boolean> {
    val density = LocalDensity.current

    val thresholdPx = remember(threshold, density) {
        with(density) { threshold.toPx() }
    }

    return remember(this, thresholdPx) {
        derivedStateOf {
            firstVisibleItemIndex > 0 || firstVisibleItemScrollOffset > thresholdPx
        }
    }
}