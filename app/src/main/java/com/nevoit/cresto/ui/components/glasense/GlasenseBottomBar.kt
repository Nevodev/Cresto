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
import com.nevoit.cresto.ui.components.CustomAnimatedVisibility
import com.nevoit.cresto.ui.components.myFadeIn
import com.nevoit.cresto.ui.components.myFadeOut
import com.nevoit.cresto.ui.gaussiangradient.smoothGradientMask
import com.nevoit.cresto.ui.gaussiangradient.smoothGradientMaskFallbackInvert
import com.nevoit.cresto.ui.theme.glasense.linearGradientMaskB2T70
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect

/**
 * A dynamic small title bar that appears with an animation.
 * It includes a background with a haze effect and a gradient mask.
 *
 * @param modifier The modifier to be applied to the container.
 * @param title The text to display as the title.
 * @param statusBarHeight The height of the system status bar.
 * @param isVisible Whether the title should be visible.
 * @param hazeState The state for the haze effect.
 * @param surfaceColor The color of the surface behind the title.
 * @param content The main content to be displayed below the title bar.
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
                        blurRadius = 2.dp
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
