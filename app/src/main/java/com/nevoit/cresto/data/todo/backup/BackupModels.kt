package com.nevoit.cresto.data.todo.backup

import kotlinx.serialization.Serializable

@Serializable
data class TodoBackupFile(
    val schemaVersion: Int = 1,
    val exportedAt: String,     // ISO_LOCAL_DATE_TIME
    val todos: List<TodoBackupDto>,
    val subTodos: List<SubTodoBackupDto>
)

@Serializable
data class TodoBackupDto(
    val id: Int,
    val title: String,
    val dueDate: String?,       // ISO_LOCAL_DATE
    val creationDateTime: String,   // ISO_LOCAL_DATE_TIME
    val isCompleted: Boolean,
    val flag: Int,
    val completedDateTime: String?  // ISO_LOCAL_DATE_TIME
)

@Serializable
data class SubTodoBackupDto(
    val id: Int,
    val parentId: Int,
    val description: String,
    val isCompleted: Boolean
)
