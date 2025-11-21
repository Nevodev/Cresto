package com.nevoit.cresto.data

import androidx.room.Embedded
import androidx.room.Relation

data class TodoItemWithSubTodos(
    @Embedded
    val todoItem: TodoItem,
    @Relation(
        parentColumn = "id",
        entityColumn = "parentId"
    )
    val subTodos: List<SubTodoItem>
)
