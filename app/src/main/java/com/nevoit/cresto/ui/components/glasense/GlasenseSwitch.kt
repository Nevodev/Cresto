package com.nevoit.cresto.ui.components.glasense

import android.graphics.Paint
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.kyant.capsule.ContinuousCapsule
import com.nevoit.cresto.ui.theme.glasense.Green500

/**
 * A custom switch with a glassmorphism-style design.
 *
 * @param enabled Controls the enabled state of the switch.
 * @param checked The current state of the switch.
 * @param onCheckedChange Callback for when the switch is toggled.
 */
@Composable
fun GlasenseSwitch(
    enabled: Boolean = true,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val density = LocalDensity.current
    val radius = with(density) { 11.dp.toPx() }
    val startPadding = with(density) { 3.dp.toPx() }
    val elevation = with(density) { 4.dp.toPx() }
    val leftX = startPadding + radius
    val moveDistance = with(density) { 18.dp.toPx() }
    // Animate the thumb's horizontal offset.
    val thumbOffsetAnimation by animateFloatAsState(
        targetValue = if (checked) moveDistance else 0f,
        animationSpec = spring(.7f, 500f)
    )

    // Animate the track color based on checked and enabled states.
    val trackColorAnimation by animateColorAsState(
        targetValue = when {
            !enabled && checked -> Green500.copy(.5f)
            !enabled && !checked -> Color(0xFF787880).copy(.12f)
            checked -> Green500
            else -> Color(0xFF787880).copy(.25f)
        },
        animationSpec = tween(durationMillis = 200),
    )

    // The container for the switch, handles clicks.
    BoxWithConstraints(
        modifier = Modifier
            .height(28.dp)
            .width(46.dp)
            .clickable(
                enabled = enabled,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                onCheckedChange(!checked)
            },
    ) {
        val canvasWidth = constraints.maxWidth.toFloat()
        val canvasHeight = constraints.maxHeight.toFloat()
        // Custom drawing for the switch.
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawTrack(
                width = canvasWidth,
                height = canvasHeight,
                color = trackColorAnimation,
                density = density
            )
            drawThumbWithShadow(
                centerX = leftX + thumbOffsetAnimation,
                centerY = leftX,
                radius = radius,
                color = Color.White,
                shadowColor = Color.Black.copy(.16f),
                elevation = elevation
            )
        }
    }
}

/**
 * Draws the switch thumb with a shadow using the native canvas.
 */
private fun DrawScope.drawThumbWithShadow(
    centerX: Float,
    centerY: Float,
    radius: Float,
    color: Color,
    shadowColor: Color,
    elevation: Float,
) {
    val shadowColorArgb = shadowColor.copy(alpha = shadowColor.alpha).toArgb()
    val colorArgb = color.toArgb()

    drawContext.canvas.nativeCanvas.apply {
        val paint = Paint().apply {
            isAntiAlias = true
        }

        paint.setShadowLayer(
            elevation,
            0f,
            elevation / 2,
            shadowColorArgb
        )

        paint.color = colorArgb
        drawCircle(
            centerX,
            centerY,
            radius,
            paint
        )
    }
}

/**
 * Draws the background track for the switch.
 */
private fun DrawScope.drawTrack(
    width: Float,
    height: Float,
    color: Color,
    density: Density
) {
    drawContext.canvas.nativeCanvas.apply {
        val size = Size(width, height)
        val outline = ContinuousCapsule.createOutline(
            size = size,
            layoutDirection = LayoutDirection.Ltr,
            density = density
        )
        drawOutline(
            outline = outline, color = color
        )
    }
}
