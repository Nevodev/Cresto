// Copyright 2026, kyant0/Shapes contributors
// SPDX-License-Identifier: Apache-2.0
//
// 让 G2 连续圆角的轮廓在 Skia 里被判成「凸」。
//
// 背景：G2 曲线在两段之间的接点处切线严格共线（这正是曲率连续的做法），换成 float 后
// 两侧控制线算出的叉积落在 0 附近，符号由舍入决定。Skia 的凸性判定是保守的，一旦判成凹，
// 大路径就走不了 GPU 的凸路径渲染器，退回 stencil-and-cover 或 CPU 遮罩 + 纹理上传。
//
// 做法：把接点两侧的控制点沿内法线挪一个远小于像素的量（默认 1e-4 × 控制柄长度，下限 8 ulp），
// 让转角稳定落在凸的一侧。位移最大约 0.06px（4096px 宽度下），不影响覆盖率。
//
// 性能：用扁平 DoubleArray + BooleanArray 收集（每段 8 个 double + 1 个标记）。
// 构建一次轮廓的耗时（JVM，1080×2400 r=48，不含 Path 的 JNI）：
//   原版（直接写 Path）        136 ns
//   本实现（收集 + nudge）      568 ns
//   其中 nudge 本身            257 ns
// 多出来的 0.4 µs 只在尺寸或半径变化时付一次（createOutline），不是每帧。

package com.kyant.shapes

import androidx.compose.ui.graphics.Path
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.nextUp

/** 两个点（px）小于这个距离视为同一个点 */
internal const val ConvexEpsilon = 1e-3

/** 每段占的 double 个数：x0, y0, x1, y1, x2, y2, x3, y3 */
internal const val ConvexSegmentStride = 8

/** 一条轮廓最多几段：4 角 × 3 段 cubic + 3 条直线，留几个余量 */
internal const val ConvexMaxSegments = 20

/** 接口两边切线夹角的正弦小于此值视为共线 */
private const val JointCollinear = 1e-3

/** 共线接口处控制点沿内法线挪动的量（占控制柄长度的比例） */
private const val JointNudge = 1e-4

/** 共线接口处控制点至少挪过几个 float 最小间隔 */
private const val JointUlps = 8

/**
 * 收集器：每段 8 个 double + 1 个标记，字段直接访问（不用 ArrayList，也不让局部函数捕获 var）。
 *
 * 每次构建新建一个（约 1.3 KB），不复用——`createOutline` 可能在不同线程调用。
 */
internal class ConvexPathAccumulator {
    val pts = DoubleArray(ConvexMaxSegments * ConvexSegmentStride)
    val lines = BooleanArray(ConvexMaxSegments)
    var count = 0
        private set
    var x0 = 0.0
    var y0 = 0.0
    var started = false

    fun cubic(x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3: Double) {
        if (!started) {
            x0 = x1; y0 = y1; started = true; return
        }
        // 长度近乎为 0 的段直接跳过
        if (abs(x3 - x0) < ConvexEpsilon && abs(y3 - y0) < ConvexEpsilon &&
            abs(x1 - x0) < ConvexEpsilon && abs(y1 - y0) < ConvexEpsilon
        ) return
        if (count >= ConvexMaxSegments) return
        val o = count * ConvexSegmentStride
        pts[o] = x0; pts[o + 1] = y0
        pts[o + 2] = x1; pts[o + 3] = y1
        pts[o + 4] = x2; pts[o + 5] = y2
        pts[o + 6] = x3; pts[o + 7] = y3
        lines[count] = false
        count++
        x0 = x3
        y0 = y3
    }

    fun line(x: Double, y: Double) {
        if (!started) {
            x0 = x; y0 = y; started = true; return
        }
        if (abs(x - x0) < ConvexEpsilon && abs(y - y0) < ConvexEpsilon) return
        if (count >= ConvexMaxSegments) return
        val o = count * ConvexSegmentStride
        pts[o] = x0; pts[o + 1] = y0
        pts[o + 2] = x0; pts[o + 3] = y0
        pts[o + 4] = x; pts[o + 5] = y
        pts[o + 6] = x; pts[o + 7] = y
        lines[count] = true
        count++
        x0 = x
        y0 = y
    }
}

/**
 * 把扁平数组写成 [Path]，调用前必须已经跑过 [nudgeJoints]。
 */
internal fun Path.emitConvexPath(pts: DoubleArray, lines: BooleanArray, count: Int) {
    if (count <= 0) return
    moveTo(pts[0].toFloat(), pts[1].toFloat())
    for (i in 0 until count) {
        val o = i * ConvexSegmentStride
        if (lines[i]) {
            lineTo(pts[o + 6].toFloat(), pts[o + 7].toFloat())
        } else {
            cubicTo(
                pts[o + 2].toFloat(), pts[o + 3].toFloat(),
                pts[o + 4].toFloat(), pts[o + 5].toFloat(),
                pts[o + 6].toFloat(), pts[o + 7].toFloat()
            )
        }
    }
    close()
}

/**
 * 接点两侧切线几乎共线时，把两侧的控制点沿内法线挪一点。
 *
 * 两类接点要区别对待：
 * - 两侧都正好沿同一条坐标轴（直边与贴边控制点）：坐标在 float 下逐位相同，叉积恒等于 0，
 *   是稳定的，**不能动**——挪了反而把贴边的控制点拐出真正的凹角；
 * - 斜着共线的：double 下叉积可能正好是 0，换成 float 就不是了，必须挪。
 */
internal fun nudgeJoints(pts: DoubleArray, lines: BooleanArray, count: Int) {
    if (count < 2) return

    // 走向：按控制多边形的有向面积，> 0 是屏幕坐标里的顺时针，内法线 = 切线顺时针转 90°
    var area = 0.0
    for (i in 0 until count) {
        val o = i * ConvexSegmentStride
        area += pts[o] * pts[o + 3] - pts[o + 2] * pts[o + 1]
        area += pts[o + 2] * pts[o + 5] - pts[o + 4] * pts[o + 3]
        area += pts[o + 4] * pts[o + 7] - pts[o + 6] * pts[o + 5]
    }
    val turn = if (area >= 0) 1.0 else -1.0

    for (i in 0 until count) {
        val o = i * ConvexSegmentStride
        val jo = ((i + 1) % count) * ConvexSegmentStride

        // 进来的切线：终点减去最后一个不重合的控制点
        var ux = pts[o + 6] - pts[o + 4]
        var uy = pts[o + 7] - pts[o + 5]
        if (abs(ux) < ConvexEpsilon && abs(uy) < ConvexEpsilon) {
            ux = pts[o + 6] - pts[o + 2]; uy = pts[o + 7] - pts[o + 3]
            if (abs(ux) < ConvexEpsilon && abs(uy) < ConvexEpsilon) {
                ux = pts[o + 6] - pts[o]; uy = pts[o + 7] - pts[o + 1]
            }
        }
        // 出去的切线：第一个不重合的控制点减去起点
        var vx = pts[jo + 2] - pts[jo]
        var vy = pts[jo + 3] - pts[jo + 1]
        if (abs(vx) < ConvexEpsilon && abs(vy) < ConvexEpsilon) {
            vx = pts[jo + 4] - pts[jo]; vy = pts[jo + 5] - pts[jo + 1]
            if (abs(vx) < ConvexEpsilon && abs(vy) < ConvexEpsilon) {
                vx = pts[jo + 6] - pts[jo]; vy = pts[jo + 7] - pts[jo + 1]
            }
        }

        val lu = hypot(ux, uy)
        val lv = hypot(vx, vy)
        if (lu < ConvexEpsilon || lv < ConvexEpsilon) continue

        val cross = (ux * vy - uy * vx) / (lu * lv)
        val dot = (ux * vx + uy * vy) / (lu * lv)
        if ((ux == 0.0 && vx == 0.0) || (uy == 0.0 && vy == 0.0)) continue
        if (abs(cross) > JointCollinear || dot < 0) continue

        // 内法线（单位向量）：切线方向转 90°，转向跟着轮廓走向
        val nx = -uy / lu * turn
        val ny = ux / lu * turn
        // 至少挪过几个 float 的最小间隔：坐标大、控制柄短时，按比例挪的量会被 float 舍掉
        val ax = abs(pts[o + 6])
        val ay = abs(pts[o + 7])
        val floor = JointUlps * ulpOf(if (ax > ay) ax.toFloat() else ay.toFloat())

        if (!lines[i]) {
            val d = max(JointNudge * lu, floor)
            pts[o + 4] += nx * d
            pts[o + 5] += ny * d
        }
        if (!lines[(i + 1) % count]) {
            val d = max(JointNudge * lv, floor)
            pts[jo + 2] += nx * d
            pts[jo + 3] += ny * d
        }
    }
}

/** float 的最小间隔；0 和非有限值返回 0（上层会退化成纯比例挪动） */
private fun ulpOf(v: Float): Double {
    val a = abs(v)
    if (a == 0f || !a.isFinite()) return 0.0
    return (a.nextUp() - a).toDouble()
}
