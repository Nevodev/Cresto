package com.nevoit.cresto.ui.components.glasense

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import com.kyant.capsule.ContinuousCapsule
import com.nevoit.cresto.ui.theme.glasense.isAppInDarkTheme

@Composable
fun GlasenseLoadingIndicator(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 32.dp,
    durationMillis: Int = 1000,
    tickShape: Shape = ContinuousCapsule
) {
    val color = when (isAppInDarkTheme()) {
        true -> Color.White.copy(.8f)
        false -> Color.Black.copy(.6f)
    }
    val tickCount = 8
    val delayPerTick = durationMillis / tickCount

    val transition = rememberInfiniteTransition(label = "IOSLoading")

    val alphaValues = List(tickCount) { index ->
        transition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.3f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    this.durationMillis = durationMillis
                    0.3f at 0 using LinearEasing
                    1.0f at (durationMillis * 0.1).toInt() using LinearEasing
                    0.3f at (durationMillis * 0.5).toInt() using LinearEasing
                    0.3f at durationMillis
                },
                repeatMode = RepeatMode.Restart,
                initialStartOffset = StartOffset(
                    offsetMillis = index * delayPerTick,
                    offsetType = StartOffsetType.FastForward
                )
            ),
            label = "Alpha$index"
        )
    }
    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .size(size)
                .align(Alignment.Center)
        ) {
            val radius = size.toPx() / 2

            val tickLength = radius * 0.6f
            val tickWidth = radius * 0.25f

            val innerRadius = radius - tickLength

            val adjustedTickSize = Size(tickLength, tickWidth)
            val outline = tickShape.createOutline(adjustedTickSize, layoutDirection, this)

            for (i in 0 until tickCount) {
                val alpha = alphaValues[i].value
                val angleDegrees = -(360f / tickCount) * i - 90f
                rotate(degrees = angleDegrees, pivot = center) {
                    translate(
                        left = center.x + innerRadius,
                        top = center.y - tickWidth / 2
                    ) {
                        drawOutline(
                            outline = outline,
                            color = color,
                            alpha = alpha,
                        )
                    }
                }
            }
        }
    }

}