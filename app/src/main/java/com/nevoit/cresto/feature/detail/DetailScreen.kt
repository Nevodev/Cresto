package com.nevoit.cresto.feature.detail

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
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
import com.kyant.shapes.Capsule
import com.nevoit.cresto.R
import com.nevoit.cresto.data.todo.EXTRA_DELETE_ID
import com.nevoit.cresto.data.todo.SubTodoItem
import com.nevoit.cresto.data.todo.TodoItem
import com.nevoit.cresto.data.todo.TodoViewModel
import com.nevoit.cresto.feature.main.rememberFlagMenuItems
import com.nevoit.cresto.theme.AppButtonColors
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.AppSpecs
import com.nevoit.cresto.theme.getFlagColor
import com.nevoit.cresto.ui.components.glasense.DialogItemData
import com.nevoit.cresto.ui.components.glasense.DialogState
import com.nevoit.cresto.ui.components.glasense.DimIndication
import com.nevoit.cresto.ui.components.glasense.DueDatePicker
import com.nevoit.cresto.ui.components.glasense.GlasenseBottomBar
import com.nevoit.cresto.ui.components.glasense.GlasenseButton
import com.nevoit.cresto.ui.components.glasense.GlasenseDialog
import com.nevoit.cresto.ui.components.glasense.GlasenseDynamicSmallTitle
import com.nevoit.cresto.ui.components.glasense.GlasenseMenu
import com.nevoit.cresto.ui.components.glasense.GlasenseMenuItem
import com.nevoit.cresto.ui.components.glasense.MenuState
import com.nevoit.cresto.ui.components.glasense.PopupDirection
import com.nevoit.cresto.ui.components.glasense.ZeroHeightDivider
import com.nevoit.cresto.ui.components.glasense.extend.overscrollSpacer
import com.nevoit.cresto.ui.components.glasense.isScrolledPast
import com.nevoit.cresto.ui.components.glasense.rememberSwipeableListState
import com.nevoit.cresto.ui.components.packed.PageContent
import com.nevoit.cresto.ui.components.packed.SubTodoItemRowAdd
import com.nevoit.cresto.ui.components.packed.SwipeableSubTodoItemRowEditable
import com.nevoit.cresto.ui.components.packed.TodoItemRowEditable
import com.nevoit.cresto.ui.components.packed.VGap
import com.nevoit.cresto.util.formatRelativeTime
import com.nevoit.glasense.component.GlasenseActivityIndicator
import com.nevoit.glasense.theme.Springs
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

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

    val surfaceColor = AppColors.pageBackground

    val backdrop = rememberLayerBackdrop {
        drawRect(
            color = surfaceColor,
            size = Size(this.size.width * 3, this.size.height * 3),
            topLeft = Offset(-this.size.width, -this.size.height)
        )
        drawContent()
    }

    val lazyListState = rememberLazyListState()
    val swipeListState = rememberSwipeableListState()

    val isSmallTitleVisible by lazyListState.isScrolledPast(statusBarHeight + 24.dp)

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

    var menuState by remember { mutableStateOf(MenuState()) }

    val showMenu: (anchorBounds: Rect, items: List<GlasenseMenuItem>) -> Unit =
        { bounds, items ->
            menuState = MenuState(isVisible = true, anchorBounds = bounds, items = items)
        }

    val dismissMenu = {
        menuState = menuState.copy(isVisible = false)
    }

    val context = LocalContext.current
    val moreMenu = rememberMoreMenuItems {
        dismissMenu()
        scope.launch {
            delay(200.milliseconds)
            viewModel.duplicateById(todoId).join()
            activity?.finish()
        }
    }
    var moreButtonBounds by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var flagButtonBounds by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var dateButtonBounds by remember { mutableStateOf(Rect.Zero) }
    var isDatePickerVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1.minutes) // 1 minute
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

    val flagMenu = rememberFlagMenuItems(noneFirst = true) { index ->
        itemWithSubTodos?.let {
            viewModel.update(it.todoItem.copy(flag = index))
        }
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
                GlasenseActivityIndicator(modifier = Modifier.fillMaxSize())
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
                            .animateItem(placementSpec = Springs.crisp())
                            .padding(top = 48.dp + statusBarHeight + 12.dp)
                    )
                }
                item(key = "edit") {
                    TodoItemRowEditable(
                        item = currentItem.todoItem,
                        onCheckedChange = { isChecked ->
                            viewModel.update(currentItem.todoItem.copy(isCompleted = isChecked))
                        },
                        modifier = Modifier.animateItem(placementSpec = Springs.crisp()),
                        onEditEnd = { string ->
                            // if update here will cause conflict
                            title = string
                        }
                    )
                    VGap()
                }
                item(key = "information") {
                    CompositionLocalProvider(
                        LocalContentColor provides AppColors.contentVariant
                    ) {
                        Column(
                            modifier = Modifier
                                .animateItem(placementSpec = Springs.crisp())
                                .fillMaxWidth()
                                .background(
                                    color = AppColors.cardBackground,
                                    shape = AppSpecs.cardShape
                                )
                                .padding(horizontal = 12.dp)

                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter =
                                        painterResource(id = R.drawable.ic_calendar),
                                    contentDescription = stringResource(R.string.due_date),
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .width(28.dp)
                                )
                                Text(
                                    text = stringResource(R.string.due_date),
                                    fontSize = 16.sp,
                                    lineHeight = 18.sp,
                                    fontWeight = FontWeight.Normal,
                                    modifier = Modifier
                                        .align(Alignment.CenterVertically)
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Box(
                                    modifier = Modifier
                                        .onGloballyPositioned { coordinates ->
                                            dateButtonBounds = coordinates.boundsInWindow()
                                        }
                                        .align(Alignment.CenterVertically)
                                        .wrapContentSize()
                                        .clip(Capsule())
                                        .background(
                                            color = AppColors.scrimNormal
                                        )
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = DimIndication()
                                        ) {
                                            isDatePickerVisible = true
                                        }
                                ) {
                                    Text(
                                        text = finalDate?.format(DateTimeFormatter.ofPattern("yyyy/M/d"))
                                            ?: stringResource(R.string.none),
                                        fontSize = 16.sp,
                                        lineHeight = 18.sp,
                                        fontWeight = FontWeight.Normal,
                                        modifier = Modifier.padding(
                                            horizontal = 8.dp,
                                            vertical = 4.dp
                                        ),
                                        color = AppColors.content
                                    )
                                }
                            }
                            ZeroHeightDivider()
                            Row(
                                modifier = Modifier
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_flag),
                                    contentDescription = stringResource(R.string.flag),
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .width(28.dp)
                                )
                                Text(
                                    text = stringResource(R.string.flag),
                                    fontSize = 16.sp,
                                    lineHeight = 18.sp,
                                    fontWeight = FontWeight.Normal,
                                    modifier = Modifier
                                        .align(Alignment.CenterVertically)
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Row(
                                    modifier = Modifier
                                        .onGloballyPositioned { coordinates ->
                                            flagButtonBounds = coordinates
                                        }
                                        .align(Alignment.CenterVertically)
                                        .wrapContentSize()
                                        .clip(Capsule())
                                        .background(
                                            color = AppColors.scrimNormal
                                        )
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = DimIndication()
                                        ) {
                                            flagButtonBounds?.let {
                                                showMenu(
                                                    it.boundsInWindow(),
                                                    flagMenu
                                                )
                                            }
                                        }
                                        .padding(
                                            horizontal = 8.dp,
                                            vertical = 4.dp
                                        ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (selectedIndex != 0) {
                                        Box(
                                            modifier = Modifier
                                                .padding(end = 6.dp)
                                                .size(12.dp)
                                                .background(
                                                    color = getFlagColor(selectedIndex),
                                                    shape = CircleShape
                                                )
                                        )
                                    }
                                    Text(
                                        text = getFlagText(selectedIndex),
                                        fontSize = 16.sp,
                                        lineHeight = 18.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = AppColors.content
                                    )
                                }
                            }
                        }
                    }
                    VGap()
                }
                item(key = "small_title") {

                    Text(
                        text = stringResource(R.string.task),
                        fontSize = 14.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = AppColors.contentVariant,
                        modifier = Modifier
                            .animateItem(placementSpec = Springs.crisp())
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 8.dp, start = 12.dp)
                    )
                }
                items(items = currentItem.subTodos, key = { it.id }) { subTodo ->
                    SwipeableSubTodoItemRowEditable(
                        listState = swipeListState,
                        subTodo = subTodo,
                        modifier = Modifier.animateItem(placementSpec = Springs.crisp()),
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
                    VGap()
                }
                item(key = "add") {
                    SubTodoItemRowAdd(
                        modifier = Modifier.animateItem(placementSpec = Springs.crisp()),
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
        val sharedInteractionSource = remember { MutableInteractionSource() }
        GlasenseButton(
            enabled = true,
            interactionSource = sharedInteractionSource,
            shape = CircleShape,
            onClick = {},
            modifier = Modifier
                .padding(top = statusBarHeight, end = 12.dp)
                .size(48.dp)
                .align(Alignment.TopEnd),
            colors = AppButtonColors.action()
        ) {
            Box(
                modifier = Modifier
                    .onGloballyPositioned { coordinates ->
                        moreButtonBounds = coordinates
                    }
                    .height(48.dp)
                    .width(48.dp)
                    .clickable(
                        interactionSource = sharedInteractionSource,
                        indication = null
                    ) {
                        moreButtonBounds?.let {
                            showMenu(
                                it.boundsInWindow(),
                                moreMenu
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_ellipsis),
                    contentDescription = stringResource(R.string.more),
                    modifier = Modifier.width(32.dp),
                    tint = AppColors.primary
                )
            }
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
                    itemWithSubTodos?.todoItem?.creationDateTime?.let {
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

    GlasenseDialog(
        dialogState = dialogState,
        backdrop = backdrop,
        onDismiss = { dismissDialog() },
        modifier = Modifier
    )

    GlasenseMenu(
        menuState = menuState,
        backdrop = backdrop,
        onDismiss = dismissMenu
    )

    DueDatePicker(
        isVisible = isDatePickerVisible,
        anchorBounds = dateButtonBounds,
        initialDate = finalDate,
        onDismiss = { isDatePickerVisible = false },
        onDateSelected = { date ->
            finalDate = date
        },
        direction = PopupDirection.Up
    )
}

@Composable
fun getFlagText(index: Int): String {
    val flagNames = listOf(
        stringResource(R.string.none),
        stringResource(R.string.flag_red),
        stringResource(R.string.flag_orange),
        stringResource(R.string.flag_yellow),
        stringResource(R.string.flag_green),
        stringResource(R.string.flag_blue),
        stringResource(R.string.flag_purple),
        stringResource(R.string.flag_gray)
    )
    return flagNames[index]
}