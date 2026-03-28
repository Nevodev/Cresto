package com.nevoit.cresto.data.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nevoit.cresto.data.statistics.DailyStat
import com.nevoit.cresto.data.statistics.TodoStat
import com.nevoit.cresto.data.utils.EventItem
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class BottomSheetUiState(
    val isVisible: Boolean = false
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

class TodoViewModel(private val repository: TodoRepository) : ViewModel() {
    val allTodos: StateFlow<List<TodoItemWithSubTodos>> = repository.allTodos.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun getTodoWithSubTodos(id: Int): Flow<TodoItemWithSubTodos?> = repository.getTodoById(id)

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
        val allIds = allTodos.value.map { it.todoItem.id }.toSet()
        if (allIds.isEmpty()) {
            clearSelections()
            return
        }

        val isAllSelected = _selectedItemIds.value.size == allIds.size &&
                _selectedItemIds.value.containsAll(allIds)

        if (isAllSelected) {
            clearSelections()
        } else {
            _selectedItemIds.value = allIds
            _isSelectionModeActive.value = true
        }
    }


    fun clearSelections() {
        _selectedItemIds.value = emptySet()
        _isSelectionModeActive.value = false
    }

    fun deleteSelectedItems() = viewModelScope.launch {
        val selectedIds = _selectedItemIds.value.toList()
        if (selectedIds.isEmpty()) return@launch

        repository.deleteByIds(selectedIds)

        clearSelections()
    }

    fun completeSelectedItems() = viewModelScope.launch {
        val selectedIds = _selectedItemIds.value.toList()
        if (selectedIds.isEmpty()) return@launch

        val completedCount = repository.getCompletedCountByIds(selectedIds)
        val allSelectedCompleted = completedCount == selectedIds.size
        val targetCompletedState = !allSelectedCompleted

        repository.updateCompletedStatusByIds(
            ids = selectedIds,
            isCompleted = targetCompletedState,
            completedDateTime = if (targetCompletedState) LocalDateTime.now() else null
        )

        clearSelections()
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

        repository.duplicateByIds(selectedIds)
        clearSelections()
    }

    fun mergeSelectedItems(newTodoTitle: String) = viewModelScope.launch {
        val selectedIds = _selectedItemIds.value
        if (selectedIds.size < 2) return@launch

        val orderedSelectedIds = allTodos.value
            .map { it.todoItem.id }
            .filter(selectedIds::contains)

        if (orderedSelectedIds.isEmpty()) return@launch

        repository.mergeByIdsAsSubTodos(orderedSelectedIds, newTodoTitle)
        clearSelections()
    }

    val selectedItemCount: StateFlow<Int> = selectedItemIds.map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    /*select*/
    fun insert(item: TodoItem) = viewModelScope.launch {
        repository.insert(item)
    }

    fun update(item: TodoItem) = viewModelScope.launch {
        viewModelScope.launch {
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
        }

    }

    fun delete(item: TodoItem) = viewModelScope.launch {
        repository.delete(item)
    }

    fun deleteById(id: Int) {
        viewModelScope.launch {
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
                val todoItemsToInsert = aiItems.map { eventItem ->
                    TodoItem(
                        title = eventItem.title,
                        dueDate = LocalDate.parse(eventItem.date, DateTimeFormatter.ISO_LOCAL_DATE)
                    )
                }

                if (todoItemsToInsert.isNotEmpty()) {
                    repository.insertAll(todoItemsToInsert)
                }

            } catch (e: Exception) {
                println("Error inserting AI-generated todos: ${e.message}")
            }
        }
    }

    private val _bottomSheetState = MutableStateFlow(BottomSheetUiState())
    val bottomSheetState = _bottomSheetState.asStateFlow()

    fun showBottomSheet() {
        _bottomSheetState.update { it.copy(isVisible = true) }
    }

    fun hideBottomSheet() {
        _bottomSheetState.update { it.copy(isVisible = false) }
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

    fun clearAllData() {
        viewModelScope.launch {
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
            repository.importFromJson(json, policy)
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
}