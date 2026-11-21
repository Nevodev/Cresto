package com.nevoit.cresto.navigation.shape

import android.os.Build
import android.view.RoundedCorner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import com.kyant.shapes.UnevenRoundedRectangle

internal data class DeviceCornerRadii(
    val topLeft: Int = 0,
    val topRight: Int = 0,
    val bottomRight: Int = 0,
    val bottomLeft: Int = 0,
)

internal class DeviceCornerState {
    var radii by mutableStateOf(DeviceCornerRadii())
        private set

    fun update(insets: android.view.WindowInsets) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

        val topLeft =
            insets.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)?.radius ?: 0
        val topRight =
            insets.getRoundedCorner(RoundedCorner.POSITION_TOP_RIGHT)?.radius ?: 0
        val bottomRight =
            insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_RIGHT)?.radius ?: 0
        val bottomLeft =
            insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT)?.radius ?: 0

        val previous = radii
        if (
            previous.topLeft == topLeft &&
            previous.topRight == topRight &&
            previous.bottomRight == bottomRight &&
            previous.bottomLeft == bottomLeft
        ) {
            return
        }

        radii = DeviceCornerRadii(
            topLeft = topLeft,
            topRight = topRight,
            bottomRight = bottomRight,
            bottomLeft = bottomLeft,
        )
    }
}

internal val LocalDeviceCorners =
    staticCompositionLocalOf<DeviceCornerState> {
        error("DeviceCornerState has not been provided")
    }

private val NoDeviceCornerRadii = DeviceCornerRadii()

@Composable
internal fun deviceCornerShape(
    padding: Dp = 0.dp,
    topLeft: Dp? = null,
    topRight: Dp? = null,
    bottomRight: Dp? = null,
    bottomLeft: Dp? = null,
    minRadius: Dp = 0.dp,
): UnevenRoundedRectangle {
    val needsDeviceCorners =
        topLeft == null ||
                topRight == null ||
                bottomRight == null ||
                bottomLeft == null

    val radii =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && needsDeviceCorners) {
            LocalDeviceCorners.current.radii
        } else {
            NoDeviceCornerRadii
        }

    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    return remember(
        radii,
        density,
        layoutDirection,
        padding,
        topLeft,
        topRight,
        bottomRight,
        bottomLeft,
        minRadius,
    ) {
        val minimum = minRadius.coerceAtLeast(0.dp)

        fun radius(devicePx: Int, override: Dp?): Dp {
            if (override != null) return override
            if (devicePx <= 0) return minimum

            return with(density) {
                (devicePx.toDp() - padding).coerceAtLeast(minimum)
            }
        }

        val tl = radius(radii.topLeft, topLeft)
        val tr = radius(radii.topRight, topRight)
        val br = radius(radii.bottomRight, bottomRight)
        val bl = radius(radii.bottomLeft, bottomLeft)
        val isLtr = layoutDirection == LayoutDirection.Ltr

        UnevenRoundedRectangle(
            topStart = if (isLtr) tl else tr,
            topEnd = if (isLtr) tr else tl,
            bottomStart = if (isLtr) bl else br,
            bottomEnd = if (isLtr) br else bl,
        )
    }
}