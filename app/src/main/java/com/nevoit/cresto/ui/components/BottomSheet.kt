package com.nevoit.cresto.ui.components

import android.graphics.Matrix
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush.Companion.verticalGradient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.capsule.ContinuousCapsule
import com.nevoit.cresto.ui.theme.glasense.Blue500
import com.nevoit.cresto.ui.theme.glasense.Violet500
import com.nevoit.cresto.ui.theme.glasense.adjustSaturationInOklab
import com.nevoit.cresto.util.deviceCornerShape
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * A composable function that displays a bottom sheet with custom animations.
 *
 * @param onDismiss Callback function to be invoked when the bottom sheet is dismissed.
 * @param onAddClick Callback function to be invoked when the "add" button inside the sheet is clicked.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BottomSheet(
    onDismiss: () -> Unit,
    onAddClick: (String, Int, LocalDate?) -> Unit
) {
    var columnHeightPx by remember { mutableIntStateOf(0) }
    // State to control the visibility of the bottom sheet and its scrim.
    var isVisible by remember { mutableStateOf(false) }
    // Animatable for the vertical offset of the bottom sheet.
    val offset = remember { Animatable(Float.MAX_VALUE) }

    // Coroutine scope for launching animations.
    val scope = rememberCoroutineScope()

    var navigationBarHeight by remember { mutableIntStateOf(0) }

    val keyboardController = LocalSoftwareKeyboardController.current

    val isImeVisible = WindowInsets.isImeVisible

    val gradientColors = listOf(
        Blue500.adjustSaturationInOklab(1.1f).convert(ColorSpaces.Oklab)
            .copy(red = 0.7f, blue = -0.3f),
        Violet500.adjustSaturationInOklab(1.5f).convert(ColorSpaces.Oklab).copy(red = 0.8f),
        Color(0xFF6FB0FF).adjustSaturationInOklab(1.1f).convert(ColorSpaces.Oklab)
            .copy(red = 0.9f, blue = -0.5f),
        Blue500.adjustSaturationInOklab(1.1f).convert(ColorSpaces.Oklab).copy(red = 0.7f),
    )

    val backdrop = rememberLayerBackdrop {
        drawContent()
    }
    val density = LocalDensity.current
    val isSystemInDarkTheme = isSystemInDarkTheme()
    // Trigger the enter animation when the composable is first composed.
    LaunchedEffect(Unit) {
        isVisible = true
    }
    // Animate the bottom sheet into view when its height is measured.
    LaunchedEffect(columnHeightPx) {
        if (columnHeightPx > 0) {
            isVisible = true
            // Snap to the initial off-screen position.
            offset.snapTo(targetValue = (columnHeightPx + navigationBarHeight).toFloat())
            // Animate to the on-screen position.
            offset.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 200,
                    delayMillis = 100,
                    easing = CubicBezierEasing(.2f, .2f, 0f, 1f)
                )
            )
        }
    }
    BackHandler() {
        scope.launch {
            isVisible = false
            // Animate the sheet out of view.
            offset.animateTo(
                targetValue = (columnHeightPx + navigationBarHeight).toFloat(),
                animationSpec = tween(
                    durationMillis = 200,
                    easing = FastOutSlowInEasing
                )
            )
            onDismiss()
        }
    }
    // Main container for the bottom sheet and scrim.
    Box(modifier = Modifier.fillMaxSize()) {
        // Scrim with animated visibility.
        CustomAnimatedVisibility(
            visible = isVisible,
            enter = myFadeIn(tween(200)),
            exit = myFadeOut(tween(200))
        ) {
            // Background scrim that dismisses the sheet on click.
            Box(
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = true,
                        onClick = {
                            if (isImeVisible) {
                                // Close keyboard first.
                                keyboardController?.hide()
                            } else {
                                scope.launch {
                                    isVisible = false
                                    // Animate the sheet out of view.
                                    offset.animateTo(
                                        targetValue = (columnHeightPx + navigationBarHeight).toFloat(),
                                        animationSpec = tween(
                                            durationMillis = 200,
                                            delayMillis = if (isImeVisible) 100 else 0,
                                            easing = FastOutSlowInEasing
                                        )
                                    )
                                    onDismiss()
                                }
                            }
                        }
                    )
                    .background(Color.Black.copy(alpha = 0.4f))
                    .fillMaxSize()
            )
        }

        // The bottom sheet content itself.
        Column(
            modifier = Modifier
                // Empty clickable to prevent clicks from passing through to the scrim.
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = true,
                    onClick = {}
                )
                .align(alignment = Alignment.BottomCenter)
                // Apply the vertical offset animation.
                .graphicsLayer {
                    translationY = offset.value
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                RotatingGlow(
                    modifier = Modifier
                        .height(64.dp)
                        .padding(horizontal = 8.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp)),
                    blurRadius = 16.dp,
                    shape = RectangleShape,
                    colors = gradientColors,
                    timeMillis = 5000,
                    backdrop = backdrop
                )
                Box(
                    modifier = Modifier
                        .height(56.dp)
                        .padding(horizontal = 12.dp)
                        .fillMaxWidth()
                        .drawPlainBackdrop(
                            backdrop = backdrop,
                            shape = { ContinuousCapsule },
                            effects = {
                                blur(64f.dp.toPx(), TileMode.Clamp)
                                colorControls(saturation = 1.1f)
                            }, onDrawSurface = {
                                val outline = ContinuousCapsule.createOutline(
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
                                if (!isSystemInDarkTheme) {
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
                                    drawOutline(
                                        outline = outline,
                                        brush = gradientBrush,
                                        style = Stroke(width = 3.dp.toPx()),
                                        blendMode = BlendMode.Plus,
                                        alpha = 0.08f
                                    )
                                }
                            })
                        .clip(ContinuousCapsule)
                ) {
                }
            }
            // Container for the AddTodoSheet content.
            Box(
                modifier = Modifier
                    .onSizeChanged { size ->
                        // Measure the height of the content.
                        columnHeightPx = size.height
                    }
                    .background(
                        shape = deviceCornerShape(
                            bottomLeft = false,
                            bottomRight = false
                        ),
                        color = MaterialTheme.colorScheme.surface
                    )
                    .fillMaxWidth()
            ) {
                // The actual sheet content.
                AddTodoSheet(onAddClick = { title, flagIndex, finalDate ->
                    scope.launch {
                        isVisible = false
                        // Animate the sheet out of view.
                        offset.animateTo(
                            targetValue = (columnHeightPx + navigationBarHeight).toFloat(),
                            animationSpec = tween(
                                durationMillis = 200,
                                delayMillis = if (isImeVisible) 100 else 0, // If the keyboard is visible, animating the bottom sheet too quick can feel jarring and unsmooth.
                                easing = FastOutSlowInEasing
                            )
                        )
                        onAddClick(title, flagIndex, finalDate)
                        onDismiss()
                    }
                }, onClose = {
                    keyboardController?.hide()
                    scope.launch {
                        isVisible = false
                        // Animate the sheet out of view.
                        offset.animateTo(
                            targetValue = (columnHeightPx + navigationBarHeight).toFloat(),
                            animationSpec = tween(
                                durationMillis = 200,
                                delayMillis = if (isImeVisible) 100 else 0,
                                easing = FastOutSlowInEasing
                            )
                        )
                        onDismiss()
                    }
                })
            }
            // Spacer to account for navigation bar and IME padding.
            Box(
                modifier = Modifier
                    .onSizeChanged { size ->
                        // Measure the height of the navigation bar area.
                        navigationBarHeight = size.height
                    }
                    .background(color = MaterialTheme.colorScheme.surface)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .imePadding()
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
fun RotatingGlow(
    modifier: Modifier,
    blurRadius: Dp,
    shape: Shape,
    colors: List<Color>,
    timeMillis: Int = 1000,
    backdrop: LayerBackdrop
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shadow_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = timeMillis, easing = LinearEasing),
        ),
        label = "rotation_angle"
    )

    val paint = remember {
        android.graphics.Paint().apply { isAntiAlias = true }
    }
    val gradientMatrix = remember { Matrix() }

    Box(
        modifier = Modifier
            .blur(blurRadius, edgeTreatment = BlurredEdgeTreatment.Unbounded)
            .then(modifier)
            .layerBackdrop(backdrop)
            .drawBehind {
                val centerX = size.width / 2
                val centerY = size.height / 2

                gradientMatrix.setRotate(rotation, centerX, centerY)

                val sweepGradient = android.graphics.SweepGradient(
                    centerX,
                    centerY,
                    colors.map { it.toArgb() }.toIntArray(),
                    null
                )
                sweepGradient.setLocalMatrix(gradientMatrix)
                paint.shader = sweepGradient

                drawIntoCanvas { canvas ->
                    val outline =
                        shape.createOutline(
                            Size(
                                size.width,
                                size.height
                            ), layoutDirection, this
                        )

                    val path = when (outline) {
                        is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
                        is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
                        is Outline.Generic -> outline.path
                    }
                    canvas.nativeCanvas.drawPath(path.asAndroidPath(), paint)
                }
            }
    )
}

@Composable
fun RotatingGlowBorder(
    modifier: Modifier = Modifier,
    strokeWidth: Dp,
    blurRadius: Dp,
    shape: Shape,
    colors: List<Color>,
    timeMillis: Int = 2000
) {
    val infiniteTransition = rememberInfiniteTransition(label = "border_glow_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = timeMillis, easing = LinearEasing),
        ),
        label = "rotation_angle"
    )

    val strokeWidthPx = with(LocalDensity.current) { strokeWidth.toPx() }

    val paint = remember(strokeWidthPx) {
        android.graphics.Paint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.STROKE
            this.strokeWidth = strokeWidthPx
        }
    }

    val gradientMatrix = remember { Matrix() }

    Box(
        modifier = Modifier
            .graphicsLayer { blendMode = BlendMode.Plus }
            .blur(radius = blurRadius, edgeTreatment = BlurredEdgeTreatment.Unbounded)
            .then(modifier)
            .drawBehind {
                val centerX = size.width / 2
                val centerY = size.height / 2

                gradientMatrix.setRotate(rotation, centerX, centerY)
                val sweepGradient = android.graphics.SweepGradient(
                    centerX,
                    centerY,
                    colors.map { it.toArgb() }.toIntArray(),
                    null
                )
                sweepGradient.setLocalMatrix(gradientMatrix)
                paint.shader = sweepGradient
                drawIntoCanvas { canvas ->
                    val outline = shape.createOutline(size, layoutDirection, this)

                    val path = when (outline) {
                        is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
                        is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
                        is Outline.Generic -> outline.path
                    }
                    canvas.nativeCanvas.drawPath(path.asAndroidPath(), paint)
                }
            }
    )
}