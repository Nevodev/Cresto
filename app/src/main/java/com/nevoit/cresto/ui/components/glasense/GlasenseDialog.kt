package com.nevoit.cresto.ui.components.glasense

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush.Companion.verticalGradient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.capsule.ContinuousRoundedRectangle
import com.nevoit.cresto.ui.theme.glasense.AppButtonColors
import com.nevoit.cresto.ui.theme.glasense.Red500
import com.nevoit.cresto.ui.theme.glasense.isAppInDarkTheme
import com.nevoit.cresto.util.g2
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

data class DialogItemData(
    val text: String,
    val icon: Painter? = null,
    val isDestructive: Boolean = false,
    val isPrimary: Boolean = true,
    val onClick: () -> Unit
)

data class DialogState(
    val isVisible: Boolean = false,
    val items: List<DialogItemData> = emptyList(),
    val title: String = "Dialog",
    val message: String? = null,
)

@Composable
fun GlasenseDialogButton(
    modifier: Modifier = Modifier,
    text: String,
    icon: Painter? = null,
    isDestructive: Boolean = false,
    isPrimary: Boolean = true,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    GlasenseButtonAlt(
        enabled = true,
        shape = ContinuousRoundedRectangle(12.dp, g2),
        onClick = { onDismiss() },
        modifier = Modifier
            .height(48.dp)
            .then(modifier)
            .then(if (isPrimary) Modifier.drawBehind {
                val outline = ContinuousRoundedRectangle(12.dp, g2).createOutline(
                    size,
                    LayoutDirection.Ltr,
                    density
                )
                val gradientBrush = verticalGradient(
                    colorStops = arrayOf(
                        0.0f to Color.White.copy(alpha = 0.2f),
                        1.0f to Color.White.copy(alpha = 0.02f)
                    )
                )
                drawOutline(
                    outline = outline,
                    brush = gradientBrush,
                    style = Stroke(width = 3.dp.toPx()),
                    blendMode = BlendMode.Plus
                )
            } else Modifier),
        colors = if (isPrimary && !isDestructive) AppButtonColors.primary()
        else if (isPrimary) AppButtonColors.primary().copy(containerColor = Red500)
        else if (isDestructive) AppButtonColors.secondary().copy(contentColor = Red500)
        else AppButtonColors.secondary()
            .copy(contentColor = AppButtonColors.secondary().contentColor.copy(1f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(painter = icon, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun GlasenseDialog(
    density: Density,
    dialogState: DialogState,
    backdrop: LayerBackdrop,
    onDismiss: () -> Unit,
    modifier: Modifier
) {
    val darkTheme = isAppInDarkTheme()
    val screenWidth =
        with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }

    var isVisible by remember { mutableStateOf(false) }

    val scaleAni = remember { Animatable(1.3f) }
    val alphaAni = remember { Animatable(0f) }
    val alphaAni2 = remember { Animatable(0f) }

    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(isVisible) {
        coroutineScope {
            launch { scaleAni.animateTo(1f, spring(1.3f, 1000f, .0001f)) }
            launch { alphaAni.animateTo(1f, tween(300, 0)) }
            launch { alphaAni2.animateTo(1f, tween(200, 0)) }
        }
    }
    val interactionSource = remember { MutableInteractionSource() }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = alphaAni2.value }
                .background(Color.Black.copy(.4f))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = true,
                    onClick = {}),
            contentAlignment = Alignment.Center,
        ) {}


        Box(
            modifier = modifier
                .width(screenWidth - 48.dp * 2)
                .dropShadow(
                    RoundedCornerShape(24.dp),
                    shadow = Shadow(
                        radius = 32.dp,
                        color = if (darkTheme) Color.Black.copy(alpha = 0.2f) else Color.Black.copy(
                            alpha = 0.1f
                        ),
                        offset = DpOffset(0.dp, 16.dp),
                        alpha = alphaAni.value
                    )
                )
                .drawPlainBackdrop(
                    backdrop = backdrop,
                    shape = { ContinuousRoundedRectangle(24.dp, g2) },
                    effects = {
                        blur(64f.dp.toPx(), TileMode.Mirror)
                    },
                    // Custom drawing on top of the blurred background to create stunning colors.
                    onDrawSurface = {
                        val outline = ContinuousRoundedRectangle(24.dp, g2).createOutline(
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
                                brush = SolidColor(Color(0xFF6C6C6C).copy(alpha = 0.7f)),
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
                                brush = SolidColor(Color(0xFFFFFFFF).copy(alpha = 0.3f)),
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
                                brush = SolidColor(Color(0xFF000000).copy(alpha = 0.4f)),
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
                    },
                    layerBlock = {
                        scaleX = scaleAni.value
                        scaleY = scaleAni.value
                        alpha = alphaAni.value
                    })
                .onGloballyPositioned { isVisible = true }
        ) {
            Column(
                modifier = Modifier
                    .padding(12.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = dialogState.title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                if (dialogState.message == null) {
                    Spacer(modifier = Modifier.height(16.dp))
                }
                dialogState.message?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp,
                        modifier = Modifier
                            .graphicsLayer(alpha = 0.5f)
                            .padding(horizontal = 4.dp)
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    dialogState.items.forEach { item ->
                        GlasenseDialogButton(
                            modifier = Modifier.weight(1f),
                            text = item.text,
                            icon = item.icon,
                            isDestructive = item.isDestructive,
                            isPrimary = item.isPrimary,
                            onDismiss = {
                                if (item.isDestructive) {
                                    scope.launch {
                                        repeat(5) {
                                            haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                                            delay(Random.nextLong(50, 70))
                                        }
                                    }
                                }
                                scope.launch {
                                    launch { alphaAni2.animateTo(0f, tween(200, 0)) }
                                    alphaAni.animateTo(0f, tween(200, 0))
                                    onDismiss()
                                    item.onClick()
                                }
                            },
                        )
                    }
                }
            }

        }
    }
}