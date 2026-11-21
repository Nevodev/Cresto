// Copyright 2026, kyant0/Shapes contributors
// SPDX-License-Identifier: Apache-2.0

package com.kyant.shapes

/**
 * 归一化角点，10 个点 (x0,y0)..(x9,y9)，单位为半径。
 * tH/tV 为 (边长一半 - 半径) / 半径，clamp 到 0..1。
 * 返回的数组可能是内部缓存，调用方只读。
 */
fun continuousCurvatureCornerPoints(
    tH: Double = 1.0,
    tV: Double = 1.0
): DoubleArray = ContinuousCurvatureRoundedRectangleCornerBuilder.Default.getCornerBezierPoints(tH, tV)
