package com.nevoit.cresto.ui.components.glasense

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
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
import com.nevoit.cresto.theme.AppButtonColors
import com.nevoit.glasense.core.component.Icon
import com.nevoit.glasense.theme.tokens.Springs
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

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

@Stable
class SwipeableListState {
    var currentOpenKey: Any? by mutableStateOf(null)
        private set

    fun setOpen(key: Any) {
        currentOpenKey = key
    }

    fun close() {
        currentOpenKey = null
    }
}

@Composable
fun rememberSwipeableListState(): SwipeableListState {
    return remember { SwipeableListState() }
}

@Composable
fun GlasenseSwipeable(
    modifier: Modifier = Modifier,
    key: Any,
    listState: SwipeableListState,
    actions: ImmutableList<SwipeableActionButton>,
    onAction: (Int) -> Unit,
    content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    var swipeState by remember(key) { mutableStateOf(SwipeState.IDLE) }
    var initialSwipeState by remember(key) { mutableStateOf(SwipeState.IDLE) }
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

    val flingOffset = remember(key) { Animatable(0f) }
    val deleteFlingOffset = remember(key) { Animatable(0f) }
    val deepSwipeAction = remember(actions) {
        var selectedAction: SwipeableActionButton? = null
        actions.forEach { action ->
            if (action.triggerOnDeepSwipe) {
                require(selectedAction == null) {
                    "Only one SwipeableActionButton can set triggerOnDeepSwipe."
                }
                selectedAction = action
            }
        }
        selectedAction
    }

    val shouldIntercept = listState.currentOpenKey != null && listState.currentOpenKey == key
    var shouldComposeActions by remember(key) { mutableStateOf(false) }

    val revealedThresholds = remember(actions, actionButtonWidthPx, gapPx) {
        actions.indices.map { index ->
            val trueIndex = actions.size - index - 1
            gapPx + actionButtonWidthPx * trueIndex + actionButtonWidthPx / 2
        }
    }
    val revealedCount by remember(key, revealedThresholds) {
        derivedStateOf { revealedThresholds.count { abs(flingOffset.value) >= it } }
    }

    var isFlyingOut by remember { mutableStateOf(false) }

    suspend fun resetVisualState() {
        isFlyingOut = false
        deleteFlingOffset.snapTo(0f)
    }

    fun executeAction(action: SwipeableActionButton) {
        if (action.isDestructive) {
            coroutineScope.launch {
                repeat(5) {
                    haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                    delay(Random.nextLong(50, 70).milliseconds)
                }
            }
            coroutineScope.launch {
                val flyOutExtraPx = totalActionsWidthPx + with(density) { 8.dp.toPx() }
                isFlyingOut = true
                deleteFlingOffset.animateTo(
                    targetValue = -(screenWidthPx + flyOutExtraPx) - flingOffset.value,
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
                swipeState = SwipeState.IDLE
                listState.close()
                onAction(action.index)
            }
        } else {
            onAction(action.index)
            coroutineScope.launch {
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            }
            coroutineScope.launch {
                swipeState = SwipeState.IDLE
                flingOffset.animateTo(0f)
                shouldComposeActions = false
                listState.close()
                resetVisualState()
            }
        }
    }

    LaunchedEffect(listState.currentOpenKey) {
        if (listState.currentOpenKey != key) {
            coroutineScope.launch {
                swipeState = SwipeState.IDLE
                flingOffset.animateTo(0f)
                shouldComposeActions = false
                resetVisualState()
            }
        }
    }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
    ) {
        if (shouldComposeActions) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(with(density) { totalActionsWidthPx.toDp() })
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {
                        }
                    )
                    .padding(end = 6.dp),
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
                        val isVisible = trueIndex < revealedCount && !isFlyingOut

                        val animation = remember { Animatable(0.6f) }
                        val alphaAnimation = remember { Animatable(0f) }
                        LaunchedEffect(isVisible) {
                            if (isVisible) {
                                launch {
                                    alphaAnimation.animateTo(
                                        1f,
                                        tween(300, easing = LinearOutSlowInEasing)
                                    )
                                }
                                animation.animateTo(1f, tween(300, easing = LinearOutSlowInEasing))
                            } else {
                                launch {
                                    alphaAnimation.animateTo(
                                        0f,
                                        tween(100, easing = LinearOutSlowInEasing)
                                    )
                                }
                                animation.animateTo(
                                    0.6f,
                                    tween(200, easing = LinearOutSlowInEasing)
                                )
                            }
                        }
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
                                    scaleX = animation.value
                                    scaleY = animation.value
                                    alpha = alphaAnimation.value
                                }
                                .size(48.dp),
                            colors = AppButtonColors.solid(
                                color = action.color,
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
                                    tint = action.iconColor,
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

        val isDeepSwipe by remember(key, deepSwipeThresholdPx) {
            derivedStateOf { flingOffset.value < -deepSwipeThresholdPx }
        }

        LaunchedEffect(isDeepSwipe) {
            if (isDeepSwipe && initialSwipeState == SwipeState.REVEALED) haptic.performHapticFeedback(
                HapticFeedbackType.GestureThresholdActivate
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationX = flingOffset.value + deleteFlingOffset.value
                }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        coroutineScope.launch {
                            val newOffset = rubberBandSwipeOffset(
                                currentOffset = flingOffset.value,
                                delta = delta,
                                revealOffsetPx = totalActionsWidthPx,
                                viewportWidthPx = screenWidthPx.toFloat()
                            )
                            flingOffset.snapTo(newOffset)
                            swipeState =
                                if (newOffset < snapThresholdPx) SwipeState.REVEALED else SwipeState.IDLE
                        }
                    },
                    onDragStarted = {
                        initialSwipeState = swipeState
                        coroutineScope.launch { resetVisualState() }
                        shouldComposeActions = true
                        listState.setOpen(key)
                    },
                    onDragStopped = { velocity ->
                        coroutineScope.launch {
                            val currentOffset = flingOffset.value
                            val isFastSwipe = velocity < -velocityThreshold
                            val startedRevealed = initialSwipeState == SwipeState.REVEALED
                            val canDeepSwipe = deepSwipeAction != null && startedRevealed

                            val shouldExecuteDeepSwipe =
                                canDeepSwipe &&
                                        velocity <= 0 &&
                                        (isDeepSwipe || isFastSwipe)

                            val shouldReveal =
                                (canDeepSwipe && isDeepSwipe && velocity > 0) ||
                                        ((currentOffset < snapThresholdPx || (isFastSwipe && currentOffset < 0)) && velocity <= 0)

                            when {
                                shouldExecuteDeepSwipe -> {
                                    executeAction(deepSwipeAction)
                                }

                                shouldReveal -> {
                                    swipeState = SwipeState.REVEALED
                                    flingOffset.animateTo(
                                        targetValue = -totalActionsWidthPx,
                                        animationSpec = Springs.bouncy(400),
                                        initialVelocity = velocity
                                    )
                                }

                                else -> {
                                    if (listState.currentOpenKey == key) {
                                        listState.close()
                                    }
                                    swipeState = SwipeState.IDLE
                                    val finalVelocity =
                                        if (currentOffset == 0f && velocity > 0) 0f else velocity
                                    flingOffset.animateTo(
                                        targetValue = 0f,
                                        animationSpec = Springs.bouncy(400),
                                        initialVelocity = finalVelocity
                                    )
                                    shouldComposeActions = false
                                    resetVisualState()
                                }
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
                            interactionSource = interactionSource,
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

@Immutable
data class SwipeableActionButton(
    val index: Int,
    val color: Color,
    val icon: Painter,
    val iconColor: Color = Color.White,
    val contentDescription: String? = null,
    val isDestructive: Boolean = false,
    val triggerOnDeepSwipe: Boolean = false
)

private const val SwipeRubberBandConstant = 0.55f

private fun rubberBandSwipeOffset(
    currentOffset: Float,
    delta: Float,
    revealOffsetPx: Float,
    viewportWidthPx: Float
): Float {
    val revealOffset = revealOffsetPx.coerceAtLeast(0f)
    val nextOffset = (currentOffset + delta).coerceAtMost(0f)
    if (nextOffset >= -revealOffset || viewportWidthPx <= 0f) return nextOffset

    val overscroll = abs(nextOffset) - revealOffset
    val previousOverscroll = (abs(currentOffset) - revealOffset).coerceAtLeast(0f)
    val dimension = viewportWidthPx.coerceAtLeast(revealOffset)
    val resistance = dimension / (dimension + SwipeRubberBandConstant * previousOverscroll)
    val resistedOverscroll = previousOverscroll + (overscroll - previousOverscroll) * resistance

    return -revealOffset - resistedOverscroll.coerceAtLeast(0f)
}
