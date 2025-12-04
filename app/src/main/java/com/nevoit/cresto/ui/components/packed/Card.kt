package com.nevoit.cresto.ui.components.packed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.capsule.ContinuousRoundedRectangle
import com.nevoit.cresto.ui.theme.glasense.CalculatedColor
import com.nevoit.cresto.util.g2

@Composable
fun CardWithTitle(
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    title: String,
    content: @Composable () -> Unit,
) {
    val hierarchicalSurfaceColor = CalculatedColor.hierarchicalSurfaceColor

    Column(
        modifier = modifier
            .background(
                color = hierarchicalSurfaceColor,
                shape = ContinuousRoundedRectangle(12.dp, g2)
            )
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, start = 12.dp, end = 12.dp, bottom = 0.dp)
                .height(16.dp)
                .then(if (icon != null) Modifier.graphicsLayer {
                    translationX = -4.dp.toPx()
                } else Modifier),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    modifier = Modifier.requiredSize(24.dp),
                    tint = MaterialTheme.colorScheme.onBackground.copy(.5f)
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = title,
                fontSize = 14.sp,
                lineHeight = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(.5f),
                modifier = Modifier
                    .weight(1f)
                    .requiredHeight(48.dp)
                    .wrapContentHeight(align = Alignment.CenterVertically),
                maxLines = 1,
                textAlign = TextAlign.Start
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 12.dp, start = 12.dp, end = 12.dp)
        ) { content() }
    }
}

@Composable
fun CardWithoutTitle(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val hierarchicalSurfaceColor = CalculatedColor.hierarchicalSurfaceColor
    Box(
        modifier = modifier
            .background(
                color = hierarchicalSurfaceColor,
                shape = ContinuousRoundedRectangle(12.dp, g2)
            )
            .fillMaxSize()
            .padding(12.dp)
    ) { content() }
}