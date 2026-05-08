package com.nevoit.cresto.data.todo

import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import androidx.room.Relation

@Immutable
data class TodoItemWithSubTodos(
    @Embedded
    val todoItem: TodoItem,
    @Relation(
        parentColumn = "id",
        entityColumn = "parentId"
    )
    val subTodos: List<SubTodoItem>
)
