package com.nevoit.cresto.data.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nevoit.cresto.data.statistics.DailyStat
import com.nevoit.cresto.data.statistics.TodoStat
import com.nevoit.cresto.data.todo.calendar.CalendarSyncSummary
import com.nevoit.cresto.data.todo.reminder.TodoAlarmScheduler
import com.nevoit.cresto.data.utils.EventItem
import com.nevoit.cresto.feature.settings.util.SortOption
import com.nevoit.cresto.feature.settings.util.SortOrder
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class BottomSheetUiState(
    val isVisible: Boolean = false,
    val initialDate: LocalDate? = null
)

data class BackupUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val exportedJson: String? = null,
    val importResult: ImportResult? = null,
    val errorMessage: String? = null
)

data class ImportPreviewUiState(
    val isChecking: Boolean = false,
    val result: TodoRepository.ImportPreviewResult? = null,
    val errorMessage: String? = null
)

data class InsightsUiState(
    val isReady: Boolean = false,
    val todayTotal: Int = 0,
    val todayCompleted: Int = 0,
    val weekDueTotal: Int = 0,
    val weekDueCompleted: Int = 0,
    val pendingTotal: Int = 0,
    val overdueTotal: Int = 0,
    val stalePendingTotal: Int = 0,
    val oldestPendingAgeDays: Long? = null,
    val weeklyCompletedTrend: List<DailyStat> = emptyList()
) {
    val todayRemaining: Int
        get() = (todayTotal - todayCompleted).coerceAtLeast(0)

    val todayProgress: Float
        get() = if (todayTotal > 0) todayCompleted.toFloat() / todayTotal else 0f

    val weekDueProgress: Float
        get() = if (weekDueTotal > 0) weekDueCompleted.toFloat() / weekDueTotal else 0f

    val weekCompletedTotal: Int
        get() = weeklyCompletedTrend.sumOf { it.count }

    val hasAnyData: Boolean
        get() = todayTotal > 0 ||
                weekDueTotal > 0 ||
                pendingTotal > 0 ||
                overdueTotal > 0 ||
                stalePendingTotal > 0 ||
                weekCompletedTotal > 0
}

private data class InsightCoreCounts(
    val todayTotal: Int,
    val todayCompleted: Int,
    val pendingTotal: Int,
    val overdueTotal: Int,
    val stalePendingTotal: Int
)

private data class InsightWeekStats(
    val dueTotal: Int,
    val dueCompleted: Int,
    val completedTrend: List<DailyStat>
)

private const val HOME_TODO_PAGE_SIZE = 30

data class HomeTodoListState(
    val todos: List<TodoItemWithSubTodos> = emptyList(),
    val totalCount: Int = 0,
    val loadedLimit: Int = HOME_TODO_PAGE_SIZE
) {
    val hasMore: Boolean
        get() = todos.size < totalCount
}

private data class HomeTodoQuery(
    val searchQuery: String,
    val sortOption: SortOption,
    val sortOrder: SortOrder,
    val limit: Int
)

class TodoViewModel(
    private val repository: TodoRepository,
    private val alarmScheduler: TodoAlarmScheduler
) : ViewModel() {
    val allTodos: StateFlow<List<TodoItemWithSubTodos>> = repository.allTodos.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun getTodoWithSubTodos(id: Int): Flow<TodoItemWithSubTodos?> = repository.getTodoById(id)

    fun getRepeatRule(id: String?): Flow<RepeatRule?> {
        return id?.let(repository::getRepeatRuleById) ?: flowOf(null)
    }

    private val _calendarSyncEvents =
        MutableSharedFlow<CalendarSyncSummary>(extraBufferCapacity = 1)
    val calendarSyncEvents = _calendarSyncEvents.asSharedFlow()

    /*select*/

    private val _selectedItemIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedItemIds: StateFlow<Set<Int>> = _selectedItemIds.asStateFlow()

    private val _isSelectionModeActive = MutableStateFlow(false)
    val isSelectionModeActive: StateFlow<Boolean> = _isSelectionModeActive.asStateFlow()

    fun enterSelectionMode(initialItemId: Int) {
        _isSelectionModeActive.value = true
        _selectedItemIds.update { it + initialItemId }
    }

    fun toggleSelection(itemId: Int) {
        _selectedItemIds.update { currentIds ->
            val newIds = if (itemId in currentIds) currentIds - itemId else currentIds + itemId
            if (newIds.isEmpty()) {
                _isSelectionModeActive.value = false
            }
            newIds
        }
    }

    fun toggleSelectAllItems() {
        val visibleIds = getVisibleTodoIds()
        if (visibleIds.isEmpty()) return

        _selectedItemIds.update { currentIds ->
            val isVisibleAllSelected = visibleIds.all(currentIds::contains)
            if (isVisibleAllSelected) {
                currentIds - visibleIds
            } else {
                currentIds + visibleIds
            }
        }

        _isSelectionModeActive.value = _selectedItemIds.value.isNotEmpty()
    }

    fun toggleSelectAllItems(visibleIds: Collection<Int>) {
        if (visibleIds.isEmpty()) return

        _selectedItemIds.update { currentIds ->
            val isVisibleAllSelected = visibleIds.all(currentIds::contains)
            if (isVisibleAllSelected) {
                currentIds - visibleIds.toSet()
            } else {
                currentIds + visibleIds.toSet()
            }
        }

        _isSelectionModeActive.value = _selectedItemIds.value.isNotEmpty()
    }

    private fun getVisibleTodoIds(): Set<Int> {
        return homeTodos.value.todos.map { it.todoItem.id }.toSet()
    }


    fun clearSelections() {
        _selectedItemIds.value = emptySet()
        _isSelectionModeActive.value = false
    }

    fun deleteSelectedItems() = viewModelScope.launch {
        val selectedIds = _selectedItemIds.value.toList()
        if (selectedIds.isEmpty()) return@launch

        repository.deleteByIds(selectedIds)
        alarmScheduler.cancelAll(selectedIds)

        clearSelections()
    }

    fun syncSelectedItemsToCalendar() = viewModelScope.launch {
        val selectedIds = _selectedItemIds.value.toList()
        if (selectedIds.isEmpty()) return@launch

        val summary = repository.syncTodosToCalendar(selectedIds)
        _calendarSyncEvents.emit(summary)
        clearSelections()
    }

    fun syncTodoToCalendar(todoId: Int) = viewModelScope.launch {
        val summary = CalendarSyncSummary.from(listOf(repository.syncTodoToCalendar(todoId)))
        _calendarSyncEvents.emit(summary)
    }

    fun completeSelectedItems() {
        val selectedIds = _selectedItemIds.value.toList()
        if (selectedIds.isEmpty()) return

        clearSelections()

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val completedCount = repository.getCompletedCountByIds(selectedIds)
                val allSelectedCompleted = completedCount == selectedIds.size
                val targetCompletedState = !allSelectedCompleted

                val result = repository.updateCompletedStatusByIds(
                    ids = selectedIds,
                    isCompleted = targetCompletedState,
                    completedDateTime = if (targetCompletedState) LocalDateTime.now() else null
                )

                if (targetCompletedState) {
                    alarmScheduler.cancelAll(result.updatedTodos.reminderTodoIds())
                    result.insertedTodos.forEach(alarmScheduler::schedule)
                } else {
                    result.updatedTodos
                        .filter { it.hasReminderConfig() }
                        .forEach(alarmScheduler::schedule)
                    alarmScheduler.cancelAll(result.deletedTodos.reminderTodoIds())
                }
            }
        }
    }

    fun flagSelectedItems(flag: Int) = viewModelScope.launch {
        val selectedIds = _selectedItemIds.value
        if (selectedIds.isEmpty()) return@launch

        repository.updateFlagByIds(selectedIds.toList(), flag)

        clearSelections()
    }

    fun duplicateSelectedItems() = viewModelScope.launch {
        val selectedIds = _selectedItemIds.value.toList()
        if (selectedIds.isEmpty()) return@launch

        repository.duplicateByIds(selectedIds).forEach { todo ->
            if (!todo.isCompleted) {
                alarmScheduler.schedule(todo)
            }
        }
        clearSelections()
    }

    fun duplicateById(todoId: Int) = viewModelScope.launch {
        repository.duplicateByIds(listOf(todoId)).forEach { todo ->
            if (!todo.isCompleted) {
                alarmScheduler.schedule(todo)
            }
        }
    }

    fun mergeSelectedItems(newTodoTitle: String) = viewModelScope.launch {
        val selectedIds = _selectedItemIds.value
        if (selectedIds.size < 2) return@launch

        val orderedSelectedIds = homeTodos.value.todos
            .map { it.todoItem.id }
            .filter(selectedIds::contains)
            .ifEmpty { selectedIds.toList() }

        if (orderedSelectedIds.isEmpty()) return@launch

        repository.mergeByIdsAsSubTodos(orderedSelectedIds, newTodoTitle)
        alarmScheduler.cancelAll(orderedSelectedIds)
        clearSelections()
    }

    val selectedItemCount: StateFlow<Int> = selectedItemIds.map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedTodos: StateFlow<List<TodoItemWithSubTodos>> = selectedItemIds
        .flatMapLatest { selectedIds ->
            if (selectedIds.isEmpty()) {
                flowOf(emptyList())
            } else {
                repository.getTodosByIds(selectedIds.toList())
                    .map { todos ->
                        val selectedOrder = selectedIds.withIndex()
                            .associate { (index, id) -> id to index }
                        todos.sortedBy { selectedOrder[it.todoItem.id] ?: Int.MAX_VALUE }
                    }
            }
        }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /*select*/
    fun insert(
        item: TodoItem,
        repeatFrequency: RepeatFrequency? = null,
        repeatRuleConfig: RepeatRuleConfig? = null
    ) = viewModelScope.launch {
        val id = repository.insert(item, repeatFrequency, repeatRuleConfig).toInt()
        alarmScheduler.schedule(repository.getTodoByIdSnapshot(id) ?: item.copy(id = id))
    }

    fun update(item: TodoItem) = viewModelScope.launch {
        alarmScheduler.cancel(item)
        val existingItem = repository.getTodoByIdSnapshot(item.id)
        val completionChanged = existingItem != null && existingItem.isCompleted != item.isCompleted

        if (completionChanged) {
            val result = repository.updateCompletedStatusByIds(
                ids = listOf(item.id),
                isCompleted = item.isCompleted,
                completedDateTime = if (item.isCompleted) LocalDateTime.now() else null
            )

            if (item.isCompleted) {
                alarmScheduler.cancel(item.id)
                result.insertedTodos.forEach(alarmScheduler::schedule)
            } else {
                result.updatedTodos.forEach(alarmScheduler::schedule)
                alarmScheduler.cancelAll(result.deletedTodos.map { it.id })
            }
            return@launch
        }

        val itemToPersist = when {
            item.isCompleted && item.completedDateTime == null -> {
                item.copy(
                    completedDateTime = LocalDateTime.now()
                )
            }

            !item.isCompleted -> {
                item.copy(
                    completedDateTime = null
                )
            }

            else -> item
        }
        repository.update(itemToPersist)

        if (!itemToPersist.isCompleted) {
            alarmScheduler.schedule(itemToPersist)
        }
    }

    fun updateRepeatRule(item: TodoItem, config: RepeatRuleConfig?) = viewModelScope.launch {
        repository.updateRepeatRuleForTodo(item, config)
    }

    fun delete(item: TodoItem) = viewModelScope.launch {
        alarmScheduler.cancel(item)
        repository.delete(item)
    }

    fun deleteById(id: Int) {
        viewModelScope.launch {
            alarmScheduler.cancel(id)
            repository.deleteById(id)
        }
    }

    // --- SubTodo Operations ---

    fun insertSubTodo(item: SubTodoItem) = viewModelScope.launch {
        repository.insertSubTodo(item)
    }

    fun updateSubTodo(item: SubTodoItem) = viewModelScope.launch {
        repository.updateSubTodo(item)
    }

    fun deleteSubTodo(item: SubTodoItem) = viewModelScope.launch {
        repository.deleteSubTodo(item)
    }

    fun insertAiGeneratedTodos(aiItems: List<EventItem>) {
        viewModelScope.launch {
            try {
                repository.insertAiGeneratedTodosWithSubTasks(aiItems).forEach { todo ->
                    if (!todo.isCompleted) {
                        alarmScheduler.schedule(todo)
                    }
                }

            } catch (e: Exception) {
                println("Error inserting AI-generated todos: ${e.message}")
            }
        }
    }

    private val _bottomSheetState = MutableStateFlow(BottomSheetUiState())
    val bottomSheetState = _bottomSheetState.asStateFlow()

    fun showBottomSheet(date: LocalDate? = null) {
        _bottomSheetState.update { it.copy(isVisible = true, initialDate = date) }
    }

    fun hideBottomSheet() {
        _bottomSheetState.update { it.copy(isVisible = false, initialDate = null) }
    }

    val statistics: StateFlow<TodoStat> = combine(
        repository.getTotalCount(),
        repository.getCompletedCount()
    ) { total, completed ->
        TodoStat(totalCount = total, completedCount = completed)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TodoStat()
    )

    val dailyStats: StateFlow<List<DailyStat>> = repository.getDailyStatistics()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val insightsToday = LocalDate.now()
    private val insightsStartDate = insightsToday.minusDays(6)
    private val insightsEndDate = insightsToday
    private val insightsEndDateTimeExclusive = insightsToday.plusDays(1).atStartOfDay()
    private val stalePendingThresholdDate = insightsToday.minusDays(7)

    private val insightCoreCounts: Flow<InsightCoreCounts> = combine(
        repository.getTodoCountByDueDate(insightsToday),
        repository.getCompletedTodoCountByDueDate(insightsToday),
        repository.getPendingTodoCount(),
        repository.getOverdueTodoCount(insightsToday),
        repository.getStalePendingTodoCount(stalePendingThresholdDate)
    ) { todayTotal, todayCompleted, pendingTotal, overdueTotal, stalePendingTotal ->
        InsightCoreCounts(
            todayTotal = todayTotal,
            todayCompleted = todayCompleted,
            pendingTotal = pendingTotal,
            overdueTotal = overdueTotal,
            stalePendingTotal = stalePendingTotal
        )
    }

    private val insightWeekStats: Flow<InsightWeekStats> = combine(
        repository.getTodoCountByDueDateRange(insightsStartDate, insightsEndDate),
        repository.getCompletedTodoCountByDueDateRange(insightsStartDate, insightsEndDate),
        repository.getCompletedStatisticsBetween(
            insightsStartDate.atStartOfDay(),
            insightsEndDateTimeExclusive
        )
    ) { dueTotal, dueCompleted, rawTrend ->
        val statsByDate = rawTrend.associateBy { it.date }
        val completedTrend = (0..6).map { dayOffset ->
            val date = insightsStartDate.plusDays(dayOffset.toLong())
            statsByDate[date] ?: DailyStat(date = date, count = 0)
        }

        InsightWeekStats(
            dueTotal = dueTotal,
            dueCompleted = dueCompleted,
            completedTrend = completedTrend
        )
    }

    val insights: StateFlow<InsightsUiState> = combine(
        insightCoreCounts,
        insightWeekStats,
        repository.getOldestPendingReferenceDate()
    ) { coreCounts, weekStats, oldestPendingReferenceDate ->
        InsightsUiState(
            isReady = true,
            todayTotal = coreCounts.todayTotal,
            todayCompleted = coreCounts.todayCompleted,
            weekDueTotal = weekStats.dueTotal,
            weekDueCompleted = weekStats.dueCompleted,
            pendingTotal = coreCounts.pendingTotal,
            overdueTotal = coreCounts.overdueTotal,
            stalePendingTotal = coreCounts.stalePendingTotal,
            oldestPendingAgeDays = oldestPendingReferenceDate?.let { referenceDate ->
                ChronoUnit.DAYS.between(
                    referenceDate,
                    insightsToday
                ).coerceAtLeast(0)
            },
            weeklyCompletedTrend = weekStats.completedTrend
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InsightsUiState(
            weeklyCompletedTrend = (0..6).map { dayOffset ->
                DailyStat(date = insightsStartDate.plusDays(dayOffset.toLong()), count = 0)
            }
        )
    )

    fun clearAllData() {
        viewModelScope.launch {
            alarmScheduler.cancelAll(repository.getReminderTodosSnapshot().map { it.id })
            repository.deleteAll()
            // clear settings
            MMKV.defaultMMKV().clearAll()
        }
    }

    private val _backupUiState = MutableStateFlow(BackupUiState())
    val backupUiState: StateFlow<BackupUiState> = _backupUiState.asStateFlow()

    fun exportBackupToJson() = viewModelScope.launch {
        _backupUiState.update {
            it.copy(
                isExporting = true,
                errorMessage = null,
                importResult = null
            )
        }

        runCatching {
            repository.exportToJson()
        }.onSuccess { json ->
            _backupUiState.update {
                it.copy(
                    isExporting = false,
                    exportedJson = json
                )
            }
        }.onFailure { e ->
            _backupUiState.update {
                it.copy(
                    isExporting = false,
                    errorMessage = e.message ?: "failed to export."
                )
            }
        }
    }

    fun importBackupFromJson(
        json: String,
        policy: DuplicatePolicy
    ) = viewModelScope.launch {
        _backupUiState.update {
            it.copy(
                isImporting = true,
                errorMessage = null,
                importResult = null
            )
        }

        runCatching {
            repository.importFromJson(json, policy).also {
                repository.getReminderTodosSnapshot().forEach(alarmScheduler::schedule)
            }
        }.onSuccess { result ->
            _backupUiState.update {
                it.copy(
                    isImporting = false,
                    importResult = result
                )
            }
        }.onFailure { e ->
            _backupUiState.update {
                it.copy(
                    isImporting = false,
                    errorMessage = e.message ?: "failed to import."
                )
            }
        }
    }

    fun clearBackupError() {
        _backupUiState.update { it.copy(errorMessage = null) }
    }

    fun clearExportedJson() {
        _backupUiState.update { it.copy(exportedJson = null) }
    }

    fun clearImportResult() {
        _backupUiState.update { it.copy(importResult = null) }
    }

    private val _importPreviewState = MutableStateFlow(ImportPreviewUiState())
    val importPreviewState: StateFlow<ImportPreviewUiState> = _importPreviewState.asStateFlow()

    fun previewImport(json: String) = viewModelScope.launch {
        _importPreviewState.update {
            it.copy(
                isChecking = true,
                errorMessage = null,
                result = null
            )
        }

        runCatching { repository.previewImportDuplicates(json) }
            .onSuccess { preview ->
                _importPreviewState.update { it.copy(isChecking = false, result = preview) }
            }
            .onFailure { e ->
                _importPreviewState.update {
                    it.copy(isChecking = false, errorMessage = e.message ?: "预检失败")
                }
            }
    }

    fun clearImportPreview() {
        _importPreviewState.value = ImportPreviewUiState()
    }

    private val _isSearchBoxOpen = MutableStateFlow(false)
    val isSearchBoxOpen: StateFlow<Boolean> = _isSearchBoxOpen.asStateFlow()

    fun openSearchBox() {
        _isSearchBoxOpen.value = true
    }

    fun closeSearchBox() {
        _isSearchBoxOpen.value = false
    }

    fun onSearchCloseIconClick() {
        if (_searchQuery.value.isNotEmpty()) {
            updateSearchQuery("")
        } else {
            closeSearchBox()
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    private val _homeTodoLimit = MutableStateFlow(HOME_TODO_PAGE_SIZE)
    private val _homeSortOption = MutableStateFlow(SortOption.DEFAULT)
    private val _homeSortOrder = MutableStateFlow(SortOrder.DESCENDING)

    @OptIn(ExperimentalCoroutinesApi::class)
    val homeTodos: StateFlow<HomeTodoListState> = combine(
        _searchQuery,
        _homeSortOption,
        _homeSortOrder,
        _homeTodoLimit
    ) { query, sortOption, sortOrder, limit ->
        HomeTodoQuery(
            searchQuery = query,
            sortOption = sortOption,
            sortOrder = sortOrder,
            limit = limit
        )
    }.flatMapLatest { request ->
        combine(
            repository.getHomeTodos(
                query = request.searchQuery,
                sortOption = request.sortOption,
                sortOrder = request.sortOrder,
                limit = request.limit
            ),
            repository.getHomeTodoCount(request.searchQuery)
        ) { todos, totalCount ->
            HomeTodoListState(
                todos = todos,
                totalCount = totalCount,
                loadedLimit = request.limit
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeTodoListState()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchedTodos: StateFlow<List<TodoItemWithSubTodos>> = _searchQuery
        .flatMapLatest { query -> repository.searchTodos(query) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun updateSearchQuery(query: String) {
        if (_searchQuery.value != query) {
            _homeTodoLimit.value = HOME_TODO_PAGE_SIZE
        }
        _searchQuery.value = query
    }

    fun updateHomeSort(sortOption: SortOption, sortOrder: SortOrder) {
        val isSortChanged = _homeSortOption.value != sortOption ||
                _homeSortOrder.value != sortOrder
        if (!isSortChanged) return

        _homeSortOption.value = sortOption
        _homeSortOrder.value = sortOrder
        _homeTodoLimit.value = HOME_TODO_PAGE_SIZE
    }

    fun loadNextHomeTodoPage() {
        if (!homeTodos.value.hasMore) return
        _homeTodoLimit.update { it + HOME_TODO_PAGE_SIZE }
    }

    private fun List<TodoItem>.reminderTodoIds(): List<Int> {
        return filter { it.hasReminderConfig() }.map { it.id }
    }

    private fun TodoItem.hasReminderConfig(): Boolean {
        return reminderMode != null
    }

    fun getTodosByDate(date: LocalDate): Flow<List<TodoItemWithSubTodos>> {
        return repository.getTodosByDate(date)
    }

    val datesWithTodo: StateFlow<Set<LocalDate>> = repository.getDatesWithTodo()
        .map { it.toSet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )
}
