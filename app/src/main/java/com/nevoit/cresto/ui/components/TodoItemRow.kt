package com.nevoit.cresto.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush.Companion.verticalGradient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kyant.capsule.ContinuousRoundedRectangle
import com.nevoit.cresto.R
import com.nevoit.cresto.data.SubTodoItem
import com.nevoit.cresto.data.TodoItem
import com.nevoit.cresto.data.TodoItemWithSubTodos
import com.nevoit.cresto.ui.components.glasense.GlasenseButton
import com.nevoit.cresto.ui.theme.glasense.Amber500
import com.nevoit.cresto.ui.theme.glasense.CalculatedColor
import com.nevoit.cresto.ui.theme.glasense.Red500
import com.nevoit.cresto.ui.theme.glasense.getFlagColor
import com.nevoit.cresto.util.g2
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import kotlin.math.abs

/**
 * A composable function that displays a single to-do item with a checkbox, title, due date, flag, and hashtag.
 *
 * @param item The [TodoItem] to display.
 * @param onCheckedChange A callback that is invoked when the checkbox is checked or unchecked.
 * @param modifier A [Modifier] for this composable.
 */
@Composable
fun TodoItemRow(
    item: TodoItemWithSubTodos,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier
) {
    val completedTask = item.subTodos.filter { it.isCompleted }
    val totalTaskCount = item.subTodos.size
    val hasTasks = !item.subTodos.isEmpty()

    val item = item.todoItem

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
            if (hasTasks) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 12.dp)
                ) {
                    Text(
                        text = item.title,
                        textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row() {
                        Text(
                            text = "Completed ",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 14.sp,
                            modifier = Modifier.alpha(0.4f)
                        )
                        Text(
                            text = "${completedTask.size}/$totalTaskCount",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 14.sp,
                            modifier = Modifier.alpha(0.4f)
                        )
                    }
                }
            } else {
                Text(
                    text = item.title,
                    textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 12.dp)
                )
            }
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
                if (hasTasks) {
                    Row() {
                        Text(
                            text = item.dueDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd")),
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 14.sp,
                            modifier = Modifier.alpha(0.4f)
                        )
                        Text(
                            text = " · Completed ",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 14.sp,
                            modifier = Modifier.alpha(0.4f)
                        )
                        Text(
                            text = "${completedTask.size}/$totalTaskCount",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 14.sp,
                            modifier = Modifier.alpha(0.4f)
                        )
                    }
                } else {
                    Text(
                        text = item.dueDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd")),
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 14.sp,
                        modifier = Modifier.alpha(0.4f)
                    )
                }
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
    item: TodoItemWithSubTodos,
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


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TodoItemRowEditable(
    item: TodoItem,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier,
    onEditEnd: (String) -> Unit,
) {

    val state = rememberTextFieldState(initialText = item.title)

    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }

    val isKeyboardVisible = WindowInsets.isImeVisible
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isKeyboardVisible) {
        if (!isKeyboardVisible && isFocused) {
            focusManager.clearFocus()
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                onEditEnd(state.text.toString())
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Row(
        modifier = modifier
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
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 12.dp)
        ) {
            BasicTextField(
                state = state,
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (isFocused && !focusState.isFocused) {
                            if (state.text.toString() != item.title) {
                                onEditEnd(state.text.toString())
                            }
                        }
                        isFocused = focusState.isFocused
                    },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                onKeyboardAction = {
                    focusManager.clearFocus()
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
            )
            if (!isFocused) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            focusRequester.requestFocus()
                        }
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubTodoItemRowEditable(
    subTodo: SubTodoItem,
    modifier: Modifier,
    onEditEnd: (String, Boolean) -> Unit,
) {
    val state = rememberTextFieldState(initialText = subTodo.description)

    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }

    val isKeyboardVisible = WindowInsets.isImeVisible
    val focusRequester = remember { FocusRequester() }
    var checked by remember { mutableStateOf(subTodo.isCompleted) }

    LaunchedEffect(isKeyboardVisible) {
        if (!isKeyboardVisible && isFocused) {
            focusManager.clearFocus()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                onEditEnd(state.text.toString(), checked)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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
            checked = checked,
            onCheckedChange = {
                checked = !checked
                onEditEnd(state.text.toString(), checked)
            }
        )
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 12.dp)
        ) {
            BasicTextField(
                state = state,
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (isFocused && !focusState.isFocused) {
                            if (state.text.toString() != subTodo.description) {
                                onEditEnd(state.text.toString(), checked)
                            }
                        }
                        isFocused = focusState.isFocused
                    },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                onKeyboardAction = {
                    focusManager.clearFocus()
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (subTodo.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                lineLimits = TextFieldLineLimits.MultiLine(
                    minHeightInLines = 1,
                    maxHeightInLines = Int.MAX_VALUE
                )
            )
            if (!isFocused) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            focusRequester.requestFocus()
                        }
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubTodoItemRowAdd(
    modifier: Modifier = Modifier,
    onEditEnd: (String, Boolean) -> Unit,
) {
    val state = rememberTextFieldState(initialText = "")
    var checked by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    var isFocused by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    fun submit() {
        val text = state.text.toString()
        if (text.isNotBlank()) {
            onEditEnd(text, checked)
            state.edit { delete(0, length) }
            checked = false
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                submit()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }


    BackHandler(isFocused, { focusManager.clearFocus() })


    Row(
        modifier = modifier
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
            checked = checked,
            onCheckedChange = { checked = !checked }
        )
        Spacer(modifier = Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 12.dp)
        ) {
            BasicTextField(
                state = state,
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (isFocused && !focusState.isFocused) {
                            submit()
                        }
                        isFocused = focusState.isFocused
                    },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                onKeyboardAction = {
                    focusManager.clearFocus()
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorator = { innerTextField ->
                    if (state.text.isEmpty() && !isFocused) {
                        Text(
                            text = "Add task",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None,
                            ),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.Center)
                        )
                    }
                    innerTextField()
                },
                lineLimits = TextFieldLineLimits.MultiLine(
                    minHeightInLines = 1,
                    maxHeightInLines = Int.MAX_VALUE
                )
            )
            if (!isFocused) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            focusRequester.requestFocus()
                        }
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
    }
}

@Composable
fun SwipeableContainer(
    actions: List<SwipeableActionButton>,
    onAction: (Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var swipeState by remember { mutableStateOf(SwipeState.IDLE) }
    var initialSwipeState by remember { mutableStateOf(SwipeState.IDLE) }
    val coroutineScope = rememberCoroutineScope()

    val density = LocalDensity.current

    val actionButtonWidth = 66.dp
    val actionButtonWidthPx = with(density) { actionButtonWidth.toPx() }

    val gapPx = with(density) { 6.dp.toPx() }

    val totalActionsWidthPx = actionButtonWidthPx * actions.size
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
                        .width(actionButtonWidth - 6.dp)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    val trueIndex = actions.size - index - 1
                    val revealThreshold =
                        gapPx + (actionButtonWidthPx - gapPx) * trueIndex + (actionButtonWidthPx - gapPx) / 2

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
                    },
                    onDragStopped = { velocity ->
                        coroutineScope.launch {
                            val currentOffset = flingOffset.value
                            val isDeepSwipe = currentOffset < -deepSwipeThresholdPx
                            val isFastSwipe = velocity < -velocityThreshold

                            if (velocity >= 0 && initialSwipeState == SwipeState.IDLE) {
                            } else if (actions.isNotEmpty() && ((isDeepSwipe && initialSwipeState == SwipeState.REVEALED) || (isFastSwipe && initialSwipeState == SwipeState.REVEALED))) {
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
                                swipeState = SwipeState.IDLE
                                flingOffset.animateTo(
                                    targetValue = 0f,
                                    animationSpec = SpringSpec(
                                        dampingRatio = 0.8f,
                                        stiffness = 1000f
                                    ),
                                    initialVelocity = velocity
                                )
                            }
                        }
                    }
                )
        ) {
            content()
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

@Composable
fun SwipeableSubTodoItemRowEditable(
    subTodo: SubTodoItem,
    onDelete: () -> Unit,
    onPromote: () -> Unit,
    modifier: Modifier,
    onEditEnd: (String, Boolean) -> Unit,
) {

    val actions = listOf(
        SwipeableActionButton(
            index = 0,
            color = Amber500,
            icon = painterResource(id = R.drawable.ic_pin),
            isDestructive = false
        ),
        SwipeableActionButton(
            index = 1,
            color = Red500,
            icon = painterResource(id = R.drawable.ic_trash),
            isDestructive = true
        )
    )

    SwipeableContainer(
        modifier = modifier,
        actions = actions,
        onAction = { index ->
            when (index) {
                0 -> onPromote()
                1 -> onDelete()
            }
        }
    ) {
        SubTodoItemRowEditable(
            subTodo = subTodo,
            modifier = modifier,
            onEditEnd = { string, boolean -> onEditEnd(string, boolean) })
    }
}