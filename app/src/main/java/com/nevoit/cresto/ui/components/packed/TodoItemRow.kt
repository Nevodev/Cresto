package com.nevoit.cresto.ui.components.packed

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
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
import com.nevoit.cresto.ui.components.glasense.GlasenseCheckbox
import com.nevoit.cresto.ui.components.glasense.SwipeableActionButton
import com.nevoit.cresto.ui.components.glasense.SwipeableContainer
import com.nevoit.cresto.ui.components.glasense.SwipeableListState
import com.nevoit.cresto.ui.theme.glasense.CalculatedColor
import com.nevoit.cresto.ui.theme.glasense.Red500
import com.nevoit.cresto.ui.theme.glasense.getFlagColor
import com.nevoit.cresto.util.g2
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
        GlasenseCheckbox(
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
    listState: SwipeableListState,
    item: TodoItemWithSubTodos,
    onDelete: () -> Unit,
    modifier: Modifier,
    onCheckedChange: (Boolean) -> Unit
) {
    val actions = listOf(
        SwipeableActionButton(
            index = 0,
            color = Red500,
            icon = painterResource(id = R.drawable.ic_trash),
            isDestructive = true
        )
    )

    SwipeableContainer(
        key = item.todoItem.id,
        listState = listState,
        modifier = Modifier,
        actions = actions,
        onAction = { index ->
            when (index) {
                0 -> onDelete()
            }
        }
    ) {
        TodoItemRow(item, onCheckedChange, modifier)
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
        GlasenseCheckbox(
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
        GlasenseCheckbox(
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
        GlasenseCheckbox(
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
fun SwipeableSubTodoItemRowEditable(
    listState: SwipeableListState,
    subTodo: SubTodoItem,
    onDelete: () -> Unit,
    onPromote: () -> Unit,
    modifier: Modifier,
    onEditEnd: (String, Boolean) -> Unit,
) {
    val actions = listOf(
        SwipeableActionButton(
            index = 0,
            color = MaterialTheme.colorScheme.primary,
            icon = painterResource(id = R.drawable.ic_duplicate),
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
        key = subTodo.id,
        listState = listState,
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
            modifier = Modifier,
            onEditEnd = { string, boolean -> onEditEnd(string, boolean) })
    }
}