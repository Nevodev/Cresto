package com.nevoit.cresto.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import com.materialkolor.ktx.harmonize
import com.nevoit.cresto.feature.settings.util.SettingsManager
import com.nevoit.glasense.theme.values.Blue500
import com.nevoit.glasense.theme.values.Gray500
import com.nevoit.glasense.theme.values.Green500
import com.nevoit.glasense.theme.values.Orange500
import com.nevoit.glasense.theme.values.Purple500
import com.nevoit.glasense.theme.values.Red500
import com.nevoit.glasense.theme.values.Yellow500

@Composable
fun getFlagColor(flag: Int): Color {
    return when (flag) {
        0 -> Color.Transparent
        1 -> Red500
        2 -> Orange500
        3 -> Yellow500
        4 -> Green500
        5 -> Blue500
        6 -> Purple500
        7 -> Gray500
        else -> Gray500
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


fun Color.adjustSaturationInOklab(factor: Float): Color {
    val oklabColor = this.convert(ColorSpaces.Oklab)

    val l = oklabColor.red
    val a = oklabColor.green
    val b = oklabColor.blue

    val newA = a * factor
    val newB = b * factor

    val newOklabColor = Color(
        red = l,
        green = newA,
        blue = newB,
        alpha = this.alpha,
        colorSpace = ColorSpaces.Oklab
    )

    return newOklabColor.convert(ColorSpaces.Srgb)
}
