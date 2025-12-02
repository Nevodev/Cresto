package com.nevoit.cresto.ui.components.packed

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun CircularTimer(
    modifier: Modifier = Modifier,
    currentMinutes: Int,
    onMinutesChange: (Int) -> Unit,
    startIcon: Painter,
    endIcon: Painter,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    tickColor: Color = MaterialTheme.colorScheme.outlineVariant,
    knobSize: Dp,
    iconSize: Dp,
    strokeWidth: Dp,
    progressColor: Color,
    trackColor: Color,
    thumbWidth: Dp,
    iconColor: Color,
    contentColor: Color,
) {
    val scope = rememberCoroutineScope()

    var isDragging by remember { mutableStateOf(false) }
    val angleAnimatable = remember { Animatable(currentMinutes * 6f) }
    var currentSnapedMinute by remember { mutableIntStateOf(currentMinutes) }

    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val center = Offset(
                            (size.width / 2).toFloat(),
                            (size.height / 2).toFloat()
                        )
                        val dx = offset.x - center.x
                        val dy = offset.y - center.y
                        val distance = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                        val currentRadius =
                            (size.width.coerceAtMost(size.height) - strokeWidth.toPx()) / 2

                        val touchThreshold = (strokeWidth).toPx()

                        val isValidTouch = distance >= (currentRadius - touchThreshold) &&
                                distance <= (currentRadius + touchThreshold)

                        if (isValidTouch) {
                            isDragging = true
                        } else {
                            isDragging = false
                        }
                    },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onDrag = { change, _ ->
                        if (isDragging) {
                            val center =
                                Offset((size.width / 2).toFloat(), (size.height / 2).toFloat())
                            val touchPoint = change.position

                            val dx = touchPoint.x - center.x
                            val dy = touchPoint.y - center.y
                            var rawAngle =
                                Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()

                            rawAngle += 90f
                            if (rawAngle < 0) rawAngle += 360f

                            val targetMinute = (rawAngle / 6f).roundToInt().coerceIn(0, 60)

                            if (targetMinute != currentSnapedMinute) {

                                val targetAngle = targetMinute * 6f

                                scope.launch {
                                    val currentVisualAngle = angleAnimatable.value

                                    val diff = kotlin.math.abs(targetAngle - currentVisualAngle)

                                    if (diff > 180) {
                                        angleAnimatable.snapTo(targetAngle)
                                    } else {
                                        angleAnimatable.animateTo(
                                            targetValue = targetAngle,
                                            animationSpec = spring(
                                                stiffness = Spring.StiffnessMedium,
                                                dampingRatio = Spring.DampingRatioNoBouncy
                                            )
                                        )
                                    }
                                }

                                currentSnapedMinute = targetMinute
                                onMinutesChange(targetMinute)
                            }
                        }
                    }
                )
            }
    ) {
        val width = size.width
        val height = size.height
        val center = Offset(width / 2, height / 2)
        val radius = (width.coerceAtMost(height) - strokeWidth.toPx()) / 2
        drawCircle(
            color = trackColor,
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth.toPx())
        )
        drawArc(
            color = progressColor,
            startAngle = -90f,
            sweepAngle = angleAnimatable.value,
            useCenter = false,
            style = Stroke(width = thumbWidth.toPx(), cap = StrokeCap.Round),
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2)
        )

        for (i in 0 until 60) {
            val angleInDegrees = i * 6f - 90f
            val angleInRad = Math.toRadians(angleInDegrees.toDouble())

            val isMajorTick = i % 5 == 0
            val tickLength = 12.dp.toPx()
            val tickWidth = 2.dp.toPx()

            val isHighlighted = i * 6f <= angleAnimatable.value && currentMinutes != 0
            val color = if (isHighlighted) primaryColor.copy(.5f) else contentColor.copy(.1f)

            val lineStartRadius = radius + tickLength / 2
            val lineEndRadius = radius - tickLength / 2

            val startX = center.x + (lineStartRadius * cos(angleInRad)).toFloat()
            val startY = center.y + (lineStartRadius * sin(angleInRad)).toFloat()

            val endX = center.x + (lineEndRadius * cos(angleInRad)).toFloat()
            val endY = center.y + (lineEndRadius * sin(angleInRad)).toFloat()

            drawLine(
                color = color,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = tickWidth,
                cap = StrokeCap.Round
            )
        }

        val knobSize = knobSize.toPx()
        val iconSize = iconSize.toPx()

        drawKnobWithIcon(
            center = center,
            radius = radius,
            angleDegrees = 0f,
            knobColor = progressColor,
            iconPainter = startIcon,
            knobSize = knobSize,
            iconSize = iconSize,
            iconTint = iconColor
        )

        drawKnobWithIcon(
            center = center,
            radius = radius,
            angleDegrees = angleAnimatable.value,
            knobColor = progressColor,
            iconPainter = endIcon,
            knobSize = knobSize,
            iconSize = iconSize,
            iconTint = iconColor
        )
    }
}

// 辅助函数：绘制带图标的圆球
private fun DrawScope.drawKnobWithIcon(
    center: Offset,
    radius: Float,
    angleDegrees: Float,
    knobColor: Color,
    iconPainter: Painter,
    knobSize: Float,
    iconSize: Float,
    iconTint: Color
) {
    // 计算圆球中心位置
    // 注意：Canvas 0度是3点钟，所以我们需要减90度来匹配我们逻辑上的12点钟
    val angleRad = Math.toRadians((angleDegrees - 90).toDouble())
    val knobCx = center.x + (radius * cos(angleRad)).toFloat()
    val knobCy = center.y + (radius * sin(angleRad)).toFloat()

    // 绘制圆球背景
    drawCircle(
        color = knobColor,
        radius = knobSize / 2,
        center = Offset(knobCx, knobCy)
    )

    // 绘制图标
    // 我们需要将Canvas原点移动到圆球中心，绘制图标，然后再移回来
    translate(left = knobCx - iconSize / 2, top = knobCy - iconSize / 2) {
        with(iconPainter) {
            draw(
                size = Size(iconSize, iconSize),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(iconTint)
            )
        }
    }
}