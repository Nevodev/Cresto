// Copyright 2026, kyant0/Shapes contributors
// SPDX-License-Identifier: Apache-2.0

package com.kyant.shapes

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.util.fastCoerceIn

internal fun roundedRectangleOutline(
    size: Size,
    radius: Float,
    style: RoundedCornerStyle
): Outline = roundedRectangleOutline(size, radius, radius, radius, radius, style)

internal fun roundedRectangleOutline(
    size: Size,
    topLeft: Float,
    topRight: Float,
    bottomRight: Float,
    bottomLeft: Float,
    style: RoundedCornerStyle
): Outline {
    val width = size.width
    val height = size.height
    val maxRadius = size.minDimension * 0.5f
    return when {
        topLeft == 0f && topRight == 0f && bottomRight == 0f && bottomLeft == 0f -> {
            Outline.Rectangle(Rect(0f, 0f, width, height))
        }

        style == RoundedCornerStyle.Circular ||
                (width == height && topLeft >= maxRadius && topRight >= maxRadius &&
                        bottomRight >= maxRadius && bottomLeft >= maxRadius) -> {
            Outline.Rounded(
                RoundRect(
                    left = 0f,
                    top = 0f,
                    right = width,
                    bottom = height,
                    topLeftCornerRadius = CornerRadius(topLeft),
                    topRightCornerRadius = CornerRadius(topRight),
                    bottomRightCornerRadius = CornerRadius(bottomRight),
                    bottomLeftCornerRadius = CornerRadius(bottomLeft)
                )
            )
        }

        else -> {
            Outline.Generic(
                continuousCurvatureRoundedRectanglePath(
                    size = size,
                    topLeft = topLeft,
                    topRight = topRight,
                    bottomRight = bottomRight,
                    bottomLeft = bottomLeft
                )
            )
        }
    }
}

/**
 * 四个角的 G2 连续曲率轮廓。
 *
 * 先把所有段收进 [ConvexPathAccumulator]（扁平数组，不分配对象列表），
 * 跑一遍 [nudgeJoints] 把共线接点掰到凸的一侧，再一次性写进 [Path]。
 * 原因见 Convexify.kt 顶部。
 */
private fun continuousCurvatureRoundedRectanglePath(
    size: Size,
    topLeft: Float,
    topRight: Float,
    bottomRight: Float,
    bottomLeft: Float,
    path: Path? = null
): Path {
    val out = path?.apply { rewind() } ?: Path()
    val width = size.width.toDouble()
    val height = size.height.toDouble()
    val acc = ConvexPathAccumulator()

    // 四角半径相同时，角点只算一次（原版也是只算一次；重复算在大半径、t<1 时要多解三次/四次方程）
    val shared = if (topLeft == topRight && topRight == bottomRight && bottomRight == topLeft) {
        cornerPointsOf(width, height, topLeft.toDouble())
    } else {
        null
    }

    // 从右上角起，顺时针：TR -> BR -> BL -> TL
    emitCorner(acc, width, height, width - topRight.toDouble(), 0.0, topRight.toDouble(), 1.0, 1.0, descending = false, p = shared)
    emitCorner(acc, width, height, width - bottomRight.toDouble(), height, bottomRight.toDouble(), 1.0, -1.0, descending = true, p = shared)
    emitCorner(acc, width, height, bottomLeft.toDouble(), height, bottomLeft.toDouble(), -1.0, -1.0, descending = false, p = shared)
    emitCorner(acc, width, height, topLeft.toDouble(), 0.0, topLeft.toDouble(), -1.0, 1.0, descending = true, p = shared)

    nudgeJoints(acc.pts, acc.lines, acc.count)
    out.emitConvexPath(acc.pts, acc.lines, acc.count)
    return out
}

/** 归一化角点；t 为 0 或 1 时 builder 内部有缓存，不分配 */
private fun cornerPointsOf(width: Double, height: Double, radius: Double): DoubleArray {
    val tW = ((width * 0.5 - radius) / radius).fastCoerceIn(0.0, 1.0)
    val tH = ((height * 0.5 - radius) / radius).fastCoerceIn(0.0, 1.0)
    return ContinuousCurvatureRoundedRectangleCornerBuilder.Default.getCornerBezierPoints(tW, tH)
}

/**
 * 把一个角落收进 [acc]。
 *
 * cx/cy: 角所在两条边的交点；sx/sy: 该角的镜像方向；descending: 沿轮廓反向遍历这个角。
 */
private fun emitCorner(
    acc: ConvexPathAccumulator,
    width: Double,
    height: Double,
    cx: Double,
    cy: Double,
    radius: Double,
    sx: Double,
    sy: Double,
    descending: Boolean,
    p: DoubleArray? = null
) {
    if (radius <= 0.0) {
        // A square corner still contributes its vertex; skipping it cuts off that side on close.
        acc.line(cx, cy)
        return
    }
    val points = p ?: cornerPointsOf(width, height, radius)
    if (points.size < 20) return

    // 入口点：正向角从 p[0] 进，反向角从 p[18] 进
    val e = if (descending) 18 else 0
    if (!acc.started) {
        acc.x0 = cx + sx * points[e] * radius
        acc.y0 = cy + sy * points[e + 1] * radius
        acc.started = true
    } else {
        acc.line(cx + sx * points[e] * radius, cy + sy * points[e + 1] * radius)
    }

    if (descending) {
        var i = 16
        while (i >= 4) {
            acc.cubic(
                cx + sx * points[i] * radius, cy + sy * points[i + 1] * radius,
                cx + sx * points[i - 2] * radius, cy + sy * points[i - 1] * radius,
                cx + sx * points[i - 4] * radius, cy + sy * points[i - 3] * radius
            )
            i -= 6
        }
    } else {
        var i = 2
        while (i <= 14) {
            acc.cubic(
                cx + sx * points[i] * radius, cy + sy * points[i + 1] * radius,
                cx + sx * points[i + 2] * radius, cy + sy * points[i + 3] * radius,
                cx + sx * points[i + 4] * radius, cy + sy * points[i + 5] * radius
            )
            i += 6
        }
    }
}
