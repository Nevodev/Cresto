package com.nevoit.cresto.data.utils

import kotlinx.serialization.Serializable

@Serializable
data class EventResponse(
    val quantity: Int,
    val items: List<EventItem>
)

@Serializable
data class EventItem(
    val title: String,
    val date: String,
    val startTime: String? = null,
    val endTime: String? = null,
    val reminderMode: String? = null,
    val reminderOffsetMinutes: Int? = null,
    val reminderDayOffset: Int? = null,
    val reminderTime: String? = null,
    val subTasks: List<String> = emptyList()
)
