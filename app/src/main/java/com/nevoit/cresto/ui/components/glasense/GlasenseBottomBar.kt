package com.nevoit.cresto.ui.components.glasense

import android.graphics.RenderEffect
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.effect
import com.nevoit.cresto.theme.LocalGlasenseSettings
import com.nevoit.cresto.ui.components.CustomAnimatedVisibility
import com.nevoit.cresto.ui.components.myFadeIn
import com.nevoit.cresto.ui.components.myFadeOut


/**
 * It's designed to sit at the bottom of the screen, often behind navigation controls,
 * providing a blurred and gradient-masked background for its content.
 * The bar's visibility can be animated.
 *
 * @param modifier The modifier to be applied to the main container.
 * @param navigationBarHeight The height of the system navigation bar, used to correctly position the effect.
 * @param height The specific height of the bottom bar itself, excluding the navigation bar area.
 * @param isVisible Controls the visibility of the bar, with fade-in/fade-out animations.
 * @param backdrop Provide Backdrop for the blur effect. This should be the same backdrop used in the parent composable to ensure consistency.
 * @param surfaceColor The color of the surface that the gradient mask will fade to/from.
 * @param content The composable content to be displayed within the bottom bar area.
 */

@Composable
fun GlasenseBottomBar(
    modifier: Modifier,
    navigationBarHeight: Dp,
    height: Dp,
    isVisible: Boolean,
    backdrop: Backdrop,
    surfaceColor: Color,
    content: @Composable () -> Unit
) {
    val blur = !LocalGlasenseSettings.current.liteMode
    // Main container for the title bar and content.
    Box(
        modifier = modifier
            .height(navigationBarHeight + height)
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
                    .height(navigationBarHeight + height)
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
float invertedY = size.y - coord.y;
float blurAlpha = smoothstep(size.y, size.y * 0.4, invertedY);
float tintAlpha = smoothstep(size.y, size.y * 0.4, invertedY);
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
        }
        // The primary content of the screen.
        content()
    }
}
