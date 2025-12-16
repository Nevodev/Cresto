package com.nevoit.cresto.ui.components.glasense

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nevoit.cresto.toolkit.gaussiangradient.smoothGradientMask
import com.nevoit.cresto.toolkit.gaussiangradient.smoothGradientMaskFallbackInvert
import com.nevoit.cresto.ui.components.CustomAnimatedVisibility
import com.nevoit.cresto.ui.components.myFadeIn
import com.nevoit.cresto.ui.components.myFadeOut
import com.nevoit.cresto.ui.theme.glasense.linearGradientMaskB2T70
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect


/**
 * It's designed to sit at the bottom of the screen, often behind navigation controls,
 * providing a blurred and gradient-masked background for its content.
 * The bar's visibility can be animated.
 *
 * @param modifier The modifier to be applied to the main container.
 * @param navigationBarHeight The height of the system navigation bar, used to correctly position the effect.
 * @param height The specific height of the bottom bar itself, excluding the navigation bar area.
 * @param isVisible Controls the visibility of the bar, with fade-in/fade-out animations.
 * @param hazeState The state object from the Haze library to manage the blur effect.
 * @param surfaceColor The color of the surface that the gradient mask will fade to/from.
 * @param content The composable content to be displayed within the bottom bar area.
 */
@OptIn(ExperimentalHazeApi::class)
@Composable
fun GlasenseBottomBar(
    modifier: Modifier,
    navigationBarHeight: Dp,
    height: Dp,
    isVisible: Boolean,
    hazeState: HazeState,
    surfaceColor: Color,
    content: @Composable () -> Unit
) {
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
                    .hazeEffect(hazeState) {
                        blurRadius = 4.dp
                        noiseFactor = 0f
                        inputScale = HazeInputScale.Fixed(0.5f)
                        mask = linearGradientMaskB2T70
                    }
                    .then(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Modifier.smoothGradientMask(
                            surfaceColor.copy(alpha = 0f),
                            surfaceColor.copy(alpha = 1f),
                            0f,
                            0.8f,
                            0.7f
                        ) else Modifier
                            .graphicsLayer { rotationZ = 180f }
                            .smoothGradientMaskFallbackInvert(surfaceColor, 0.7f)
                    )
            ) {}
        }
        // The primary content of the screen.
        content()
    }
}
