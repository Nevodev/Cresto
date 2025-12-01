package com.nevoit.cresto.ui.components.packed

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.nevoit.cresto.R
import com.nevoit.cresto.ui.components.glasense.GlasenseCheckbox
import com.nevoit.cresto.ui.components.glasense.GlasenseSwitch
import com.nevoit.cresto.ui.components.glasense.ZeroHeightDivider
import com.nevoit.cresto.ui.components.glasense.rememberCheckBoxState

@Composable
fun ColorModeSelector(
    backgroundColor: Color,
    currentMode: Int,
    onChange: (Int) -> Unit
) {
    // 0 is light, 1 is dark, 2 is auto
    val isAutomatic = currentMode == 2

    val onBackground = MaterialTheme.colorScheme.onBackground
    var returnMode by remember { mutableIntStateOf(0) }
    val transparency by animateFloatAsState(
        targetValue = if (isAutomatic) .6f else 1f,
        animationSpec = tween(durationMillis = 200),
    )
    val systemColorMode = isSystemInDarkTheme()
    val selectionState =
        rememberCheckBoxState(
            initialSelection = if (currentMode == 1) "dark" else if (currentMode == 0) "light" else {
                if (systemColorMode) "dark" else "light"
            }
        )
    LaunchedEffect(systemColorMode) {
        if (systemColorMode && isAutomatic) {
            selectionState.select("dark")
        }
        if (!systemColorMode && isAutomatic) {
            selectionState.select("light")
        }
    }
    LaunchedEffect(selectionState.selectedValue) {
        if (selectionState.selectedValue == "light" && !isAutomatic && currentMode != 0) {
            returnMode = 0
            onChange(returnMode)
        }
        if (selectionState.selectedValue == "dark" && !isAutomatic && currentMode != 1) {
            returnMode = 1
            onChange(returnMode)
        }
    }
    val interactionSource = remember { MutableInteractionSource() }

    ConfigItemContainer(
        backgroundColor = backgroundColor
    ) {
        Column {
            Box {
                if (isAutomatic) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = {})
                            .zIndex(99f)
                    )
                }
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
                        Spacer(modifier = Modifier.height(8.dp))
                        GlasenseCheckbox(state = selectionState, value = "light")
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
                        Spacer(modifier = Modifier.height(8.dp))
                        GlasenseCheckbox(state = selectionState, value = "dark")
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            ZeroHeightDivider(
                color = onBackground.copy(.1f),
                width = 1.dp,
                blendMode = BlendMode.SrcOver
            )
            Spacer(modifier = Modifier.height(8.dp))
            ConfigItem(title = "Automatic") {
                GlasenseSwitch(
                    enabled = true,
                    checked = isAutomatic,
                    onCheckedChange = { isChecked ->
                        val newMode = if (isChecked) {
                            selectionState.select(if (systemColorMode) "dark" else "light")
                            2
                        } else {
                            selectionState.select(if (systemColorMode) "dark" else "light")
                            if (systemColorMode) 1 else 0
                        }
                        onChange(newMode)
                    })
            }
        }
    }
}