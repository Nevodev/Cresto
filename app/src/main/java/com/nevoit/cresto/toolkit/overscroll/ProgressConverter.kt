package com.nevoit.cresto.toolkit.overscroll

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sign

/**
 * A functional interface for converting a linear progress value into a non-linear one.
 * This is useful for creating custom easing effects for animations and gestures.
 */
fun interface ProgressConverter {

    /**
     * Converts the given linear progress value.
     *
     * @param progress The input progress, typically in the range [-1, 1] or [0, 1].
     * @return The converted, non-linear progress value.
     */
    fun convert(progress: Float, fraction: Float): Float

    companion object {

        /**
         * A default implementation that applies a non-linear, ease-out-like transformation.
         * The transformation becomes less sensitive as the absolute progress approaches its limit.
         */
        val Default: ProgressConverter =
            ProgressConverter { progress, fraction ->
                // Uses an exponential function to create a curve that changes quickly at the
                // beginning and more slowly as it approaches its maximum.
                (1f - exp(-abs(progress * fraction))) * progress.sign
            }
    }
}