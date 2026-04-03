package com.nevoit.cresto.ui.components.glasense

import android.graphics.BlurMaskFilter
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.nativePaint
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.shapes.RoundedRectangle
import com.nevoit.cresto.R
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.isAppInDarkTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class MenuItemData(
    val text: String,
    val icon: Painter,
    val iconColor: Color = Color.Unspecified,
    val isDestructive: Boolean = false,
    val onClick: () -> Unit
) : GlasenseMenuItem

object MenuDivider : GlasenseMenuItem

data class SelectiveMenuItemData(
    val text: String,
    val icon: Painter,
    val isSelected: () -> Boolean,
    val onClick: () -> Unit
) : GlasenseMenuItem

sealed interface GlasenseMenuItem

data class MenuState(
    val isVisible: Boolean = false,
    val anchorPosition: Offset = Offset.Zero,
    val items: List<GlasenseMenuItem> = emptyList()
)

private enum class PopupCorner { LeftTop, RightTop, RightBottom, LeftBottom }

private data class PopupPlacement(
    val x: Float,
    val y: Float,
    val origin: TransformOrigin
)

private fun cornerToOrigin(corner: PopupCorner): TransformOrigin = when (corner) {
    PopupCorner.LeftTop -> TransformOrigin(0f, 0f)
    PopupCorner.RightTop -> TransformOrigin(1f, 0f)
    PopupCorner.RightBottom -> TransformOrigin(1f, 1f)
    PopupCorner.LeftBottom -> TransformOrigin(0f, 1f)
}

private fun pickPlacement(
    anchor: Offset,
    menuSize: IntSize,
    viewport: IntSize,
    marginPx: Float
): PopupPlacement {
    fun overflow(x: Float, y: Float): Float {
        val left = (marginPx - x).coerceAtLeast(0f)
        val top = (marginPx - y).coerceAtLeast(0f)
        val right = (x + menuSize.width - (viewport.width - marginPx)).coerceAtLeast(0f)
        val bottom = (y + menuSize.height - (viewport.height - marginPx)).coerceAtLeast(0f)
        return left + top + right + bottom
    }

    val candidates = listOf(
        PopupCorner.LeftTop to Offset(anchor.x, anchor.y),
        PopupCorner.RightTop to Offset(anchor.x - menuSize.width, anchor.y),
        PopupCorner.RightBottom to Offset(anchor.x - menuSize.width, anchor.y - menuSize.height),
        PopupCorner.LeftBottom to Offset(anchor.x, anchor.y - menuSize.height),
    )

    val chosen = candidates.firstOrNull { (_, p) -> overflow(p.x, p.y) == 0f }
        ?: candidates.minBy { (_, p) -> overflow(p.x, p.y) }

    val clampedX = chosen.second.x.coerceIn(
        marginPx,
        (viewport.width - menuSize.width - marginPx).coerceAtLeast(marginPx)
    )
    val clampedY = chosen.second.y.coerceIn(
        marginPx,
        (viewport.height - menuSize.height - marginPx).coerceAtLeast(marginPx)
    )

    return PopupPlacement(clampedX, clampedY, cornerToOrigin(chosen.first))
}

/**
 * A menu with GlasenseBackgroundBlur Style, using [LayerBackdrop] for blurred background.
 *
 * @param menuState State object containing menu items and anchor position.
 * @param backdrop The [LayerBackdrop] instance for rendering the background effect.
 * @param onDismiss Lambda to be called to dismiss the menu.
 * @param modifier The modifier to be applied to the menu container.
 */
@Composable
fun GlasenseMenu(
    menuState: MenuState,
    backdrop: LayerBackdrop,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuSize by remember { mutableStateOf(IntSize.Zero) }
    val windowInfo = LocalWindowInfo.current
    val viewport = IntSize(windowInfo.containerSize.width, windowInfo.containerSize.height)
    val menuWidthPx = with(LocalDensity.current) { 228.dp.roundToPx() }
    val fallbackHeightPx = with(LocalDensity.current) { 228.dp.roundToPx() }
    val effectiveMenuSize =
        if (menuSize == IntSize.Zero) IntSize(menuWidthPx, fallbackHeightPx) else menuSize
    val density = LocalDensity.current
    val placement = remember(menuState.anchorPosition, effectiveMenuSize, viewport) {
        pickPlacement(
            anchor = menuState.anchorPosition,
            menuSize = effectiveMenuSize,
            viewport = viewport,
            marginPx = with(density) { 8.dp.toPx() }
        )
    }
    val scaleAni = remember { Animatable(0.4f) }
    val alphaAni = remember { Animatable(0f) }
    var isMenuInComposition by remember { mutableStateOf(false) }

    LaunchedEffect(menuState.isVisible) {
        if (menuState.isVisible) {
            delay(50)
            isMenuInComposition = true
            coroutineScope {
                launch { scaleAni.animateTo(1f, spring(0.8f, 450f, 0.001f)) }
                launch { alphaAni.animateTo(1f) }
            }
        } else {
            delay(50)
            coroutineScope {
                launch { scaleAni.animateTo(0.4f, spring(0.7f, 600f)) }
                launch { alphaAni.animateTo(0f) }
            }
            isMenuInComposition = false
        }
    }

    val darkTheme = isAppInDarkTheme()

    val shadowRadiusPx = with(LocalDensity.current) { 32.dp.toPx() }
    val shadowDyPx = with(LocalDensity.current) { 16.dp.toPx() }

    val shadowPaint = remember {
        Paint().nativePaint.apply {
            isAntiAlias = true
            maskFilter = BlurMaskFilter(shadowRadiusPx, BlurMaskFilter.Blur.NORMAL)
        }
    }
    val shadowBaseColor = if (darkTheme) Color.Black.copy(alpha = 0.4f) else Color.Black.copy(
        alpha = 0.1f
    )

    val shape = RoundedCornerShape(16.dp)

    if (menuState.isVisible) {
        BackHandler() { onDismiss() }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )
    }
    if (isMenuInComposition) {
        Box(
            modifier = modifier
                .zIndex(99f) // Ensure the menu appears above other content.
                .width(228.dp)
                .onSizeChanged { menuSize = it }
                .graphicsLayer {
                    translationX = placement.x
                    translationY = placement.y
                    scaleX = scaleAni.value
                    scaleY = scaleAni.value
                    transformOrigin = placement.origin
                }
                .drawBehind {
                    val currentAlpha = alphaAni.value

                    if (currentAlpha > 0f) {
                        val paintColor =
                            shadowBaseColor.copy(alpha = shadowBaseColor.alpha * currentAlpha)
                        shadowPaint.color = paintColor.toArgb()

                        drawIntoCanvas { canvas ->
                            canvas.save()
                            canvas.translate(0f, shadowDyPx)

                            when (val outline =
                                shape.createOutline(size, layoutDirection, this)) {
                                is Outline.Rectangle -> {
                                    canvas.nativeCanvas.drawRect(
                                        outline.rect.left,
                                        outline.rect.top,
                                        outline.rect.right,
                                        outline.rect.bottom,
                                        shadowPaint
                                    )
                                }

                                is Outline.Rounded -> {
                                    canvas.nativeCanvas.drawRoundRect(
                                        outline.roundRect.left, outline.roundRect.top,
                                        outline.roundRect.right, outline.roundRect.bottom,
                                        outline.roundRect.bottomLeftCornerRadius.x,
                                        outline.roundRect.bottomLeftCornerRadius.y,
                                        shadowPaint
                                    )
                                }

                                is Outline.Generic -> {
                                    canvas.nativeCanvas.drawPath(
                                        outline.path.asAndroidPath(),
                                        shadowPaint
                                    )
                                }
                            }

                            canvas.restore()
                        }
                    }
                }
                // Core of the blur effect, drawing a blurred version of the content behind it.
                .drawPlainBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(16.dp) },
                    layerBlock = {
                        alpha = alphaAni.value
                    },
                    effects = {
                        blur(64f.dp.toPx(), TileMode.Mirror)
                    },
                    // Custom drawing on top of the blurred background to create stunning colors.
                    onDrawSurface = {
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
                        }
                    }
                )
                .glasenseHighlight(16.dp)
        ) {
            // Display the actual menu items.
            CustomMenuContent(items = menuState.items, onDismiss = onDismiss)
        }
    }
}

/**
 * Composable that arranges a list of menu items vertically.
 *
 * @param items The list of [MenuItemData] to display.
 * @param onDismiss Lambda to be called when a menu item is clicked, typically to close the menu.
 */
@Composable
fun CustomMenuContent(items: List<GlasenseMenuItem>, onDismiss: () -> Unit) {
    val darkTheme = isAppInDarkTheme()
    // Define divider color based on the current theme.
    val dividerColor = if (darkTheme) Color.White.copy(.1f) else Color.Black.copy(.1f)

    Column {
        items.forEachIndexed { index, item ->
            when (item) {
                is MenuItemData -> {
                    CustomMenuItem(
                        text = item.text,
                        icon = item.icon,
                        iconColor = item.iconColor,
                        isDestructive = item.isDestructive,
                        onClick = {
                            onDismiss()
                            item.onClick()
                        }
                    )
                    // Add a divider between items, but not after the last one.
                    if (index < items.size - 1 && items[index + 1] !is MenuDivider) {
                        ZeroHeightDivider(
                            color = dividerColor,
                            width = 1.dp,
                            blendMode = BlendMode.Luminosity
                        )
                    }
                }

                is MenuDivider -> {
                    Spacer(
                        modifier = Modifier
                            .graphicsLayer {
                                blendMode = BlendMode.Luminosity
                                alpha = 0.5f
                            }
                            .fillMaxWidth()
                            .height(12.dp)
                            .background(color = dividerColor)
                    )
                }

                is SelectiveMenuItemData -> {
                    CustomSelectiveMenuItem(
                        text = item.text,
                        icon = item.icon,
                        isSelected = item.isSelected,
                        onClick = {
                            onDismiss()
                            item.onClick()
                        }
                    )
                    if (index < items.size - 1 && items[index + 1] !is MenuDivider) {
                        ZeroHeightDivider(
                            color = dividerColor,
                            width = 1.dp,
                            blendMode = BlendMode.Luminosity
                        )
                    }
                }
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
    iconColor: Color,
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
            tint = if (isDestructive) contentColor else if (iconColor == Color.Unspecified) AppColors.content else iconColor,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun CustomSelectiveMenuItem(
    text: String,
    icon: Painter,
    isSelected: () -> Boolean,
    onClick: () -> Unit
) {
    val contentColor = AppColors.content
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
        if (isSelected()) {
            Icon(
                painter = painterResource(R.drawable.ic_checkmark),
                contentDescription = text,
                tint = AppColors.primary,
                modifier = Modifier
                    .size(24.dp)
                    .offset(x = (-4).dp)
            )
        } else {
            Spacer(modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            color = contentColor,
            fontSize = 16.sp,
            lineHeight = 16.sp,
            modifier = Modifier.weight(1f)
        )
        Icon(
            painter = icon,
            contentDescription = text,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
    }
}
