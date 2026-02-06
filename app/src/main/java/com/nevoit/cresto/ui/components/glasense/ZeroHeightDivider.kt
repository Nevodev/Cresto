package com.nevoit.cresto.ui.components.glasense

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nevoit.cresto.ui.theme.glasense.AppColors

/**
 * A composable that draws a horizontal line with zero height.
 * This is useful for creating dividers that don't take up any vertical space in the layout.
 *
 * @param color The color of the divider line.
 * @param width The stroke width of the divider line.
 * @param modifier The modifier to be applied to the divider.
 * @param blendMode The blend mode to be applied when drawing the line.
 */
@Composable
fun ZeroHeightDivider(
    modifier: Modifier = Modifier,
    color: Color = AppColors.content.copy(.1f),
    width: Dp = 1.dp,
    blendMode: BlendMode = BlendMode.SrcOver
) {
    Spacer(
        modifier = Modifier
            .then(modifier)
            .drawBehind {
                drawLine(
                    color = color,
                    start = Offset(x = 0f, y = 0f),
                    end = Offset(this.size.width, y = 0f),
                    strokeWidth = width.toPx(),
                    blendMode = blendMode
                )
            }
            .fillMaxWidth()
            .height(0.dp))
}
