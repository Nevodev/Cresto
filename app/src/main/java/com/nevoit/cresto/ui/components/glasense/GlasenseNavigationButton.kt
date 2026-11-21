package com.nevoit.cresto.ui.components.glasense

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.effect
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.NavigationButtonActiveColors
import com.nevoit.cresto.theme.NavigationButtonNormalColors
import com.nevoit.glasense.material.MaterialRecipes
import com.nevoit.glasense.material.glass
import com.nevoit.glasense.material.rememberMaterialRenderEffectOrNull
import com.nevoit.glasense.theme.GlasenseTheme

/**
 * A custom navigation button with active and inactive states.
 *
 * @param modifier The modifier to be applied to the button.
 * @param isActive Whether the button is currently active.
 * @param onClick The callback to be invoked when the button is clicked.
 * @param backdrop The backdrop layer for the glassmorphism effect.
 * @param content The content to be displayed inside the button.
 */
@Composable
fun GlasenseNavigationButton(
    modifier: Modifier = Modifier,
    isActive: Boolean,
    onClick: () -> Unit,
    backdrop: LayerBackdrop,
    liquidGlass: Boolean = false,
    content: @Composable () -> Unit
) {
    val tint = AppColors.primary

    val materialEffect =
        rememberMaterialRenderEffectOrNull(
            if (liquidGlass) MaterialRecipes.thin() else MaterialRecipes.appBar()
        )
    val backdropModifier = materialEffect?.let { renderEffect ->
        if (liquidGlass) {
            Modifier.glass(
                backdrop = backdrop,
                shape = Capsule(),
                shadow = {
                    Shadow(
                        radius = 24.dp,
                        color = Color.Black.copy(alpha = 0.08f),
                        offset = DpOffset(0.dp, 8.dp)
                    )
                },
                materialEffect = materialEffect,
                onDrawSurface = {
                    if (isActive) {
                        drawRect(tint, blendMode = BlendMode.Hue, alpha = .8f)
                        drawRect(tint.copy(alpha = 0.7f))
                    }
                })
        } else {
            Modifier.drawBackdrop(
                backdrop = backdrop,
                shape = { Capsule() },
                shadow = {
                    Shadow(
                        radius = 24.dp,
                        color = Color.Black.copy(alpha = 0.08f),
                        offset = DpOffset(0.dp, 8.dp)
                    )
                },
                innerShadow = null,
                highlight = null,
                effects = {
                    padding = 32f.dp.toPx() * 2
                    if (!isActive) {
                        effect(renderEffect)
                        blur(32f.dp.toPx(), TileMode.Clamp)
                    }
                },
                onDrawSurface = {
                    if (isActive) {
                        drawRect(tint)
                    }
                }
            )
        }
    } ?: Modifier
        .dropShadow(
            Capsule(),
            androidx.compose.ui.graphics.shadow.Shadow(
                24.dp,
                Color.Black.copy(alpha = 0.08f),
                0.dp,
                DpOffset(0.dp, 8.dp)
            )
        )
        .clip(Capsule())
        .background(if (isActive) tint else GlasenseTheme.colors.cardBackground)

    val finalModifier =
        Modifier
            .fillMaxSize()
            .then(backdropModifier)
            .then(if (!liquidGlass) Modifier.glasenseHighlight(100.dp) else Modifier)

    // The base button with shape, click handling, shadow, and colors.
    GlasenseButton(
        shape = Capsule(),
        onClick = onClick,
        modifier = modifier
            .fillMaxHeight(),
        colors = if (isActive) NavigationButtonActiveColors.primary() else NavigationButtonNormalColors.primary(),
        animated = false
    ) {
        // Box to apply the background modifier and center the content.
        Box(modifier = finalModifier, contentAlignment = Alignment.Center) {
            content()
        }
    }
}
