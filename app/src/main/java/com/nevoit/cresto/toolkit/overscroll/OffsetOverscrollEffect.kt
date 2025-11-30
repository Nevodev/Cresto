package com.nevoit.cresto.toolkit.overscroll

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Velocity
import com.nevoit.cresto.toolkit.layout.singleRelativeLayoutWithLayer
import com.nevoit.cresto.util.ProgressConverter
import com.nevoit.cresto.util.SpaceVectorConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign


@Composable
fun rememberOffsetOverscrollEffect(
    orientation: Orientation,
    animationSpec: AnimationSpec<Float> = OffsetOverscrollEffect.DefaultAnimationSpec,
    progressConverter: ProgressConverter = ProgressConverter.Default
): OffsetOverscrollEffect {
    val animationScope = rememberCoroutineScope()
    return remember(orientation, animationScope, animationSpec, progressConverter) {
        OffsetOverscrollEffect(
            orientation = orientation,
            animationScope = animationScope,
            animationSpec = animationSpec,
            progressConverter = progressConverter
        )
    }
}

class OffsetOverscrollEffect(
    private val orientation: Orientation,
    private val animationScope: CoroutineScope,
    private val animationSpec: AnimationSpec<Float>,
    private val progressConverter: ProgressConverter = ProgressConverter.Default
) : OverscrollEffect, SpaceVectorConverter by SpaceVectorConverter(orientation) {

    private val overscrollOffsetAnimation =
        Animatable(0f, 0.5f)

    override val isInProgress: Boolean = false

    override fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset
    ): Offset {
        val deltaForAxis = delta.toFloat()

        // If we're currently overscrolled, and the user scrolls in the opposite direction, we need
        // to "relax" the overscroll by consuming some of the scroll delta to bring it back towards
        // zero.
        val currentOffset = overscrollOffsetAnimation.value
        val sameDirection = deltaForAxis.sign == currentOffset.sign
        val consumedByPreScroll =
            if (abs(currentOffset) > 0.5f && !sameDirection) {
                // The user has scrolled in the opposite direction.
                val prevOverscrollValue = currentOffset
                val newOverscrollValue = currentOffset + deltaForAxis
                if (sign(prevOverscrollValue) != sign(newOverscrollValue)) {
                    // Enough to completely cancel the overscroll. We snap the overscroll value
                    // back to zero and consume the corresponding amount of the scroll delta.
                    animationScope.launch {
                        overscrollOffsetAnimation.snapTo(0f)
                    }
                    -prevOverscrollValue
                } else {
                    // Not enough to cancel the overscroll. We update the overscroll value
                    // accordingly and consume the entire scroll delta.
                    animationScope.launch {
                        overscrollOffsetAnimation.snapTo(newOverscrollValue)
                    }
                    deltaForAxis
                }
            } else {
                0f
            }.toOffset()

        // After handling any overscroll relaxation, we pass the remaining scroll delta to the
        // standard scrolling logic.
        val leftForScroll = delta - consumedByPreScroll
        val consumedByScroll = performScroll(leftForScroll)
        val overscrollDelta = leftForScroll - consumedByScroll

        // If the user is dragging (not flinging), and there's any remaining scroll delta after the
        // standard scrolling logic has been applied, we add it to the overscroll.
        if (source == NestedScrollSource.UserInput && abs(overscrollDelta.toFloat()) > 0.5f) {
            animationScope.launch {
                overscrollOffsetAnimation.snapTo(currentOffset + overscrollDelta.toFloat())
            }
        }

        return consumedByPreScroll + consumedByScroll
    }

    override suspend fun applyToFling(
        velocity: Velocity,
        performFling: suspend (Velocity) -> Velocity
    ) {
        var unconsumed = velocity.toFloat()
        if (unconsumed > 0f) {
            if (overscrollOffsetAnimation.value == 0f) {
                var frameTimeNanos = 1000L / 60L * 1_000_000L
                animationScope.launch {
                    var start = System.currentTimeMillis()
                    awaitFrame()
                    frameTimeNanos = (System.currentTimeMillis() - start) * 1_000_000L
                    start = System.currentTimeMillis()
                    awaitFrame()
                    frameTimeNanos = (System.currentTimeMillis() - start) * 1_000_000L
                }

                unconsumed -= performFling(
                    when (orientation) {
                        Orientation.Horizontal -> Velocity(unconsumed, velocity.y)
                        Orientation.Vertical -> Velocity(velocity.x, unconsumed)
                    }
                ).toFloat()
                if (unconsumed != 0f) {
                    overscrollOffsetAnimation.snapTo(
                        animationSpec.vectorize(Float.VectorConverter).getValueFromNanos(
                            frameTimeNanos,
                            AnimationVector(0f),
                            AnimationVector(0f),
                            AnimationVector(unconsumed)
                        ).value
                    )
                    overscrollOffsetAnimation.animateTo(0f, animationSpec, unconsumed)
                }
            } else if (overscrollOffsetAnimation.value > 0f) {
                overscrollOffsetAnimation.animateTo(0f, animationSpec, unconsumed)
            } else {
                try {
                    overscrollOffsetAnimation.animateTo(0f, animationSpec, unconsumed) {
                        if (value <= 0f) {
                            unconsumed = this.velocity
                            animationScope.launch { snapTo(0f) }
                        }
                    }
                } finally {
                    unconsumed -= performFling(
                        when (orientation) {
                            Orientation.Horizontal -> Velocity(unconsumed, velocity.y)
                            Orientation.Vertical -> Velocity(velocity.x, unconsumed)
                        }
                    ).toFloat()
                    if (unconsumed != 0f) {
                        overscrollOffsetAnimation.animateTo(0f, animationSpec, unconsumed)
                    }
                }
            }
        } else if (unconsumed < 0f) {
            if (overscrollOffsetAnimation.value == 0f) {
                var frameTimeNanos = 1000L / 60L * 1_000_000L
                animationScope.launch {
                    var start = System.currentTimeMillis()
                    awaitFrame()
                    frameTimeNanos = (System.currentTimeMillis() - start) * 1_000_000L
                    start = System.currentTimeMillis()
                    awaitFrame()
                    frameTimeNanos = (System.currentTimeMillis() - start) * 1_000_000L
                }

                unconsumed -= performFling(
                    when (orientation) {
                        Orientation.Horizontal -> Velocity(unconsumed, velocity.y)
                        Orientation.Vertical -> Velocity(velocity.x, unconsumed)
                    }
                ).toFloat()
                if (unconsumed != 0f) {
                    overscrollOffsetAnimation.snapTo(
                        animationSpec.vectorize(Float.VectorConverter).getValueFromNanos(
                            frameTimeNanos,
                            AnimationVector(0f),
                            AnimationVector(0f),
                            AnimationVector(unconsumed)
                        ).value
                    )
                    overscrollOffsetAnimation.animateTo(0f, animationSpec, unconsumed)
                }
            } else if (overscrollOffsetAnimation.value < 0f) {
                overscrollOffsetAnimation.animateTo(0f, animationSpec, unconsumed)
            } else {
                try {
                    overscrollOffsetAnimation.animateTo(0f, animationSpec, unconsumed) {
                        if (value <= 0f) {
                            unconsumed = this.velocity
                            animationScope.launch { snapTo(0f) }
                        }
                    }
                } finally {
                    unconsumed -= performFling(
                        when (orientation) {
                            Orientation.Horizontal -> Velocity(unconsumed, velocity.y)
                            Orientation.Vertical -> Velocity(velocity.x, unconsumed)
                        }
                    ).toFloat()
                    if (unconsumed != 0f) {
                        overscrollOffsetAnimation.animateTo(0f, animationSpec, unconsumed)
                    }
                }
            }
        } else {
            performFling(velocity)
            if (overscrollOffsetAnimation.value != 0f) {
                overscrollOffsetAnimation.animateTo(0f, animationSpec)
            }
        }
    }

    override val node: DelegatableNode = object : LayoutModifierNode, Modifier.Node() {

        override val shouldAutoInvalidate: Boolean = false

        override fun MeasureScope.measure(
            measurable: Measurable,
            constraints: Constraints
        ): MeasureResult {
            val placeable = measurable.measure(constraints)
            val maxDistance = when (orientation) {
                Orientation.Horizontal -> constraints.maxWidth.toFloat()
                Orientation.Vertical -> constraints.maxHeight.toFloat()
            }
            return singleRelativeLayoutWithLayer(placeable) {
                val overscrollDistance = overscrollOffsetAnimation.value
                if (overscrollDistance != 0f) {
                    val offsetPx = computeOffset(overscrollDistance, maxDistance)
                    when (orientation) {
                        Orientation.Horizontal -> translationX = offsetPx
                        Orientation.Vertical -> translationY = offsetPx
                    }
                }
            }
        }
    }

    private fun computeOffset(overscrollDistance: Float, maxDistance: Float): Float {
        val progress = progressConverter.convert(overscrollDistance / maxDistance)
        return progress * maxDistance
    }

    internal companion object {

        val DefaultAnimationSpec = spring(1f, 150f, 0.5f)
    }
}
