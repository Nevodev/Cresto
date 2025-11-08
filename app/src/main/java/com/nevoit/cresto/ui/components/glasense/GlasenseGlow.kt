package com.nevoit.cresto.ui.components.glasense

import android.graphics.Matrix
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop

@Composable
fun RotatingGlow(
    modifier: Modifier,
    blurRadius: Dp,
    shape: Shape,
    colors: List<Color>,
    timeMillis: Int = 1000,
    backdrop: LayerBackdrop
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

    val paint = remember {
        android.graphics.Paint().apply { isAntiAlias = true }
    }
    val gradientMatrix = remember { Matrix() }

    Box(
        modifier = Modifier
            .blur(blurRadius, edgeTreatment = BlurredEdgeTreatment.Unbounded)
            .then(modifier)
            .layerBackdrop(backdrop)
            .drawBehind {
                val centerX = size.width / 2
                val centerY = size.height / 2

                gradientMatrix.setRotate(rotation, centerX, centerY)

                val sweepGradient = android.graphics.SweepGradient(
                    centerX,
                    centerY,
                    colors.map { it.toArgb() }.toIntArray(),
                    null
                )
                sweepGradient.setLocalMatrix(gradientMatrix)
                paint.shader = sweepGradient

                drawIntoCanvas { canvas ->
                    val outline =
                        shape.createOutline(
                            Size(
                                size.width,
                                size.height
                            ), layoutDirection, this
                        )

                    val path = when (outline) {
                        is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
                        is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
                        is Outline.Generic -> outline.path
                    }
                    canvas.nativeCanvas.drawPath(path.asAndroidPath(), paint)
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
    timeMillis: Int = 2000
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

    val strokeWidthPx = with(LocalDensity.current) { strokeWidth.toPx() }

    val paint = remember(strokeWidthPx) {
        android.graphics.Paint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.STROKE
            this.strokeWidth = strokeWidthPx
        }
    }

    val gradientMatrix = remember { Matrix() }

    Box(
        modifier = Modifier
            .graphicsLayer { blendMode = BlendMode.Plus }
            .blur(radius = blurRadius, edgeTreatment = BlurredEdgeTreatment.Unbounded)
            .then(modifier)
            .drawBehind {
                val centerX = size.width / 2
                val centerY = size.height / 2

                gradientMatrix.setRotate(rotation, centerX, centerY)
                val sweepGradient = android.graphics.SweepGradient(
                    centerX,
                    centerY,
                    colors.map { it.toArgb() }.toIntArray(),
                    null
                )
                sweepGradient.setLocalMatrix(gradientMatrix)
                paint.shader = sweepGradient
                drawIntoCanvas { canvas ->
                    val outline = shape.createOutline(size, layoutDirection, this)

                    val path = when (outline) {
                        is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
                        is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
                        is Outline.Generic -> outline.path
                    }
                    canvas.nativeCanvas.drawPath(path.asAndroidPath(), paint)
                }
            }
    )
}

