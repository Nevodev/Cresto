package com.nevoit.cresto.ui.modifier

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import com.nevoit.glasense.theme.Springs

/**
 * A modifier that adds a 3D tilt effect when the element is pressed,
 * similar to Windows 10 tiles.
 * 
 * This version uses [pointerInput] directly to ensure zero delay feedback,
 * even when placed inside scrollable containers.
 *
 * @param maxTilt The maximum tilt angle in degrees.
 * @param maxScale The scale factor when pressed.
 */
fun Modifier.tiltOnPress(
    maxTilt: Float = 20f,
    maxScale: Float = 0.98f
): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var tapOffset by remember { mutableStateOf(Offset.Zero) }

    val tiltX by animateFloatAsState(
        targetValue = if (isPressed && size.height > 0) {
            val centerY = size.height / 2f
            val deltaY = (tapOffset.y - centerY) / centerY
            -deltaY.coerceIn(-1f, 1f) * maxTilt
        } else 0f,
        animationSpec = if (isPressed) Springs.smooth(durationMillis = 300) else Springs.bouncy(
            extraBounce = 0.2
        ),
        label = "tiltX"
    )

    val tiltY by animateFloatAsState(
        targetValue = if (isPressed && size.width > 0) {
            val centerX = size.width / 2f
            val deltaX = (tapOffset.x - centerX) / centerX
            deltaX.coerceIn(-1f, 1f) * maxTilt
        } else 0f,
        animationSpec = if (isPressed) Springs.smooth(durationMillis = 300) else Springs.bouncy(
            extraBounce = 0.2
        ),
        label = "tiltY"
    )

    val scale by animateFloatAsState(
        targetValue = if (isPressed) maxScale else 1f,
        animationSpec = if (isPressed) Springs.smooth(
            durationMillis = 300,
            visibilityThreshold = 0.0000001f
        ) else Springs.bouncy(
            extraBounce = 0.2,
            visibilityThreshold = 0.0000001f
        ),
        label = "scale"
    )

    this
        .onGloballyPositioned { size = it.size }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull()
                    if (change != null) {
                        if (change.pressed) {
                            isPressed = true
                            tapOffset = change.position
                        } else {
                            isPressed = false
                        }
                    }
                }
            }
        }
        .graphicsLayer {
            rotationX = tiltX
            rotationY = tiltY
            scaleX = scale
            scaleY = scale
            cameraDistance = 12f * density
        }
}
