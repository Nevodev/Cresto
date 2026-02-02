package com.nevoit.cresto.ui.components.glasense

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush.Companion.verticalGradient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.capsule.ContinuousRoundedRectangle
import com.nevoit.cresto.ui.theme.glasense.AppColors
import com.nevoit.cresto.ui.theme.glasense.isAppInDarkTheme

data class MenuItemData(
    val text: String,
    val icon: Painter,
    val isDestructive: Boolean = false,
    val onClick: () -> Unit
)

data class MenuState(
    val isVisible: Boolean = false,
    val anchorPosition: Offset = Offset.Zero,
    val items: List<MenuItemData> = emptyList()
)

/**
 * Composable that arranges a list of menu items vertically.
 *
 * @param items The list of [MenuItemData] to display.
 * @param onDismiss Lambda to be called when a menu item is clicked, typically to close the menu.
 */
@Composable
fun CustomMenuContent(items: List<MenuItemData>, onDismiss: () -> Unit) {
    val darkTheme = isAppInDarkTheme()
    // Define divider color based on the current theme.
    val dividerColor = if (darkTheme) Color.White.copy(.1f) else Color.Black.copy(.1f)

    Column {
        items.forEachIndexed { index, item ->
            CustomMenuItem(
                text = item.text,
                icon = item.icon,
                isDestructive = item.isDestructive,
                onClick = {
                    onDismiss()
                    item.onClick()
                }
            )
            // Add a divider between items, but not after the last one.
            if (index < items.size - 1) {
                ZeroHeightDivider(
                    color = dividerColor,
                    width = 1.dp,
                    blendMode = BlendMode.Luminosity
                )
            }
        }
    }
}

/**
 * A single menu item with an icon, text, and a custom click feedback effect.
 *
 * @param text The text to display for the menu item.
 * @param icon The icon painter for the menu item.
 * @param isDestructive If true, the item is styled with a "destructive action" color (e.g., red).
 * @param onClick Lambda to be executed when the item is clicked.
 */
@Composable
private fun CustomMenuItem(
    text: String,
    icon: Painter,
    isDestructive: Boolean,
    onClick: () -> Unit
) {
    // Determine the content color based on whether the action is destructive.
    val contentColor = if (isDestructive) AppColors.error else AppColors.content
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .height(48.dp)
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                onClick = onClick,
                indication = DimIndication()
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = 16.sp,
            lineHeight = 16.sp
        )
        Icon(
            painter = icon,
            contentDescription = text,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * A menu with GlasenseBackgroundBlur Style, using [LayerBackdrop] for blurred background.
 *
 * @param density The screen density, used for pixel conversions.
 * @param menuState State object containing menu items and anchor position.
 * @param backdrop The [LayerBackdrop] instance for rendering the background effect.
 * @param onDismiss Lambda to be called to dismiss the menu.
 * @param modifier The modifier to be applied to the menu container.
 * @param alphaAni A lambda providing the current alpha for animations.
 */
@Composable
fun GlasenseMenu(
    density: Density,
    menuState: MenuState,
    backdrop: LayerBackdrop,
    onDismiss: () -> Unit,
    modifier: Modifier,
    alphaAni: () -> Float
) {
    val darkTheme = isAppInDarkTheme()
    Box(
        modifier = Modifier
            // Position the menu at the anchor point defined in menuState.
            .offset(
                x = with(density) { menuState.anchorPosition.x.toDp() },
                y = with(density) { menuState.anchorPosition.y.toDp() }
            )
            .zIndex(99f) // Ensure the menu appears above other content.
            .then(modifier)
            .dropShadow(
                RoundedCornerShape(16.dp),
                shadow = Shadow(
                    radius = 32.dp,
                    color = if (darkTheme) Color.Black.copy(alpha = 0.4f) else Color.Black.copy(
                        alpha = 0.1f
                    ),
                    offset = DpOffset(0.dp, 16.dp),
                    alpha = alphaAni()
                )
            )
            // Core of the blur effect, drawing a blurred version of the content behind it.
            .drawPlainBackdrop(
                backdrop = backdrop,
                shape = { ContinuousRoundedRectangle(16.dp) },
                layerBlock = {
                    alpha = alphaAni()
                },
                effects = {
                    blur(64f.dp.toPx(), TileMode.Mirror)
                },
                // Custom drawing on top of the blurred background to create stunning colors.
                onDrawSurface = {
                    val outline = ContinuousRoundedRectangle(16.dp).createOutline(
                        size = size,
                        layoutDirection = LayoutDirection.Ltr,
                        density = density
                    )
                    val gradientBrush = verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.White.copy(alpha = 1f),
                            1.0f to Color.White.copy(alpha = 0.2f)
                        )
                    )
                    // The drawing logic is different for light and dark themes.
                    if (!darkTheme) {
                        drawRect(
                            brush = SolidColor(Color(0xFF272727).copy(alpha = 0.2f)),
                            style = Fill,
                            blendMode = BlendMode.Luminosity,
                        )
                        drawRect(
                            brush = SolidColor(Color(0xFF252525).copy(alpha = 1f)),
                            style = Fill,
                            blendMode = BlendMode.Plus,
                        )
                        drawRect(
                            brush = SolidColor(Color(0xFF555555).copy(alpha = 0.5f)),
                            style = Fill,
                            blendMode = BlendMode.ColorDodge,
                        )
                        drawRect(
                            brush = SolidColor(Color(0xFFFFFFFF).copy(alpha = 0.2f)),
                            style = Fill,
                            blendMode = BlendMode.SrcOver,
                        )
                        drawOutline(
                            outline = outline,
                            brush = gradientBrush,
                            style = Stroke(width = 3.dp.toPx()),
                            blendMode = BlendMode.Plus,
                            alpha = 0.08f
                        )
                    } else {
                        drawRect(
                            brush = SolidColor(Color(0xFF000000).copy(alpha = 0.5f)),
                            style = Fill,
                            blendMode = BlendMode.Luminosity,
                        )
                        drawRect(
                            brush = SolidColor(Color(0xFF252525).copy(alpha = 1f)),
                            style = Fill,
                            blendMode = BlendMode.Plus,
                        )
                        drawRect(
                            brush = SolidColor(Color(0xFF4B4B4B).copy(alpha = 0.5f)),
                            style = Fill,
                            blendMode = BlendMode.ColorDodge,
                        )
                        drawRect(
                            brush = SolidColor(Color(0xFF000000).copy(alpha = 0.3f)),
                            style = Fill,
                            blendMode = BlendMode.SrcOver,
                        )
                        drawOutline(
                            outline = outline,
                            brush = gradientBrush,
                            style = Stroke(width = 3.dp.toPx()),
                            blendMode = BlendMode.Plus,
                            alpha = 0.08f
                        )
                    }
                }
            )
    ) {
        // Display the actual menu items.
        CustomMenuContent(items = menuState.items, onDismiss = onDismiss)
    }
}
