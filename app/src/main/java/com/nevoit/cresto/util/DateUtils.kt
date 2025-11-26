package com.nevoit.cresto.util

import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun formatRelativeTime(dateTime: LocalDateTime): String {
    val now = LocalDateTime.now()
    val duration = Duration.between(dateTime, now)

    return when {
        duration.toMinutes() < 1 -> "just now"
        duration.toHours() < 1 -> "${duration.toMinutes()} minute${
            if (duration.toMinutes().toInt() == 1) "" else "s"
        } ago"

        duration.toDays() < 1 -> "${duration.toHours()} hour${
            if (duration.toHours().toInt() == 1) "" else "s"
        } ago"

        duration.toDays() < 2 -> "yesterday"
        else -> dateTime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
    }
}
