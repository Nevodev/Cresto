package com.nevoit.cresto.ui.theme.glasense

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush.Companion.verticalGradient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.capsule.ContinuousRoundedRectangle
import com.nevoit.cresto.util.g2

fun Modifier.glasenseHighlight(
    cornerRadius: Dp = 12.dp,
    strokeWidth: Dp = 3.dp
): Modifier = this.drawBehind() {
    val outline =
        ContinuousRoundedRectangle(cornerRadius, g2).createOutline(
            size,
            layoutDirection,
            this
        )
    val gradientBrush = verticalGradient(
        colorStops = arrayOf(
            0.0f to Color.White.copy(alpha = 0.2f),
            1.0f to Color.White.copy(alpha = 0.02f)
        )
    )
    drawOutline(
        outline = outline,
        brush = gradientBrush,
        style = Stroke(width = strokeWidth.toPx()),
        blendMode = BlendMode.Plus
    )
}