package com.nevoit.cresto.ui.components.packed

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nevoit.cresto.R
import com.nevoit.cresto.feature.settings.CustomSwitchRow
import com.nevoit.cresto.ui.components.glasense.GlasenseCheckbox
import com.nevoit.glasense.component.ListScope
import com.nevoit.glasense.core.component.Text
import com.nevoit.glasense.theme.GlasenseThemeMode

private enum class ColorMode(val value: Int) {
    Light(GlasenseThemeMode.LIGHT),
    Dark(GlasenseThemeMode.DARK),
    System(GlasenseThemeMode.SYSTEM);

    companion object {
        fun from(value: Int): ColorMode {
            return entries.firstOrNull { it.value == value } ?: System
        }
    }
}

private fun ColorMode.effectiveMode(systemInDarkTheme: Boolean): ColorMode {
    return when (this) {
        ColorMode.Light,
        ColorMode.Dark -> this

        ColorMode.System -> if (systemInDarkTheme) ColorMode.Dark else ColorMode.Light
    }
}

fun ListScope.ColorModeSelector(
    currentMode: Int,
    systemInDarkTheme: Boolean,
    onChange: (Int) -> Unit
) {
    val colorMode = ColorMode.from(currentMode)
    val isAutomatic = colorMode == ColorMode.System
    val effectiveMode = colorMode.effectiveMode(systemInDarkTheme)

    Section {
        Row {
            ColorModeOptions(
                selectedMode = effectiveMode,
                enabled = !isAutomatic,
                onModeSelected = { selectedMode ->
                    if (selectedMode != colorMode) {
                        onChange(selectedMode.value)
                    }
                }
            )
        }
        CustomSwitchRow(
            checked = isAutomatic,
            onCheckedChange = { isChecked ->
                val newMode = if (isChecked) ColorMode.System else effectiveMode
                if (newMode != colorMode) {
                    onChange(newMode.value)
                }
            }) {
            Text(stringResource(R.string.automatic))
        }
    }
}

@Composable
private fun ColorModeOptions(
    selectedMode: ColorMode,
    enabled: Boolean,
    onModeSelected: (ColorMode) -> Unit
) {
    val transparency by animateFloatAsState(
        targetValue = if (enabled) 1f else .6f,
        animationSpec = tween(durationMillis = 200),
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 8.dp)
            .graphicsLayer { alpha = transparency },
        horizontalArrangement = Arrangement.Center
    ) {
        ColorModeOption(
            mode = ColorMode.Light,
            selected = selectedMode == ColorMode.Light,
            enabled = enabled,
            imageRes = R.drawable.light_mode,
            imageContentDescription = stringResource(R.string.light_mode_image),
            label = stringResource(R.string.light_mode),
            onSelected = onModeSelected
        )
        Spacer(modifier = Modifier.width(72.dp))
        ColorModeOption(
            mode = ColorMode.Dark,
            selected = selectedMode == ColorMode.Dark,
            enabled = enabled,
            imageRes = R.drawable.dark_mode,
            imageContentDescription = stringResource(R.string.dark_mode_image),
            label = stringResource(R.string.dark_mode),
            onSelected = onModeSelected
        )
    }
}

@Composable
private fun ColorModeOption(
    mode: ColorMode,
    selected: Boolean,
    enabled: Boolean,
    imageRes: Int,
    imageContentDescription: String,
    label: String,
    onSelected: (ColorMode) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = imageContentDescription,
            modifier = Modifier
                .width(64.dp)
                .height(128.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 16.sp,
            lineHeight = 16.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        GlasenseCheckbox(
            checked = selected,
            onCheckedChange = { checked ->
                if (checked) {
                    onSelected(mode)
                }
            },
            enabled = enabled,
            role = Role.RadioButton
        )
    }
}
