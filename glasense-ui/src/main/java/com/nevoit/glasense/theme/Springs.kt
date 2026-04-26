package com.nevoit.glasense.theme

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

object Springs {
    fun <T> smooth(
        duration: Int = 500,
        extraBounce: Double = 0.0,
        visibilityThreshold: T? = null
    ): SpringSpec<T> = spring(
        durationMillis = duration,
        bounce = 0.0 + extraBounce,
        visibilityThreshold = visibilityThreshold
    )

    fun <T> crisp(
        duration: Int = 300,
        extraBounce: Double = 0.0,
        visibilityThreshold: T? = null
    ): SpringSpec<T> = spring(
        durationMillis = duration,
        bounce = 0.1 + extraBounce,
        visibilityThreshold = visibilityThreshold
    )

    fun <T> snappy(
        duration: Int = 500,
        extraBounce: Double = 0.0,
        visibilityThreshold: T? = null
    ): SpringSpec<T> = spring(
        durationMillis = duration,
        bounce = 0.15 + extraBounce,
        visibilityThreshold = visibilityThreshold
    )

    fun <T> bouncy(
        duration: Int = 500,
        extraBounce: Double = 0.0,
        visibilityThreshold: T? = null
    ): SpringSpec<T> = spring(
        durationMillis = duration,
        bounce = 0.3 + extraBounce,
        visibilityThreshold = visibilityThreshold
    )
}