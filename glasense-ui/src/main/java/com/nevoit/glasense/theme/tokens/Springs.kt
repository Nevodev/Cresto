package com.nevoit.glasense.theme.tokens

import androidx.compose.animation.core.SpringSpec
import com.nevoit.glasense.core.animation.spring

object Springs {
    fun <T> smooth(
        durationMillis: Int = 500,
        extraBounce: Double = 0.0,
        visibilityThreshold: T? = null
    ): SpringSpec<T> = spring(
        durationMillis = durationMillis,
        bounce = 0.0 + extraBounce,
        visibilityThreshold = visibilityThreshold
    )

    fun <T> crisp(
        durationMillis: Int = 300,
        extraBounce: Double = 0.0,
        visibilityThreshold: T? = null
    ): SpringSpec<T> = spring(
        durationMillis = durationMillis,
        bounce = 0.1 + extraBounce,
        visibilityThreshold = visibilityThreshold
    )

    fun <T> snappy(
        durationMillis: Int = 500,
        extraBounce: Double = 0.0,
        visibilityThreshold: T? = null
    ): SpringSpec<T> = spring(
        durationMillis = durationMillis,
        bounce = 0.15 + extraBounce,
        visibilityThreshold = visibilityThreshold
    )

    fun <T> bouncy(
        durationMillis: Int = 500,
        extraBounce: Double = 0.0,
        visibilityThreshold: T? = null
    ): SpringSpec<T> = spring(
        durationMillis = durationMillis,
        bounce = 0.3 + extraBounce,
        visibilityThreshold = visibilityThreshold
    )
}