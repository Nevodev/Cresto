package com.nevoit.glasense.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import com.kyant.shapes.Capsule
import com.nevoit.glasense.theme.GlasenseTheme
import com.nevoit.glasense.theme.tokens.Springs

private const val TrackAlpha = 0.3f

enum class ProgressViewStyle {
    Circular,
    Linear
}

@Composable
fun ProgressView(
    modifier: Modifier = Modifier,
    dark: Boolean = !GlasenseTheme.darkTheme
) {
    IndeterminateProgressView(
        modifier = modifier,
        dark = dark
    )
}

@Composable
fun ProgressView(
    modifier: Modifier = Modifier,
    value: Float,
    progressViewStyle: ProgressViewStyle = ProgressViewStyle.Linear,
    color: Color = GlasenseTheme.colors.primary
) {
    when (progressViewStyle) {
        ProgressViewStyle.Linear -> LinearDeterminateProgressView(
            modifier = modifier,
            value = value,
            color = color
        )

        ProgressViewStyle.Circular -> CircularDeterminateProgressView(
            modifier = modifier,
            value = value,
            color = color
        )
    }
}

@Composable
internal fun IndeterminateProgressView(
    modifier: Modifier = Modifier,
    dark: Boolean,
    durationMillis: Int = 1000,
    tickShape: Shape = Capsule()
) {
    val color = when (dark) {
        true -> Color.Black.copy(.6f)
        false -> Color.White.copy(.8f)
    }

    val tickCount = 8
    val delayPerTick = durationMillis / tickCount

    val transition = rememberInfiniteTransition(label = "GlasenseLoadingTransition")

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
            label = "TickAlpha$index"
        )
    }

    Canvas(
        modifier = modifier
            .size(32.dp)
    ) {
        val radius = this.size.minDimension / 2

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

@Composable
internal fun LinearDeterminateProgressView(
    modifier: Modifier = Modifier,
    value: Float,
    color: Color
) {
    val targetProgress = value.coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = Springs.smooth(),
        label = "GlasenseLinearProgressAnimation",
        visibilityThreshold = 0.00001f
    )

    Canvas(
        modifier = Modifier
            .height(6.dp)
            .defaultMinSize(minWidth = 64.dp)
            .then(modifier)
    ) {
        val trackOutline = Capsule().createOutline(size, layoutDirection, this)
        val progressOutline = Capsule().createOutline(
            Size(width = size.width * animatedProgress, height = size.height),
            layoutDirection,
            this
        )

        drawOutline(outline = trackOutline, color = color.copy(alpha = TrackAlpha))
        drawOutline(outline = progressOutline, color = color)
    }
}

@Composable
internal fun CircularDeterminateProgressView(
    modifier: Modifier = Modifier,
    value: Float,
    color: Color
) {
    val targetProgress = value.coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = Springs.smooth(),
        label = "GlasenseCircularProgressAnimation",
        visibilityThreshold = 0.00001f
    )

    Canvas(
        modifier = modifier
            .size(32.dp)
    ) {
        val strokeWidth = size.minDimension * 0.125f

        val arcSize = size.minDimension - strokeWidth
        val topLeftOffset = Offset(
            x = (size.width - arcSize) / 2f,
            y = (size.height - arcSize) / 2f
        )

        drawCircle(
            color = color.copy(alpha = TrackAlpha),
            radius = arcSize / 2f,
            center = center,
            style = Stroke(width = strokeWidth)
        )

        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * animatedProgress,
            useCenter = false,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round
            ),
            size = Size(width = arcSize, height = arcSize),
            topLeft = topLeftOffset
        )
    }
}
