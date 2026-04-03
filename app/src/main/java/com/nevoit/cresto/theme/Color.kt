package com.nevoit.cresto.theme

import androidx.compose.ui.graphics.Color
import com.nevoit.glasense.theme.Blue500
import com.nevoit.glasense.theme.Gray500
import com.nevoit.glasense.theme.Green500
import com.nevoit.glasense.theme.Orange500
import com.nevoit.glasense.theme.Purple500
import com.nevoit.glasense.theme.Red500
import com.nevoit.glasense.theme.Yellow500

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
