package com.nevoit.cresto.feature.home

import com.nevoit.cresto.data.todo.TodoItem
import com.nevoit.cresto.data.todo.TodoItemWithSubTodos
import com.nevoit.cresto.feature.settings.util.SortOption
import com.nevoit.cresto.feature.settings.util.SortOrder
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class TodoSorterTest {

    @Test
    fun flagFallback_usesCreationTime_forIncompleteTodos() {
        val earlyCreated = todo(
            id = 1,
            title = "same-flag",
            flag = 3,
            creationDateTime = dt("2026-03-01T08:00:00"),
            completedDateTime = dt("2026-03-03T08:00:00")
        )
        val laterCreated = todo(
            id = 2,
            title = "same-flag",
            flag = 3,
            creationDateTime = dt("2026-03-02T08:00:00"),
            completedDateTime = dt("2026-03-01T08:00:00")
        )

        val sorted = sortTodos(
            list = listOf(earlyCreated, laterCreated),
            option = SortOption.FLAG,
            order = SortOrder.ASCENDING,
            type = TodoListType.INCOMPLETED
        )

        assertEquals(listOf(1, 2), sorted.map { it.todoItem.id })
    }

    @Test
    fun flagFallback_usesCompletedTime_forCompletedTodos() {
        val earlyCreatedLateCompleted = todo(
            id = 1,
            title = "same-flag",
            flag = 0,
            creationDateTime = dt("2026-03-01T08:00:00"),
            completedDateTime = dt("2026-03-03T08:00:00")
        )
        val laterCreatedEarlyCompleted = todo(
            id = 2,
            title = "same-flag",
            flag = 0,
            creationDateTime = dt("2026-03-02T08:00:00"),
            completedDateTime = dt("2026-03-01T08:00:00")
        )

        val sorted = sortTodos(
            list = listOf(earlyCreatedLateCompleted, laterCreatedEarlyCompleted),
            option = SortOption.FLAG,
            order = SortOrder.ASCENDING,
            type = TodoListType.COMPLETED
        )

        assertEquals(listOf(2, 1), sorted.map { it.todoItem.id })
    }

    @Test
    fun titleFallback_usesCreationTime_forIncompleteTodos() {
        val first = todo(
            id = 10,
            title = "same-title",
            creationDateTime = dt("2026-01-10T10:00:00"),
            completedDateTime = dt("2026-01-12T10:00:00")
        )
        val second = todo(
            id = 11,
            title = "same-title",
            creationDateTime = dt("2026-01-11T10:00:00"),
            completedDateTime = dt("2026-01-09T10:00:00")
        )

        val sorted = sortTodos(
            list = listOf(second, first),
            option = SortOption.TITLE,
            order = SortOrder.ASCENDING,
            type = TodoListType.INCOMPLETED
        )

        assertEquals(listOf(10, 11), sorted.map { it.todoItem.id })
    }

    @Test
    fun titleFallback_usesCompletedTime_forCompletedTodos() {
        val first = todo(
            id = 20,
            title = "same-title",
            creationDateTime = dt("2026-01-10T10:00:00"),
            completedDateTime = dt("2026-01-12T10:00:00")
        )
        val second = todo(
            id = 21,
            title = "same-title",
            creationDateTime = dt("2026-01-11T10:00:00"),
            completedDateTime = dt("2026-01-09T10:00:00")
        )

        val sorted = sortTodos(
            list = listOf(first, second),
            option = SortOption.TITLE,
            order = SortOrder.ASCENDING,
            type = TodoListType.COMPLETED
        )

        assertEquals(listOf(21, 20), sorted.map { it.todoItem.id })
    }

    private fun todo(
        id: Int,
        title: String,
        flag: Int = 0,
        creationDateTime: LocalDateTime,
        completedDateTime: LocalDateTime? = null
    ): TodoItemWithSubTodos {
        return TodoItemWithSubTodos(
            todoItem = TodoItem(
                id = id,
                title = title,
                flag = flag,
                creationDateTime = creationDateTime,
                completedDateTime = completedDateTime
            ),
            subTodos = emptyList()
        )
    }

    private fun dt(value: String): LocalDateTime = LocalDateTime.parse(value)
}

