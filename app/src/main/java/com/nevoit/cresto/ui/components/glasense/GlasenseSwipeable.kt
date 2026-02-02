package com.nevoit.cresto.ui.components.glasense

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import com.nevoit.cresto.ui.components.CustomAnimatedVisibility
import com.nevoit.cresto.ui.components.myFadeIn
import com.nevoit.cresto.ui.components.myFadeOut
import com.nevoit.cresto.ui.components.myScaleIn
import com.nevoit.cresto.ui.components.myScaleOut
import com.nevoit.cresto.ui.theme.glasense.glasenseHighlight
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random

enum class SwipeState {
    /**
     * The initial state where the to-do item is not swiped.
     */
    IDLE,

    /**
     * The state where the to-do item is swiped to reveal the delete button.
     */
    REVEALED
}

class SwipeableListState {
    // 保存当前展开的 Item 的唯一标识符 (Key)
    // 如果为 null，表示没有 Item 展开
    var currentOpenKey: Any? by mutableStateOf(null)
        private set

    // 当某个 Item 准备展开时调用此方法
    fun setOpen(key: Any) {
        currentOpenKey = key
    }

    // 关闭所有（或者特定的）
    fun close() {
        currentOpenKey = null
    }
}

// 2. 提供一个 remember 函数方便使用
@Composable
fun rememberSwipeableListState(): SwipeableListState {
    return remember { SwipeableListState() }
}

@Composable
fun SwipeableContainer(
    key: Any,
    listState: SwipeableListState,
    actions: List<SwipeableActionButton>,
    onAction: (Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    var swipeState by remember { mutableStateOf(SwipeState.IDLE) }
    var initialSwipeState by remember { mutableStateOf(SwipeState.IDLE) }
    val coroutineScope = rememberCoroutineScope()

    val density = LocalDensity.current

    val actionButtonWidth = 60.dp
    val actionButtonWidthPx = with(density) { actionButtonWidth.toPx() }

    val gapPx = with(density) { 6.dp.toPx() }

    val totalActionsWidthPx = actionButtonWidthPx * actions.size + gapPx * 2
    val snapThresholdPx = -totalActionsWidthPx / 2

    val deepSwipeThresholdPx = totalActionsWidthPx + actionButtonWidthPx

    val velocityThreshold = with(density) { 500.dp.toPx() }

    val screenWidthPx = LocalWindowInfo.current.containerSize.width

    val flingOffset = remember { Animatable(0f) }
    val deleteFlingOffset = remember { Animatable(0f) }

    val animatedOffset by animateFloatAsState(
        targetValue = flingOffset.value,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 500f
        )
    )

    val shouldIntercept = listState.currentOpenKey != null

    val scale = remember { Animatable(1f) }
    val alphaAni = remember { Animatable(1f) }

    fun reset() {
        coroutineScope.launch {
            swipeState = SwipeState.IDLE
            flingOffset.snapTo(0f)
            deleteFlingOffset.snapTo(0f)
            scale.snapTo(1f)
            alphaAni.snapTo(1f)
        }
    }


    fun executeAction(action: SwipeableActionButton) {
        if (action.isDestructive) {
            coroutineScope.launch {
                repeat(5) {
                    haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                    delay(Random.nextLong(50, 70))
                }
            }
            coroutineScope.launch {
                val jobs = listOf(
                    launch { scale.animateTo(0.8f, tween(100)) },
                    launch { alphaAni.animateTo(0f, tween(100)) },
                    launch {
                        deleteFlingOffset.animateTo(
                            targetValue = -screenWidthPx - flingOffset.value,
                            animationSpec = tween(
                                100,
                                easing = CubicBezierEasing(
                                    0.2f,
                                    0f,
                                    0.56f,
                                    0.48f
                                )
                            )
                        )
                    }
                )
                jobs.joinAll()
                swipeState = SwipeState.IDLE
                onAction(action.index)
            }
        } else {
            onAction(action.index)
            coroutineScope.launch {
                swipeState = SwipeState.IDLE
                flingOffset.animateTo(0f)
            }
        }
    }

    LaunchedEffect(listState.currentOpenKey) {
        if (listState.currentOpenKey != key && swipeState != SwipeState.IDLE) {
            coroutineScope.launch {
                swipeState = SwipeState.IDLE
                flingOffset.animateTo(0f)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(with(density) { totalActionsWidthPx.toDp() })
                .padding(end = 6.dp)
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            actions.forEachIndexed { index, action ->
                Box(
                    modifier = Modifier
                        .width(actionButtonWidth)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    val trueIndex = actions.size - index - 1
                    val revealThreshold =
                        gapPx + actionButtonWidthPx * trueIndex + actionButtonWidthPx / 2

                    val isVisible = abs(flingOffset.value) >= revealThreshold

                    CustomAnimatedVisibility(
                        visible = isVisible,
                        modifier = Modifier
                            .width(48.dp)
                            .height(48.dp),
                        enter = myScaleIn(
                            tween(300, 0, LinearOutSlowInEasing),
                            0.6f
                        ) + myFadeIn(tween(200)),
                        exit = myScaleOut(
                            tween(200, 0, LinearOutSlowInEasing),
                            0.6f
                        ) + myFadeOut(tween(100))
                    ) {
                        GlasenseButton(
                            enabled = true,
                            shape = CircleShape,
                            onClick = {
                                coroutineScope.launch {
                                    executeAction(action)
                                }
                            },
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = scale.value
                                    scaleY = scale.value
                                    alpha = alphaAni.value
                                }
                                .size(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = action.color,
                                contentColor = Color.White
                            ),
                            animated = true
                        ) {
                            Box(
                                modifier = Modifier
                                    .glasenseHighlight(100.dp)
                                    .fillMaxSize(), contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = action.icon,
                                    contentDescription = action.contentDescription,
                                    modifier = Modifier
                                        .width(28.dp)
                                        .height(28.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        val currentOffset = flingOffset.value
        val isDeepSwipe = currentOffset < -deepSwipeThresholdPx
        LaunchedEffect(isDeepSwipe) {
            if (isDeepSwipe && initialSwipeState == SwipeState.REVEALED) haptic.performHapticFeedback(
                HapticFeedbackType.GestureThresholdActivate
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationX = animatedOffset + deleteFlingOffset.value
                }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        coroutineScope.launch {
                            val newOffset = (flingOffset.value + delta).coerceAtMost(0f)
                            flingOffset.snapTo(newOffset)
                            swipeState =
                                if (newOffset < snapThresholdPx) SwipeState.REVEALED else SwipeState.IDLE
                        }
                    },
                    onDragStarted = {
                        initialSwipeState = swipeState
                        listState.setOpen(key)
                    },
                    onDragStopped = { velocity ->
                        coroutineScope.launch {

                            val isFastSwipe = velocity < -velocityThreshold
                            if (actions.isNotEmpty() && ((isDeepSwipe && initialSwipeState == SwipeState.REVEALED) || (isFastSwipe && initialSwipeState == SwipeState.REVEALED))) {
                                executeAction(actions.last())
                            } else if ((currentOffset < snapThresholdPx || (isFastSwipe && currentOffset < 0)) && velocity <= 0) {
                                swipeState = SwipeState.REVEALED
                                flingOffset.animateTo(
                                    targetValue = -totalActionsWidthPx,
                                    animationSpec = SpringSpec(
                                        dampingRatio = 0.8f,
                                        stiffness = 1000f
                                    ),
                                    initialVelocity = velocity
                                )
                            } else {
                                if (listState.currentOpenKey == key) {
                                    listState.close()
                                }
                                swipeState = SwipeState.IDLE
                                val finalVelocity =
                                    if (currentOffset == 0f && velocity > 0) 0f else velocity
                                flingOffset.animateTo(
                                    targetValue = 0f,
                                    animationSpec = SpringSpec(
                                        dampingRatio = 0.8f,
                                        stiffness = 1000f
                                    ),
                                    initialVelocity = finalVelocity
                                )
                            }
                        }
                    }
                )
        ) {
            content()

            if (shouldIntercept) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                listState.close()
                            }
                        )
                )
            }
        }
    }
}

data class SwipeableActionButton(
    val index: Int,
    val color: Color,
    val icon: Painter,
    val contentDescription: String? = null,
    val isDestructive: Boolean = false
)
