package com.nevoit.cresto.ui.detailscreen

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.capsule.ContinuousCapsule
import com.nevoit.cresto.R
import com.nevoit.cresto.data.SubTodoItem
import com.nevoit.cresto.data.TodoItem
import com.nevoit.cresto.ui.components.CustomAnimatedVisibility
import com.nevoit.cresto.ui.components.DynamicSmallTitle
import com.nevoit.cresto.ui.components.HorizontalFlagPicker
import com.nevoit.cresto.ui.components.HorizontalPresetDatePicker
import com.nevoit.cresto.ui.components.SelectedButton
import com.nevoit.cresto.ui.components.SubTodoItemRowAdd
import com.nevoit.cresto.ui.components.SwipeableSubTodoItemRowEditable
import com.nevoit.cresto.ui.components.TodoItemRowEditable
import com.nevoit.cresto.ui.components.glasense.DialogItemData
import com.nevoit.cresto.ui.components.glasense.DialogState
import com.nevoit.cresto.ui.components.glasense.GlasenseBottomBar
import com.nevoit.cresto.ui.components.glasense.GlasenseButton
import com.nevoit.cresto.ui.components.glasense.GlasenseButtonAlt
import com.nevoit.cresto.ui.components.glasense.GlasenseDialog
import com.nevoit.cresto.ui.components.glasense.rememberSwipeableListState
import com.nevoit.cresto.ui.components.myFadeIn
import com.nevoit.cresto.ui.components.myFadeOut
import com.nevoit.cresto.ui.components.myScaleIn
import com.nevoit.cresto.ui.components.myScaleOut
import com.nevoit.cresto.ui.overscroll.OffsetOverscrollFactory
import com.nevoit.cresto.ui.theme.glasense.AppButtonColors
import com.nevoit.cresto.ui.theme.glasense.CalculatedColor
import com.nevoit.cresto.ui.theme.glasense.Red500
import com.nevoit.cresto.ui.theme.glasense.getFlagColor
import com.nevoit.cresto.ui.viewmodel.TodoViewModel
import com.nevoit.cresto.util.formatRelativeTime
import com.nevoit.cresto.util.g2
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalHazeApi::class)
@Composable
fun DetailScreen(
    todoId: Int,
    viewModel: TodoViewModel = viewModel()
) {
    val activity = LocalActivity.current
    val itemWithSubTodos by viewModel.getTodoWithSubTodos(todoId).collectAsState(initial = null)

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val density = LocalDensity.current
    val thresholdPx = if (statusBarHeight > 0.dp) {
        with(density) {
            (statusBarHeight + 24.dp).toPx()
        }
    } else 0f

    val hazeState = rememberHazeState()

    val onSurfaceContainer = CalculatedColor.onSurfaceContainer
    val surfaceColor = CalculatedColor.hierarchicalBackgroundColor

    val backdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }

    val lazyListState = rememberLazyListState()
    val swipeListState = rememberSwipeableListState()

    val isSmallTitleVisible by remember(thresholdPx) { derivedStateOf { ((lazyListState.firstVisibleItemIndex == 0) && (lazyListState.firstVisibleItemScrollOffset > thresholdPx)) || lazyListState.firstVisibleItemIndex > 0 } }

    val darkMode = isSystemInDarkTheme()

    var finalDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var title by remember { mutableStateOf("") }

    var ticker by remember { mutableIntStateOf(0) }

    val deleteDialogItems = listOf(
        DialogItemData(
            "Cancel",
            onClick = {},
            isPrimary = false
        ),
        DialogItemData(
            "Delete",
            icon = painterResource(R.drawable.ic_trash),
            onClick = {
                activity?.finish()
                itemWithSubTodos?.todoItem?.let {
                    viewModel.delete(itemWithSubTodos!!.todoItem)
                }
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

    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaceColor)
            .layerBackdrop(backdrop)
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .hazeSource(hazeState, 0f)
                .fillMaxSize()
                .padding(0.dp)
                .background(surfaceColor)
                .imePadding(),
            contentPadding = PaddingValues(
                start = 12.dp,
                top = 0.dp,
                end = 12.dp,
                bottom = 64.dp + navigationBarHeight * 2
            )
        ) {
            item(key = "status_bar") {
                Box(
                    modifier = Modifier
                        .animateItem(placementSpec = spring(0.9f, 400f))
                        .padding(top = 48.dp + statusBarHeight + 12.dp)
                )
            }
            item(key = "edit") {
                if (itemWithSubTodos != null) {
                    itemWithSubTodos?.let {
                        TodoItemRowEditable(
                            item = it.todoItem,
                            onCheckedChange = { isChecked ->
                                viewModel.update(it.todoItem.copy(isCompleted = isChecked))
                            },
                            modifier = Modifier.animateItem(placementSpec = spring(0.9f, 400f)),
                            onEditEnd = { string ->
                                // if update here will cause conflict
                                title = string
                            }
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
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
                        val expandedWidth = totalWidth - (collapsedSize * 1) - (spacerSize * 1)
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
                                shape = ContinuousCapsule(g2),
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
                                        enter = myFadeIn(animationSpec = tween(delayMillis = 100)) + myScaleIn(
                                            animationSpec = tween(delayMillis = 100),
                                            initialScale = 0.9f
                                        ),
                                        exit = myFadeOut(animationSpec = tween(durationMillis = 100)) + myScaleOut(
                                            animationSpec = tween(delayMillis = 100),
                                            targetScale = 0.9f
                                        )
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_calendar),
                                            contentDescription = "Due Date",
                                            modifier = Modifier.width(28.dp)
                                        )
                                    }
                                    // Animated visibility for the date picker.
                                    CustomAnimatedVisibility(
                                        visible = selectedButton == SelectedButton.DUE_DATE,
                                        enter = myFadeIn(animationSpec = tween(delayMillis = 100)) + myScaleIn(
                                            animationSpec = tween(delayMillis = 100),
                                            initialScale = 0.9f
                                        ),
                                        exit = myFadeOut(animationSpec = tween(durationMillis = 100)) + myScaleOut(
                                            animationSpec = tween(delayMillis = 100),
                                            targetScale = 0.9f
                                        )
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
                                shape = ContinuousCapsule(g2),
                                onClick = {
                                    selectedButton = if (selectedButton == SelectedButton.FLAG) {
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
                                        enter = myFadeIn(animationSpec = tween(delayMillis = 100)) + myScaleIn(
                                            animationSpec = tween(delayMillis = 100),
                                            initialScale = 0.9f
                                        ),
                                        exit = myFadeOut(animationSpec = tween(durationMillis = 100)) + myScaleOut(
                                            animationSpec = tween(delayMillis = 100),
                                            targetScale = 0.9f
                                        )
                                    ) {
                                        val displayColor = getFlagColor(selectedIndex)
                                        Icon(
                                            painter = if (displayColor == Color.Transparent) {
                                                painterResource(id = R.drawable.ic_flag)
                                            } else {
                                                painterResource(id = R.drawable.ic_flag_fill)
                                            },
                                            contentDescription = "Flag",
                                            modifier = Modifier.width(28.dp),
                                            tint = if (displayColor == Color.Transparent) {
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5F)
                                            } else {
                                                displayColor
                                            }
                                        )
                                    }
                                    // Animated visibility for the flag picker.
                                    CustomAnimatedVisibility(
                                        visible = selectedButton == SelectedButton.FLAG,
                                        enter = myFadeIn(animationSpec = tween(delayMillis = 100)) + myScaleIn(
                                            animationSpec = tween(delayMillis = 100),
                                            initialScale = 0.9f
                                        ),
                                        exit = myFadeOut(animationSpec = tween(durationMillis = 100)) + myScaleOut(
                                            animationSpec = tween(delayMillis = 100),
                                            targetScale = 0.9f
                                        )
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
                    text = "Task",
                    fontSize = 14.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onBackground.copy(.5f),
                    modifier = Modifier
                        .animateItem(placementSpec = spring(0.9f, 400f))
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 8.dp, start = 12.dp)
                )
            }
            itemWithSubTodos?.subTodos?.let { subTodos ->
                items(items = subTodos, key = { it.id }) { subTodo ->
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
            }
            item(key = "add") {
                itemWithSubTodos?.todoItem?.let { parentTodo ->
                    SubTodoItemRowAdd(
                        modifier = Modifier.animateItem(placementSpec = spring(0.9f, 400f)),
                        onEditEnd = { description, checked ->
                            viewModel.insertSubTodo(
                                SubTodoItem(
                                    parentId = parentTodo.id,
                                    description = description,
                                    isCompleted = checked
                                )
                            )
                        }
                    )
                }
            }

        }
        // A small title that dynamically appears at the top when the user scrolls down
        DynamicSmallTitle(
            modifier = Modifier.align(Alignment.TopCenter),
            title = itemWithSubTodos?.todoItem?.title ?: "Detail",
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
            colors = ButtonDefaults.buttonColors(
                containerColor = onSurfaceContainer,
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_forward_nav),
                contentDescription = "Back",
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
                        formatRelativeTime(it)
                    } ?: ""
                }
                Text(
                    text = "Created $creationTime",
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
                GlasenseButton(
                    enabled = true,
                    shape = CircleShape,
                    onClick = {
                        showDialog(
                            deleteDialogItems,
                            "Delete current todo?",
                            "This will delete current todo${if (itemWithSubTodos?.subTodos?.isEmpty() == true) "" else " and its task${if (itemWithSubTodos?.subTodos?.size == 1) "" else ("s")}"} permanently. This action cannot be undone."
                        )
                    },
                    modifier = Modifier
                        .size(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = onSurfaceContainer,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_trash),
                        contentDescription = "Delete Current Todo",
                        modifier = Modifier.width(32.dp),
                        tint = Red500
                    )
                }
            }

        }
    }

    if (dialogState.isVisible) {
        GlasenseDialog(
            density = density,
            dialogState = dialogState,
            backdrop = backdrop,
            onDismiss = { dismissDialog() },
            modifier = Modifier
        )
    }
}
