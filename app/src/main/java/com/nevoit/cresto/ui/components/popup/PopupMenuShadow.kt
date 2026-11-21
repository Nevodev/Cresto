package com.nevoit.cresto.ui.components.popup

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.LruCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.graphics.createBitmap
import com.kyant.shapes.RoundedRectangularShape
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt

private const val BlurRadius = 24f
private const val OffsetY = 8f
private const val ShadowAlpha = 0.1f

// The mask uses one pixel per dp; bilinear sampling is sufficient for its blurred edges.
private val MaskDensity = Density(1f)

private data class ShadowKey(val shape: RoundedRectangularShape, val direction: LayoutDirection)
private class ShadowMask(val bitmap: Bitmap, val padding: Int, val cap: Int) {
    val patches: Array<Rect> = run {
        val cuts = intArrayOf(0, cap, bitmap.width - cap, bitmap.width)
        Array(9) { index ->
            val x = index % 3
            val y = index / 3
            Rect(cuts[x], cuts[y], cuts[x + 1], cuts[y + 1])
        }
    }
}

private val ShadowMasks = LruCache<ShadowKey, ShadowMask>(8)

internal class PopupMenuShadow(shape: RoundedRectangularShape, direction: LayoutDirection) {
    private val mask = ShadowKey(shape, direction).let { key ->
        ShadowMasks[key] ?: createShadowMask(key).also { ShadowMasks.put(key, it) }
    }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        color = Color.BLACK
    }
    private val destination = RectF()
    private val x = FloatArray(4)
    private val y = FloatArray(4)

    fun DrawScope.draw(size: Size, alpha: Float) {
        if (size.width <= 0f || size.height <= 0f || alpha <= 0f) return
        paint.alpha = (255f * ShadowAlpha * alpha.coerceIn(0f, 1f)).roundToInt()
        if (paint.alpha == 0) return
        val padding = mask.padding * density
        val cap = mask.cap * density
        val width = size.width + padding * 2f
        val height = size.height + padding * 2f
        val capX = min(cap, width / 2f)
        val capY = min(cap, height / 2f)
        x[0] = -padding
        x[1] = x[0] + capX
        x[3] = size.width + padding
        x[2] = x[3] - capX
        y[0] = OffsetY * density - padding
        y[1] = y[0] + capY
        y[3] = OffsetY * density + size.height + padding
        y[2] = y[3] - capY
        drawIntoCanvas { canvas ->
            for (index in 0 until 9) {
                val column = index % 3
                val row = index / 3
                destination.set(x[column], y[row], x[column + 1], y[row + 1])
                if (!destination.isEmpty) {
                    canvas.nativeCanvas.drawBitmap(
                        mask.bitmap,
                        mask.patches[index],
                        destination,
                        paint
                    )
                }
            }
        }
    }
}

private fun createShadowMask(key: ShadowKey): ShadowMask {
    val corners = key.shape.corners(Size(1024f, 1024f), key.direction, MaskDensity)
    val corner = maxOf(corners.topLeft, corners.topRight, corners.bottomLeft, corners.bottomRight)
    val padding = ceil(BlurRadius * 2f).toInt()
    val innerCap = ceil(corner + BlurRadius).toInt()
    val contentSize = innerCap * 2 + 1
    val bitmapSize = contentSize + padding * 2
    val bitmap = createBitmap(bitmapSize, bitmapSize, Bitmap.Config.ALPHA_8)
    val outline = key.shape.createOutline(
        Size(contentSize.toFloat(), contentSize.toFloat()), key.direction, MaskDensity,
    )
    val path = when (outline) {
        is Outline.Generic -> outline.path
        is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
        is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
    }
    val canvas = Canvas(bitmap)
    canvas.translate(padding.toFloat(), padding.toFloat())
    canvas.drawPath(path.asAndroidPath(), Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        maskFilter = BlurMaskFilter(BlurRadius, BlurMaskFilter.Blur.NORMAL)
    })
    bitmap.prepareToDraw()
    return ShadowMask(bitmap, padding, padding + innerCap)
}
