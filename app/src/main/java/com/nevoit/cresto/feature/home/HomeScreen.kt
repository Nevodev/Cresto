package com.nevoit.cresto.feature.home

import android.app.Activity
import android.content.Intent
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.nevoit.cresto.data.todo.EXTRA_TODO_ID
import com.nevoit.cresto.data.todo.HomeGroupFilter
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
    viewModel: TodoViewModel
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
        listOf(HomeGroupFilter.All, HomeGroupFilter.Ungrouped) +
                homeGroups.map { HomeGroupFilter.Group(it.id) }
    }
    val newTodoGroupId = (selectedHomeGroupFilter as? HomeGroupFilter.Group)?.id

    val (incompleteTodos, completeTodos) = remember(
        homeTodos,
        currentSortOption,
        currentSortOrder
    ) {
        val incomplete = sortTodos(
            list = homeTodos.filter { !it.todoItem.isCompleted },
            option = currentSortOption,
            order = currentSortOrder,
            type = TodoListType.INCOMPLETED
        )
        val complete = sortTodos(
            list = homeTodos.filter { it.todoItem.isCompleted },
            option = currentSortOption,
            order = currentSortOrder,
            type = TodoListType.COMPLETED
        )
        incomplete to complete
    }
    var completedVisible by rememberSaveable { mutableStateOf(true) }

    var showConfetti by remember { mutableStateOf(false) }
    var confettiHideJob by remember { mutableStateOf<Job?>(null) }
    var latestCheckboxTapPosition by remember { mutableStateOf(Offset.Unspecified) }
    var confettiTriggerPosition by remember { mutableStateOf(Offset.Unspecified) }
    val pendingUpdateJobs = remember { mutableStateMapOf<Int, Job>() }

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
        tabPadding = true
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
                    modifier = Modifier.animateItem(placementSpec = Springs.crisp()),
                    title = allTodosTitle
                )
            }
        }

        item(key = "chips") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FolderChipButton { }
                HGap(8.dp)
                GlasenseChipGroup(
                    items = homeGroupFilters,
                    selectedItem = selectedHomeGroupFilter,
                    itemLabel = { filter ->
                        when (filter) {
                            HomeGroupFilter.All -> allFilterTitle
                            HomeGroupFilter.Ungrouped -> ungroupedTodosTitle
                            is HomeGroupFilter.Group -> homeGroupNames[filter.id] ?: allFilterTitle
                        }
                    },
                    onItemSelected = viewModel::updateHomeGroupFilter
                )
            }
            VGap()
        }

        itemsIndexed(
            items = incompleteTodos,
            key = { _, item -> item.todoItem.id },
        ) { index, item ->
            var isChecked by remember(item.todoItem.id) { mutableStateOf(item.todoItem.isCompleted) }

            // Keep local state in sync with source of truth when there's no pending optimistic update.
            LaunchedEffect(item.todoItem.isCompleted) {
                if (pendingUpdateJobs[item.todoItem.id] == null) {
                    isChecked = item.todoItem.isCompleted
                }
            }

            val displayItem = remember(item, isChecked) {
                if (item.todoItem.isCompleted == isChecked) item
                else item.copy(todoItem = item.todoItem.copy(isCompleted = isChecked))
            }


            TodoListItemRow(
                item = displayItem,
                isDueTodayMarkerEnabled = isDueTodayMarkerEnabled,
                isOverdueMarkerEnabled = isOverdueMarkerEnabled,
                isSelected = item.todoItem.id in selectedItemIds,
                isSelectionModeActive = isSelectionModeActive,
                overlayInteractionSource = interactionSource,
                swipeListState = swipeListState,
                onEnterSelection = { viewModel.enterSelectionMode(item.todoItem.id) },
                onToggleSelection = { viewModel.toggleSelection(item.todoItem.id) },
                onOpenDetail = {
                    val intent = Intent(context, DetailActivity::class.java).apply {
                        putExtra(EXTRA_TODO_ID, item.todoItem.id)
                    }
                    launcher.launch(intent)
                },
                onCheckboxTapPosition = { position ->
                    latestCheckboxTapPosition = position
                },
                onCheckedChange = { checked ->
                    val todoId = item.todoItem.id
                    val wasChecked = isChecked

                    if (!wasChecked && checked) {
                        val now = SystemClock.elapsedRealtime()
                        if (now - lastCompletionSoundAtMs >= completionSoundThrottleMs) {
                            completionSoundPlayer.playIfAllowed()
                            lastCompletionSoundAtMs = now
                        }
                    }

                    if (checked && incompleteTodos.size == 1) {
                        confettiTriggerPosition = latestCheckboxTapPosition
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

                    pendingUpdateJobs[todoId]?.cancel()

                    isChecked = checked
                    val updateJob = scope.launch {
                        delay(300.milliseconds)
                        viewModel.update(item.todoItem.copy(isCompleted = checked))
                    }
                    pendingUpdateJobs[todoId] = updateJob
                    updateJob.invokeOnCompletion {
                        if (pendingUpdateJobs[todoId] === updateJob) {
                            pendingUpdateJobs.remove(todoId)
                        }
                    }
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
                    TodoListItemRow(
                        item = item,
                        isDueTodayMarkerEnabled = isDueTodayMarkerEnabled,
                        isOverdueMarkerEnabled = isOverdueMarkerEnabled,
                        isSelected = item.todoItem.id in selectedItemIds,
                        isSelectionModeActive = isSelectionModeActive,
                        overlayInteractionSource = interactionSource,
                        swipeListState = swipeListState,
                        onEnterSelection = { viewModel.enterSelectionMode(item.todoItem.id) },
                        onToggleSelection = { viewModel.toggleSelection(item.todoItem.id) },
                        onOpenDetail = {
                            val intent = Intent(context, DetailActivity::class.java).apply {
                                putExtra(EXTRA_TODO_ID, item.todoItem.id)
                            }
                            launcher.launch(intent)
                        },
                        onCheckedChange = { isChecked ->
                            viewModel.update(item.todoItem.copy(isCompleted = isChecked))
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
