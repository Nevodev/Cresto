package com.nevoit.cresto.ui.components.glasense

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Constraints
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.util.deviceCornerShape
import com.nevoit.glasense.theme.Springs
import kotlinx.coroutines.launch

@Composable
fun GlasenseBottomSheet(
    onDismissed: () -> Unit,
    onDismissRequest: (slideOut: () -> Unit) -> Unit = { slideOut -> slideOut() },
    content: @Composable BoxScope.(slideOut: () -> Unit) -> Unit,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val windowInfo = LocalWindowInfo.current

    var isVisible by remember { mutableStateOf(false) }
    var hasSlidIn by remember { mutableStateOf(false) }

    val bottomSheetHeight =
        windowInfo.containerDpSize.height - WindowInsets.statusBars.asPaddingValues()
            .calculateTopPadding()
    val bottomSheetHeightPx = with(density) { bottomSheetHeight.toPx() }

    val offset = remember { Animatable(bottomSheetHeightPx) }

    var isReady by remember { mutableStateOf(false) }

    LaunchedEffect(isReady, bottomSheetHeightPx) {
        if (isReady) {
            isVisible = true
            if (!hasSlidIn) {
                offset.animateTo(
                    targetValue = 0f,
                    animationSpec = Springs.smooth(250)
                )
                hasSlidIn = true
            } else {
                offset.snapTo(0f)
            }
        }
    }

    val bottomSheetShape = deviceCornerShape(
        bottomLeft = false,
        bottomRight = false
    )

    var isDismissing by remember { mutableStateOf(false) }
    fun slideOut() {
        if (isDismissing) return
        scope.launch {
            isDismissing = true
            isVisible = false
            offset.animateTo(
                targetValue = bottomSheetHeightPx,
                animationSpec = tween(
                    durationMillis = 150,
                    easing = CubicBezierEasing(.4f, .0f, 1f, 1f)
                )
            )
            onDismissed()
        }
    }

    BackHandler {
        onDismissRequest(::slideOut)
    }
    Box(modifier = Modifier.fillMaxSize()) {
        DismissScrim(visible = isVisible, onDismiss = {
            onDismissRequest(::slideOut)
        })

        Box(
            modifier = Modifier
                .graphicsLayer {
                    translationY = offset.value
                }
                .layout { measurable, constraints ->
                    val unboundedConstraints = constraints.copy(maxHeight = Constraints.Infinity)
                    val placeable = measurable.measure(unboundedConstraints)
                    layout(placeable.width, constraints.maxHeight) {
                        placeable.place(0, constraints.maxHeight - placeable.height)
                        if (!isReady) isReady = true
                    }
                }
                .height(bottomSheetHeight)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = true,
                    onClick = {}
                )
                .clip(
                    bottomSheetShape
                )
                .background(
                    color = AppColors.pageBackgroundElevated
                )
                .fillMaxWidth()
        ) {
            content(::slideOut)
        }
    }
}

@Composable
private fun DismissScrim(
    visible: Boolean,
    onDismiss: () -> Unit
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 0.4f else 0f,
        animationSpec = tween(200)
    )
    Box(
        modifier = Modifier
            .graphicsLayer {
                this.alpha = alpha
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = visible,
                onClick = onDismiss
            )
            .background(Color.Black)
            .fillMaxSize()
    )
}