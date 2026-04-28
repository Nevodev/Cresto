package com.nevoit.cresto.data.todo.backup

import kotlinx.serialization.Serializable

@Serializable
data class TodoBackupFile(
    val schemaVersion: Int = 2,
    val exportedAt: String,     // ISO_LOCAL_DATE_TIME
    val todos: List<TodoBackupDto>,
    val subTodos: List<SubTodoBackupDto>,
    val recurringRules: List<RecurringTodoRuleBackupDto> = emptyList()
)

@Serializable
data class TodoBackupDto(
    val id: Int,
    val title: String,
    val dueDate: String?,       // ISO_LOCAL_DATE
    val creationDateTime: String,   // ISO_LOCAL_DATE_TIME
    val isCompleted: Boolean,
    val flag: Int,
    val completedDateTime: String?,  // ISO_LOCAL_DATE_TIME
    val recurringRuleId: Int? = null
)

@Serializable
data class SubTodoBackupDto(
    val id: Int,
    val parentId: Int,
    val description: String,
    val isCompleted: Boolean
)

@Serializable
data class RecurringTodoRuleBackupDto(
    val id: Int,
    val title: String,
    val rrule: String,
    val startDate: String,      // ISO_LOCAL_DATE
    val endDate: String?,       // ISO_LOCAL_DATE
    val nextDueDate: String?,   // ISO_LOCAL_DATE
    val flag: Int,
    val isActive: Boolean,
    val creationDateTime: String,   // ISO_LOCAL_DATE_TIME
    val updatedDateTime: String     // ISO_LOCAL_DATE_TIME
)
