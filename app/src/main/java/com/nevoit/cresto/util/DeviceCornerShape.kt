package com.nevoit.cresto.util

import android.view.RoundedCorner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.shapes.UnevenRoundedRectangle

/**
 * Creates a shape that mirrors the device's physical screen corners.
 *
 * @param padding Optional padding to inset the shape.
 * @param topLeft Whether to round the top-left corner.
 * @param topRight Whether to round the top-right corner.
 * @param bottomRight Whether to round the bottom-right corner.
 * @param bottomLeft Whether to round the bottom-left corner.
 * @return An [UnevenRoundedRectangle] that can be used as a shape in Composables.
 */
@Composable
fun deviceCornerShape(
    padding: Dp = 0.dp,
    topLeft: Boolean = true,
    topRight: Boolean = true,
    bottomRight: Boolean = true,
    bottomLeft: Boolean = true
): UnevenRoundedRectangle {
    val view = LocalView.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current //make sure that the shape updates correctly

    fun getCornerRadius(position: Int, status: Boolean): Dp {
        val insets = view.rootWindowInsets
        return if (!status) {
            0f.dp
        } else {
            val radius =
                with(density) { insets.getRoundedCorner(position)?.radius?.toFloat()?.toDp() }
            if (radius == null || radius <= 16f.dp) 16f.dp else radius
        }
    }

    return remember(view, configuration, padding, topLeft, topRight, bottomRight, bottomLeft) {
        val topLeftRadius = getCornerRadius(RoundedCorner.POSITION_TOP_LEFT, topLeft)
        val topRightRadius = getCornerRadius(RoundedCorner.POSITION_TOP_RIGHT, topRight)
        val bottomRightRadius = getCornerRadius(RoundedCorner.POSITION_BOTTOM_RIGHT, bottomRight)
        val bottomLeftRadius = getCornerRadius(RoundedCorner.POSITION_BOTTOM_LEFT, bottomLeft)

        UnevenRoundedRectangle(
            topStart = topLeftRadius - padding,
            topEnd = topRightRadius - padding,
            bottomEnd = bottomRightRadius - padding,
            bottomStart = bottomLeftRadius - padding
        )
    }
}
