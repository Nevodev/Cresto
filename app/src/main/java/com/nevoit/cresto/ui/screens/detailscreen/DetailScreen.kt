package com.nevoit.cresto.ui.screens.detailscreen

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.capsule.ContinuousCapsule
import com.nevoit.cresto.R
import com.nevoit.cresto.data.todo.EXTRA_DELETE_ID
import com.nevoit.cresto.data.todo.SubTodoItem
import com.nevoit.cresto.data.todo.TodoItem
import com.nevoit.cresto.data.todo.TodoViewModel
import com.nevoit.cresto.toolkit.overscroll.OffsetOverscrollFactory
import com.nevoit.cresto.ui.components.CustomAnimatedVisibility
import com.nevoit.cresto.ui.components.bottomsheet.SelectedButton
import com.nevoit.cresto.ui.components.glasense.DialogItemData
import com.nevoit.cresto.ui.components.glasense.DialogState
import com.nevoit.cresto.ui.components.glasense.GlasenseBottomBar
import com.nevoit.cresto.ui.components.glasense.GlasenseButton
import com.nevoit.cresto.ui.components.glasense.GlasenseButtonAlt
import com.nevoit.cresto.ui.components.glasense.GlasenseDialog
import com.nevoit.cresto.ui.components.glasense.GlasenseDynamicSmallTitle
import com.nevoit.cresto.ui.components.glasense.GlasenseLoadingIndicator
import com.nevoit.cresto.ui.components.glasense.extend.overscrollSpacer
import com.nevoit.cresto.ui.components.glasense.isScrolledPast
import com.nevoit.cresto.ui.components.glasense.rememberSwipeableListState
import com.nevoit.cresto.ui.components.packed.HorizontalFlagPicker
import com.nevoit.cresto.ui.components.packed.HorizontalPresetDatePicker
import com.nevoit.cresto.ui.components.packed.PageContent
import com.nevoit.cresto.ui.components.packed.SubTodoItemRowAdd
import com.nevoit.cresto.ui.components.packed.SwipeableSubTodoItemRowEditable
import com.nevoit.cresto.ui.components.packed.TodoItemRowEditable
import com.nevoit.cresto.ui.theme.glasense.AppButtonColors
import com.nevoit.cresto.ui.theme.glasense.AppColors
import com.nevoit.cresto.ui.theme.glasense.defaultEnterTransition
import com.nevoit.cresto.ui.theme.glasense.defaultExitTransition
import com.nevoit.cresto.ui.theme.glasense.getFlagColor
import com.nevoit.cresto.ui.theme.glasense.isAppInDarkTheme
import com.nevoit.cresto.util.formatRelativeTime
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalHazeApi::class, ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    todoId: Int,
    viewModel: TodoViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()

    val activity = LocalActivity.current
    val itemWithSubTodos by viewModel.getTodoWithSubTodos(todoId).collectAsState(initial = null)
    val currentItem = itemWithSubTodos

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val hazeState = rememberHazeState()

    val onSurfaceContainer = AppColors.scrimNormal
    val surfaceColor = AppColors.pageBackground

    val backdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }

    val lazyListState = rememberLazyListState()
    val swipeListState = rememberSwipeableListState()

    val isSmallTitleVisible by lazyListState.isScrolledPast(statusBarHeight + 24.dp)

    val darkMode = isAppInDarkTheme()

    var finalDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var title by remember { mutableStateOf("") }

    var ticker by remember { mutableIntStateOf(0) }

    val deleteDialogItems = listOf(
        DialogItemData(
            stringResource(R.string.cancel),
            onClick = {},
            isPrimary = false
        ),
        DialogItemData(
            stringResource(R.string.delete),
            icon = painterResource(R.drawable.ic_trash),
            onClick = {
                val resultIntent = Intent().apply {
                    putExtra(EXTRA_DELETE_ID, todoId)
                }
                activity?.setResult(Activity.RESULT_OK, resultIntent)
                activity?.finish()
            },
            isPrimary = true,
            isDestructive = true
        )
    )

    var dialogState by remember { mutableStateOf(DialogState()) }

    val showDialog: (items: List<DialogItemData>, title: String, message: String?) -> Unit =
        { items, title, message ->
            dialogState =
                DialogState(isVisible = true, items = items, title = title, message = message)
        }

    val dismissDialog = {
        dialogState = dialogState.copy(isVisible = false)
    }

    val context = LocalContext.current


    LaunchedEffect(Unit) {
        while (true) {
            delay(60000L) // 1 minute
            ticker++
        }
    }

    LaunchedEffect(itemWithSubTodos) {
        if (itemWithSubTodos != null) {
            finalDate = itemWithSubTodos?.todoItem?.dueDate
            selectedIndex = itemWithSubTodos?.todoItem?.flag ?: 0
            title = itemWithSubTodos?.todoItem?.title ?: ""
        }
    }

    LaunchedEffect(selectedIndex) {
        itemWithSubTodos?.let {
            viewModel.update(it.todoItem.copy(flag = selectedIndex))
        }
    }
    LaunchedEffect(finalDate) {
        itemWithSubTodos?.let {
            viewModel.update(it.todoItem.copy(dueDate = finalDate))
        }
    }
    LaunchedEffect(title) {
        itemWithSubTodos?.let {
            viewModel.update(it.todoItem.copy(title = title))
        }
    }

    val animationScope = rememberCoroutineScope()
    // Create a custom overscroll factory.
    val overscrollFactory = remember {
        OffsetOverscrollFactory(
            orientation = Orientation.Horizontal,
            animationScope = animationScope,
        )
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaceColor)
            .layerBackdrop(backdrop)
    ) {
        if (currentItem == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                GlasenseLoadingIndicator(modifier = Modifier.fillMaxSize())
            }
        } else {
            PageContent(
                state = lazyListState,
                modifier = Modifier
                    .hazeSource(hazeState, 0f)
                    .imePadding(),
                tabPadding = false,
                bottomPadding = 64.dp + navigationBarHeight * 2
            ) {
                item(key = "status_bar") {
                    Box(
                        modifier = Modifier
                            .animateItem(placementSpec = spring(0.9f, 400f))
                            .padding(top = 48.dp + statusBarHeight + 12.dp)
                    )
                }
                item(key = "edit") {
                    TodoItemRowEditable(
                        item = currentItem.todoItem,
                        onCheckedChange = { isChecked ->
                            viewModel.update(currentItem.todoItem.copy(isCompleted = isChecked))
                        },
                        modifier = Modifier.animateItem(placementSpec = spring(0.9f, 400f)),
                        onEditEnd = { string ->
                            // if update here will cause conflict
                            title = string
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                item(key = "controls") {
                    CompositionLocalProvider(
                        LocalOverscrollFactory provides overscrollFactory
                    ) {
                        BoxWithConstraints(
                            modifier = Modifier
                                .animateItem(placementSpec = spring(0.9f, 400f))
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            var selectedButton by remember { mutableStateOf(SelectedButton.NONE) }

                            val totalWidth = this.maxWidth

                            // Calculate the width of the buttons based on the selected state.
                            val collapsedSize = 48.dp
                            val spacerSize = 12.dp
                            val expandedWidth =
                                totalWidth - (collapsedSize * 1) - (spacerSize * 1)
                            val defaultWidth = (totalWidth - (spacerSize * 1)) / 2

                            // Animate the width of the due date button.
                            val dueDateWidth by animateDpAsState(
                                targetValue = when (selectedButton) {
                                    SelectedButton.DUE_DATE -> expandedWidth
                                    SelectedButton.NONE -> defaultWidth
                                    else -> collapsedSize
                                },
                                animationSpec = spring(
                                    dampingRatio = 0.7f,
                                    stiffness = 300f
                                )
                            )
                            // Animate the width of the flag button.
                            val flagWidth by animateDpAsState(
                                targetValue = when (selectedButton) {
                                    SelectedButton.FLAG -> expandedWidth
                                    SelectedButton.NONE -> defaultWidth
                                    else -> collapsedSize
                                },
                                animationSpec = spring(
                                    dampingRatio = 0.7f,
                                    stiffness = 300f
                                )
                            )
                            // Animate the width of the hashtag button.
                            val hashtagWidth by animateDpAsState(
                                targetValue = when (selectedButton) {
                                    SelectedButton.HASHTAG -> expandedWidth
                                    SelectedButton.NONE -> defaultWidth
                                    else -> collapsedSize
                                },
                                animationSpec = spring(
                                    dampingRatio = 0.7f,
                                    stiffness = 300f
                                )
                            )
                            Row(
                                modifier = Modifier
                                    .width(totalWidth)
                                    .height(48.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Due date button.
                                GlasenseButtonAlt(
                                    enabled = true,
                                    shape = ContinuousCapsule,
                                    onClick = {
                                        selectedButton =
                                            if (selectedButton == SelectedButton.DUE_DATE) {
                                                SelectedButton.NONE
                                            } else {
                                                SelectedButton.DUE_DATE
                                            }
                                    },
                                    modifier = Modifier
                                        .height(48.dp)
                                        .width(dueDateWidth),
                                    colors = if (darkMode) AppButtonColors.secondary().copy(
                                        containerColor = AppButtonColors.secondary().containerColor.copy(
                                            .1f
                                        )
                                    ) else AppButtonColors.secondary(),
                                    indication = true
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Animated visibility for the due date icon.
                                        CustomAnimatedVisibility(
                                            visible = selectedButton != SelectedButton.DUE_DATE,
                                            enter = defaultEnterTransition,
                                            exit = defaultExitTransition
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_calendar),
                                                contentDescription = stringResource(R.string.due_date),
                                                modifier = Modifier.width(28.dp)
                                            )
                                        }
                                        // Animated visibility for the date picker.
                                        CustomAnimatedVisibility(
                                            visible = selectedButton == SelectedButton.DUE_DATE,
                                            enter = defaultEnterTransition,
                                            exit = defaultExitTransition
                                        ) {
                                            HorizontalPresetDatePicker(
                                                initialDate = finalDate,
                                                onDateSelected = {
                                                    finalDate = it
                                                    selectedButton = SelectedButton.NONE
                                                }
                                            )

                                        }
                                    }

                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                // Flag button.
                                GlasenseButtonAlt(
                                    enabled = true,
                                    shape = ContinuousCapsule,
                                    onClick = {
                                        selectedButton =
                                            if (selectedButton == SelectedButton.FLAG) {
                                                SelectedButton.NONE
                                            } else {
                                                SelectedButton.FLAG
                                            }
                                    },
                                    modifier = Modifier
                                        .height(48.dp)
                                        .width(flagWidth),
                                    colors = if (darkMode) AppButtonColors.secondary().copy(
                                        containerColor = AppButtonColors.secondary().containerColor.copy(
                                            .1f
                                        )
                                    ) else AppButtonColors.secondary(),
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Animated visibility for the flag icon.
                                        CustomAnimatedVisibility(
                                            visible = selectedButton != SelectedButton.FLAG,
                                            enter = defaultEnterTransition,
                                            exit = defaultExitTransition
                                        ) {
                                            val displayColor = getFlagColor(selectedIndex)
                                            Icon(
                                                painter = if (displayColor == Color.Transparent) {
                                                    painterResource(id = R.drawable.ic_flag)
                                                } else {
                                                    painterResource(id = R.drawable.ic_flag_fill)
                                                },
                                                contentDescription = stringResource(R.string.flag),
                                                modifier = Modifier.width(28.dp),
                                                tint = if (displayColor == Color.Transparent) {
                                                    AppColors.content.copy(
                                                        alpha = 0.5F
                                                    )
                                                } else {
                                                    displayColor
                                                }
                                            )
                                        }
                                        // Animated visibility for the flag picker.
                                        CustomAnimatedVisibility(
                                            visible = selectedButton == SelectedButton.FLAG,
                                            enter = defaultEnterTransition,
                                            exit = defaultExitTransition
                                        ) {
                                            HorizontalFlagPicker(
                                                selectedIndex = selectedIndex,
                                                onIndexSelected = { newIndex ->
                                                    selectedIndex = newIndex
                                                    selectedButton = SelectedButton.NONE
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                        }
                    }
                }
                item(key = "small_title") {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.task),
                        fontSize = 14.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = AppColors.contentVariant,
                        modifier = Modifier
                            .animateItem(placementSpec = spring(0.9f, 400f))
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 8.dp, start = 12.dp)
                    )
                }
                items(items = currentItem.subTodos, key = { it.id }) { subTodo ->
                    SwipeableSubTodoItemRowEditable(
                        listState = swipeListState,
                        subTodo = subTodo,
                        modifier = Modifier.animateItem(placementSpec = spring(0.9f, 400f)),
                        onEditEnd = { string, checked ->
                            viewModel.updateSubTodo(
                                subTodo.copy(
                                    description = string,
                                    isCompleted = checked
                                )
                            )
                        },
                        onDelete = {
                            viewModel.deleteSubTodo(subTodo)
                        },
                        onPromote = {
                            scope.launch {
                                viewModel.insert(
                                    TodoItem(
                                        title = subTodo.description,
                                        isCompleted = subTodo.isCompleted
                                    )
                                )
                                viewModel.deleteSubTodo(subTodo)
                            }
                        },
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                item(key = "add") {
                    SubTodoItemRowAdd(
                        modifier = Modifier.animateItem(placementSpec = spring(0.9f, 400f)),
                        onEditEnd = { description, checked ->
                            viewModel.insertSubTodo(
                                SubTodoItem(
                                    parentId = currentItem.todoItem.id,
                                    description = description,
                                    isCompleted = checked
                                )
                            )
                        }
                    )
                }
                overscrollSpacer(lazyListState)
            }
        }
        // A small title that dynamically appears at the top when the user scrolls down
        GlasenseDynamicSmallTitle(
            modifier = Modifier.align(Alignment.TopCenter),
            title = itemWithSubTodos?.todoItem?.title ?: stringResource(R.string.detail),
            statusBarHeight = statusBarHeight,
            isVisible = isSmallTitleVisible,
            hazeState = hazeState,
            surfaceColor = surfaceColor
        ) {
            // This lambda is empty as the component handles its own content
        }
        // Back button positioned at the top-start of the screen
        GlasenseButton(
            enabled = true,
            shape = CircleShape,
            onClick = { activity?.finish() }, // Closes the current activity, navigating back
            modifier = Modifier
                .padding(top = statusBarHeight, start = 12.dp)
                .size(48.dp)
                .align(Alignment.TopStart),
            colors = AppButtonColors.action()
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_forward_nav),
                contentDescription = stringResource(R.string.back),
                modifier = Modifier.width(32.dp)
            )
        }
        GlasenseBottomBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            navigationBarHeight = navigationBarHeight,
            isVisible = true,
            hazeState = hazeState,
            surfaceColor = surfaceColor,
            height = 64.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = navigationBarHeight + 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val creationTime = remember(itemWithSubTodos, ticker) {
                    itemWithSubTodos?.todoItem?.creationDate?.let {
                        formatRelativeTime(it, context)
                    } ?: ""
                }
                Text(
                    text = stringResource(R.string.created, creationTime),
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .weight(1f),
                    fontSize = 14.sp,
                    lineHeight = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        shadow = Shadow(
                            color = surfaceColor.copy(alpha = 1f),
                            offset = Offset(x = 0f, y = 0f),
                            blurRadius = 8f
                        )
                    )
                )
                val subTodoCount = itemWithSubTodos?.subTodos?.size ?: 0
                val deleteTodoSimpleText = stringResource(R.string.delete_todo_simple)
                val deletePluralsText =
                    pluralStringResource(R.plurals.delete_todo_with_subtasks, subTodoCount)
                val deleteCurrentTodoText = stringResource(R.string.delete_current_todo)
                val message = if (subTodoCount == 0) {
                    deleteTodoSimpleText
                } else {
                    deletePluralsText
                }
                GlasenseButton(
                    enabled = true,
                    shape = CircleShape,
                    onClick = {
                        showDialog(
                            deleteDialogItems,
                            deleteCurrentTodoText,
                            message
                        )
                    },
                    modifier = Modifier
                        .size(48.dp),
                    colors = AppButtonColors.action()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_trash),
                        contentDescription = stringResource(R.string.delete_current_todo),
                        modifier = Modifier.width(32.dp),
                        tint = AppColors.error
                    )
                }
            }

        }
    }

    if (dialogState.isVisible) {
        GlasenseDialog(
            dialogState = dialogState,
            backdrop = backdrop,
            onDismiss = { dismissDialog() },
            modifier = Modifier
        )
    }
}
