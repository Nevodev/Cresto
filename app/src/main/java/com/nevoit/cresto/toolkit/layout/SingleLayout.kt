package com.nevoit.cresto.toolkit.layout

import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection

fun MeasureScope.singleLayout(placeable: Placeable): MeasureResult {

    return object : MeasureResult {

        override val width = placeable.width
        override val height = placeable.height
        override val alignmentLines = emptyMap<AlignmentLine, Int>()
        override val rulers = null

        private val placementScope =
            SimplePlacementScope(width, layoutDirection, density, fontScale)

        override fun placeChildren() {
            with(placementScope) {
                placeable.place(IntOffset.Zero)
            }
        }
    }
}

fun MeasureScope.singleLayoutWithLayer(
    placeable: Placeable,
    layerBlock: GraphicsLayerScope.() -> Unit = NoOpLayerBlock
): MeasureResult {

    return object : MeasureResult {

        override val width = placeable.width
        override val height = placeable.height
        override val alignmentLines = emptyMap<AlignmentLine, Int>()
        override val rulers = null

        private val placementScope =
            SimplePlacementScope(width, layoutDirection, density, fontScale)

        override fun placeChildren() {
            with(placementScope) {
                placeable.placeWithLayer(IntOffset.Zero, layerBlock = layerBlock)
            }
        }
    }
}

fun MeasureScope.singleRelativeLayout(placeable: Placeable): MeasureResult {

    return object : MeasureResult {

        override val width = placeable.width
        override val height = placeable.height
        override val alignmentLines = emptyMap<AlignmentLine, Int>()
        override val rulers = null

        private val placementScope =
            SimplePlacementScope(width, layoutDirection, density, fontScale)

        override fun placeChildren() {
            with(placementScope) {
                placeable.placeRelative(IntOffset.Zero)
            }
        }
    }
}

fun MeasureScope.singleRelativeLayoutWithLayer(
    placeable: Placeable,
    layerBlock: GraphicsLayerScope.() -> Unit = NoOpLayerBlock
): MeasureResult {

    return object : MeasureResult {

        override val width = placeable.width
        override val height = placeable.height
        override val alignmentLines = emptyMap<AlignmentLine, Int>()
        override val rulers = null

        private val placementScope =
            SimplePlacementScope(width, layoutDirection, density, fontScale)

        override fun placeChildren() {
            with(placementScope) {
                placeable.placeWithLayer(IntOffset.Zero, layerBlock = layerBlock)
            }
        }
    }
}

private class SimplePlacementScope(
    override val parentWidth: Int,
    override val parentLayoutDirection: LayoutDirection,
    override val density: Float,
    override val fontScale: Float
) : Placeable.PlacementScope()

private val NoOpLayerBlock: GraphicsLayerScope.() -> Unit = {}
