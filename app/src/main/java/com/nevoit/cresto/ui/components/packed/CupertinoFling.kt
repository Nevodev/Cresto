package com.nevoit.cresto.ui.components.packed

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.FloatDecayAnimationSpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.generateDecayAnimationSpec
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToLong

private const val UIScrollViewDecelerationRateNormal = 0.998f
private const val UIScrollViewDecelerationRateFast = 0.990f
private const val ABS_VELOCITY_THRESHOLD = 0.5f //half a pixel
private const val SECONDS_TO_NANOS = 1_000_000_000L

@Composable
fun rememberCupertinoDecaySpec(
    decelerationRate: Float = UIScrollViewDecelerationRateNormal
): DecayAnimationSpec<Float> {
    return remember(decelerationRate) {
        CupertinoScrollDecayAnimationSpec(decelerationRate).generateDecayAnimationSpec<Float>()
    }
}

/**
 * Cupertino-like fling behavior for Android Compose so list inertia feels closer to iOS.
 */
@Composable
fun rememberCupertinoFlingBehavior(
    decelerationRate: Float = UIScrollViewDecelerationRateNormal
): FlingBehavior {
    val decaySpec = remember(decelerationRate) {
        CupertinoScrollDecayAnimationSpec(decelerationRate).generateDecayAnimationSpec<Float>()
    }

    return remember(decaySpec) {
        CupertinoLikeFlingBehavior(decaySpec)
    }
}

private class CupertinoLikeFlingBehavior(
    private val flingDecay: DecayAnimationSpec<Float>
) : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        if (abs(initialVelocity) <= 1f) {
            return initialVelocity
        }

        var velocityLeft = initialVelocity
        var lastValue = 0f

        val animationState = AnimationState(initialValue = 0f, initialVelocity = initialVelocity)

        try {
            animationState.animateDecay(flingDecay) {
                val delta = value - lastValue
                val consumed = scrollBy(delta)
                lastValue = value
                velocityLeft = this.velocity

                if (abs(delta - consumed) > ABS_VELOCITY_THRESHOLD) {
                    this.cancelAnimation()
                }
            }
        } catch (_: CancellationException) {
            velocityLeft = animationState.velocity
        }

        return velocityLeft
    }
}

private class CupertinoScrollDecayAnimationSpec(
    private val decelerationRate: Float
) : FloatDecayAnimationSpec {
    private val coefficient: Float = 1000f * ln(decelerationRate)

    override val absVelocityThreshold: Float = ABS_VELOCITY_THRESHOLD

    override fun getTargetValue(initialValue: Float, initialVelocity: Float): Float =
        initialValue - initialVelocity / coefficient

    override fun getValueFromNanos(
        playTimeNanos: Long,
        initialValue: Float,
        initialVelocity: Float
    ): Float {
        val playTimeSeconds = nanosToSeconds(playTimeNanos).toFloat()
        val velocityIntegral =
            (decelerationRate.pow(1000f * playTimeSeconds) - 1f) / coefficient * initialVelocity
        return initialValue + velocityIntegral
    }

    override fun getDurationNanos(initialValue: Float, initialVelocity: Float): Long {
        val velocity = abs(initialVelocity)
        if (velocity < absVelocityThreshold) {
            return 0
        }

        val seconds = ln(-coefficient * absVelocityThreshold / velocity) / coefficient
        return secondsToNanos(seconds)
    }

    override fun getVelocityFromNanos(
        playTimeNanos: Long,
        initialValue: Float,
        initialVelocity: Float
    ): Float {
        val playTimeSeconds = nanosToSeconds(playTimeNanos).toFloat()
        return initialVelocity * decelerationRate.pow(1000f * playTimeSeconds)
    }
}

private fun secondsToNanos(seconds: Float): Long =
    (seconds.toDouble() * SECONDS_TO_NANOS).roundToLong()

private fun nanosToSeconds(nanos: Long): Double =
    nanos.toDouble() / SECONDS_TO_NANOS

