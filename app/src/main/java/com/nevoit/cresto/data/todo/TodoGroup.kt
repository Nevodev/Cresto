package com.nevoit.cresto.data.todo

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "todo_groups",
    indices = [
        Index(value = ["name"], unique = true),
        Index(value = ["sortOrder"])
    ]
)
data class TodoGroup(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val color: Int = 0,
    val sortOrder: Int = 0
)
