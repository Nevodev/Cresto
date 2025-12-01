package com.nevoit.cresto.ui.components.glasense

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.capsule.ContinuousCapsule
import com.nevoit.cresto.ui.theme.glasense.NavigationButtonActiveColors
import com.nevoit.cresto.ui.theme.glasense.NavigationButtonNormalColors
import com.nevoit.cresto.ui.theme.glasense.isAppInDarkTheme
import com.nevoit.cresto.util.g2

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
    content: @Composable () -> Unit
) {
    val darkTheme = isAppInDarkTheme()
    // Modifier for drawing the button's background based on its state.
    val finalModifier = if (isActive) {
        // Active state: draw a simple gradient outline.
        Modifier
            .fillMaxSize()
            .drawBehind {
                val outline = ContinuousCapsule(g2).createOutline(
                    size = Size(this.size.width - 1.5.dp.toPx(), this.size.height - 1.5.dp.toPx()),
                    layoutDirection = this.layoutDirection,
                    density = this
                )
                val gradientBrush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to Color.White.copy(alpha = 0.2f),
                        1.0f to Color.White.copy(alpha = 0.02f)
                    )
                )
                translate(0.75.dp.toPx(), 0.75.dp.toPx()) {
                    drawOutline(
                        outline = outline,
                        brush = gradientBrush,
                        style = Stroke(width = 1.5.dp.toPx()),
                        blendMode = BlendMode.Plus
                    )
                }
            }
    } else {
        // Inactive state: draw a blurred backdrop with different effects for light/dark theme.
        Modifier
            .fillMaxSize()
            .drawPlainBackdrop(
                backdrop = backdrop,
                shape = { ContinuousCapsule },
                effects = {
                    blur(32f.dp.toPx(), TileMode.Decal)
                },
                onDrawSurface = {
                    val outline = ContinuousCapsule(g2).createOutline(
                        size = Size(
                            this.size.width - 1.5.dp.toPx(),
                            this.size.height - 1.5.dp.toPx()
                        ),
                        layoutDirection = this.layoutDirection,
                        density = this
                    )
                    val gradientBrush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.White.copy(alpha = 0.2f),
                            1.0f to Color.White.copy(alpha = 0.02f)
                        )
                    )
                    // Light theme inactive style.
                    if (!darkTheme && !isActive) {
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
                        translate(0.75.dp.toPx(), 0.75.dp.toPx()) {
                            drawOutline(
                                outline = outline,
                                brush = gradientBrush,
                                style = Stroke(width = 1.5.dp.toPx()),
                                blendMode = BlendMode.Plus
                            )
                        }
                        // Dark theme inactive style.
                    } else if (darkTheme && !isActive) {
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
                        translate(0.75.dp.toPx(), 0.75.dp.toPx()) {
                            drawOutline(
                                outline = outline,
                                brush = gradientBrush,
                                style = Stroke(width = 1.5.dp.toPx()),
                                blendMode = BlendMode.Plus
                            )
                        }
                    }
                }
            )
    }

    // The base button with shape, click handling, shadow, and colors.
    GlasenseButton(
        shape = ContinuousCapsule(g2),
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
