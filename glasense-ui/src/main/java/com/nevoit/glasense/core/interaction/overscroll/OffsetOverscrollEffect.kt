package com.nevoit.glasense.core.interaction.overscroll

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.spring
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.util.fastRoundToInt
import com.nevoit.glasense.core.interaction.overscroll.util.NoOpShape
import com.nevoit.glasense.core.interaction.overscroll.util.ProgressConverter
import com.nevoit.glasense.core.interaction.overscroll.util.singleRelativeLayoutWithLayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

class OffsetOverscrollEffect(
    private val animationScope: CoroutineScope,
    private val animationSpec: AnimationSpec<Float>,
    private val maxFraction: Float,
) : OverscrollEffect {

    private var offset by mutableStateOf(Offset.Zero)
    private var axis = Axis.None
    private var springJob: Job? = null
    private var flingToScrollOffset = Offset.Zero

    override val isInProgress: Boolean = false

    override fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset
    ): Offset {
        if (axis == Axis.None) {
            axis = if (abs(delta.y) >= abs(delta.x)) Axis.Vertical else Axis.Horizontal
        }

        val isUserInput = source == NestedScrollSource.UserInput
        if (isUserInput) {
            springJob?.cancel()
            springJob = null
        }

        var unconsumed = delta + flingToScrollOffset
        flingToScrollOffset = Offset.Zero

        if (offset != Offset.Zero) {
            unconsumed -= consumeOffset(unconsumed)
            if (unconsumed == Offset.Zero) return delta
        }

        unconsumed -= performScroll(unconsumed)
        if (unconsumed == Offset.Zero) return delta

        if (source == NestedScrollSource.UserInput) {
            unconsumed -= consumeOffset(unconsumed)
        }
//        else {
//            consumeOffset(unconsumed)
//        }

        return delta - unconsumed
    }

    override suspend fun applyToFling(
        velocity: Velocity,
        performFling: suspend (Velocity) -> Velocity
    ) {
        var unconsumed = velocity

        if (offset != Offset.Zero) {
            springJob = CoroutineScope(currentCoroutineContext()).launch {
                AnimationState(offset.toFloat(), unconsumed.toFloat())
                    .animateTo(0f, animationSpec) {
                        unconsumed = unconsumed.copyWith(this.velocity)
                        var unconsumedOffset = value.toOffset() - offset
                        unconsumedOffset -= consumeOffset(unconsumedOffset)
                        if (offset.toFloat() == 0f) {
                            flingToScrollOffset = unconsumedOffset
                            cancelAnimation()
                        }
                    }
            }
            springJob?.join()
            springJob = null
        }

        var frameTimeNanos = 1000L / 60L * 1_000_000L
        if (offset.toFloat() == 0f && unconsumed.toFloat() != 0f) {
            animationScope.launch {
                var start = System.currentTimeMillis()
                awaitFrame()
                frameTimeNanos = (System.currentTimeMillis() - start) * 1_000_000L
                start = System.currentTimeMillis()
                awaitFrame()
                frameTimeNanos = (System.currentTimeMillis() - start) * 1_000_000L
            }
        }

        unconsumed -= performFling(unconsumed)

        if (unconsumed.toFloat() != 0f) {
            springJob = CoroutineScope(currentCoroutineContext()).launch {
                if (offset.toFloat() == 0f) {
                    offset = animationSpec.vectorize(Float.VectorConverter).getValueFromNanos(
                        frameTimeNanos,
                        AnimationVector(0f),
                        AnimationVector(0f),
                        AnimationVector(unconsumed.toFloat())
                    ).value.toOffset()
                }
                AnimationState(offset.toFloat(), unconsumed.toFloat())
                    .animateTo(0f, animationSpec) {
                        offset = offset.copyWith(value)
                    }
            }
            springJob?.join()
            springJob = null
        }
    }

    override val node: DelegatableNode = object : LayoutModifierNode, Modifier.Node() {

        override val shouldAutoInvalidate: Boolean = false

        override fun MeasureScope.measure(
            measurable: Measurable,
            constraints: Constraints
        ): MeasureResult {
            val placeable = measurable.measure(constraints)
            val maxWidth = constraints.maxWidth.toFloat()
            val maxHeight = constraints.maxHeight.toFloat()

            return singleRelativeLayoutWithLayer(placeable) {
                shape = NoOpShape

                val currentAxis = axis
                val maxDistance = when (currentAxis) {
                    Axis.Horizontal -> maxWidth
                    Axis.Vertical -> maxHeight
                    Axis.None -> 0f
                }

                val overscrollDistance = offset.toFloat()
                if (currentAxis != Axis.None && maxDistance > 0f && overscrollDistance != 0f) {
                    val offsetPx = computeOffset(overscrollDistance, maxDistance).fastRoundToInt()
                    when (currentAxis) {
                        Axis.Horizontal -> translationX = offsetPx.toFloat()
                        Axis.Vertical -> translationY = offsetPx.toFloat()
                    }
                }
            }
        }
    }

    private fun computeOffset(overscrollDistance: Float, maxDistance: Float): Float {
        val progress = ProgressConverter.convert(overscrollDistance / maxDistance, maxFraction)
        return progress * maxDistance
    }

    private fun consumeOffset(delta: Offset): Offset {
        val oldOffset = offset.toFloat()
        val delta = delta.toFloat()
        val consumed =
            if (oldOffset == 0f || (oldOffset + delta).sign == oldOffset.sign) {
                delta
            } else {
                -oldOffset
            }
        offset = offset.copyWith(oldOffset + consumed)
        return consumed.toOffset()
    }

    private fun Offset.toFloat(): Float {
        return when (axis) {
            Axis.Vertical -> y
            Axis.Horizontal -> x
            Axis.None -> 0f
        }
    }

    private fun Velocity.toFloat(): Float {
        return when (axis) {
            Axis.Vertical -> y
            Axis.Horizontal -> x
            Axis.None -> 0f
        }
    }

    private fun Float.toOffset(): Offset {
        return when (axis) {
            Axis.Vertical -> Offset(0f, this)
            Axis.Horizontal -> Offset(this, 0f)
            Axis.None -> Offset.Zero
        }
    }

    private fun Offset.copyWith(value: Float): Offset {
        return when (axis) {
            Axis.Vertical -> copy(y = value)
            Axis.Horizontal -> copy(x = value)
            Axis.None -> this
        }
    }

    private fun Velocity.copyWith(value: Float): Velocity {
        return when (axis) {
            Axis.Vertical -> copy(y = value)
            Axis.Horizontal -> copy(x = value)
            Axis.None -> this
        }
    }

    private enum class Axis {
        None,
        Vertical,
        Horizontal
    }

    internal companion object {

        val DefaultAnimationSpec = spring(1f, 150f, 0.5f)
    }
}
