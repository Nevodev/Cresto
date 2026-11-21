package com.nevoit.cresto.feature.home

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyItemScope
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.nevoit.cresto.R
import com.nevoit.cresto.data.todo.HomeGroupFilter
import com.nevoit.cresto.data.todo.TodoItemWithSubTodos
import com.nevoit.cresto.data.todo.TodoViewModel
import com.nevoit.cresto.navigation.AppDestination
import com.nevoit.cresto.navigation.LocalAppNavigation
import com.nevoit.cresto.navigation.NavigationCaller
import com.nevoit.cresto.feature.settings.util.SettingsViewModel
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
    onOpenGroupBottomSheet: () -> Unit
) {
    val settingsViewModel: SettingsViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val navigation = LocalAppNavigation.current
    val completionSoundPlayer = remember(context) { HomeCompletionSoundPlayer(context) }
    var lastCompletionSoundAtMs by remember { mutableLongStateOf(0L) }
    val completionSoundThrottleMs = 200L

    DisposableEffect(completionSoundPlayer) {
        onDispose { completionSoundPlayer.release() }
    }

    val homeSections by viewModel.homeSections.collectAsStateWithLifecycle()
    val homeGroups by viewModel.homeGroups.collectAsStateWithLifecycle()
    val selectedHomeGroupFilter by viewModel.homeGroupFilter.collectAsStateWithLifecycle()
    val selectedItemIds by viewModel.selectedItemIds.collectAsState()
    val isSelectionModeActive by viewModel.isSelectionModeActive.collectAsState()
    val isSearchBoxOpen by viewModel.isSearchBoxOpen.collectAsState()

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // val colorMode = if (MaterialTheme.colorScheme.background == Color.White) true else false

    val lazyListState = rememberLazyListState()

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

    val isDueTodayMarkerEnabled by settingsViewModel.isDueTodayMarker
    val isOverdueMarkerEnabled by settingsViewModel.isOverdueMarker

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

    val (pinnedTodos, incompleteTodos, completeTodos) = homeSections
    val incompleteTodoCount = incompleteTodos.size + pinnedTodos.count { !it.todoItem.isCompleted }
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

    if (isSelectionModeActive) {
        BackHandler { viewModel.clearSelections() }
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

    PageContent(
        state = lazyListState,
        modifier = Modifier.layerBackdrop(backdrop),
        tabPadding = true,
        horizontalPadding = false
    ) {
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
                itemsIndexed(
                    items = pinnedTodos,
                    key = { _, item -> item.todoItem.id }
                ) { index, item ->
                    HomeTodoListItemRow(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        item = item,
                        isDueTodayMarkerEnabled = isDueTodayMarkerEnabled,
                        isOverdueMarkerEnabled = isOverdueMarkerEnabled,
                        isSelected = item.todoItem.id in selectedItemIds,
                        isSelectionModeActive = isSelectionModeActive,
                        overlayInteractionSource = interactionSource,
                        swipeListState = swipeListState,
                        onEnterSelection = { viewModel.enterSelectionMode(item.todoItem.id) },
                        onToggleSelection = { viewModel.toggleSelection(item.todoItem.id) },
                        onOpenDetail = { caller ->
                            navigation.navigate(AppDestination.Detail(item.todoItem.id), caller)
                        },
                        onCompleted = onTodoCompleted,
                        onCheckedChange = { isChecked ->
                            viewModel.update(item.todoItem.copy(isCompleted = isChecked))
                        },
                        onTogglePinned = {
                            viewModel.updatePinned(item.todoItem.id, !item.todoItem.isPinned)
                        },
                        onDelete = { viewModel.delete(item.todoItem) }
                    )

                    if (index != pinnedTodos.lastIndex ||
                        incompleteTodos.isNotEmpty() || completeTodos.isNotEmpty()
                    ) {
                        VGap()
                    }
                }
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

        itemsIndexed(
            items = incompleteTodos,
            key = { _, item -> item.todoItem.id },
        ) { index, item ->
            HomeTodoListItemRow(
                modifier = Modifier.padding(horizontal = 12.dp),
                item = item,
                isDueTodayMarkerEnabled = isDueTodayMarkerEnabled,
                isOverdueMarkerEnabled = isOverdueMarkerEnabled,
                isSelected = item.todoItem.id in selectedItemIds,
                isSelectionModeActive = isSelectionModeActive,
                overlayInteractionSource = interactionSource,
                swipeListState = swipeListState,
                onEnterSelection = { viewModel.enterSelectionMode(item.todoItem.id) },
                onToggleSelection = { viewModel.toggleSelection(item.todoItem.id) },
                onOpenDetail = { caller ->
                    navigation.navigate(AppDestination.Detail(item.todoItem.id), caller)
                },
                onCompleted = onTodoCompleted,
                onCheckedChange = { isChecked ->
                    viewModel.update(item.todoItem.copy(isCompleted = isChecked))
                },
                onTogglePinned = {
                    viewModel.updatePinned(item.todoItem.id, !item.todoItem.isPinned)
                },
                onDelete = { viewModel.delete(item.todoItem) }
            )

            if (completeTodos.isNotEmpty() || index != incompleteTodos.lastIndex) {
                VGap()
            }
        }

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
                itemsIndexed(
                    items = completeTodos,
                    key = { _, item -> item.todoItem.id },
                ) { index, item ->
                    HomeTodoListItemRow(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        item = item,
                        isDueTodayMarkerEnabled = isDueTodayMarkerEnabled,
                        isOverdueMarkerEnabled = isOverdueMarkerEnabled,
                        isSelected = item.todoItem.id in selectedItemIds,
                        isSelectionModeActive = isSelectionModeActive,
                        overlayInteractionSource = interactionSource,
                        swipeListState = swipeListState,
                        onEnterSelection = { viewModel.enterSelectionMode(item.todoItem.id) },
                        onToggleSelection = { viewModel.toggleSelection(item.todoItem.id) },
                        onOpenDetail = { caller ->
                            navigation.navigate(AppDestination.Detail(item.todoItem.id), caller)
                        },
                        onCompleted = onTodoCompleted,
                        onCheckedChange = { isChecked ->
                            viewModel.update(item.todoItem.copy(isCompleted = isChecked))
                        },
                        onTogglePinned = {
                            viewModel.updatePinned(item.todoItem.id, !item.todoItem.isPinned)
                        },
                        onDelete = { viewModel.delete(item.todoItem) }
                    )

                    if (index != completeTodos.lastIndex) {
                        VGap()
                    }
                }
            }
        }
        paddingItem(lazyListState)
    }
    CompleteConfettiOverlay(visible = showConfetti, position = confettiTriggerPosition)
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
    onOpenDetail: (NavigationCaller) -> Unit,
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
