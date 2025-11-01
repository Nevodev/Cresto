package com.nevoit.cresto.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
