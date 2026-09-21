package com.nevoit.cresto.feature.home

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.nevoit.cresto.R
import com.nevoit.cresto.data.todo.EXTRA_TODO_ID
import com.nevoit.cresto.data.todo.HomeGroupFilter
import com.nevoit.cresto.data.todo.TodoItemWithSubTodos
import com.nevoit.cresto.data.todo.TodoViewModel
import com.nevoit.cresto.feature.detail.DetailActivity
import com.nevoit.cresto.feature.settings.util.SettingsManager
import com.nevoit.cresto.feature.settings.util.SettingsViewModel
import com.nevoit.cresto.feature.settings.util.SortOption
import com.nevoit.cresto.feature.settings.util.SortOrder
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.ui.components.glasense.FolderChipButton
import com.nevoit.cresto.ui.components.glasense.GlasenseChipGroup
import com.nevoit.cresto.ui.components.glasense.GlasenseMenuItem
import com.nevoit.cresto.ui.components.glasense.GlasensePageHeader
import com.nevoit.cresto.ui.components.glasense.SwipeableListState
import com.nevoit.cresto.ui.components.glasense.isScrolledPast
import com.nevoit.cresto.ui.components.glasense.rememberSwipeableListState
import com.nevoit.cresto.ui.components.packed.PageContent
import com.nevoit.glasense.component.paddingItem
import com.nevoit.glasense.core.component.HGap
import com.nevoit.glasense.core.component.VGap
import com.nevoit.glasense.theme.tokens.Springs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Composable
fun BoxScope.HomeScreen(
    showMenu: (anchorBounds: Rect, items: List<GlasenseMenuItem>) -> Unit,
    viewModel: TodoViewModel,
    onOpenGroupBottomSheet: () -> Unit,
    isImmersive: Boolean,
    onImmersiveChange: (Boolean) -> Unit
) {
    val settingsViewModel: SettingsViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val completionSoundPlayer = remember(context) { HomeCompletionSoundPlayer(context) }
    var lastCompletionSoundAtMs by remember { mutableLongStateOf(0L) }
    val completionSoundThrottleMs = 200L

    DisposableEffect(completionSoundPlayer) {
        onDispose { completionSoundPlayer.release() }
    }

    val homeTodos by viewModel.homeTodos.collectAsStateWithLifecycle()
    val homeGroups by viewModel.homeGroups.collectAsStateWithLifecycle()
    val selectedHomeGroupFilter by viewModel.homeGroupFilter.collectAsStateWithLifecycle()
    val selectedItemIds by viewModel.selectedItemIds.collectAsState()
    val isSelectionModeActive by viewModel.isSelectionModeActive.collectAsState()
    val isSearchBoxOpen by viewModel.isSearchBoxOpen.collectAsState()

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // val colorMode = if (MaterialTheme.colorScheme.background == Color.White) true else false

    val lazyListState = rememberLazyListState()

    val configuration = LocalConfiguration.current
    val useTwoColumns = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val hapticFeedback = LocalHapticFeedback.current
    val pullThresholdPx = with(LocalDensity.current) { IMMERSIVE_PULL_THRESHOLD.toPx() }
    val latestOnImmersiveChange by rememberUpdatedState(onImmersiveChange)

    // 首页下拉进入沉浸模式。这里直接读手指位移（正数表示向下），不再走 nestedScroll 的位移符号判断：
    // 那套符号约定在不同 Compose 版本里容易搞反，一旦反了手势就完全不触发。
    // 只在列表已经位于顶部时累积位移，位移达到阈值即进入，并给一次轻震动作为反馈。
    val immersivePullModifier = Modifier.pointerInput(
        isImmersive,
        isSearchBoxOpen,
        isSelectionModeActive
    ) {
        if (isImmersive || isSearchBoxOpen || isSelectionModeActive) return@pointerInput
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var pulledPx = 0f
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break
                val dy = change.position.y - change.previousPosition.y
                val isAtTop = lazyListState.firstVisibleItemIndex == 0 &&
                        lazyListState.firstVisibleItemScrollOffset == 0
                if (!isAtTop || dy <= 0f) {
                    pulledPx = 0f
                } else {
                    pulledPx += dy
                    if (pulledPx >= pullThresholdPx) {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                        latestOnImmersiveChange(true)
                        break
                    }
                }
            }
        }
    }

    val swipeListState = rememberSwipeableListState()
    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (lazyListState.isScrollInProgress) {
            swipeListState.close()
        }
    }
    LaunchedEffect(isSelectionModeActive) {
        if (!isSelectionModeActive) {
            swipeListState.close()
        }
    }

    val isSmallTitleVisible by lazyListState.isScrolledPast(if (isSearchBoxOpen) 0.dp else statusBarHeight + 24.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val menuItemsSort = rememberSortMenuItems()

    val sortOptionOrdinal by SettingsManager.sortOptionState
    val sortOrderOrdinal by SettingsManager.sortOrderState
    val isDueTodayMarkerEnabled by settingsViewModel.isDueTodayMarker
    val isOverdueMarkerEnabled by settingsViewModel.isOverdueMarker

    val currentSortOption = remember(sortOptionOrdinal) {
        SortOption.entries.getOrElse(sortOptionOrdinal) { SortOption.DEFAULT }
    }
    val currentSortOrder = remember(sortOrderOrdinal) {
        SortOrder.entries.getOrElse(sortOrderOrdinal) { SortOrder.DESCENDING }
    }
    val allFilterTitle = stringResource(R.string.all)
    val allTodosTitle = stringResource(R.string.todos)
    val ungroupedTodosTitle = stringResource(R.string.ungrouped_todos)
    val homeGroupNames = remember(homeGroups) { homeGroups.associate { it.id to it.name } }
    val homeGroupFilters = remember(homeGroups) {
        listOf(HomeGroupFilter.All) +
                homeGroups.map { HomeGroupFilter.Group(it.id) } +
                HomeGroupFilter.Ungrouped
    }
    val chipListState = rememberLazyListState()
    val currentHomeGroupFilters by rememberUpdatedState(homeGroupFilters)
    LaunchedEffect(viewModel, chipListState) {
        viewModel.homeGroupSelectionEvents.collect { selectedFilter ->
            val targetIndex = currentHomeGroupFilters.indexOf(selectedFilter)
            if (targetIndex >= 0) {
                chipListState.animateScrollToItem(targetIndex)
            }
        }
    }
    val newTodoGroupId = (selectedHomeGroupFilter as? HomeGroupFilter.Group)?.id

    val (pinnedTodos, incompleteTodos, completeTodos) = remember(
        homeTodos,
        currentSortOption,
        currentSortOrder
    ) {
        buildHomeTodoSections(
            list = homeTodos,
            option = currentSortOption,
            order = currentSortOrder
        )
    }
    val incompleteTodoCount = homeTodos.count { !it.todoItem.isCompleted }
    var pinnedVisible by rememberSaveable { mutableStateOf(true) }
    var completedVisible by rememberSaveable { mutableStateOf(true) }

    var showConfetti by remember { mutableStateOf(false) }
    var confettiHideJob by remember { mutableStateOf<Job?>(null) }
    var confettiTriggerPosition by remember { mutableStateOf(Offset.Unspecified) }
    val onTodoCompleted: (Offset) -> Unit = { position ->
        val now = SystemClock.elapsedRealtime()
        if (now - lastCompletionSoundAtMs >= completionSoundThrottleMs) {
            completionSoundPlayer.playIfAllowed()
            lastCompletionSoundAtMs = now
        }

        if (incompleteTodoCount == 1) {
            confettiTriggerPosition = position
            confettiHideJob?.cancel()
            scope.launch {
                if (showConfetti) {
                    showConfetti = false
                    withFrameNanos { }
                }
                showConfetti = true
                confettiHideJob = launch {
                    delay(2.seconds)
                    showConfetti = false
                }
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val deleteId = result.data?.getIntExtra("extra_delete_id", -1) ?: -1
            if (deleteId != -1) {
                scope.launch {
                    delay(300.milliseconds)
                    viewModel.deleteById(deleteId)
                }
            }
        }
    }
    if (isSelectionModeActive) {
        BackHandler { viewModel.clearSelections() }
    }
    BackHandler(enabled = isImmersive) {
        onImmersiveChange(false)
    }
    LaunchedEffect(isSearchBoxOpen) {
        if (isSearchBoxOpen) {
            swipeListState.close()
        }
    }

    val backdroundColor = AppColors.pageBackground

    val backdrop = rememberLayerBackdrop {
        drawRect(
            color = backdroundColor,
            size = Size(this.size.width * 3, this.size.height * 3),
            topLeft = Offset(-this.size.width, -this.size.height)
        )
        drawContent()
    }
    // 沉浸模式下横屏改为两列排布。顶栏（搜索/排序/加号）、标题行与分组选择行由下面的
    // 条件渲染隐藏，底栏由 MainScreen 统一淡出；待办列表本身仍用同一批卡片组件渲染。
    val twoColumns = isImmersive && useTwoColumns
    val rowContext = remember(
        viewModel,
        isDueTodayMarkerEnabled,
        isOverdueMarkerEnabled,
        selectedItemIds,
        isSelectionModeActive,
        interactionSource,
        swipeListState,
        onTodoCompleted
    ) {
        HomeTodoRowContext(
            viewModel = viewModel,
            isDueTodayMarkerEnabled = isDueTodayMarkerEnabled,
            isOverdueMarkerEnabled = isOverdueMarkerEnabled,
            selectedItemIds = selectedItemIds,
            isSelectionModeActive = isSelectionModeActive,
            overlayInteractionSource = interactionSource,
            swipeListState = swipeListState,
            onOpenDetail = { todoId ->
                val intent = Intent(context, DetailActivity::class.java).apply {
                    putExtra(EXTRA_TODO_ID, todoId)
                }
                launcher.launch(intent)
            },
            onCompleted = onTodoCompleted
        )
    }


    PageContent(
        state = lazyListState,
        modifier = Modifier
            .layerBackdrop(backdrop)
            .then(immersivePullModifier),
        tabPadding = !isImmersive,
        horizontalPadding = false
    ) {
        if (isImmersive) {
            // 沉浸模式：隐藏“待办事项”标题行与分组选择行，只保留状态栏安全距离。
            item(key = "immersive_top_padding") {
                Box(
                    modifier = Modifier
                        .statusBarsPadding()
                        .height(12.dp)
                )
            }
        } else {
            if (isSearchBoxOpen) {
                item(key = "top_padding") {
                    Box(
                        modifier = Modifier
                            .animateItem(placementSpec = Springs.crisp())
                            .statusBarsPadding()
                            .height(48.dp + 12.dp + 48.dp + 12.dp)
                    )
                }
            } else {
                item(key = "title") {
                    GlasensePageHeader(
                        modifier = Modifier
                            .animateItem(placementSpec = Springs.crisp())
                            .padding(horizontal = 12.dp),
                        title = allTodosTitle
                    )
                }
            }

            item(key = "chips") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(placementSpec = Springs.crisp()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HGap(12.dp)
                    FolderChipButton(onClick = onOpenGroupBottomSheet)
                    HGap(8.dp)
                    GlasenseChipGroup(
                        state = chipListState,
                        items = homeGroupFilters,
                        selectedItem = selectedHomeGroupFilter,
                        itemLabel = { filter ->
                            when (filter) {
                                HomeGroupFilter.All -> allFilterTitle
                                HomeGroupFilter.Ungrouped -> ungroupedTodosTitle
                                is HomeGroupFilter.Group -> homeGroupNames[filter.id] ?: allFilterTitle
                            }
                        },
                        onItemSelected = viewModel::updateHomeGroupFilter,
                        contentPadding = PaddingValues(end = 12.dp)
                    )
                }
                VGap()
            }
        }

        if (pinnedTodos.isNotEmpty()) {
            item(key = "pinned_title") {
                TodoListSectionHead(
                    title = stringResource(R.string.pinned),
                    isExpanded = pinnedVisible
                ) {
                    pinnedVisible = !pinnedVisible
                }
            }
            if (pinnedVisible) {
                homeTodoRows(
                    todos = pinnedTodos,
                    context = rowContext,
                    twoColumns = twoColumns,
                    gapAfterLast = incompleteTodos.isNotEmpty() || completeTodos.isNotEmpty()
                )
            }
        }

        if (pinnedTodos.isNotEmpty() && incompleteTodos.isNotEmpty()) {
            item(key = "incomplete_title") {
                TodoListSectionHead(
                    title = stringResource(R.string.incomplete),
                    showExpandIcon = false
                )
            }
        }

        homeTodoRows(
            todos = incompleteTodos,
            context = rowContext,
            twoColumns = twoColumns,
            gapAfterLast = completeTodos.isNotEmpty()
        )

        if (completeTodos.isNotEmpty()) {
            item(key = "small_title") {
                TodoListSectionHead(
                    title = stringResource(R.string.completed),
                    isExpanded = completedVisible
                ) {
                    completedVisible = !completedVisible
                }
            }
            if (completedVisible) {
                homeTodoRows(
                    todos = completeTodos,
                    context = rowContext,
                    twoColumns = twoColumns,
                    gapAfterLast = false
                )
            }
        }
        paddingItem(lazyListState)
    }
    CompleteConfettiOverlay(visible = showConfetti, position = confettiTriggerPosition)
    // 沉浸模式下顶栏（搜索框、排序菜单、加号）整体隐藏。
    if (!isImmersive) {
        HomeTopAppBar(
            menuController = showMenu,
            menuItems = menuItemsSort,
            title = allTodosTitle,
            isTitleVisible = isSmallTitleVisible,
            backdrop = backdrop,
            viewModel = viewModel,
            newTodoGroupId = newTodoGroupId
        )
    }
}

@Composable
private fun LazyItemScope.HomeTodoListItemRow(
    modifier: Modifier,
    item: TodoItemWithSubTodos,
    isDueTodayMarkerEnabled: Boolean,
    isOverdueMarkerEnabled: Boolean,
    isSelected: Boolean,
    isSelectionModeActive: Boolean,
    overlayInteractionSource: MutableInteractionSource,
    swipeListState: SwipeableListState,
    onEnterSelection: () -> Unit,
    onToggleSelection: () -> Unit,
    onOpenDetail: () -> Unit,
    onCompleted: (Offset) -> Unit,
    onCheckedChange: (Boolean) -> Unit,
    onTogglePinned: () -> Unit,
    onDelete: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isChecked by remember(item.todoItem.id) {
        mutableStateOf(item.todoItem.isCompleted)
    }
    var checkboxTapPosition by remember(item.todoItem.id) {
        mutableStateOf(Offset.Unspecified)
    }
    var pendingUpdateJob by remember(item.todoItem.id) {
        mutableStateOf<Job?>(null)
    }

    LaunchedEffect(item.todoItem.isCompleted) {
        if (pendingUpdateJob == null) {
            isChecked = item.todoItem.isCompleted
        }
    }

    val displayItem = remember(item, isChecked) {
        if (item.todoItem.isCompleted == isChecked) item
        else item.copy(todoItem = item.todoItem.copy(isCompleted = isChecked))
    }

    TodoListItemRow(
        modifier = modifier,
        item = displayItem,
        isDueTodayMarkerEnabled = isDueTodayMarkerEnabled,
        isOverdueMarkerEnabled = isOverdueMarkerEnabled,
        isSelected = isSelected,
        isSelectionModeActive = isSelectionModeActive,
        overlayInteractionSource = overlayInteractionSource,
        swipeListState = swipeListState,
        onEnterSelection = onEnterSelection,
        onToggleSelection = onToggleSelection,
        onOpenDetail = onOpenDetail,
        onCheckboxTapPosition = { checkboxTapPosition = it },
        onCheckedChange = { checked ->
            if (item.todoItem.isCompleted) {
                onCheckedChange(checked)
            } else {
                if (!isChecked && checked) {
                    onCompleted(checkboxTapPosition)
                }

                pendingUpdateJob?.cancel()
                isChecked = checked
                val updateJob = scope.launch {
                    delay(300.milliseconds)
                    onCheckedChange(checked)
                }
                pendingUpdateJob = updateJob
                updateJob.invokeOnCompletion {
                    if (pendingUpdateJob === updateJob) {
                        pendingUpdateJob = null
                    }
                }
            }
        },
        onTogglePinned = onTogglePinned,
        onDelete = onDelete
    )
}

/** 首页下拉进入沉浸模式所需的拖动位移阈值。 */
private val IMMERSIVE_PULL_THRESHOLD = 72.dp

/** 首页待办行渲染所需的共享上下文，避免单列与横屏双列两种排布重复传递参数。 */
private class HomeTodoRowContext(
    val viewModel: TodoViewModel,
    val isDueTodayMarkerEnabled: Boolean,
    val isOverdueMarkerEnabled: Boolean,
    val selectedItemIds: Set<Int>,
    val isSelectionModeActive: Boolean,
    val overlayInteractionSource: MutableInteractionSource,
    val swipeListState: SwipeableListState,
    val onOpenDetail: (Int) -> Unit,
    val onCompleted: (Offset) -> Unit
)

@Composable
private fun LazyItemScope.HomeTodoRow(
    context: HomeTodoRowContext,
    item: TodoItemWithSubTodos,
    modifier: Modifier = Modifier
) {
    HomeTodoListItemRow(
        modifier = modifier,
        item = item,
        isDueTodayMarkerEnabled = context.isDueTodayMarkerEnabled,
        isOverdueMarkerEnabled = context.isOverdueMarkerEnabled,
        isSelected = item.todoItem.id in context.selectedItemIds,
        isSelectionModeActive = context.isSelectionModeActive,
        overlayInteractionSource = context.overlayInteractionSource,
        swipeListState = context.swipeListState,
        onEnterSelection = { context.viewModel.enterSelectionMode(item.todoItem.id) },
        onToggleSelection = { context.viewModel.toggleSelection(item.todoItem.id) },
        onOpenDetail = { context.onOpenDetail(item.todoItem.id) },
        onCompleted = context.onCompleted,
        onCheckedChange = { isChecked ->
            context.viewModel.update(item.todoItem.copy(isCompleted = isChecked))
        },
        onTogglePinned = {
            context.viewModel.updatePinned(item.todoItem.id, !item.todoItem.isPinned)
        },
        onDelete = { context.viewModel.delete(item.todoItem) }
    )
}

/**
 * 发射一组待办。twoColumns 为 true 时按每行两个排布（横屏沉浸模式），
 * 用的仍是单列时同一个行组件，所以外观与交互保持一致。
 */
private fun LazyListScope.homeTodoRows(
    todos: List<TodoItemWithSubTodos>,
    context: HomeTodoRowContext,
    twoColumns: Boolean,
    gapAfterLast: Boolean
) {
    if (!twoColumns) {
        itemsIndexed(
            items = todos,
            key = { _, item -> item.todoItem.id }
        ) { index, item ->
            HomeTodoRow(
                context = context,
                item = item,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            if (gapAfterLast || index != todos.lastIndex) {
                VGap()
            }
        }
        return
    }

    val rows = todos.chunked(2)
    itemsIndexed(
        items = rows,
        key = { _, row -> row.first().todoItem.id }
    ) { index, row ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp)
        ) {
            row.forEach { todo ->
                HomeTodoRow(
                    context = context,
                    item = todo,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp)
                )
            }
            if (row.size == 1) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
        if (gapAfterLast || index != rows.lastIndex) {
            VGap()
        }
    }
}
