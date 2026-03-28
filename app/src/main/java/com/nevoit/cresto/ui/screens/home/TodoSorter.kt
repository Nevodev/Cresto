package com.nevoit.cresto.ui.screens.home

import com.nevoit.cresto.data.todo.TodoItemWithSubTodos
import com.nevoit.cresto.ui.screens.settings.util.SortOption
import com.nevoit.cresto.ui.screens.settings.util.SortOrder
import java.text.Collator

fun sortTodos(
    list: List<TodoItemWithSubTodos>,
    option: SortOption,
    order: SortOrder
): List<TodoItemWithSubTodos> {
    val currentSortOption =
        SortOption.entries.getOrElse(option.ordinal) { SortOption.DEFAULT }
    val currentSortOrder =
        SortOrder.entries.getOrElse(order.ordinal) { SortOrder.DESCENDING }

    val collator = Collator.getInstance()
    val comparator = Comparator<TodoItemWithSubTodos> { a, b ->
        val itemA = a.todoItem
        val itemB = b.todoItem

        val compareByCreationDate = if (currentSortOrder == SortOrder.ASCENDING) {
            itemA.creationDateTime.compareTo(itemB.creationDateTime)
        } else {
            itemB.creationDateTime.compareTo(itemA.creationDateTime)
        }

        val compareResult = when (currentSortOption) {
            SortOption.DEFAULT -> {
                compareByCreationDate
            }

            SortOption.DUE_DATE -> {
                val dateA = itemA.dueDate
                val dateB = itemB.dueDate

                if (dateA == null && dateB == null) {
                    compareByCreationDate
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
                    if (dueDateCompare != 0) dueDateCompare else compareByCreationDate
                }
            }

            SortOption.FLAG -> {
                val flagA = itemA.flag
                val flagB = itemB.flag
                val isAEmpty = flagA == 0
                val isBEmpty = flagB == 0

                if (isAEmpty && isBEmpty) {
                    0
                } else if (isAEmpty) {
                    1
                } else if (isBEmpty) {
                    -1
                } else {
                    if (currentSortOrder == SortOrder.ASCENDING) {
                        flagA.compareTo(flagB)
                    } else {
                        flagB.compareTo(flagA)
                    }
                }
            }

            SortOption.TITLE -> {
                if (currentSortOrder == SortOrder.ASCENDING) {
                    collator.compare(itemA.title, itemB.title)
                } else {
                    collator.compare(itemB.title, itemA.title)
                }
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