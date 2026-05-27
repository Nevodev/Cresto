package com.nevoit.cresto.ui.components.glasense

import android.graphics.BlurMaskFilter
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.nativePaint
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.AppSpecs
import com.nevoit.cresto.theme.isAppInDarkTheme
import com.nevoit.glasense.core.modifier.cachedClip
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.max

data class PopupState(
    val isVisible: Boolean = false,
    val anchorBounds: Rect = Rect.Zero
)

enum class PopupDirection {
    Up, Down, Left, Right, UpLeft, UpRight, DownLeft, DownRight, Auto
}

private data class AnchorPopupPlacement(
    val x: Float,
    val y: Float,
    val origin: TransformOrigin
)

private fun pickPopupPlacement(
    anchorBounds: Rect,
    popupSize: IntSize,
    availableBounds: Rect,
    marginPx: Float,
    gapPx: Float,
    preferredDirection: PopupDirection = PopupDirection.Auto
): AnchorPopupPlacement {
    val minX = availableBounds.left + marginPx
    val minY = availableBounds.top + marginPx
    val maxX = (availableBounds.right - popupSize.width - marginPx).coerceAtLeast(minX)
    val maxY = (availableBounds.bottom - popupSize.height - marginPx).coerceAtLeast(minY)

    fun overflow(x: Float, y: Float): Float {
        val left = (minX - x).coerceAtLeast(0f)
        val top = (minY - y).coerceAtLeast(0f)
        val right = (x - maxX).coerceAtLeast(0f)
        val bottom = (y - maxY).coerceAtLeast(0f)
        return left + top + right + bottom
    }

    val anchorCenterX = (anchorBounds.left + anchorBounds.right) / 2f
    val anchorCenterY = (anchorBounds.top + anchorBounds.bottom) / 2f

    val targetXCenter = anchorCenterX - popupSize.width / 2f
    val targetYCenter = anchorCenterY - popupSize.height / 2f

    val topPos = anchorBounds.top - popupSize.height - gapPx
    val bottomPos = anchorBounds.bottom + gapPx
    val leftPos = anchorBounds.left - popupSize.width - gapPx
    val rightPos = anchorBounds.right + gapPx

    val alignLeft = anchorBounds.left
    val alignRight = anchorBounds.right - popupSize.width

    val candidates = when (preferredDirection) {
        PopupDirection.Up -> listOf(
            Offset(targetXCenter, topPos),
            Offset(targetXCenter, bottomPos)
        )

        PopupDirection.Down -> listOf(
            Offset(targetXCenter, bottomPos),
            Offset(targetXCenter, topPos)
        )

        PopupDirection.Left -> listOf(
            Offset(leftPos, targetYCenter),
            Offset(rightPos, targetYCenter)
        )

        PopupDirection.Right -> listOf(
            Offset(rightPos, targetYCenter),
            Offset(leftPos, targetYCenter)
        )

        PopupDirection.UpLeft -> listOf(
            Offset(alignLeft, topPos),
            Offset(alignLeft, bottomPos),
            Offset(alignRight, topPos)
        )

        PopupDirection.UpRight -> listOf(
            Offset(alignRight, topPos),
            Offset(alignRight, bottomPos),
            Offset(alignLeft, topPos)
        )

        PopupDirection.DownLeft -> listOf(
            Offset(alignLeft, bottomPos),
            Offset(alignLeft, topPos),
            Offset(alignRight, bottomPos)
        )

        PopupDirection.DownRight -> listOf(
            Offset(alignRight, bottomPos),
            Offset(alignRight, topPos),
            Offset(alignLeft, bottomPos)
        )

        PopupDirection.Auto -> listOf(
            Offset(targetXCenter, bottomPos),
            Offset(targetXCenter, topPos),
            Offset(leftPos, targetYCenter),
            Offset(rightPos, targetYCenter)
        )
    }

    val chosen = candidates.firstOrNull { p -> overflow(p.x, p.y) == 0f }
        ?: candidates.minBy { p -> overflow(p.x, p.y) }

    val clampedX = chosen.x.coerceIn(minX, maxX)
    val clampedY = chosen.y.coerceIn(minY, maxY)

    val originX = ((anchorCenterX - clampedX) / popupSize.width.toFloat()).coerceIn(0f, 1f)
    val originY = ((anchorCenterY - clampedY) / popupSize.height.toFloat()).coerceIn(0f, 1f)

    return AnchorPopupPlacement(clampedX, clampedY, TransformOrigin(originX, originY))
}

@Composable
fun GlasensePopup(
    popupState: PopupState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp? = null,
    direction: PopupDirection = PopupDirection.Auto,
    shape: Shape = AppSpecs.dialogShape,
    containerColor: Color = AppColors.cardBackground,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    popupMargin: Dp = 8.dp,
    anchorGap: Dp = 8.dp,
    dismissOnOutsideClick: Boolean = true,
    dismissOnBackPress: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    var popupSize by remember { mutableStateOf(IntSize.Zero) }
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
    var placementAnchorBounds by remember { mutableStateOf(popupState.anchorBounds) }
    var placementAvailableBounds by remember { mutableStateOf(liveAvailableBounds) }
    LaunchedEffect(popupState.isVisible) {
        if (popupState.isVisible) {
            placementAnchorBounds = popupState.anchorBounds
            placementAvailableBounds = liveAvailableBounds
        }
    }
    val fallbackWidthPx = with(density) { (width ?: 280.dp).roundToPx() }
    val fallbackHeightPx = with(density) { 280.dp.roundToPx() }
    val effectivePopupSize =
        if (popupSize == IntSize.Zero) IntSize(fallbackWidthPx, fallbackHeightPx) else popupSize

    val placement = remember(
        placementAnchorBounds,
        effectivePopupSize,
        placementAvailableBounds,
        popupMargin,
        anchorGap,
        direction,
        density
    ) {
        pickPopupPlacement(
            anchorBounds = placementAnchorBounds,
            popupSize = effectivePopupSize,
            availableBounds = placementAvailableBounds,
            marginPx = with(density) { popupMargin.toPx() },
            gapPx = with(density) { anchorGap.toPx() },
            preferredDirection = direction
        )
    }

    var isReady by remember { mutableStateOf(false) }

    val scaleAni = remember { Animatable(0.4f) }
    val alphaAni = remember { Animatable(0f) }
    var isPopupInComposition by remember { mutableStateOf(false) }
    val hapticController = LocalHapticFeedback.current

    LaunchedEffect(isReady, isPopupInComposition, popupState.isVisible) {
        if (isReady && isPopupInComposition) {
            coroutineScope {
                launch { scaleAni.animateTo(1f, spring(0.8f, 450f, 0.001f)) }
                launch { alphaAni.animateTo(1f) }
            }
        }
    }

    LaunchedEffect(popupState.isVisible) {
        if (popupState.isVisible) {
            hapticController.performHapticFeedback(HapticFeedbackType.ContextClick)
            isPopupInComposition = true
        } else {
            coroutineScope {
                launch { scaleAni.animateTo(0.4f, spring(0.7f, 600f)) }
                launch { alphaAni.animateTo(0f) }
            }
            isPopupInComposition = false
            isReady = false
        }
    }

    val shadowBaseColor =
        if (isAppInDarkTheme()) Color.Black.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.1f)
    val shadowRadiusPx = with(density) { 32.dp.toPx() }
    val shadowDyPx = with(density) { 16.dp.toPx() }
    val shadowPaint = remember(density) {
        Paint().nativePaint.apply {
            isAntiAlias = true
            maskFilter = BlurMaskFilter(shadowRadiusPx, BlurMaskFilter.Blur.NORMAL)
        }
    }

    if (popupState.isVisible) {
        if (dismissOnBackPress) {
            BackHandler(onBack = onDismiss)
        }
        if (dismissOnOutsideClick) {
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
    }

    if (isPopupInComposition) {
        Column(
            modifier = modifier
                .zIndex(99f)
                .then(if (width != null) Modifier.width(width) else Modifier)
                .onSizeChanged { popupSize = it }
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

                            when (val outline = shape.createOutline(size, layoutDirection, this)) {
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
                                        outline.roundRect.left,
                                        outline.roundRect.top,
                                        outline.roundRect.right,
                                        outline.roundRect.bottom,
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
                .graphicsLayer { alpha = alphaAni.value }
                .cachedClip(shape)
                .background(color = containerColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
                .padding(contentPadding)
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) {
                        placeable.place(0, 0)
                        if (!isReady) isReady = true
                    }
                }
        ) {
            content()
        }
    }
}
