package com.nevoit.cresto.util

import android.content.Context
import com.nevoit.cresto.R
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

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
            R.plurals.hours_ago,
            duration.toHours().toInt(),
            duration.toHours()
        )

        duration.toDays() < 2 -> context.getString(R.string.yesterday)
        else -> dateTime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
    }
}

fun formatRelativeRealTime(dateTime: LocalDateTime, context: Context): String {
    val now = LocalDateTime.now()
    val today = LocalDate.now()
    val targetDate = dateTime.toLocalDate()

    val durationSeconds = ChronoUnit.SECONDS.between(dateTime, now)

    return when {
        durationSeconds < 60 -> context.getString(R.string.just_now)

        durationSeconds < 300 -> {
            val minutes = durationSeconds / 60
            context.resources.getQuantityString(
                R.plurals.minutes_ago,
                minutes.toInt(),
                minutes
            )
        }

        targetDate.isEqual(today) -> {
            dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
        }

        targetDate.isEqual(today.minusDays(1)) -> {
            "${context.getString(R.string.yesterday)} ${
                dateTime.format(
                    DateTimeFormatter.ofPattern(
                        "HH:mm"
                    )
                )
            }"
        }

        // 5. 其他日期：显示 "02-06 14:30"
        else -> {
            dateTime.format(DateTimeFormatter.ofPattern("MM/dd HH:mm"))
        }
    }
}
