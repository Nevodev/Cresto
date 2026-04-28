package com.nevoit.cresto.data.todo

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(
    tableName = "recurring_todo_rules",
    indices = [Index(value = ["isActive", "nextDueDate"])]
)
data class RecurringTodoRule(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val rrule: String,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val nextDueDate: LocalDate? = startDate,
    val flag: Int = 0,
    val isActive: Boolean = true,
    val creationDateTime: LocalDateTime = LocalDateTime.now(),
    val updatedDateTime: LocalDateTime = LocalDateTime.now()
)
