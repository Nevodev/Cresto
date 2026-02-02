package com.nevoit.cresto.ui.theme.glasense

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.kyant.capsule.ContinuousRoundedRectangle

fun Modifier.glasenseHighlight(
    cornerRadius: Dp,
    strokeWidth: Dp = 1.5.dp
): Modifier = this then GlasenseHighlightElement(cornerRadius, strokeWidth)

private data class GlasenseHighlightElement(
    val cornerRadius: Dp,
    val strokeWidth: Dp
) : ModifierNodeElement<GlasenseHighlightNode>() {

    override fun create(): GlasenseHighlightNode {
        return GlasenseHighlightNode(cornerRadius, strokeWidth)
    }

    override fun update(node: GlasenseHighlightNode) {
        node.update(cornerRadius, strokeWidth)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "glasenseHighlight"
        properties["cornerRadius"] = cornerRadius
        properties["strokeWidth"] = strokeWidth
    }
}

private class GlasenseHighlightNode(
    var cornerRadius: Dp,
    var strokeWidth: Dp
) : Modifier.Node(), DrawModifierNode {

    private val gradientBrush = Brush.verticalGradient(
        0.0f to Color.White.copy(alpha = 0.2f),
        1.0f to Color.White.copy(alpha = 0.02f)
    )

    private var shape = ContinuousRoundedRectangle(cornerRadius)

    private var cachedOutline: Outline? = null

    private var cachedClipPath: Path? = null

    private var lastSize: Size = Size.Unspecified
    private var lastLayoutDirection: LayoutDirection? = null

    fun update(newCornerRadius: Dp, newStrokeWidth: Dp) {
        var needsInvalidate = false

        if (cornerRadius != newCornerRadius) {
            cornerRadius = newCornerRadius
            shape = ContinuousRoundedRectangle(newCornerRadius)
            cachedOutline = null
            cachedClipPath = null
            needsInvalidate = true
        }

        if (strokeWidth != newStrokeWidth) {
            strokeWidth = newStrokeWidth
            needsInvalidate = true
        }

        if (needsInvalidate) {
            invalidateDraw()
        }
    }

    override fun ContentDrawScope.draw() {
        if (cachedOutline == null || size != lastSize || layoutDirection != lastLayoutDirection) {
            cachedOutline = shape.createOutline(size, layoutDirection, this)

            cachedClipPath = when (val outline = cachedOutline!!) {
                is Outline.Generic -> outline.path
                is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
                is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
            }

            lastSize = size
            lastLayoutDirection = layoutDirection
        }

        val outline = cachedOutline!!
        val clipPath = cachedClipPath!!

        drawContent()

        clipPath(path = clipPath) {
            drawOutline(
                outline = outline,
                brush = gradientBrush,
                style = Stroke(width = strokeWidth.toPx() * 2),
                blendMode = BlendMode.Plus
            )
        }
    }
}