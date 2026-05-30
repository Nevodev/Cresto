package com.nevoit.cresto.ui.components.glasense

import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.SweepGradient
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp

@Composable
fun RotatingGlow(
    modifier: Modifier,
    blurRadius: Dp,
    edgeTreatment: BlurredEdgeTreatment = BlurredEdgeTreatment.Unbounded,
    colors: List<Color>,
    timeMillis: Int = 1000
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shadow_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = timeMillis, easing = LinearEasing),
        ),
        label = "rotation_angle"
    )

    val colorsState = androidx.compose.runtime.rememberUpdatedState(colors)

    Box(
        modifier = Modifier
            .blur(blurRadius, edgeTreatment = edgeTreatment)
            .then(modifier)
            .drawWithCache {
                val currentColors = colorsState.value

                val centerX = size.width / 2f
                val centerY = size.height / 2f

                val sweepGradient = SweepGradient(
                    centerX,
                    centerY,
                    currentColors.map { it.toArgb() }.toIntArray(),
                    null
                )

                val gradientMatrix = Matrix()
                val paint = Paint().apply {
                    isAntiAlias = true
                }

                onDrawBehind {
                    gradientMatrix.setRotate(rotation, centerX, centerY)
                    sweepGradient.setLocalMatrix(gradientMatrix)
                    paint.shader = sweepGradient

                    drawContext.canvas.nativeCanvas.drawRect(
                        0f, 0f, size.width, size.height, paint
                    )
                }
            }
    )
}

@Composable
fun RotatingGlowBorder(
    modifier: Modifier = Modifier,
    strokeWidth: Dp,
    blurRadius: Dp,
    shape: Shape,
    colors: List<Color>,
    timeMillis: Int = 2000,
    blendMode: BlendMode = BlendMode.Plus,
    edgeTreatment: BlurredEdgeTreatment = BlurredEdgeTreatment.Unbounded
) {
    val infiniteTransition = rememberInfiniteTransition(label = "border_glow_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = timeMillis, easing = LinearEasing),
        ),
        label = "rotation_angle"
    )

    val shapeState = androidx.compose.runtime.rememberUpdatedState(shape)
    val colorsState = androidx.compose.runtime.rememberUpdatedState(colors)
    val strokeWidthState = androidx.compose.runtime.rememberUpdatedState(strokeWidth)

    Box(
        modifier = Modifier
            .graphicsLayer { this.blendMode = blendMode }
            .blur(radius = blurRadius, edgeTreatment = edgeTreatment)
            .then(modifier)
            .drawWithCache {
                val currentShape = shapeState.value
                val currentColors = colorsState.value
                val currentStrokeWidth = strokeWidthState.value

                val centerX = size.width / 2f
                val centerY = size.height / 2f

                val sweepGradient = SweepGradient(
                    centerX,
                    centerY,
                    currentColors.map { it.toArgb() }.toIntArray(),
                    null
                )

                val gradientMatrix = Matrix()
                val paint = Paint().apply {
                    isAntiAlias = true
                    style = Paint.Style.STROKE
                    this.strokeWidth = currentStrokeWidth.toPx()
                }

                val outline = currentShape.createOutline(size, layoutDirection, this)
                val path = when (outline) {
                    is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
                    is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
                    is Outline.Generic -> outline.path
                }.asAndroidPath()

                onDrawBehind {
                    gradientMatrix.setRotate(rotation, centerX, centerY)
                    sweepGradient.setLocalMatrix(gradientMatrix)
                    paint.shader = sweepGradient

                    drawContext.canvas.nativeCanvas.drawPath(path, paint)
                }
            }
    )
}
