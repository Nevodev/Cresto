package com.nevoit.cresto.util

import android.content.Context
import com.nevoit.cresto.R
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun formatRelativeTime(dateTime: LocalDateTime, context: Context): String {
    val now = LocalDateTime.now()
    val duration = Duration.between(dateTime, now)

    return when {
        duration.toMinutes() < 1 -> context.getString(R.string.just_now)
        duration.toHours() < 1 -> context.resources.getQuantityString(
            R.plurals.minutes_ago,
            duration.toMinutes().toInt(),
            duration.toMinutes()
        )

        duration.toDays() < 1 -> context.resources.getQuantityString(
            R.plurals.minutes_ago,
            duration.toHours().toInt(),
            duration.toHours()
        )

        duration.toDays() < 2 -> context.getString(R.string.yesterday)
        else -> dateTime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
    }
}
