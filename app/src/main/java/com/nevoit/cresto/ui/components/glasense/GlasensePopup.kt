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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.AppSpecs
import com.nevoit.cresto.theme.isAppInDarkTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class PopupState(
    val isVisible: Boolean = false,
    val anchorBounds: Rect = Rect.Zero
)

private enum class AnchorPopupCorner { LeftTop, RightTop, RightBottom, LeftBottom }

private data class AnchorPopupPlacement(
    val x: Float,
    val y: Float,
    val origin: TransformOrigin
)

private fun pickPopupPlacement(
    anchorBounds: Rect,
    popupSize: IntSize,
    viewport: IntSize,
    marginPx: Float,
    gapPx: Float
): AnchorPopupPlacement {
    fun overflow(x: Float, y: Float): Float {
        val left = (marginPx - x).coerceAtLeast(0f)
        val top = (marginPx - y).coerceAtLeast(0f)
        val right = (x + popupSize.width - (viewport.width - marginPx)).coerceAtLeast(0f)
        val bottom = (y + popupSize.height - (viewport.height - marginPx)).coerceAtLeast(0f)
        return left + top + right + bottom
    }

    val anchorCenterX = (anchorBounds.left + anchorBounds.right) / 2f
    val anchorCenterY = (anchorBounds.top + anchorBounds.bottom) / 2f
    val targetX = anchorCenterX - popupSize.width / 2f

    val candidates = listOf(
        AnchorPopupCorner.LeftTop to Offset(targetX, anchorBounds.bottom + gapPx),
        AnchorPopupCorner.LeftBottom to Offset(
            targetX,
            anchorBounds.top - popupSize.height - gapPx
        ),
    )

    val chosen = candidates.firstOrNull { (_, p) -> overflow(p.x, p.y) == 0f }
        ?: candidates.minBy { (_, p) -> overflow(p.x, p.y) }

    val clampedX = chosen.second.x.coerceIn(
        marginPx,
        (viewport.width - popupSize.width - marginPx).coerceAtLeast(marginPx)
    )
    val clampedY = chosen.second.y.coerceIn(
        marginPx,
        (viewport.height - popupSize.height - marginPx).coerceAtLeast(marginPx)
    )

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
    val fallbackWidthPx = with(density) { (width ?: 280.dp).roundToPx() }
    val fallbackHeightPx = with(density) { 280.dp.roundToPx() }
    val effectivePopupSize =
        if (popupSize == IntSize.Zero) IntSize(fallbackWidthPx, fallbackHeightPx) else popupSize

    val placement = remember(
        popupState.anchorBounds,
        effectivePopupSize,
        viewport,
        popupMargin,
        anchorGap,
        density
    ) {
        pickPopupPlacement(
            anchorBounds = popupState.anchorBounds,
            popupSize = effectivePopupSize,
            viewport = viewport,
            marginPx = with(density) { popupMargin.toPx() },
            gapPx = with(density) { anchorGap.toPx() }
        )
    }

    val scaleAni = remember { Animatable(0.4f) }
    val alphaAni = remember { Animatable(0f) }
    var isPopupInComposition by remember { mutableStateOf(false) }
    val hapticController = LocalHapticFeedback.current
    LaunchedEffect(popupState.isVisible) {
        if (popupState.isVisible) {
            hapticController.performHapticFeedback(HapticFeedbackType.ContextClick)
            delay(50)
            isPopupInComposition = true
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
            isPopupInComposition = false
        }
    }

    val shadowBaseColor =
        if (isAppInDarkTheme()) Color.Black.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.1f)
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
                .background(color = containerColor, shape = shape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
                .padding(contentPadding)
        ) {
            content()
        }
    }
}

