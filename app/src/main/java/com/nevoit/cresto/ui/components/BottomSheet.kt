package com.nevoit.cresto.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush.Companion.verticalGradient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.capsule.ContinuousCapsule
import com.nevoit.cresto.CrestoApplication
import com.nevoit.cresto.R
import com.nevoit.cresto.ui.TodoViewModel
import com.nevoit.cresto.ui.TodoViewModelFactory
import com.nevoit.cresto.ui.components.glasense.DialogItemData
import com.nevoit.cresto.ui.components.glasense.GlasenseButton
import com.nevoit.cresto.ui.components.glasense.RotatingGlow
import com.nevoit.cresto.ui.components.glasense.RotatingGlowBorder
import com.nevoit.cresto.ui.theme.glasense.AppButtonColors
import com.nevoit.cresto.ui.theme.glasense.gradientColorsDark
import com.nevoit.cresto.ui.theme.glasense.gradientColorsLight
import com.nevoit.cresto.ui.theme.glasense.highlightColorsDark
import com.nevoit.cresto.ui.theme.glasense.highlightColorsLight
import com.nevoit.cresto.ui.viewmodel.AiSideEffect
import com.nevoit.cresto.ui.viewmodel.AiViewModel
import com.nevoit.cresto.ui.viewmodel.UiState
import com.nevoit.cresto.util.deviceCornerShape
import kotlinx.coroutines.delay
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
    onAddClick: (String, Int, LocalDate?) -> Unit,
    aiViewModel: AiViewModel = viewModel(),
    showDialog: (items: List<DialogItemData>, title: String, message: String?) -> Unit,
) {
    val uiState by aiViewModel.uiState.collectAsState()

    var columnHeightPx by remember { mutableIntStateOf(0) }
    // State to control the visibility of the bottom sheet and its scrim.
    var isVisible by remember { mutableStateOf(false) }
    // Animatable for the vertical offset of the bottom sheet.
    val offset = remember { Animatable(Float.MAX_VALUE) }

    val scaleAnimation = remember { Animatable(0f) }

    // Coroutine scope for launching animations.
    val scope = rememberCoroutineScope()

    var navigationBarHeight by remember { mutableIntStateOf(0) }

    val keyboardController = LocalSoftwareKeyboardController.current

    val isImeVisible = WindowInsets.isImeVisible

    val isSystemInDarkTheme = isSystemInDarkTheme()

    val gradientColors = if (isSystemInDarkTheme) {
        gradientColorsDark
    } else {
        gradientColorsLight
    }

    val highlightColors = if (isSystemInDarkTheme) {
        highlightColorsDark
    } else {
        highlightColorsLight
    }

    val backdrop = rememberLayerBackdrop {
        drawContent()
    }
    val density = LocalDensity.current
    val state = rememberTextFieldState()
    // Trigger the enter animation when the composable is first composed.
    val application = LocalContext.current.applicationContext as CrestoApplication
    val viewModel: TodoViewModel = viewModel(
        factory = TodoViewModelFactory(application.repository)
    )

    val errorDialogItems = listOf(
        DialogItemData(
            "OK",
            onClick = {},
            isPrimary = true,
            isDestructive = true
        )
    )
    val isLoading = uiState is UiState.Loading

    LaunchedEffect(true) {
        aiViewModel.sideEffect.collect { effect ->
            when (effect) {
                is AiSideEffect.ProcessSuccess -> {
                    viewModel.insertAiGeneratedTodos(effect.response.items)
                }

                is AiSideEffect.ShowError -> {
                    showDialog(
                        errorDialogItems,
                        "Error",
                        effect.message
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        isVisible = true
    }
    // Animate the bottom sheet into view when its height is measured.
    LaunchedEffect(columnHeightPx) {
        if (columnHeightPx > 0) {
            isVisible = true
            scope.launch {
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
            scope.launch {
                delay(300)
                scaleAnimation.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(0.8f, 300f, 0.001f)
                )
            }

        }
    }
    BackHandler() {
        scope.launch {
            isVisible = false
            // Animate the sheet out of view.
            scope.launch {
                scaleAnimation.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(200)
                )
            }
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
                                    scope.launch {
                                        scaleAnimation.animateTo(
                                            targetValue = 0f,
                                            animationSpec = tween(200)
                                        )
                                        aiViewModel.cancelRequest()
                                        aiViewModel.clearState()
                                    }
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
                    .padding(bottom = 12.dp)
                    .graphicsLayer {
                        scaleX = scaleAnimation.value
                        scaleY = scaleAnimation.value
                    },
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
                    RotatingGlowBorder(
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 4.dp,
                        blurRadius = 4.dp,
                        shape = ContinuousCapsule,
                        colors = highlightColors,
                        timeMillis = 3000
                    )
                    if (!isLoading) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(start = 16.dp)
                                ) {
                                    BasicTextField(
                                        state = state,
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .fillMaxWidth(),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                        onKeyboardAction = {
                                            aiViewModel.generateContent(
                                                state.text.toString(),
                                                "AIzaSyBaJWyB648-Vaoe6436FbX3f5OtEslnW_M"
                                            )
                                        },
                                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                                    )
                                    if (state.text.isBlank()) {
                                        Text(
                                            "Extract from text...",
                                            modifier = Modifier
                                                .align(Alignment.CenterStart)
                                                .fillMaxWidth()
                                                .graphicsLayer {
                                                    alpha = 0.5f
                                                    blendMode =
                                                        if (isSystemInDarkTheme) BlendMode.Plus else BlendMode.Luminosity
                                                },
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = if (!isSystemInDarkTheme) Color(0xFF545454) else MaterialTheme.typography.bodyLarge.color
                                        )
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier.size(56.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CustomAnimatedVisibility(
                                    visible = !state.text.isBlank(),
                                    enter = myFadeIn(animationSpec = tween(delayMillis = 100)) + myScaleIn(
                                        animationSpec = tween(delayMillis = 100),
                                        initialScale = 0.9f
                                    ),
                                    exit = myFadeOut(animationSpec = tween(durationMillis = 100)) + myScaleOut(
                                        animationSpec = tween(delayMillis = 100),
                                        targetScale = 0.9f
                                    )
                                ) {
                                    GlasenseButton(
                                        enabled = true,
                                        shape = CircleShape,
                                        onClick = {
                                            aiViewModel.generateContent(
                                                state.text.toString(),
                                                "AIzaSyBaJWyB648-Vaoe6436FbX3f5OtEslnW_M"
                                            )
                                        },
                                        modifier = Modifier
                                            .width(40.dp)
                                            .height(40.dp)
                                            .align(Alignment.Center),
                                        colors = AppButtonColors.primary()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .drawBehind {
                                                    val outline = CircleShape.createOutline(
                                                        size = this.size,
                                                        layoutDirection = this.layoutDirection,
                                                        density = this,
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
                                                }, contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_arrow_up),
                                                contentDescription = "Extract",
                                                modifier = Modifier.width(28.dp)
                                            )
                                        }
                                    }
                                }
                                CustomAnimatedVisibility(
                                    visible = state.text.isBlank(),
                                    enter = myFadeIn(animationSpec = tween(delayMillis = 100)) + myScaleIn(
                                        animationSpec = tween(delayMillis = 100),
                                        initialScale = 0.9f
                                    ),
                                    exit = myFadeOut(animationSpec = tween(durationMillis = 100)) + myScaleOut(
                                        animationSpec = tween(delayMillis = 100),
                                        targetScale = 0.9f
                                    )
                                ) {
                                    GlasenseButton(
                                        enabled = true,
                                        shape = CircleShape,
                                        onClick = { },
                                        modifier = Modifier
                                            .width(40.dp)
                                            .height(40.dp)
                                            .align(Alignment.Center),
                                        colors = AppButtonColors.primary().copy(
                                            containerColor = Color.Transparent,
                                            contentColor = MaterialTheme.colorScheme.onBackground
                                        )
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_photo),
                                            contentDescription = "Extract from image",
                                            modifier = Modifier
                                                .size(32.dp)
                                                .graphicsLayer {
                                                    alpha = 0.5f
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            RotatingGlowBorder(
                                modifier = Modifier.fillMaxSize(),
                                strokeWidth = 8.dp,
                                blurRadius = 8.dp,
                                shape = ContinuousCapsule,
                                colors = highlightColors,
                                timeMillis = 3000
                            )
                            Text(
                                "Extracting...",
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .graphicsLayer {
                                        alpha = 0.5f
                                        blendMode =
                                            if (isSystemInDarkTheme) BlendMode.Plus else BlendMode.Luminosity
                                    },
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (!isSystemInDarkTheme) Color(0xFF545454) else MaterialTheme.typography.bodyLarge.color
                            )
                        }
                    }
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
                        scope.launch {
                            scaleAnimation.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(200)
                            )
                            aiViewModel.cancelRequest()
                            aiViewModel.clearState()
                        }
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
                        scope.launch {
                            scaleAnimation.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(200)
                            )
                            aiViewModel.cancelRequest()
                            aiViewModel.clearState()
                        }
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