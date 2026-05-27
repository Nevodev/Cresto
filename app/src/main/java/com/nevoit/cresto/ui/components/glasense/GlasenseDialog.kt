package com.nevoit.cresto.ui.components.glasense

import android.graphics.BlurMaskFilter
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.nativePaint
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.effect
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.nevoit.cresto.feature.settings.util.SettingsManager
import com.nevoit.cresto.theme.AppButtonColors
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.AppSpecs
import com.nevoit.cresto.theme.LocalGlasenseSettings
import com.nevoit.cresto.theme.isAppInDarkTheme
import com.nevoit.cresto.ui.components.glasense.material.MaterialRecipes
import com.nevoit.cresto.ui.components.glasense.material.rememberMaterialRenderEffect
import com.nevoit.glasense.core.component.Icon
import com.nevoit.glasense.core.component.Text
import com.nevoit.glasense.theme.GlasenseTheme
import com.nevoit.glasense.theme.tokens.Springs
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

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
    val shape = AppSpecs.buttonShape
    val corner = AppSpecs.buttonCorner
    GlasenseButtonAlt(
        enabled = true,
        shape = shape,
        onClick = { onDismiss() },
        modifier = Modifier
            .defaultMinSize(minHeight = 48.dp)
            .then(modifier)
            .then(if (isPrimary) Modifier.glasenseHighlight(corner) else Modifier),
        colors = if (isPrimary && !isDestructive) AppButtonColors.primary()
        else if (isPrimary) AppButtonColors.primary()
            .copy(containerColor = AppColors.error, contentColor = AppColors.onError)
        else if (isDestructive) AppButtonColors.secondary().copy(contentColor = AppColors.error)
        else AppButtonColors.secondary()
            .copy(contentColor = AppButtonColors.secondary().contentColor.copy(1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(painter = icon, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text,
                style = GlasenseTheme.type.body,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun GlasenseDialog(
    dialogState: DialogState,
    backdrop: Backdrop,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val surfaceColor = AppColors.cardBackground

    var liquidGlass by SettingsManager.isLiquidGlassState

    val blur = !LocalGlasenseSettings.current.liteMode
    val darkTheme = isAppInDarkTheme()
    val screenWidth =
        with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }

    val shadowBaseColor =
        if (darkTheme) Color.Black.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.1f)
    val dialogShape = AppSpecs.dialogShape
    val shadowRadiusPx = with(LocalDensity.current) { 32.dp.toPx() }
    val shadowDyPx = with(LocalDensity.current) { 16.dp.toPx() }
    val shadowPaint = remember {
        Paint().nativePaint.apply {
            isAntiAlias = true
            maskFilter = BlurMaskFilter(shadowRadiusPx, BlurMaskFilter.Blur.NORMAL)
        }
    }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val material = rememberMaterialRenderEffect(MaterialRecipes.regular())

    if (dialogState.isVisible) {
        var isReady by remember { mutableStateOf(true) }

        val scaleAni = remember { Animatable(1.15f) }
        val alphaAni = remember { Animatable(0f) }
        val alphaAni2 = remember { Animatable(0f) }

        LaunchedEffect(isReady) {
            if (isReady) {
                coroutineScope {
                    launch {
                        scaleAni.animateTo(
                            1f,
                            Springs.smooth(durationMillis = 400, visibilityThreshold = 0.0001f)
                        )
                    }
                    launch { alphaAni.animateTo(1f, tween(300)) }
                    launch { alphaAni2.animateTo(1f, tween(300)) }
                }
            }
        }

        BackHandler { }
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
                        onClick = {})
            )

            Box(
                modifier = modifier
                    .width(screenWidth - 48.dp * 2)
                    .drawBehind {
                        val currentAlpha = alphaAni.value

                        if (currentAlpha > 0f) {
                            val paintColor =
                                shadowBaseColor.copy(alpha = shadowBaseColor.alpha * currentAlpha)
                            shadowPaint.color = paintColor.toArgb()

                            drawIntoCanvas { canvas ->
                                canvas.save()
                                canvas.translate(0f, shadowDyPx)

                                val outline = dialogShape.createOutline(size, layoutDirection, this)

                                when (outline) {
                                    is androidx.compose.ui.graphics.Outline.Rectangle -> {
                                        canvas.nativeCanvas.drawRect(
                                            outline.rect.left,
                                            outline.rect.top,
                                            outline.rect.right,
                                            outline.rect.bottom,
                                            shadowPaint
                                        )
                                    }

                                    is androidx.compose.ui.graphics.Outline.Rounded -> {
                                        canvas.nativeCanvas.drawRoundRect(
                                            outline.roundRect.left, outline.roundRect.top,
                                            outline.roundRect.right, outline.roundRect.bottom,
                                            outline.roundRect.bottomLeftCornerRadius.x,
                                            outline.roundRect.bottomLeftCornerRadius.y,
                                            shadowPaint
                                        )
                                    }

                                    is androidx.compose.ui.graphics.Outline.Generic -> {
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
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { dialogShape },
                        effects = {
                            if (blur && !liquidGlass) {
                                padding = 32f.dp.toPx() * 2
                                effect(material)
                                blur(32f.dp.toPx(), TileMode.Mirror)
                            } else if (blur) {
                                padding = 16f.dp.toPx() * 2
                                effect(material)
                                blur(
                                    16.dp.toPx(),
                                    TileMode.Mirror
                                )
                                lens(24f.dp.toPx(), 48f.dp.toPx(), depthEffect = true)
                            }
                        },
                        highlight = { if (liquidGlass) Highlight.Default else null },
                        shadow = null,
                        innerShadow = null,
                        // Custom drawing on top of the blurred background to create stunning colors.
                        onDrawSurface = {
                            if (!blur) drawRect(
                                brush = SolidColor(surfaceColor),
                                style = Fill
                            )
                        },
                        layerBlock = {
                            scaleX = scaleAni.value
                            scaleY = scaleAni.value
                            alpha = alphaAni.value
                        })
                    .then(if (!liquidGlass) Modifier.glasenseHighlight(AppSpecs.dialogCorner) else Modifier)

            ) {
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = dialogState.title,
                        style = GlasenseTheme.type.smallTitle,
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
                            style = GlasenseTheme.type.body,
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
                                                delay(Random.nextLong(50, 70).milliseconds)
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
}