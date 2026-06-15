package com.nevoit.cresto.ui.components.glasense

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.effect
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.NavigationButtonActiveColors
import com.nevoit.cresto.theme.NavigationButtonNormalColors
import com.nevoit.cresto.ui.components.glasense.material.MaterialRecipes
import com.nevoit.cresto.ui.components.glasense.material.rememberMaterialRenderEffect

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

    val renderEffect =
        rememberMaterialRenderEffect(if (liquidGlass) MaterialRecipes.thin() else MaterialRecipes.appBar())

    val finalModifier =
        Modifier
            .fillMaxSize()
            .drawBackdrop(
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
                highlight = {
                    if (liquidGlass) Highlight.Default.copy(
                        style = HighlightStyle.Default(
                            angle = 90f
                        )
                    ) else null
                },
                effects = {
                    padding = if (liquidGlass) 8f.dp.toPx() * 2 else 32f.dp.toPx() * 2
                    if (!isActive) {
                        effect(renderEffect)
                        blur(if (liquidGlass) 8f.dp.toPx() else 32f.dp.toPx(), TileMode.Clamp)
                        if (liquidGlass) lens(16f.dp.toPx(), 48f.dp.toPx())
                    }
                    if (isActive) {
                        if (liquidGlass) {
                            blur(8f.dp.toPx(), TileMode.Clamp)
                            lens(16f.dp.toPx(), 48f.dp.toPx())
                        }
                    }
                },
                onDrawSurface = {
                    if (liquidGlass && isActive) {
                        drawRect(tint, blendMode = BlendMode.Hue, alpha = .8f)
                        drawRect(tint.copy(alpha = 0.7f))
                    }
                    if (isActive && !liquidGlass) {
                        drawRect(tint)
                    }
                }
            )
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
