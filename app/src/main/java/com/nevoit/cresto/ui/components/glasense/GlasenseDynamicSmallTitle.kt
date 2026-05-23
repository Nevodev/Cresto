package com.nevoit.cresto.ui.components.glasense

import android.graphics.RenderEffect
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseInQuad
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
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
import com.nevoit.cresto.ui.components.CustomAnimatedVisibility
import com.nevoit.cresto.ui.components.myFadeIn
import com.nevoit.cresto.ui.components.myFadeOut
import com.nevoit.cresto.ui.components.myScaleIn
import com.nevoit.cresto.ui.components.myScaleOut
import com.nevoit.glasense.core.component.Text
import com.nevoit.glasense.theme.GlasenseTheme

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

    // Main container for the title bar and content.
    Box(
        modifier = modifier
            .height(48.dp + statusBarHeight + 48.dp)
            .fillMaxWidth()
    ) {
        // Animated background with haze and gradient effect.
        CustomAnimatedVisibility(
            visible = isVisible,
            enter = myFadeIn(),
            exit = myFadeOut()
        ) {
            Box(
                modifier = Modifier
                    .height(48.dp + statusBarHeight + 48.dp)
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
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
//                    .smoothGradientMask(
//                        surfaceColor.copy(alpha = 1f),
//                        surfaceColor.copy(alpha = 0f),
//                        0.5f,
//                        0.5f,
//                        0.7f
//                    )
            ) {}
        }
        // The primary content of the screen.
        content()
        // Animated title text.
        CustomAnimatedVisibility(
            visible = isVisible,
            enter = myScaleIn(
                tween(200, 0, CubicBezierEasing(0.2f, 0.2f, 0f, 1f)),
                0.9f
            ) + myFadeIn(tween(100)),
            exit = myScaleOut(
                tween(200, 0, EaseInQuad),
                0.9f
            ) + myFadeOut(tween(200)),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = statusBarHeight, bottom = 48.dp)
        ) {
            Text(
                title,
                style = GlasenseTheme.type.smallTitle.merge(textStyle),
                maxLines = 1,
                modifier = Modifier.padding(horizontal = titleHorizontalPadding),
                overflow = TextOverflow.Ellipsis,
            )
        }
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