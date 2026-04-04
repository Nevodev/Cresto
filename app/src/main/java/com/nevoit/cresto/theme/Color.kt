package com.nevoit.cresto.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.materialkolor.ktx.harmonize
import com.nevoit.cresto.feature.settings.util.SettingsManager
import com.nevoit.glasense.theme.Blue500
import com.nevoit.glasense.theme.Gray500
import com.nevoit.glasense.theme.Green500
import com.nevoit.glasense.theme.Orange500
import com.nevoit.glasense.theme.Purple500
import com.nevoit.glasense.theme.Red500
import com.nevoit.glasense.theme.Yellow500

@Composable
fun getFlagColor(flag: Int): Color {
    return when (flag) {
        0 -> Color.Transparent
        1 -> harmonize(Red500)
        2 -> harmonize(Orange500)
        3 -> harmonize(Yellow500)
        4 -> harmonize(Green500)
        5 -> harmonize(Blue500)
        6 -> harmonize(Purple500)
        7 -> harmonize(Gray500)
        else -> harmonize(Gray500)
    }
}

@Composable
fun harmonize(color: Color): Color {
    val isUseDynamicColor by SettingsManager.isUseDynamicColorState
    val primary = AppColors.primary

    return if (isUseDynamicColor) {
        color.harmonize(primary)
    } else {
        color
    }
}
