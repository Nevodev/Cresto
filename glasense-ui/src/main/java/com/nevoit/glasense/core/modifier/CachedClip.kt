package com.nevoit.glasense.core.modifier

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.composed
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult

fun Modifier.cachedClip(
    shape: Shape,
    clip: Boolean = true
): Modifier = composed {
    if (!clip) {
        return@composed this
    }

    val drawCacheBlock = remember(shape) {
        val block: CacheDrawScope.() -> DrawResult = {
            val mask = ImageBitmap(
                width = size.width.toInt().coerceAtLeast(1),
                height = size.height.toInt().coerceAtLeast(1),
                config = ImageBitmapConfig.Alpha8
            )

            val canvas = Canvas(mask)

            val paint = Paint().apply {
                color = Color.White
                isAntiAlias = true
            }

            val outline = shape.createOutline(size, layoutDirection, this)

            canvas.drawOutline(
                outline = outline,
                paint = paint
            )

            val layerPaint = Paint()

            val bounds = Rect(Offset.Zero, size)

            onDrawWithContent {
                drawIntoCanvas { canvas ->
                    canvas.saveLayer(bounds, layerPaint)
                }

                drawContent()

                drawImage(
                    image = mask,
                    topLeft = Offset.Zero,
                    blendMode = BlendMode.DstIn
                )

                drawIntoCanvas { canvas ->
                    canvas.restore()
                }
            }
        }
        block
    }

    this.drawWithCache(drawCacheBlock)
}