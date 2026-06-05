package com.nevoit.glasense.component

import android.graphics.Paint
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.kyant.shapes.Capsule
import com.nevoit.glasense.theme.LocalGlasenseColors
import androidx.compose.animation.Animatable as ColorAnimatable
import androidx.compose.animation.core.Animatable as FloatAnimatable

@Composable
fun Switch(
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Switch(
        enabled = enabled,
        interactionSource = interactionSource,
        checked = checked,
        onCheckedChange = onCheckedChange,
        disabledAlpha = 0.5f
    )
}

@Composable
internal fun Switch(
    enabled: Boolean,
    interactionSource: MutableInteractionSource,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    disabledAlpha: Float
) {
    val haptic = LocalHapticFeedback.current
    val colors = LocalGlasenseColors.current
    val currentOnCheckedChange = rememberUpdatedState(onCheckedChange)

    val targetTrackColor = when {
        !enabled && checked -> colors.activeTrack.copy(alpha = colors.activeTrack.alpha * disabledAlpha)
        !enabled -> colors.inactiveTrack.copy(alpha = colors.inactiveTrack.alpha * disabledAlpha)
        checked -> colors.activeTrack
        else -> colors.inactiveTrack
    }

    val targetThumbColor = when {
        !enabled && checked -> colors.activeThumb.copy(alpha = colors.activeThumb.alpha * disabledAlpha)
        !enabled -> colors.inactiveThumb.copy(alpha = colors.inactiveThumb.alpha * disabledAlpha)
        checked -> colors.activeThumb
        else -> colors.inactiveThumb
    }

    val thumbProgress = remember {
        FloatAnimatable(if (checked) 1f else 0f)
    }

    val trackColor = remember {
        ColorAnimatable(targetTrackColor)
    }

    val thumbColor = remember {
        ColorAnimatable(targetThumbColor)
    }

    LaunchedEffect(checked) {
        thumbProgress.animateTo(
            targetValue = if (checked) 1f else 0f,
            animationSpec = spring(
                dampingRatio = .7f,
                stiffness = 500f
            )
        )
    }

    LaunchedEffect(targetTrackColor) {
        trackColor.animateTo(
            targetValue = targetTrackColor,
            animationSpec = tween(durationMillis = 200)
        )
    }

    LaunchedEffect(targetThumbColor) {
        thumbColor.animateTo(
            targetValue = targetThumbColor,
            animationSpec = tween(durationMillis = 200)
        )
    }

    val capsule = remember { Capsule() }

    Box(
        modifier = Modifier
            .width(46.dp)
            .height(28.dp)
            .clickable(
                enabled = enabled,
                indication = null,
                interactionSource = interactionSource
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                currentOnCheckedChange.value(!checked)
            }
            .drawWithCache {
                val thumbRadius = 11.dp.toPx()
                val startPadding = 3.dp.toPx()
                val elevation = 4.dp.toPx()
                val moveDistance = 18.dp.toPx()

                val leftX = startPadding + thumbRadius
                val centerY = size.height / 2f

                val trackOutline = capsule.createOutline(
                    size = size,
                    layoutDirection = layoutDirection,
                    density = this
                )

                val thumbPaint = Paint().apply {
                    isAntiAlias = true
                    setShadowLayer(
                        elevation,
                        0f,
                        elevation / 2f,
                        Color.Black.copy(alpha = .16f).toArgb()
                    )
                }

                onDrawBehind {
                    drawOutline(
                        outline = trackOutline,
                        color = trackColor.value
                    )

                    drawContext.canvas.nativeCanvas.drawCircle(
                        leftX + moveDistance * thumbProgress.value,
                        centerY,
                        thumbRadius,
                        thumbPaint.apply {
                            color = thumbColor.value.toArgb()
                        }
                    )
                }
            }
    )
}
