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
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nevoit.cresto.R
import com.nevoit.cresto.data.todo.SubTodoItem
import com.nevoit.cresto.data.todo.TodoItem
import com.nevoit.cresto.data.todo.TodoItemWithSubTodos
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.AppSpecs
import com.nevoit.cresto.theme.defaultEnterTransition
import com.nevoit.cresto.theme.defaultExitTransition
import com.nevoit.cresto.theme.getFlagColor
import com.nevoit.cresto.theme.harmonize
import com.nevoit.cresto.ui.components.CustomAnimatedVisibility
import com.nevoit.cresto.ui.components.glasense.GlasenseCheckbox
import com.nevoit.cresto.ui.components.glasense.SwipeableActionButton
import com.nevoit.cresto.ui.components.glasense.SwipeableContainer
import com.nevoit.cresto.ui.components.glasense.SwipeableListState
import com.nevoit.cresto.ui.components.glasense.extend.LineThroughBasicTextField
import com.nevoit.cresto.ui.components.glasense.extend.LineThroughText
import com.nevoit.glasense.theme.Red500
import com.nevoit.glasense.theme.Yellow500
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * A composable function that displays a single to-do item with a checkbox, title, due date, flag.
 *
 * @param item The [TodoItem] to display.
 * @param onCheckedChange A callback that is invoked when the checkbox is checked or unchecked.
 * @param modifier A [Modifier] for this composable.
 */
@Composable
fun TodoItemRow(
    item: TodoItemWithSubTodos,
    isDueTodayMarkerEnabled: Boolean,
    isOverdueMarkerEnabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onCheckboxTapPosition: (Offset) -> Unit = {},
    modifier: Modifier
) {
    val completedTask = item.subTodos.filter { it.isCompleted }
    val totalTaskCount = item.subTodos.size
    val hasTasks = !item.subTodos.isEmpty()

    val item = item.todoItem

    val completedText = stringResource(R.string.completed)
    val flagText = stringResource(R.string.flag)
    val dueTodayText = stringResource(R.string.due_today_text)

    val density = LocalDensity.current

    Row(
        modifier = Modifier
            .defaultMinSize(minHeight = 68.dp)
            .fillMaxWidth()
            .background(
                color = AppColors.cardBackground,
                shape = AppSpecs.cardShape,
            )
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(12.dp))
        GlasenseCheckbox(
            checked = item.isCompleted,
            onCheckedChange = onCheckedChange,
            onTapPosition = onCheckboxTapPosition
        )
        Spacer(modifier = Modifier.width(12.dp))
        // If the to-do item has no due date, display only the title.
        if (item.dueDate == null && !hasTasks) {
            LineThroughText(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                lineThrough = item.isCompleted,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp)
            )
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp)
            ) {
                LineThroughText(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    lineThrough = item.isCompleted
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row {
                    if (item.dueDate != null) {
                        val today = LocalDate.now()

                        val isToday = item.dueDate == today
                        val isExpired = item.dueDate < today

                        val formattedDate = if (item.dueDate.year == today.year) {
                            item.dueDate.format(DateTimeFormatter.ofPattern("M/d"))
                        } else {
                            item.dueDate.format(DateTimeFormatter.ofPattern("yyyy/M/d"))
                        }

                        val dueDateText = if (isToday) {
                            if (isDueTodayMarkerEnabled && !item.isCompleted) {
                                dueTodayText
                            } else {
                                formattedDate
                            }
                        } else if (isExpired) {
                            if (isOverdueMarkerEnabled && !item.isCompleted) {
                                stringResource(R.string.overdue_with_date, formattedDate)
                            } else {
                                formattedDate
                            }
                        } else {
                            formattedDate
                        }

                        val color = if (isToday) {
                            if (isDueTodayMarkerEnabled && !item.isCompleted) harmonize(Yellow500) else AppColors.content.copy(
                                .4f
                            )
                        } else if (isExpired) {
                            if (isOverdueMarkerEnabled && !item.isCompleted) harmonize(Red500) else AppColors.content.copy(
                                .4f
                            )
                        } else {
                            AppColors.content.copy(.4f)
                        }
                        Icon(
                            painter = painterResource(R.drawable.ic_calendar_alt),
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier
                                .size(density.run { 18.sp.toDp() })
                                .align(Alignment.CenterVertically)
                        )

                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = dueDateText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 14.sp,
                            color = color
                        )
                    }
                    if (item.dueDate != null && hasTasks) {
                        Text(
                            text = " · ",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 14.sp,
                            modifier = Modifier.alpha(0.4f)
                        )
                    }
                    if (hasTasks) {
                        val color = AppColors.content
                        val areCompleted = totalTaskCount == completedTask.size
                        Row(
                            modifier = Modifier
                                .alpha(0.4f)
                                .then(
                                    if (areCompleted) Modifier.lineThrough(
                                        color,
                                        width = 1.5.dp
                                    ) else Modifier
                                )
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_list_bullet_clipboard),
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier
                                    .size(density.run { 18.sp.toDp() })
                                    .align(Alignment.CenterVertically)
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = "$completedText ",
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 14.sp,
                                color = color
                            )
                            Text(
                                text = "${completedTask.size}/$totalTaskCount",
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 14.sp,
                                color = color
                            )
                        }
                    }
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
                    contentDescription = flagText,
                    modifier = Modifier.fillMaxSize(),
                    tint = getFlagColor(item.flag)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
    }
}


@Composable
fun SwipeableTodoItem(
    listState: SwipeableListState,
    item: TodoItemWithSubTodos,
    isDueTodayMarkerEnabled: Boolean,
    isOverdueMarkerEnabled: Boolean,
    onDelete: () -> Unit,
    modifier: Modifier,
    onCheckedChange: (Boolean) -> Unit,
    onCheckboxTapPosition: (Offset) -> Unit = {}
) {
    val actions = listOf(
        SwipeableActionButton(
            index = 0,
            color = AppColors.error,
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
        TodoItemRow(
            item = item,
            isDueTodayMarkerEnabled = isDueTodayMarkerEnabled,
            isOverdueMarkerEnabled = isOverdueMarkerEnabled,
            onCheckedChange = onCheckedChange,
            onCheckboxTapPosition = onCheckboxTapPosition,
            modifier = modifier
        )
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
                color = AppColors.cardBackground,
                shape = AppSpecs.cardShape,
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
            LineThroughBasicTextField(
                lineThrough = item.isCompleted,
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
                    color = AppColors.content,
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
                color = AppColors.cardBackground,
                shape = AppSpecs.cardShape,
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
            LineThroughBasicTextField(
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
                    color = AppColors.content
                ),
                lineThrough = subTodo.isCompleted
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


    BackHandler(isFocused) {
        focusManager.clearFocus()
    }

    val addTaskText = stringResource(R.string.add_task)

    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 68.dp)
            .fillMaxWidth()
            .background(
                color = AppColors.cardBackground,
                shape = AppSpecs.cardShape,
            )
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(12.dp))
        Box(modifier = Modifier.size(24.dp)) {
            CustomAnimatedVisibility(
                visible = isFocused,
                enter = defaultEnterTransition,
                exit = defaultExitTransition
            ) {
                GlasenseCheckbox(
                    checked = checked,
                    onCheckedChange = { checked = !checked }
                )
            }
            CustomAnimatedVisibility(
                visible = !isFocused,
                enter = defaultEnterTransition,
                exit = defaultExitTransition
            ) {
                Icon(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .requiredSize(32.dp),
                    painter = painterResource(id = R.drawable.ic_add),
                    contentDescription = null,
                    tint = AppColors.primary
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 12.dp)
        ) {
            LineThroughBasicTextField(
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
                    color = AppColors.content
                ),
                decorator = { innerTextField ->
                    if (state.text.isEmpty() && !isFocused) {
                        Text(
                            text = addTaskText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = AppColors.content
                            ),
                            color = AppColors.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.Center)
                        )
                    }
                    innerTextField()
                },
                lineThrough = checked
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
            color = AppColors.primary,
            icon = painterResource(id = R.drawable.ic_duplicate_plus),
            isDestructive = false
        ),
        SwipeableActionButton(
            index = 1,
            color = AppColors.error,
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

fun Modifier.lineThrough(color: Color, width: Dp): Modifier = drawWithContent {
    val inset = width.toPx() / 2
    drawContent()
    drawLine(
        color = color,
        start = Offset(inset, size.height / 2),
        end = Offset(size.width - inset, size.height / 2),
        strokeWidth = width.toPx(),
        cap = StrokeCap.Round
    )
}