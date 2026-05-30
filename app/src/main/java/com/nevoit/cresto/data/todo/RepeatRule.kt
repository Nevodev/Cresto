package com.nevoit.cresto.data.todo

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalDate

enum class RepeatFrequency {
    Daily,
    Weekly,
    Monthly,
    Yearly
}

data class RepeatRuleConfig(
    val frequency: RepeatFrequency,
    val interval: Int = 1,
    val weekdays: Set<DayOfWeek> = emptySet(),
    val monthDay: Int? = null,
    val endDate: LocalDate? = null,
    val maxOccurrences: Int? = null
)

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

fun RepeatRule.nextOccurrence(after: LocalDate, occurrenceCount: Int): LocalDate? {
    if (!createNextOnCompletion) return null
    if (maxOccurrences != null && occurrenceCount >= maxOccurrences) return null

    val step = interval.coerceAtLeast(1).toLong()
    val nextDate = when (frequency) {
        RepeatFrequency.Daily -> after.plusDays(step)
        RepeatFrequency.Weekly -> nextWeeklyOccurrence(after, step)
        RepeatFrequency.Monthly -> nextMonthlyOccurrence(after, step)
        RepeatFrequency.Yearly -> after.plusYears(step)
    }

    if (endDate != null && nextDate.isAfter(endDate)) return null
    return nextDate
}

private fun RepeatRule.nextWeeklyOccurrence(after: LocalDate, step: Long): LocalDate {
    val selectedDays = weekdays
        ?.split(',')
        ?.mapNotNull { runCatching { DayOfWeek.valueOf(it) }.getOrNull() }
        ?.toSet()
        .orEmpty()
    if (selectedDays.isEmpty()) return after.plusWeeks(step)

    var candidate = after.plusDays(1)
    val maxDaysToCheck = step * 7L + 7L
    repeat(maxDaysToCheck.toInt()) {
        val weekDistance = java.time.temporal.ChronoUnit.WEEKS.between(anchorDate, candidate)
        if (weekDistance >= 0 && weekDistance % step == 0L && candidate.dayOfWeek in selectedDays) {
            return candidate
        }
        candidate = candidate.plusDays(1)
    }
    return after.plusWeeks(step)
}

private fun RepeatRule.nextMonthlyOccurrence(after: LocalDate, step: Long): LocalDate {
    val day = monthDay ?: return after.plusMonths(step)
    var candidateMonth = after.withDayOfMonth(1)
    repeat(240) {
        val monthDistance = java.time.temporal.ChronoUnit.MONTHS.between(
            anchorDate.withDayOfMonth(1),
            candidateMonth
        )
        if (monthDistance >= 0 && monthDistance % step == 0L) {
            val date = candidateMonth.withDayOfMonth(day.coerceAtMost(candidateMonth.lengthOfMonth()))
            if (date.isAfter(after)) return date
        }
        candidateMonth = candidateMonth.plusMonths(1)
    }
    return after.plusMonths(step)
}
