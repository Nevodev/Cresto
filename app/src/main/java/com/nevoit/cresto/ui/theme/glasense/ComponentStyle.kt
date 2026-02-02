package com.nevoit.cresto.ui.theme.glasense

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

object AppButtonColors {
    @Composable
    fun primary() = ButtonDefaults.buttonColors(
        containerColor = AppColors.primary,
        contentColor = AppColors.onPrimary,
        disabledContainerColor = AppColors.content.copy(alpha = 0.1F),
        disabledContentColor = AppColors.content.copy(alpha = 0.3F)
    )

    @Composable
    fun secondary() = ButtonDefaults.buttonColors(
        containerColor = AppColors.content.copy(alpha = 0.05F),
        contentColor = AppColors.content.copy(alpha = 0.5F),
    )
}

object NavigationButtonActiveColors {
    @Composable
    fun primary() = ButtonDefaults.buttonColors(
        containerColor = AppColors.primary,
        contentColor = AppColors.onPrimary
    )
}

object NavigationButtonNormalColors {
    @Composable
    fun primary() = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.background.copy(0f),
        contentColor = AppColors.content.copy(0.8f)
    )
}