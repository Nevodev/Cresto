package com.nevoit.cresto.ui.components.glasense

import android.graphics.Paint
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousCapsule
import com.nevoit.cresto.ui.theme.glasense.AppColors
import com.nevoit.cresto.ui.theme.glasense.GlasenseColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

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
    colors: GlasenseColors = AppColors,
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

    val haptic = LocalHapticFeedback.current

    // Animate the track color based on checked and enabled states.
    val trackColorAnimation by animateColorAsState(
        targetValue = when {
            !enabled && checked -> colors.activeTrack.copy(.5f)
            !enabled && !checked -> colors.inactiveTrack.copy(.5f)
            checked -> colors.activeTrack
            else -> colors.inactiveTrack
        },
        animationSpec = tween(durationMillis = 200),
    )

    val thumbColorAnimation by animateColorAsState(
        targetValue = when {
            !enabled && checked -> colors.activeThumb.copy(.5f)
            !enabled && !checked -> colors.inactiveThumb.copy(.5f)
            checked -> colors.activeThumb
            else -> colors.inactiveThumb
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
                haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
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
                color = thumbColorAnimation,
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

@Composable
fun GlasenseSwitch(
    liquidGlass: Boolean = true,
    backgroundColor: Color,
    enabled: Boolean = true,
    checked: Boolean,
    colors: GlasenseColors = AppColors,
    onCheckedChange: (Boolean) -> Unit
) {
    if (liquidGlass) {
        val trackBackdrop = rememberLayerBackdrop() {
            drawRect(backgroundColor)
            drawContent()
        }

        val scope = rememberCoroutineScope()

        val density = LocalDensity.current
        val startPadding = with(density) { 3.dp.toPx() }
        val moveDistance = with(density) { 18.dp.toPx() }

        val haptic = LocalHapticFeedback.current

        val trackColorAnimation by animateColorAsState(
            targetValue = when {
                !enabled && checked -> colors.activeTrack.copy(.5f)
                !enabled && !checked -> colors.inactiveTrack.copy(.5f)
                checked -> colors.activeTrack
                else -> colors.inactiveTrack
            },
            animationSpec = tween(durationMillis = 200),
        )

        val thumbColorAnimation by animateColorAsState(
            targetValue = when {
                !enabled && checked -> colors.activeThumb.copy(.5f)
                !enabled && !checked -> colors.inactiveThumb.copy(.5f)
                checked -> colors.activeThumb
                else -> colors.inactiveThumb
            },
            animationSpec = tween(durationMillis = 200),
        )

        val currentChecked by rememberUpdatedState(checked)
        val currentOnCheckedChange by rememberUpdatedState(onCheckedChange)

        val physicsController = remember(scope) {
            object {
                val offsetAnim = Animatable(if (checked) moveDistance else 0f)
                val scaleAnim = Animatable(1f)

                val offsetSpec = spring<Float>(0.7f, 500f)
                val scaleSpec = spring<Float>(0.7f, 500f)

                val pressedScale = 1.7f

                var scaleJob: Job? = null

                var isInteractionInProgress = false

                val scaleProgress: Float
                    get() = ((1f - scaleAnim.value) / (1f - pressedScale)).coerceIn(0f, 1f)

                fun press() {
                    isInteractionInProgress = true

                    scaleJob?.cancel()
                    scaleJob = scope.launch {
                        scaleAnim.animateTo(pressedScale, scaleSpec)
                    }
                }

                fun toggle(newChecked: Boolean) {
                    isInteractionInProgress = true
                    val targetOffset = if (newChecked) moveDistance else 0f

                    scope.launch {
                        val threshold = 1f + (pressedScale - 1f) * 0.5f

                        if (scaleAnim.value < threshold) {
                            try {
                                kotlinx.coroutines.withTimeout(200) {
                                    snapshotFlow { scaleAnim.value }
                                        .filter { it >= threshold }
                                        .first()
                                }
                            } catch (e: Exception) {
                            }
                        }

                        offsetAnim.animateTo(targetOffset, offsetSpec)

                        isInteractionInProgress = false
                    }

                    scaleJob?.cancel()
                    scaleJob = scope.launch {
                        val holdPressJob = launch {
                            scaleAnim.animateTo(pressedScale, scaleSpec)
                        }

                        awaitFrame()
                        if (offsetAnim.value != targetOffset) {
                            snapshotFlow { offsetAnim.value }
                                .filter { abs(it - targetOffset) < (moveDistance * 0.1f) }
                                .first()
                        }

                        holdPressJob.cancel()
                        scaleAnim.animateTo(1f, scaleSpec)
                    }
                }

                fun cancel() {
                    isInteractionInProgress = false

                    scaleJob?.cancel()
                    scaleJob = scope.launch {
                        scaleAnim.animateTo(1f, scaleSpec)
                    }
                }

                fun sync(isChecked: Boolean) {
                    if (isInteractionInProgress) return

                    val target = if (isChecked) moveDistance else 0f
                    if (offsetAnim.targetValue != target) {
                        scope.launch { offsetAnim.animateTo(target, offsetSpec) }
                    }
                }
            }
        }

        LaunchedEffect(checked) {
            physicsController.sync(checked)
        }

        BoxWithConstraints(
            modifier = Modifier
                .height(28.dp)
                .width(46.dp)
                .pointerInput(enabled) {
                    if (enabled) {
                        detectTapGestures(
                            onPress = {
                                physicsController.press()

                                val success = tryAwaitRelease()

                                if (success) {
                                    haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                                    val newChecked = !currentChecked
                                    currentOnCheckedChange(newChecked)
                                    physicsController.toggle(newChecked)
                                } else {
                                    physicsController.cancel()
                                }
                            }
                        )
                    }
                },
        ) {
            val canvasWidth = constraints.maxWidth.toFloat()
            val canvasHeight = constraints.maxHeight.toFloat()
            Canvas(
                modifier = Modifier
                    .alpha(0f)
                    .layerBackdrop(trackBackdrop)
                    .fillMaxSize()
            ) {
                translate(left = startPadding, top = startPadding) {
                    drawTrack(
                        width = canvasWidth - startPadding * 2,
                        height = canvasHeight - startPadding * 2,
                        color = trackColorAnimation,
                        density = density
                    )
                }
            }
            Box(
                Modifier
                    .drawBehind {
                        drawTrack(
                            width = canvasWidth,
                            height = canvasHeight,
                            color = trackColorAnimation,
                            density = density
                        )
                    }
                    .graphicsLayer {
                        translationX = startPadding + physicsController.offsetAnim.value
                        translationY = startPadding
                    }
                    .drawBackdrop(
                        backdrop = trackBackdrop,
                        shape = { CircleShape },
                        effects = {
                            lens(
                                5f.dp.toPx(),
                                10f.dp.toPx(),
                                chromaticAberration = true
                            )
                        },
                        highlight = {
                            Highlight.Ambient.copy(
                                width = Highlight.Ambient.width / 1.5f,
                                blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                                alpha = physicsController.scaleProgress
                            )
                        },
                        shadow = {
                            Shadow(
                                radius = 4f.dp,
                                color = Color.Black.copy(.16f),
                                offset = DpOffset(0f.dp, 2f.dp)
                            )
                        },
                        innerShadow = {
                            InnerShadow(
                                radius = 4f.dp * physicsController.scaleProgress,
                                alpha = physicsController.scaleProgress
                            )
                        },
                        layerBlock = {
                            scaleX = physicsController.scaleAnim.value
                            scaleY = physicsController.scaleAnim.value
                        },
                        onDrawSurface = {
                            drawRect(thumbColorAnimation.copy(alpha = 1f - physicsController.scaleProgress))
                        }
                    )
                    .size(22.dp)
            )
        }
    } else {
        GlasenseSwitch(enabled = enabled, checked = checked, onCheckedChange = onCheckedChange)
    }
}