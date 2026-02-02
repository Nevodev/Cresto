package com.nevoit.cresto.ui.components.glasense

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.capsule.ContinuousCapsule
import com.nevoit.cresto.ui.theme.glasense.AppColors
import com.nevoit.cresto.ui.theme.glasense.NavigationButtonActiveColors
import com.nevoit.cresto.ui.theme.glasense.NavigationButtonNormalColors
import com.nevoit.cresto.ui.theme.glasense.glasenseHighlight
import com.nevoit.cresto.ui.theme.glasense.isAppInDarkTheme

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
    val darkTheme = isAppInDarkTheme()
    val tint = AppColors.primary
    // Modifier for drawing the button's background based on its state.
    val finalModifier = if (isActive && !liquidGlass) {
        Modifier
            .fillMaxSize()
            .glasenseHighlight(100.dp)
    } else {
        Modifier
            .fillMaxSize()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { ContinuousCapsule },
                shadow = null,
                innerShadow = null,
                highlight = { if (liquidGlass) Highlight.Default else null },
                effects = {
                    blur(if (liquidGlass) 8f.dp.toPx() else 32f.dp.toPx(), TileMode.Decal)
                    if (liquidGlass) lens(16f.dp.toPx(), 32f.dp.toPx())
                },
                onDrawSurface = {
                    if (!isActive) {
                        if (!darkTheme) {
                            drawRect(
                                brush = SolidColor(Color(0xFF888888).copy(alpha = 0.7f)),
                                style = Fill,
                                blendMode = BlendMode.Luminosity
                            )
                            drawRect(
                                brush = SolidColor(Color(0xFF5F5F5F).copy(alpha = 1f)),
                                style = Fill,
                                blendMode = BlendMode.ColorDodge
                            )
                            drawRect(
                                brush = SolidColor(Color(0xFF555555).copy(alpha = 0.5f)),
                                style = Fill,
                                blendMode = BlendMode.ColorDodge
                            )
                            drawRect(
                                brush = SolidColor(Color(0xFFFFFFFF).copy(alpha = 0.1f)),
                                style = Fill,
                                blendMode = BlendMode.SrcOver
                            )
                            // Dark theme inactive style.
                        } else {
                            drawRect(
                                brush = SolidColor(Color(0xFFFFFFFF).copy(alpha = 0.1f)),
                                style = Fill
                            )
                            drawRect(
                                brush = SolidColor(Color(0xFF555555).copy(alpha = 0.5f)),
                                style = Fill,
                                blendMode = BlendMode.ColorDodge
                            )
                            drawRect(
                                brush = SolidColor(Color(0xFF000000).copy(alpha = 0.2f)),
                                style = Fill,
                                blendMode = BlendMode.Luminosity
                            )
                            drawRect(
                                brush = SolidColor(Color(0xFF000000).copy(alpha = 0.2f)),
                                style = Fill,
                                blendMode = BlendMode.Overlay
                            )
                            drawRect(
                                brush = SolidColor(Color(0xFF595959).copy(alpha = 0.4f)),
                                style = Fill,
                                blendMode = BlendMode.Luminosity
                            )
                        }
                    }
                    if (liquidGlass && isActive) {
                        drawRect(tint, blendMode = BlendMode.Hue, alpha = .8f)
                        drawRect(tint.copy(alpha = 0.7f))
                    }
                }
            )
            .then(if (!liquidGlass) Modifier.glasenseHighlight(100.dp) else Modifier)
    }

    // The base button with shape, click handling, shadow, and colors.
    GlasenseButton(
        shape = ContinuousCapsule,
        onClick = onClick,
        modifier = modifier
            .fillMaxHeight()
            .dropShadow(
                ContinuousCapsule,
                Shadow(
                    radius = 24.dp,
                    color = Color.Black.copy(alpha = 0.08f),
                    spread = 0.dp,
                    offset = DpOffset(0.dp, 8.dp)
                )
            ),
        colors = if (isActive) NavigationButtonActiveColors.primary() else NavigationButtonNormalColors.primary(),
        animated = false
    ) {
        // Box to apply the background modifier and center the content.
        Box(modifier = finalModifier, contentAlignment = Alignment.Center) {
            content()
        }
    }
}
