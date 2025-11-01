package com.nevoit.cresto.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush.Companion.verticalGradient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.capsule.ContinuousRoundedRectangle
import com.nevoit.cresto.R
import com.nevoit.cresto.data.TodoItem
import com.nevoit.cresto.ui.components.glasense.GlasenseButton
import com.nevoit.cresto.ui.theme.glasense.CalculatedColor
import com.nevoit.cresto.ui.theme.glasense.Red500
import com.nevoit.cresto.ui.theme.glasense.getFlagColor
import com.nevoit.cresto.util.g2
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

/**
 * A composable function that displays a single to-do item with a checkbox, title, due date, flag, and hashtag.
 *
 * @param item The [TodoItem] to display.
 * @param onCheckedChange A callback that is invoked when the checkbox is checked or unchecked.
 * @param modifier A [Modifier] for this composable.
 */
@Composable
fun TodoItemRow(
    item: TodoItem,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier
) {

    Row(
        modifier = Modifier
            .defaultMinSize(minHeight = 68.dp)
            .fillMaxWidth()
            .background(
                color = CalculatedColor.hierarchicalSurfaceColor,
                shape = ContinuousRoundedRectangle(12.dp, g2),
            )
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(12.dp))
        CustomCheckbox(
            checked = item.isCompleted,
            onCheckedChange = onCheckedChange
        )
        Spacer(modifier = Modifier.width(12.dp))
        // If the to-do item has no due date, display only the title.
        if (item.dueDate == null) {
            Text(
                text = item.title,
                textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp)
            )
        } else {
            // If the to-do item has a due date, display both the title and the due date.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    text = item.title,
                    textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = item.dueDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd")),
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 14.sp,
                    modifier = Modifier.alpha(0.4f)
                )
            }
        }

        // If the to-do item has a flag, display the flag icon.
        if (getFlagColor(item.flag) != Color.Transparent) {
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(24.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_flag_fill),
                    contentDescription = "Flag",
                    modifier = Modifier.fillMaxSize(),
                    tint = getFlagColor(item.flag)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        // If the to-do item has a hashtag, display it.
        Box(
            modifier = Modifier
                .height(32.dp)
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
        ) {
            item.hashtag?.let { Text(text = it, color = MaterialTheme.colorScheme.onBackground) }
        }
    }
}

/**
 * An enum representing the two possible states of the swipeable to-do item.
 */
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

/**
 * A composable that makes a [TodoItemRow] swipeable to reveal a delete button.
 *
 * @param item The [TodoItem] to display.
 * @param isRevealed Whether the delete button is revealed.
 * @param onExpand A callback that is invoked when the swipe is started.
 * @param onCollapse A callback that is invoked when the swipe is cancelled.
 * @param onCheckedChange A callback that is invoked when the checkbox is checked or unchecked.
 * @param onDeleteClick A callback that is invoked when the delete button is clicked.
 * @param modifier A [Modifier] for this composable.
 */
@Composable
fun SwipeableTodoItem(
    item: TodoItem,
    isRevealed: Boolean,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    onCheckedChange: (Boolean) -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier
) {
    // The current swipe state of the to-do item.
    var swipeState by remember { mutableStateOf(SwipeState.IDLE) }
    // The swipe state of the to-do item when the drag started.
    var initialSwipeState by remember { mutableStateOf(SwipeState.IDLE) }
    val coroutineScope = rememberCoroutineScope()

    val density = LocalDensity.current
    // The width of the delete button in pixels.
    val revealButtonWidthPx = with(density) { 72.dp.toPx() }
    // The swipe distance threshold to reveal the delete button.
    val swipeThresholdPx = with(density) { (-72 / 2 - 16).dp.toPx() }
    // The swipe distance threshold to trigger the delete action.
    val deleteDistanceThresholdPx = revealButtonWidthPx * 2
    // The velocity threshold to trigger the delete action.
    val velocityThreshold = with(density) { 500.dp.toPx() }
    val screenWidthPx = LocalWindowInfo.current.containerSize.width

    // The fling offset of the to-do item.
    val flingOffset = remember { Animatable(0f) }
    // The fling offset of the to-do item when it is being deleted.
    val deleteFlingOffset = remember { Animatable(0f) }
    // The animated offset of the to-do item.
    val animatedOffset by animateFloatAsState(
        targetValue = flingOffset.value,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 500f
        )
    )

    // The scale of the delete button.
    val scale = remember { Animatable(1f) }
    // The alpha of the delete button.
    val alphaAni = remember { Animatable(1f) }

    // A LaunchedEffect that collapses the to-do item if it is not revealed.
    LaunchedEffect(isRevealed) {
        if (!isRevealed && flingOffset.value != 0f) {
            coroutineScope.launch {
                flingOffset.snapTo(0f)
                swipeState = SwipeState.IDLE
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        // The delete button.
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .align(Alignment.CenterEnd)
                .width(72.dp),
            contentAlignment = Alignment.Center
        ) {
            CustomAnimatedVisibility(
                visible = (swipeState == SwipeState.REVEALED),
                modifier = Modifier
                    .width(48.dp)
                    .height(48.dp),
                enter = myScaleIn(
                    tween(200, 0, LinearOutSlowInEasing),
                    0.6f
                ) + myFadeIn(tween(100)),
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
                            val jobs = listOf(
                                // Animate the scale and alpha of the delete button.
                                launch { scale.animateTo(0.8f, tween(100)) },
                                launch { alphaAni.animateTo(0f, tween(100)) },
                                // Animate the to-do item off the screen.
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
                            onDeleteClick()
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
                        containerColor = Red500,
                        contentColor = Color.White
                    ),
                    animated = true
                ) {
                    Box(
                        modifier = Modifier
                            .drawBehind() {
                                // Draw a gradient border around the delete button.
                                val gradientBrush = verticalGradient(
                                    colorStops = arrayOf(
                                        0.0f to Color.White.copy(alpha = 0.2f),
                                        1.0f to Color.White.copy(alpha = 0.02f)
                                    )
                                )
                                drawCircle(
                                    brush = gradientBrush,
                                    style = Stroke(width = 3.dp.toPx()),
                                    blendMode = BlendMode.Plus
                                )
                            }
                            .fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.ic_trash),
                            contentDescription = "Delete Task",
                            modifier = Modifier
                                .width(28.dp)
                                .height(28.dp)
                        )
                    }
                }
            }
        }

        // The swipeable to-do item.
        Box(
            modifier = Modifier
                .graphicsLayer {
                    translationX =
                        animatedOffset + deleteFlingOffset.value
                }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        coroutineScope.launch {
                            // Update the fling offset and swipe state.
                            val newOffsetX = (flingOffset.value + delta).coerceAtMost(0f)
                            flingOffset.snapTo(newOffsetX)
                            swipeState =
                                if (newOffsetX < swipeThresholdPx) SwipeState.REVEALED else SwipeState.IDLE
                        }
                    },
                    onDragStarted = {
                        // Save the initial swipe state and expand the to-do item.
                        initialSwipeState = swipeState
                        if (swipeState == SwipeState.IDLE) {
                            onExpand()
                        }
                    },
                    onDragStopped = { velocity ->
                        coroutineScope.launch {
                            // If the to-do item is swiped far enough, delete it.
                            if (initialSwipeState == SwipeState.REVEALED && ((flingOffset.value < -deleteDistanceThresholdPx && velocity < -velocityThreshold) || (flingOffset.value < -deleteDistanceThresholdPx && velocity <= 0))) {
                                coroutineScope.launch {
                                    swipeState = SwipeState.IDLE
                                    deleteFlingOffset.animateTo(
                                        targetValue = -screenWidthPx + revealButtonWidthPx,
                                        animationSpec = TweenSpec(
                                            durationMillis = 150,
                                            delay = 0,
                                            easing = CubicBezierEasing(
                                                0.2f,
                                                0f,
                                                0.56f,
                                                0.48f
                                            )
                                        ),
                                        initialVelocity = velocity
                                    )
                                    onDeleteClick()
                                }
                            } else if ((flingOffset.value < -revealButtonWidthPx / 2) || (velocity < -velocityThreshold && flingOffset.value < -revealButtonWidthPx / 4)) {
                                // If the to-do item is swiped far enough, reveal the delete button.
                                swipeState = SwipeState.REVEALED
                                coroutineScope.launch {
                                    flingOffset.animateTo(
                                        targetValue = -revealButtonWidthPx,
                                        animationSpec = SpringSpec(
                                            dampingRatio = 0.8f,
                                            stiffness = 1000f
                                        ),
                                        initialVelocity = velocity
                                    )
                                }
                                onExpand()
                            } else {
                                // Otherwise, collapse the to-do item.
                                swipeState = SwipeState.IDLE
                                coroutineScope.launch { flingOffset.snapTo(0f) }
                                onCollapse()
                            }
                        }
                    }
                )
        ) {
            TodoItemRow(item, onCheckedChange, modifier)
        }
    }
}
