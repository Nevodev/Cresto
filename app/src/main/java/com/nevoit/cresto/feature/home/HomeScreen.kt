package com.nevoit.cresto.feature.home

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nevoit.cresto.R
import com.nevoit.cresto.data.todo.EXTRA_TODO_ID
import com.nevoit.cresto.data.todo.TodoViewModel
import com.nevoit.cresto.feature.detail.DetailActivity
import com.nevoit.cresto.feature.settings.util.SettingsManager
import com.nevoit.cresto.feature.settings.util.SettingsViewModel
import com.nevoit.cresto.feature.settings.util.SortOption
import com.nevoit.cresto.feature.settings.util.SortOrder
import com.nevoit.cresto.ui.components.glasense.GlasenseMenuItem
import com.nevoit.cresto.ui.components.glasense.GlasensePageHeader
import com.nevoit.cresto.ui.components.glasense.extend.overscrollSpacer
import com.nevoit.cresto.ui.components.glasense.isScrolledPast
import com.nevoit.cresto.ui.components.glasense.rememberSwipeableListState
import com.nevoit.cresto.ui.components.packed.PageContent
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds


@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeApi::class)
@Composable
fun BoxScope.HomeScreen(
    showMenu: (anchorPosition: Offset, items: List<GlasenseMenuItem>) -> Unit,
    viewModel: TodoViewModel
) {
    val settingsViewModel: SettingsViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val todoList by viewModel.allTodos.collectAsStateWithLifecycle()
    val selectedItemIds by viewModel.selectedItemIds.collectAsState()
    val isSelectionModeActive by viewModel.isSelectionModeActive.collectAsState()

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val hazeState = rememberHazeState()

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

    val isSmallTitleVisible by lazyListState.isScrolledPast(statusBarHeight + 24.dp)
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

    val (rawIncompleteTodos, rawCompleteTodos) = remember(todoList) {
        todoList.partition { !it.todoItem.isCompleted }
    }
    val incompleteTodos = remember(rawIncompleteTodos, currentSortOption, currentSortOrder) {
        sortTodos(
            list = rawIncompleteTodos,
            option = currentSortOption,
            order = currentSortOrder,
            type = TodoListType.INCOMPLETED
        )
    }
    val completeTodos = remember(rawCompleteTodos, currentSortOption, currentSortOrder) {
        sortTodos(
            list = rawCompleteTodos,
            option = currentSortOption,
            order = currentSortOrder,
            type = TodoListType.COMPLETED
        )
    }
    var completedVisible by remember { mutableStateOf(true) }
    var showConfetti by remember { mutableStateOf(false) }
    var confettiHideJob by remember { mutableStateOf<Job?>(null) }
    var latestCheckboxTapPosition by remember { mutableStateOf(Offset.Unspecified) }
    var confettiTriggerPosition by remember { mutableStateOf(Offset.Unspecified) }
    val pendingUpdateJobs = remember { mutableStateMapOf<Int, Job>() }

    val incompleteCount = incompleteTodos.size

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val deleteId = result.data?.getIntExtra("extra_delete_id", -1) ?: -1
            if (deleteId != -1) {
                scope.launch {
                    delay(300)
                    viewModel.deleteById(deleteId)
                }
            }
        }
    }
    if (isSelectionModeActive) {
        BackHandler { viewModel.clearSelections() }
    }

    PageContent(
        state = lazyListState,
        modifier = Modifier
            .hazeSource(hazeState, 0f),
        tabPadding = true
    ) {
        item {
            GlasensePageHeader(
                title = stringResource(R.string.all_todos)
            )
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
                    if (checked && incompleteCount == 1) {
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
                        delay(300)
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
                Spacer(modifier = Modifier.height(12.dp))
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
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
        overscrollSpacer(lazyListState)
    }
    CompleteConfettiOverlay(visible = showConfetti, position = confettiTriggerPosition)
    HomeTopAppBar(
        menuController = showMenu,
        menuItems = menuItemsSort,
        isTitleVisible = isSmallTitleVisible,
        hazeState = hazeState,
        viewModel = viewModel,
    )
}