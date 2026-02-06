package com.nevoit.cresto.ui.theme.glasense

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.nevoit.cresto.ui.components.glasense.GlasenseButtonColors

object AppButtonColors {
    @Composable
    fun primary() = GlasenseButtonColors(
        containerColor = AppColors.primary,
        contentColor = AppColors.onPrimary,
        disabledContainerColor = AppColors.content.copy(alpha = 0.1F),
        disabledContentColor = AppColors.content.copy(alpha = 0.3F)
    )

    @Composable
    fun secondary() = GlasenseButtonColors(
        containerColor = AppColors.content.copy(alpha = 0.05F),
        contentColor = AppColors.content.copy(alpha = 0.5F),
        disabledContainerColor = AppColors.content.copy(alpha = 0.025F),
        disabledContentColor = AppColors.content.copy(alpha = 0.25F)
    )

    @Composable
    fun action() = GlasenseButtonColors(
        containerColor = AppColors.scrimNormal,
        contentColor = AppColors.primary,
        disabledContainerColor = AppColors.scrimNormal.copy(0.5f),
        disabledContentColor = AppColors.primary.copy(0.5f)
    )

    @Composable
    fun solid(color: Color, contentColor: Color) = GlasenseButtonColors(
        containerColor = color,
        contentColor = contentColor,
        disabledContainerColor = color.copy(0.5f),
        disabledContentColor = contentColor.copy(0.5f)
    )
}

object NavigationButtonActiveColors {
    @Composable
    fun primary() = GlasenseButtonColors(
        containerColor = AppColors.primary,
        contentColor = AppColors.onPrimary,
        disabledContainerColor = AppColors.primary.copy(0.5f),
        disabledContentColor = AppColors.onPrimary.copy(0.5f)
    )
}

object NavigationButtonNormalColors {
    @Composable
    fun primary() = GlasenseButtonColors(
        containerColor = Color.Transparent,
        contentColor = AppColors.content.copy(0.8f),
        disabledContainerColor = Color.Transparent,
        disabledContentColor = AppColors.content.copy(0.4f)
    )
}