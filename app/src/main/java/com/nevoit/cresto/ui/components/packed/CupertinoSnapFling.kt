package com.nevoit.cresto.ui.components.packed

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.math.abs
import kotlin.math.sign

private const val DEFAULT_MIN_FLING_VELOCITY = 1f
private const val DEFAULT_UNCONSUMED_DELTA_THRESHOLD = 0.5f

@Composable
fun rememberGlasenseSnapFlingBehavior(
    lazyListState: LazyListState,
    snapPosition: SnapPosition = SnapPosition.Center,
    decayAnimationSpec: DecayAnimationSpec<Float> = rememberCupertinoDecaySpec(),
    snapAnimationSpec: AnimationSpec<Float> = spring(stiffness = 200f),
    minFlingVelocity: Float = DEFAULT_MIN_FLING_VELOCITY,
    unconsumedDeltaThreshold: Float = DEFAULT_UNCONSUMED_DELTA_THRESHOLD,
): FlingBehavior {
    val provider = remember(lazyListState, snapPosition) {
        SnapLayoutInfoProvider(lazyListState = lazyListState, snapPosition = snapPosition)
    }
    return rememberGlasenseSnapFlingBehavior(
        snapLayoutInfoProvider = provider,
        decayAnimationSpec = decayAnimationSpec,
        snapAnimationSpec = snapAnimationSpec,
        minFlingVelocity = minFlingVelocity,
        unconsumedDeltaThreshold = unconsumedDeltaThreshold,
    )
}

@Composable
fun rememberGlasenseSnapFlingBehavior(
    snapLayoutInfoProvider: SnapLayoutInfoProvider,
    decayAnimationSpec: DecayAnimationSpec<Float> = rememberCupertinoDecaySpec(),
    snapAnimationSpec: AnimationSpec<Float> = spring(stiffness = 200f),
    minFlingVelocity: Float = DEFAULT_MIN_FLING_VELOCITY,
    unconsumedDeltaThreshold: Float = DEFAULT_UNCONSUMED_DELTA_THRESHOLD,
): FlingBehavior {
    return remember(
        snapLayoutInfoProvider,
        decayAnimationSpec,
        snapAnimationSpec,
        minFlingVelocity,
        unconsumedDeltaThreshold,
    ) {
        VelocityPreservingSnapFlingBehavior(
            snapLayoutInfoProvider = snapLayoutInfoProvider,
            decayAnimationSpec = decayAnimationSpec,
            snapAnimationSpec = snapAnimationSpec,
            minFlingVelocity = minFlingVelocity,
            unconsumedDeltaThreshold = unconsumedDeltaThreshold,
        )
    }
}

private class VelocityPreservingSnapFlingBehavior(
    private val snapLayoutInfoProvider: SnapLayoutInfoProvider,
    private val decayAnimationSpec: DecayAnimationSpec<Float>,
    private val snapAnimationSpec: AnimationSpec<Float>,
    private val minFlingVelocity: Float,
    private val unconsumedDeltaThreshold: Float,
) : FlingBehavior {

    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        var velocityLeft = initialVelocity

        val decayTarget = decayAnimationSpec.calculateTargetValue(
            initialValue = 0f,
            initialVelocity = initialVelocity
        )
        val approachOffset =
            snapLayoutInfoProvider.calculateApproachOffset(initialVelocity, decayTarget)

        if (!approachOffset.isNaN() && abs(approachOffset) > 0f && abs(initialVelocity) >= minFlingVelocity) {
            velocityLeft = runApproach(approachOffset, velocityLeft)
        }

        // For interrupted drags / very low release speed, force a settle-to-nearest snap.
        val snapVelocity = if (abs(velocityLeft) >= minFlingVelocity) velocityLeft else 0f
        val snapOffset = snapLayoutInfoProvider.calculateSnapOffset(snapVelocity)
        if (snapOffset.isNaN() || abs(snapOffset) <= 0f) {
            return velocityLeft
        }

        return runSnap(snapOffset, snapVelocity)
    }

    private suspend fun ScrollScope.runApproach(offset: Float, initialVelocity: Float): Float {
        var velocityLeft = initialVelocity
        var remainingOffset = offset
        var lastValue = 0f
        val offsetSign = sign(offset)

        val state = AnimationState(initialValue = 0f, initialVelocity = initialVelocity)

        state.animateDecay(decayAnimationSpec) {
            val delta = value - lastValue
            val consumed = scrollBy(delta)
            lastValue = value
            remainingOffset -= consumed
            velocityLeft = this.velocity

            val hitBound = abs(delta - consumed) > unconsumedDeltaThreshold
            val reachedApproach = offsetSign != 0f && sign(remainingOffset) != offsetSign
            if (hitBound || reachedApproach) {
                this.cancelAnimation()
            }
        }

        return velocityLeft
    }

    private suspend fun ScrollScope.runSnap(targetOffset: Float, initialVelocity: Float): Float {
        var velocityLeft = initialVelocity
        var lastValue = 0f
        val state = AnimationState(initialValue = 0f, initialVelocity = initialVelocity)

        state.animateTo(targetValue = targetOffset, animationSpec = snapAnimationSpec) {
            val delta = value - lastValue
            val consumed = scrollBy(delta)
            lastValue = value
            velocityLeft = this.velocity

            if (abs(delta - consumed) > unconsumedDeltaThreshold) {
                this.cancelAnimation()
            }
        }

        return velocityLeft
    }
}


