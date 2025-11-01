package com.nevoit.cresto.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nevoit.cresto.R
import com.nevoit.cresto.ui.components.glasense.GlasenseSwitch

@Composable
fun ColorModeSelector(
    backgroundColor: Color,
    currentMode: Int,
    colorMode: Int,
    onChange: (Int) -> Unit
) {
    val isAutomatic = currentMode == 0

    val onBackground = MaterialTheme.colorScheme.onBackground
    val density = LocalDensity.current
    val dp = with(density) {
        1.dp.toPx()
    }
    var returnMode by remember { mutableIntStateOf(0) }
    val transparency by animateFloatAsState(
        targetValue = if (isAutomatic) .6f else 1f,
        animationSpec = tween(durationMillis = 200),
    )
    ConfigItemContainer(
        backgroundColor = backgroundColor
    ) {
        Column() {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .graphicsLayer { alpha = transparency },
                horizontalArrangement = Arrangement.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(R.drawable.light_mode),
                        contentDescription = "Light Mode Image",
                        modifier = Modifier
                            .width(64.dp)
                            .height(128.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Light", fontSize = 16.sp, lineHeight = 16.sp)
                }
                Spacer(modifier = Modifier.width(72.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(R.drawable.dark_mode),
                        contentDescription = "Dark Mode Image",
                        modifier = Modifier
                            .width(64.dp)
                            .height(128.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Dark", fontSize = 16.sp, lineHeight = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Spacer(
                modifier = Modifier
                    .drawBehind {
                        drawLine(
                            color = onBackground.copy(.1f),
                            start = Offset(x = 0f, y = 0f),
                            end = Offset(this.size.width, y = 0f),
                            strokeWidth = dp
                        )
                    }
                    .fillMaxWidth()
                    .height(0.dp))
            Spacer(modifier = Modifier.height(8.dp))
            ConfigItem(title = "Automatic") {
                GlasenseSwitch(
                    enabled = true,
                    checked = isAutomatic,
                    onCheckedChange = {
                        returnMode = if (currentMode == 0) {
                            if (colorMode == 0) 1 else 2
                        } else {
                            0
                        }
                        onChange(returnMode)
                    })
            }
        }
    }
}