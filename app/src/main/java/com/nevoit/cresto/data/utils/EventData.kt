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
    val date: String
)
