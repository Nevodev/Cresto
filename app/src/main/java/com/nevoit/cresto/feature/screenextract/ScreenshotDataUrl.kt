package com.nevoit.cresto.feature.screenextract

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.scale
import java.io.ByteArrayOutputStream
import java.util.Base64
import kotlin.math.roundToInt

private const val DEFAULT_MAX_SCREENSHOT_BYTES = 5 * 1024 * 1024
private const val DEFAULT_TARGET_SHORT_SIDE_PX = 1080
private const val DEFAULT_JPEG_QUALITY = 82

fun ByteArray.toPngDataUrl(maxSizeBytes: Int = DEFAULT_MAX_SCREENSHOT_BYTES): String {
    require(isNotEmpty()) { "截图为空" }
    require(size <= maxSizeBytes) { "截图过大，请重试" }

    val base64 = Base64.getEncoder().encodeToString(this)
    return "data:image/png;base64,$base64"
}

fun ByteArray.toCompressedScreenshotDataUrl(
    targetShortSidePx: Int = DEFAULT_TARGET_SHORT_SIDE_PX,
    jpegQuality: Int = DEFAULT_JPEG_QUALITY,
    maxSizeBytes: Int = DEFAULT_MAX_SCREENSHOT_BYTES
): String {
    require(isNotEmpty()) { "截图为空" }

    val bitmap = BitmapFactory.decodeByteArray(this, 0, size)
        ?: throw IllegalArgumentException("无法解析截图")

    val compressedBytes = bitmap.useBitmap { source ->
        val scaled = source.scaleToShortSide(targetShortSidePx)
        scaled.useBitmap { target ->
            ByteArrayOutputStream().use { output ->
                target.compress(Bitmap.CompressFormat.JPEG, jpegQuality.coerceIn(1, 100), output)
                output.toByteArray()
            }
        }
    }

    require(compressedBytes.size <= maxSizeBytes) { "压缩后截图仍过大，请重试" }

    val base64 = Base64.getEncoder().encodeToString(compressedBytes)
    return "data:image/jpeg;base64,$base64"
}

fun ByteArray.toCompressedSharedImageDataUrl(
    targetShortSidePx: Int = DEFAULT_TARGET_SHORT_SIDE_PX,
    jpegQuality: Int = DEFAULT_JPEG_QUALITY,
    maxSizeBytes: Int = DEFAULT_MAX_SCREENSHOT_BYTES
): String {
    require(isNotEmpty()) { "图片为空" }

    val bitmap = BitmapFactory.decodeByteArray(this, 0, size)
        ?: throw IllegalArgumentException("无法解析图片")

    val compressedBytes = bitmap.useBitmap { source ->
        val scaled = source.scaleToShortSide(targetShortSidePx)
        scaled.useBitmap { target ->
            ByteArrayOutputStream().use { output ->
                target.compress(Bitmap.CompressFormat.JPEG, jpegQuality.coerceIn(1, 100), output)
                output.toByteArray()
            }
        }
    }

    require(compressedBytes.size <= maxSizeBytes) { "压缩后图片仍过大，请重试" }

    val base64 = Base64.getEncoder().encodeToString(compressedBytes)
    return "data:image/jpeg;base64,$base64"
}

private fun Bitmap.scaleToShortSide(targetShortSidePx: Int): Bitmap {
    val shortSide = width.coerceAtMost(height)
    if (shortSide <= targetShortSidePx) return this

    val scale = targetShortSidePx.toFloat() / shortSide.toFloat()
    val targetWidth = (width * scale).roundToInt().coerceAtLeast(1)
    val targetHeight = (height * scale).roundToInt().coerceAtLeast(1)
    return this.scale(targetWidth, targetHeight)
}

private inline fun <T> Bitmap.useBitmap(block: (Bitmap) -> T): T {
    return try {
        block(this)
    } finally {
        if (!isRecycled) recycle()
    }
}
