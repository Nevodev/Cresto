package com.nevoit.cresto.ui.theme.glasense

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color


object CalculatedColor {

    val onSurfaceContainer: Color
        @Composable
        get() {
            val isDark = isAppInDarkTheme()
            val onSurfaceColor = MaterialTheme.colorScheme.onSurface
            return if (!isDark) {
                onSurfaceColor.copy(alpha = 0.05f)
            } else {
                onSurfaceColor.copy(alpha = 0.1f)
            }
        }
    val onSurfaceContainerBold: Color
        @Composable
        get() {
            val isDark = isAppInDarkTheme()
            val onSurfaceColor = MaterialTheme.colorScheme.onSurface
            return if (!isDark) {
                onSurfaceColor.copy(alpha = 0.2f)
            } else {
                onSurfaceColor.copy(alpha = 0.4f)
            }
        }
    val hierarchicalBackgroundColor: Color
        @Composable
        get() {
            val isDark = isAppInDarkTheme()
            return if (isDark) {
                MaterialTheme.colorScheme.background
            } else {
                MaterialTheme.colorScheme.surface
            }
        }
    val hierarchicalSurfaceColor: Color
        @Composable
        get() {
            val isDark = isAppInDarkTheme()
            return if (isDark) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.background
            }
        }
}