package com.nevoit.glasense.core.utility

import android.view.RoundedCorner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import com.kyant.shapes.UnevenRoundedRectangle

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
    val configuration = LocalConfiguration.current

    return remember(view, configuration, padding, topLeft, topRight, bottomRight, bottomLeft) {
        fun getCornerRadius(position: Int, enabled: Boolean): Dp {
            if (!enabled) return 0.dp

            val insets = view.rootWindowInsets
            val radius =
                with(density) { insets.getRoundedCorner(position)?.radius?.toFloat()?.toDp() }

            return if (radius == null || radius <= 16f.dp) (16f.dp - padding).coerceAtLeast(0.dp) else (radius - padding).coerceAtLeast(
                0.dp
            )
        }


        val topLeftRadius = getCornerRadius(RoundedCorner.POSITION_TOP_LEFT, topLeft)
        val topRightRadius = getCornerRadius(RoundedCorner.POSITION_TOP_RIGHT, topRight)
        val bottomRightRadius = getCornerRadius(RoundedCorner.POSITION_BOTTOM_RIGHT, bottomRight)
        val bottomLeftRadius = getCornerRadius(RoundedCorner.POSITION_BOTTOM_LEFT, bottomLeft)

        UnevenRoundedRectangle(
            topStart = topLeftRadius,
            topEnd = topRightRadius,
            bottomEnd = bottomRightRadius,
            bottomStart = bottomLeftRadius
        )
    }
}
