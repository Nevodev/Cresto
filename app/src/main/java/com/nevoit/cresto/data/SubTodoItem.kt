package com.nevoit.cresto.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "sub_todo_items",
    foreignKeys = [
        ForeignKey(
            entity = TodoItem::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SubTodoItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val parentId: Int,
    val description: String,
    val isCompleted: Boolean = false
)
