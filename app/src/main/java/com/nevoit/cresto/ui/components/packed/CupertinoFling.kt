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
import kotlinx.coroutines.CancellationException
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToLong

private const val UIScrollViewDecelerationRateNormal = 0.998f
private const val SecondsToNanos: Long = 1_000_000_000L
private const val decayStopVelocityThreshold =
    50.0f //weird value but looks natural in practice, similar to the default threshold used by iOS

@Composable
fun rememberCupertinoDecaySpec(
    decelerationRate: Float = UIScrollViewDecelerationRateNormal
): DecayAnimationSpec<Float> {
    return remember(decelerationRate) {
        CupertinoScrollDecayAnimationSpec(decelerationRate).generateDecayAnimationSpec<Float>()
    }
}

@Composable
fun rememberCupertinoFlingBehavior(
    decelerationRate: Float = UIScrollViewDecelerationRateNormal
): FlingBehavior {
    val decaySpec = rememberCupertinoDecaySpec(decelerationRate)
    return remember(decaySpec) {
        CupertinoFlingBehavior(decaySpec)
    }
}

private class CupertinoFlingBehavior(
    private val flingDecay: DecayAnimationSpec<Float>,
    private val velocityThreshold: Float = 500f
) : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        if (abs(initialVelocity) < velocityThreshold) {
            return 0f
        }

        if (abs(initialVelocity) > 1f) {
            var velocityLeft = initialVelocity
            var lastValue = 0f

            AnimationState(
                initialValue = 0f,
                initialVelocity = initialVelocity,
            ).animateDecay(flingDecay) {
                val delta = value - lastValue
                val consumed = try {
                    scrollBy(delta)
                } catch (exception: CancellationException) {
                    0.0f
                }
                lastValue = value
                velocityLeft = this.velocity

                if (abs(delta - consumed) > 0.5f) {
                    this.cancelAnimation()
                }
            }
            return velocityLeft
        } else {
            return initialVelocity
        }
    }
}

private class CupertinoScrollDecayAnimationSpec(
    private val decelerationRate: Float = UIScrollViewDecelerationRateNormal
) : FloatDecayAnimationSpec {
    private val coefficient: Float = 1000f * ln(decelerationRate)

    override val absVelocityThreshold: Float = decayStopVelocityThreshold

    override fun getTargetValue(initialValue: Float, initialVelocity: Float): Float {
        val absVelocity = abs(initialVelocity)
        if (absVelocity <= absVelocityThreshold) {
            return initialValue
        }

        val targetVelocity = kotlin.math.sign(initialVelocity) * absVelocityThreshold

        return initialValue + (targetVelocity - initialVelocity) / coefficient
    }

    override fun getValueFromNanos(
        playTimeNanos: Long,
        initialValue: Float,
        initialVelocity: Float
    ): Float {
        val playTimeSeconds = convertNanosToSeconds(playTimeNanos).toFloat()
        val initialVelocityOverTimeIntegral =
            (decelerationRate.pow(1000f * playTimeSeconds) - 1f) / coefficient * initialVelocity
        return initialValue + initialVelocityOverTimeIntegral
    }

    override fun getDurationNanos(initialValue: Float, initialVelocity: Float): Long {
        val absVelocity = abs(initialVelocity)

        if (absVelocity < absVelocityThreshold) {
            return 0
        }

        val seconds = ln(absVelocityThreshold / absVelocity) / coefficient

        return convertSecondsToNanos(seconds)
    }

    override fun getVelocityFromNanos(
        playTimeNanos: Long,
        initialValue: Float,
        initialVelocity: Float
    ): Float {
        val playTimeSeconds = convertNanosToSeconds(playTimeNanos).toFloat()

        return initialVelocity * decelerationRate.pow(1000f * playTimeSeconds)
    }
}

private fun convertSecondsToNanos(seconds: Float): Long =
    (seconds.toDouble() * SecondsToNanos).roundToLong()

private fun convertNanosToSeconds(nanos: Long): Double =
    nanos.toDouble() / SecondsToNanos