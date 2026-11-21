package com.nevoit.cresto.navigation.clip

import android.util.LruCache
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.kyant.shapes.RoundedRectangle
import com.kyant.shapes.RoundedRectangularShape
import com.kyant.shapes.continuousCurvatureCornerPoints
import kotlin.math.ceil
import kotlin.math.roundToInt

private const val CornerMaskPadding = 2f

fun Modifier.cornerMaskClip(
    radius: Dp,
    enabled: Boolean = true,
): Modifier = cornerMaskClip(RoundedRectangle(radius), enabled)

fun Modifier.cornerMaskClip(
    shape: RoundedRectangularShape,
    enabled: Boolean = true,
): Modifier = then(CornerMaskClipElement(shape, enabled))

/** Must be inside an existing offscreen layer; DstOut would otherwise erase the backdrop too. */
internal fun Modifier.cornerMaskClipInLayer(
    shape: RoundedRectangularShape,
    enabled: Boolean = true,
): Modifier = if (!enabled) this else drawWithCache {
    val bounds = Rect(Offset.Zero, size)
    val corners = shape.corners(size, layoutDirection, this)
    onDrawWithContent {
        drawContent()
        drawCornerMasks(bounds, corners)
    }
}

/** The enclosing offscreen layer must contain only the content being masked. */
internal fun Modifier.cornerMaskClipInLayer(
    bounds: (Size) -> Rect,
    shape: () -> RoundedRectangularShape,
    maximumRadiusPx: Float? = null,
): Modifier = drawWithCache {
    val maskRadius = maximumRadiusPx?.let { ceil(it) }
    // Keep a strong reference: an unrelated mask must not evict this texture mid-transition.
    val mask = maskRadius?.takeIf { it > 0f }?.let {
        CornerMaskCache.obtain(it, it * 4f, it * 4f)
    }
    onDrawWithContent {
        val bounds = bounds(size)
        if (bounds.width <= 0f || bounds.height <= 0f) return@onDrawWithContent
        val corners = shape().corners(bounds.size, layoutDirection, this)
        val canvas = drawContext.canvas
        canvas.save()
        try {
            canvas.clipRect(bounds)
            drawContent()
            if (maskRadius == null) {
                drawCornerMasks(bounds, corners)
            } else if (mask != null) {
                drawScaledCornerMasks(mask, maskRadius, bounds, corners)
            }
        } finally {
            canvas.restore()
        }
    }
}

fun Modifier.cornerMaskClip(
    bounds: (Size) -> Rect,
    shape: () -> RoundedRectangularShape,
): Modifier = then(CornerMaskClipElement(null, true, bounds, shape))

/** Paints over the corners against a known opaque background without an offscreen layer. */
fun Modifier.cornerMaskCover(
    shape: RoundedRectangularShape,
    color: Color,
    enabled: Boolean = true,
): Modifier = then(CornerMaskClipElement(shape, enabled, coverColor = color))

private class CornerMaskClipElement(
    val shape: RoundedRectangularShape?,
    val enabled: Boolean,
    val bounds: ((Size) -> Rect)? = null,
    val shapeProducer: (() -> RoundedRectangularShape)? = null,
    val coverColor: Color = Color.Unspecified,
) : ModifierNodeElement<CornerMaskClipNode>() {

    override fun create(): CornerMaskClipNode =
        CornerMaskClipNode(shape, enabled, bounds, shapeProducer, coverColor)

    override fun update(node: CornerMaskClipNode) {
        node.shape = shape
        node.enabled = enabled
        node.bounds = bounds
        node.shapeProducer = shapeProducer
        node.coverColor = coverColor
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "cornerMaskClip"
        properties["shape"] = shape
        properties["enabled"] = enabled
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CornerMaskClipElement) return false
        return shape == other.shape && enabled == other.enabled &&
                bounds == other.bounds && shapeProducer == other.shapeProducer &&
                coverColor == other.coverColor
    }

    override fun hashCode(): Int {
        var result = shape?.hashCode() ?: 0
        result = 31 * result + enabled.hashCode()
        result = 31 * result + (bounds?.hashCode() ?: 0)
        result = 31 * result + (shapeProducer?.hashCode() ?: 0)
        result = 31 * result + coverColor.hashCode()
        return result
    }
}

private class CornerMaskClipNode(
    var shape: RoundedRectangularShape?,
    var enabled: Boolean,
    var bounds: ((Size) -> Rect)?,
    var shapeProducer: (() -> RoundedRectangularShape)?,
    coverColor: Color,
) : Modifier.Node(), DrawModifierNode {

    var coverColor = coverColor
        set(value) {
            field = value
            coverFilter = if (value == Color.Unspecified) null else ColorFilter.tint(value)
        }
    private var coverFilter = if (coverColor == Color.Unspecified) null else ColorFilter.tint(coverColor)

    private var cornersSize = Size.Zero
    private var cornersShape: RoundedRectangularShape? = null
    private var cornersDirection: LayoutDirection? = null
    private var corners: RoundedRectangularShape.Corners? = null

    private fun cornersOf(
        shape: RoundedRectangularShape,
        width: Float,
        height: Float,
        direction: LayoutDirection,
        density: Density
    ): RoundedRectangularShape.Corners {
        val cached = corners
        if (cached != null &&
            cornersSize.width == width &&
            cornersSize.height == height &&
            cornersShape == shape &&
            cornersDirection == direction
        ) {
            return cached
        }
        val computed = shape.corners(Size(width, height), direction, density)
        cornersSize = Size(width, height)
        cornersShape = shape
        cornersDirection = direction
        corners = computed
        return computed
    }

    override fun ContentDrawScope.draw() {
        if (!enabled) {
            drawContent()
            return
        }

        val bounds = bounds?.invoke(size) ?: Rect(Offset.Zero, size)
        val width = bounds.width
        val height = bounds.height
        if (width <= 0f || height <= 0f) {
            return
        }

        val shape = shapeProducer?.invoke() ?: checkNotNull(shape)
        val corners = cornersOf(shape, width, height, layoutDirection, this)
        val filter = coverFilter
        if (filter != null) {
            val canvas = drawContext.canvas
            canvas.save()
            try {
                canvas.clipRect(bounds)
                drawContent()
            } finally {
                canvas.restore()
            }
            // The masks extend past both adjacent edges by CornerMaskPadding. Clipping them to
            // the content rectangle would discard that overlap and expose fractional edge seams.
            drawCornerMasks(bounds, corners, filter)
            return
        }
        drawCornerMaskClipped(bounds, corners) { this@draw.drawContent() }
    }
}

// Recorded layers need the same mask without introducing a second layout or outline clip.
internal fun DrawScope.cornerMaskClip(
    shape: RoundedRectangularShape,
    content: DrawScope.() -> Unit,
) {
    drawCornerMaskClipped(
        Rect(Offset.Zero, size),
        shape.corners(size, layoutDirection, this),
        content,
    )
}

internal fun DrawScope.cornerMaskClip(
    bounds: Rect,
    shape: RoundedRectangularShape,
    content: DrawScope.() -> Unit,
) {
    drawCornerMaskClipped(
        bounds,
        shape.corners(bounds.size, layoutDirection, this),
        content,
    )
}

private inline fun DrawScope.drawCornerMaskClipped(
    bounds: Rect,
    corners: RoundedRectangularShape.Corners,
    content: DrawScope.() -> Unit,
) {
    val width = bounds.width
    val height = bounds.height
    if (width <= 0f || height <= 0f) return
    if (corners.topLeft <= 0f && corners.topRight <= 0f &&
        corners.bottomRight <= 0f && corners.bottomLeft <= 0f
    ) {
        val canvas = drawContext.canvas
        canvas.save()
        try {
            canvas.clipRect(bounds)
            content()
        } finally {
            canvas.restore()
        }
        return
    }

    val canvas = drawContext.canvas
    canvas.save()
    try {
        // saveLayer bounds are allocation hints; the moving rectangle must also clip content.
        canvas.clipRect(bounds)
        canvas.saveLayer(bounds, MaskLayerPaint)
        try {
            content()
            drawCornerMasks(bounds, corners)
        } finally {
            canvas.restore()
        }
    } finally {
        canvas.restore()
    }
}

private fun DrawScope.drawCornerMasks(
    bounds: Rect,
    corners: RoundedRectangularShape.Corners,
    colorFilter: ColorFilter? = null,
) {
    val topLeft = CornerMaskCache.obtain(corners.topLeft, bounds.width, bounds.height)
    val topRight = CornerMaskCache.obtain(corners.topRight, bounds.width, bounds.height)
    val bottomRight = CornerMaskCache.obtain(corners.bottomRight, bounds.width, bounds.height)
    val bottomLeft = CornerMaskCache.obtain(corners.bottomLeft, bounds.width, bounds.height)
    if (topLeft != null) drawCornerMask(topLeft, bounds, false, false, colorFilter)
    if (topRight != null) drawCornerMask(topRight, bounds, true, false, colorFilter)
    if (bottomRight != null) drawCornerMask(bottomRight, bounds, true, true, colorFilter)
    if (bottomLeft != null) drawCornerMask(bottomLeft, bounds, false, true, colorFilter)
}

private fun DrawScope.drawCornerMask(
    mask: ImageBitmap,
    bounds: Rect,
    mirrorX: Boolean,
    mirrorY: Boolean,
    colorFilter: ColorFilter?,
    scale: Float = 1f,
) {
    if (!mirrorX && !mirrorY && scale == 1f) {
        drawImage(
            mask,
            topLeft = bounds.topLeft - Offset(CornerMaskPadding, CornerMaskPadding),
            colorFilter = colorFilter,
            blendMode = if (colorFilter == null) BlendMode.DstOut else BlendMode.SrcOver,
        )
        return
    }

    val canvas = drawContext.canvas
    canvas.save()
    try {
        canvas.translate(
            if (mirrorX) bounds.right else bounds.left,
            if (mirrorY) bounds.bottom else bounds.top
        )
        canvas.scale(
            if (mirrorX) -scale else scale,
            if (mirrorY) -scale else scale
        )
        drawImage(
            mask,
            topLeft = Offset(-CornerMaskPadding, -CornerMaskPadding),
            colorFilter = colorFilter,
            blendMode = if (colorFilter == null) BlendMode.DstOut else BlendMode.SrcOver,
        )
    } finally {
        canvas.restore()
    }
}

private fun DrawScope.drawScaledCornerMasks(
    mask: ImageBitmap,
    maskRadius: Float,
    bounds: Rect,
    corners: RoundedRectangularShape.Corners,
) {
    if (corners.topLeft > 0f) {
        drawCornerMask(mask, bounds, false, false, null, corners.topLeft / maskRadius)
    }
    if (corners.topRight > 0f) {
        drawCornerMask(mask, bounds, true, false, null, corners.topRight / maskRadius)
    }
    if (corners.bottomRight > 0f) {
        drawCornerMask(mask, bounds, true, true, null, corners.bottomRight / maskRadius)
    }
    if (corners.bottomLeft > 0f) {
        drawCornerMask(mask, bounds, false, true, null, corners.bottomLeft / maskRadius)
    }
}

private object CornerMaskCache {

    private const val RADIUS_STEPS = 4
    private const val T_STEPS = 64
    private const val MAX_PIXELS = 512 * 1024

    private val cache = object : LruCache<Long, ImageBitmap>(MAX_PIXELS) {
        override fun sizeOf(key: Long, value: ImageBitmap): Int =
            value.width * value.height
    }

    fun obtain(radiusPx: Float, width: Float, height: Float): ImageBitmap? {
        if (radiusPx <= 0f) return null
        val tW = ((width * 0.5f - radiusPx) / radiusPx).coerceIn(0f, 1f)
        val tH = ((height * 0.5f - radiusPx) / radiusPx).coerceIn(0f, 1f)

        val radiusKey = (radiusPx * RADIUS_STEPS).roundToInt()
        if (radiusKey <= 0) return null
        val tWKey = (tW * T_STEPS).roundToInt()
        val tHKey = (tH * T_STEPS).roundToInt()
        val key = (radiusKey.toLong() shl 20) or (tWKey.toLong() shl 10) or tHKey.toLong()

        cache.get(key)?.let { return it }

        val bitmap = createCornerMask(
            radius = radiusKey.toFloat() / RADIUS_STEPS,
            tW = tWKey.toFloat() / T_STEPS,
            tH = tHKey.toFloat() / T_STEPS
        ) ?: return null
        cache.put(key, bitmap)
        return bitmap
    }
}

private fun createCornerMask(radius: Float, tW: Float, tH: Float): ImageBitmap? {
    val points = continuousCurvatureCornerPoints(tW.toDouble(), tH.toDouble())
    if (points.size < 20) return null

    // Top-left corner local space: x = (1 - px) * r, y = py * r
    var maxX = 0f
    var maxY = 0f
    var index = 0
    while (index < 20) {
        val x = (1.0 - points[index]).toFloat() * radius
        val y = points[index + 1].toFloat() * radius
        if (x > maxX) maxX = x
        if (y > maxY) maxY = y
        index += 2
    }

    val width = ceil(maxX + CornerMaskPadding + 1f).toInt()
    val height = ceil(maxY + CornerMaskPadding + 1f).toInt()
    if (width <= 0 || height <= 0) return null

    val path = Path()
    path.moveTo(
        (1.0 - points[0]).toFloat() * radius + CornerMaskPadding,
        points[1].toFloat() * radius + CornerMaskPadding,
    )
    index = 2
    while (index <= 14) {
        path.cubicTo(
            (1.0 - points[index]).toFloat() * radius + CornerMaskPadding,
            points[index + 1].toFloat() * radius + CornerMaskPadding,
            (1.0 - points[index + 2]).toFloat() * radius + CornerMaskPadding,
            points[index + 3].toFloat() * radius + CornerMaskPadding,
            (1.0 - points[index + 4]).toFloat() * radius + CornerMaskPadding,
            points[index + 5].toFloat() * radius + CornerMaskPadding,
        )
        index += 6
    }
    // Keep the mask opaque beyond the two straight edges. At fractional origins, filtering a
    // bitmap that ends exactly on the clip edge leaves a partially erased one-pixel seam.
    path.lineTo(0f, maxY + CornerMaskPadding)
    path.lineTo(0f, 0f)
    path.lineTo(maxX + CornerMaskPadding, 0f)
    path.close()

    val bitmap = ImageBitmap(width, height, ImageBitmapConfig.Alpha8)
    val canvas = Canvas(bitmap)
    canvas.drawPath(path, MaskFillPaint)
    return bitmap
}

private val MaskFillPaint = Paint().apply {
    color = Color.White
}

private val MaskLayerPaint = Paint()
