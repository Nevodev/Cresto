package com.nevoit.glasense.core.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nevoit.glasense.theme.LocalGlasenseContentColor

@Composable
fun VDivider(
    modifier: Modifier = Modifier,
    color: Color = LocalGlasenseContentColor.current.copy(.1f),
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

@Composable
fun HDivider(
    modifier: Modifier = Modifier,
    color: Color = LocalGlasenseContentColor.current.copy(.1f),
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
                    end = Offset(x = 0f, y = this.size.height),
                    strokeWidth = width.toPx(),
                    blendMode = blendMode
                )
            }
            .fillMaxHeight()
            .width(0.dp))
}
