package com.nevoit.cresto.data.todo.backup

import kotlinx.serialization.Serializable

@Serializable
data class TodoBackupFile(
    val schemaVersion: Int = 4,
    val exportedAt: String,     // ISO_LOCAL_DATE_TIME
    val todos: List<TodoBackupDto>,
    val subTodos: List<SubTodoBackupDto>,
    val repeatRules: List<RepeatRuleBackupDto> = emptyList(),
    val groups: List<TodoGroupBackupDto> = emptyList()
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
    val startTime: String?,     // ISO_LOCAL_TIME
    val endTime: String?,       // ISO_LOCAL_TIME
    val reminderMode: String? = null,
    val reminderOffsetMinutes: Int? = null,
    val reminderDayOffset: Int? = null,
    val reminderTime: String? = null, // ISO_LOCAL_TIME
    val reminderPersistent: Boolean = false,
    val reminderStrong: Boolean = false,
    val repeatRuleId: String? = null,
    val seriesId: String? = null,
    val occurrenceDate: String? = null, // ISO_LOCAL_DATE
    val generatedFromTodoId: Int? = null,
    val occurrenceEditedAt: String? = null, // ISO_LOCAL_DATE_TIME
    val groupId: Int? = null
)

@Serializable
data class SubTodoBackupDto(
    val id: Int,
    val parentId: Int,
    val description: String,
    val isCompleted: Boolean
)

@Serializable
data class RepeatRuleBackupDto(
    val id: String,
    val seriesId: String,
    val frequency: String,
    val interval: Int = 1,
    val weekdays: String? = null,
    val monthDay: Int? = null,
    val monthDays: String? = null,
    val months: String? = null,
    val endDate: String? = null, // ISO_LOCAL_DATE
    val maxOccurrences: Int? = null,
    val anchorDate: String, // ISO_LOCAL_DATE
    val createNextOnCompletion: Boolean = true
)

@Serializable
data class TodoGroupBackupDto(
    val id: Int,
    val name: String,
    val color: Int = 0,
    val sortOrder: Int = 0
)
