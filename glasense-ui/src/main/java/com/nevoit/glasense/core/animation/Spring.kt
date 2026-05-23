package com.nevoit.glasense.core.animation

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import kotlin.math.PI

fun <T> spring(
    durationMillis: Int = 500,
    bounce: Double = 0.0,
    visibilityThreshold: T? = null
): SpringSpec<T> {
    require(durationMillis > 0) { "Duration must be positive" }

    val safeBounce = bounce.coerceIn(-0.99999, 1.0)
    val duration = durationMillis / 1000.0

    val stiffness = (2.0 * PI / duration).let { it * it }.toFloat()

    val dampingRatio = if (safeBounce >= 0) {
        (1.0 - safeBounce).toFloat()
    } else {
        (1.0 / (1.0 + safeBounce)).toFloat()
    }

    return spring(
        dampingRatio = dampingRatio,
        stiffness = stiffness,
        visibilityThreshold = visibilityThreshold
    )
}