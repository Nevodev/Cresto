package com.nevoit.cresto.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nevoit.cresto.data.EventItem
import com.nevoit.cresto.data.SubTodoItem
import com.nevoit.cresto.data.TodoItem
import com.nevoit.cresto.data.TodoItemWithSubTodos
import com.nevoit.cresto.repository.TodoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class BottomSheetUiState(
    val isVisible: Boolean = false
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

    fun clearSelections() {
        _selectedItemIds.value = emptySet()
        _isSelectionModeActive.value = false
    }

    fun deleteSelectedItems() {
        val selectedIds = _selectedItemIds.value
        val itemsToDelete = allTodos.value.filter { it.todoItem.id in selectedIds }

        viewModelScope.launch {
            itemsToDelete.forEach { item ->
                delete(item.todoItem)
            }
            clearSelections()
        }
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
        repository.update(item)
    }

    fun delete(item: TodoItem) = viewModelScope.launch {
        repository.delete(item)
        _revealedItemId.value = null
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

    // Swipe to delete
    private val _revealedItemId = MutableStateFlow<Int?>(null)

    val revealedItemId: StateFlow<Int?> = _revealedItemId

    fun onItemExpanded(itemId: Int) {
        if (_revealedItemId.value == itemId) return
        viewModelScope.launch {
            _revealedItemId.value = itemId
        }
    }

    fun onItemCollapsed(itemId: Int) {
        if (_revealedItemId.value != itemId) return
        viewModelScope.launch {
            _revealedItemId.value = null
        }
    }

    fun collapseRevealedItem() {
        if (_revealedItemId.value == null) return
        viewModelScope.launch {
            _revealedItemId.value = null
        }
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
}

class TodoViewModelFactory(private val repository: TodoRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TodoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TodoViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}