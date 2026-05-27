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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.nativePaint
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.effect
import com.kyant.shapes.RoundedRectangle
import com.nevoit.cresto.R
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.isAppInDarkTheme
import com.nevoit.cresto.ui.components.glasense.material.MaterialRecipes
import com.nevoit.cresto.ui.components.glasense.material.rememberMaterialRenderEffect
import com.nevoit.glasense.core.component.Icon
import com.nevoit.glasense.core.component.Text
import com.nevoit.glasense.core.component.VDivider
import com.nevoit.glasense.core.interaction.DimIndication
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.max

data class MenuItemData(
    val text: String,
    val icon: Painter? = null,
    val iconColor: Color = Color.Unspecified,
    val isDestructive: Boolean = false,
    val onClick: () -> Unit
) : GlasenseMenuItem

object MenuDivider : GlasenseMenuItem

data class SelectiveMenuItemData(
    val text: String,
    val icon: Painter? = null,
    val isSelected: () -> Boolean,
    val onClick: () -> Unit
) : GlasenseMenuItem

sealed interface GlasenseMenuItem

data class MenuState(
    val isVisible: Boolean = false,
    val anchorBounds: Rect = Rect.Zero,
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
    anchorBounds: Rect,
    menuSize: IntSize,
    availableBounds: Rect,
    marginPx: Float,
    gapPx: Float
): PopupPlacement {
    val minX = availableBounds.left + marginPx
    val minY = availableBounds.top + marginPx
    val maxX = (availableBounds.right - menuSize.width - marginPx).coerceAtLeast(minX)
    val maxY = (availableBounds.bottom - menuSize.height - marginPx).coerceAtLeast(minY)

    fun overflow(x: Float, y: Float): Float {
        val left = (minX - x).coerceAtLeast(0f)
        val top = (minY - y).coerceAtLeast(0f)
        val right = (x - maxX).coerceAtLeast(0f)
        val bottom = (y - maxY).coerceAtLeast(0f)
        return left + top + right + bottom
    }

    val candidates = listOf(
        PopupCorner.LeftTop to Offset(anchorBounds.left, anchorBounds.bottom + gapPx),
        PopupCorner.RightTop to Offset(
            anchorBounds.right - menuSize.width,
            anchorBounds.bottom + gapPx
        ),
        PopupCorner.RightBottom to Offset(
            anchorBounds.right - menuSize.width,
            anchorBounds.top - menuSize.height - gapPx
        ),
        PopupCorner.LeftBottom to Offset(
            anchorBounds.left,
            anchorBounds.top - menuSize.height - gapPx
        ),
    )

    val chosen = candidates.firstOrNull { (_, p) -> overflow(p.x, p.y) == 0f }
        ?: candidates.minBy { (_, p) -> overflow(p.x, p.y) }

    val clampedX = chosen.second.x.coerceIn(minX, maxX)
    val clampedY = chosen.second.y.coerceIn(minY, maxY)

    return PopupPlacement(clampedX, clampedY, cornerToOrigin(chosen.first))
}

/**
 * A menu with GlasenseBackgroundBlur Style, using [LayerBackdrop] for blurred background.
 *
 * @param menuState State object containing menu items and launcher bounds.
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
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val statusBarTopPx = WindowInsets.statusBars.getTop(density).toFloat()
    val navigationBarLeftPx =
        WindowInsets.navigationBars.getLeft(density, layoutDirection).toFloat()
    val navigationBarRightPx =
        WindowInsets.navigationBars.getRight(density, layoutDirection).toFloat()
    val navigationBarBottomPx = WindowInsets.navigationBars.getBottom(density).toFloat()
    val imeBottomPx = WindowInsets.ime.getBottom(density).toFloat()
    val liveAvailableBounds = Rect(
        left = navigationBarLeftPx,
        top = statusBarTopPx,
        right = (viewport.width - navigationBarRightPx).coerceAtLeast(navigationBarLeftPx),
        bottom = (viewport.height - max(navigationBarBottomPx, imeBottomPx)).coerceAtLeast(
            statusBarTopPx
        )
    )
    var placementAnchorBounds by remember { mutableStateOf(menuState.anchorBounds) }
    var placementAvailableBounds by remember { mutableStateOf(liveAvailableBounds) }
    LaunchedEffect(menuState.isVisible) {
        if (menuState.isVisible) {
            placementAnchorBounds = menuState.anchorBounds
            placementAvailableBounds = liveAvailableBounds
        }
    }
    val menuWidthPx = with(density) { 228.dp.roundToPx() }
    val fallbackHeightPx = with(density) { 228.dp.roundToPx() }
    val effectiveMenuSize =
        if (menuSize == IntSize.Zero) IntSize(menuWidthPx, fallbackHeightPx) else menuSize
    val placement = remember(placementAnchorBounds, effectiveMenuSize, placementAvailableBounds) {
        pickPlacement(
            anchorBounds = placementAnchorBounds,
            menuSize = effectiveMenuSize,
            availableBounds = placementAvailableBounds,
            marginPx = with(density) { 8.dp.toPx() },
            gapPx = with(density) { 8.dp.toPx() }
        )
    }

    var isReady by remember { mutableStateOf(false) }

    val scaleAni = remember { Animatable(0.4f) }
    val alphaAni = remember { Animatable(0f) }
    var isMenuInComposition by remember { mutableStateOf(false) }
    val hapticController = LocalHapticFeedback.current

    LaunchedEffect(isReady, isMenuInComposition, menuState.isVisible) {
        if (isReady && isMenuInComposition) {
            coroutineScope {
                launch { scaleAni.animateTo(1f, spring(0.8f, 450f, 0.001f)) }
                launch { alphaAni.animateTo(1f) }
            }
        }
    }

    LaunchedEffect(menuState.isVisible) {
        if (menuState.isVisible) {
            hapticController.performHapticFeedback(HapticFeedbackType.ContextClick)
            isMenuInComposition = true

        } else {
            coroutineScope {
                launch { scaleAni.animateTo(0.4f, spring(0.7f, 600f)) }
                launch { alphaAni.animateTo(0f) }
            }
            isMenuInComposition = false
            isReady = false
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

    val renderEffect = rememberMaterialRenderEffect(MaterialRecipes.menu())

    if (menuState.isVisible) {
        BackHandler { onDismiss() }
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
                .zIndex(99f)
                .graphicsLayer {
                    translationX = placement.x
                    translationY = placement.y
                    scaleX = scaleAni.value
                    scaleY = scaleAni.value
                    transformOrigin = placement.origin
                }
                .width(228.dp)
                .onSizeChanged { menuSize = it }
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
                        padding = 50.dp.toPx() * 2
                        effect(renderEffect)
                        blur(50.dp.toPx())
                    }
                )
                .glasenseHighlight(16.dp)
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) {
                        placeable.place(0, 0)
                        if (!isReady) isReady = true
                    }
                }
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
                        VDivider(
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
                        VDivider(
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
    icon: Painter? = null,
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
        if (icon != null) {
            Icon(
                painter = icon,
                contentDescription = text,
                tint = if (isDestructive) contentColor else if (iconColor == Color.Unspecified) AppColors.content else iconColor,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun CustomSelectiveMenuItem(
    text: String,
    icon: Painter? = null,
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
        if (icon != null) {
            Icon(
                painter = icon,
                contentDescription = text,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
