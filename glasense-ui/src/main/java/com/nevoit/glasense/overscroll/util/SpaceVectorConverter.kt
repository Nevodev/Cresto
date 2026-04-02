package com.nevoit.glasense.overscroll.util

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity

/**
 * A factory that returns a [SpaceVectorConverter] for a given [androidx.compose.foundation.gestures.Orientation].
 * This allows for generic handling of 1D values in a 2D space, depending on the orientation.
 */
fun SpaceVectorConverter(orientation: Orientation): SpaceVectorConverter =
    when (orientation) {
        Orientation.Horizontal -> HorizontalConverter
        Orientation.Vertical -> VerticalConverter
    }

/**
 * An interface to convert between 1D scalar values (Float, Int) and 2D space vectors
 * (Offset, Velocity, IntOffset). This abstracts away the orientation (horizontal or vertical)
 * to allow for writing orientation-agnostic logic.
 */
interface SpaceVectorConverter {

    fun Offset.toFloat(): Float

    fun Velocity.toFloat(): Float

    fun IntOffset.toInt(): Int

    fun Float.toOffset(): Offset

    fun Float.toVelocity(): Velocity

    fun Int.toIntOffset(): IntOffset
}

/**
 * A [SpaceVectorConverter] for the horizontal orientation, using the x-axis.
 */
private data object HorizontalConverter : SpaceVectorConverter {

    override fun Offset.toFloat() = x

    override fun Velocity.toFloat() = x

    override fun IntOffset.toInt() = x

    override fun Float.toOffset() = Offset(this, 0f)

    override fun Float.toVelocity() = Velocity(this, 0f)

    override fun Int.toIntOffset() = IntOffset(this, 0)
}

/**
 * A [SpaceVectorConverter] for the vertical orientation, using the y-axis.
 */
private data object VerticalConverter : SpaceVectorConverter {

    override fun Offset.toFloat() = y

    override fun Velocity.toFloat() = y

    override fun IntOffset.toInt() = y

    override fun Float.toOffset() = Offset(0f, this)

    override fun Float.toVelocity() = Velocity(0f, this)

    override fun Int.toIntOffset() = IntOffset(0, this)
}
