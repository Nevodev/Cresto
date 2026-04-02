package com.nevoit.cresto.feature.home

import com.nevoit.cresto.data.todo.TodoItemWithSubTodos
import com.nevoit.cresto.feature.settings.util.SortOption
import com.nevoit.cresto.feature.settings.util.SortOrder
import java.text.Collator

enum class TodoListType {
    INCOMPLETED,
    COMPLETED
}

fun sortTodos(
    list: List<TodoItemWithSubTodos>,
    option: SortOption,
    order: SortOrder,
    type: TodoListType
): List<TodoItemWithSubTodos> {
    val currentSortOption =
        SortOption.entries.getOrElse(option.ordinal) { SortOption.DEFAULT }
    val currentSortOrder =
        SortOrder.entries.getOrElse(order.ordinal) { SortOrder.DESCENDING }

    val collator = Collator.getInstance()
    val comparator = Comparator<TodoItemWithSubTodos> { a, b ->
        val itemA = a.todoItem
        val itemB = b.todoItem

        val baseDateA = when (type) {
            TodoListType.INCOMPLETED -> itemA.creationDateTime
            TodoListType.COMPLETED -> itemA.completedDateTime
        }
        val baseDateB = when (type) {
            TodoListType.INCOMPLETED -> itemB.creationDateTime
            TodoListType.COMPLETED -> itemB.completedDateTime
        }

        val compareByBaseDateTime = when {
            baseDateA == null && baseDateB == null -> 0
            baseDateA == null -> 1
            baseDateB == null -> -1
            currentSortOrder == SortOrder.ASCENDING -> baseDateA.compareTo(baseDateB)
            else -> baseDateB.compareTo(baseDateA)
        }

        val compareResult = when (currentSortOption) {
            SortOption.DEFAULT -> {
                compareByBaseDateTime
            }

            SortOption.DUE_DATE -> {
                val dateA = itemA.dueDate
                val dateB = itemB.dueDate

                if (dateA == null && dateB == null) {
                    compareByBaseDateTime
                } else if (dateA == null) {
                    1
                } else if (dateB == null) {
                    -1
                } else {
                    val dueDateCompare = if (currentSortOrder == SortOrder.ASCENDING) {
                        dateA.compareTo(dateB)
                    } else {
                        dateB.compareTo(dateA)
                    }
                    if (dueDateCompare != 0) dueDateCompare else compareByBaseDateTime
                }
            }

            SortOption.FLAG -> {
                val flagA = itemA.flag
                val flagB = itemB.flag
                val isAEmpty = flagA == 0
                val isBEmpty = flagB == 0

                if (isAEmpty && isBEmpty) {
                    compareByBaseDateTime
                } else if (isAEmpty) {
                    1
                } else if (isBEmpty) {
                    -1
                } else {
                    val flagCompare = if (currentSortOrder == SortOrder.ASCENDING) {
                        flagA.compareTo(flagB)
                    } else {
                        flagB.compareTo(flagA)
                    }
                    if (flagCompare != 0) flagCompare else compareByBaseDateTime
                }
            }

            SortOption.TITLE -> {
                val titleCompare = if (currentSortOrder == SortOrder.ASCENDING) {
                    collator.compare(itemA.title, itemB.title)
                } else {
                    collator.compare(itemB.title, itemA.title)
                }
                if (titleCompare != 0) titleCompare else compareByBaseDateTime
            }
        }

        if (compareResult != 0) {
            compareResult
        } else {
            if (currentSortOrder == SortOrder.ASCENDING) {
                itemA.id.compareTo(itemB.id)
            } else {
                itemB.id.compareTo(itemA.id)
            }
        }
    }
    return list.sortedWith(comparator)
}