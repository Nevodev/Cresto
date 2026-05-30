package com.nevoit.cresto.data.todo

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class RepeatFrequency {
    Daily,
    Weekly,
    Monthly,
    Yearly
}

@Entity(
    tableName = "repeat_rules",
    indices = [Index(value = ["seriesId"])]
)
data class RepeatRule(
    @PrimaryKey
    val id: String,
    val seriesId: String,
    val frequency: RepeatFrequency,
    val interval: Int = 1,
    val weekdays: String? = null,
    val monthDay: Int? = null,
    val endDate: LocalDate? = null,
    val maxOccurrences: Int? = null,
    val anchorDate: LocalDate,
    val createNextOnCompletion: Boolean = true
)

fun RepeatRule.nextOccurrence(after: LocalDate): LocalDate? {
    if (!createNextOnCompletion) return null

    val step = interval.coerceAtLeast(1).toLong()
    val nextDate = when (frequency) {
        RepeatFrequency.Daily -> after.plusDays(step)
        RepeatFrequency.Weekly -> after.plusWeeks(step)
        RepeatFrequency.Monthly -> after.plusMonths(step)
        RepeatFrequency.Yearly -> after.plusYears(step)
    }

    if (endDate != null && nextDate.isAfter(endDate)) return null
    return nextDate
}
