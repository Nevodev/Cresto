package com.nevoit.cresto.data.todo

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

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
    val monthDays: Set<Int> = emptySet(),
    val months: Set<Int> = emptySet(),
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
    val monthDays: String? = null,
    val months: String? = null,
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
        RepeatFrequency.Yearly -> nextYearlyOccurrence(after, step)
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
    if (monthDay == null && monthDays == null) return after.plusMonths(step)

    val selectedDays = selectedMonthDays()
    var candidateMonth = after.withDayOfMonth(1)
    repeat(240) {
        val monthDistance = java.time.temporal.ChronoUnit.MONTHS.between(
            anchorDate.withDayOfMonth(1),
            candidateMonth
        )
        if (monthDistance >= 0 && monthDistance % step == 0L) {
            selectedDays
                .map { day ->
                    candidateMonth.withDayOfMonth(
                        day.coerceAtMost(candidateMonth.lengthOfMonth())
                    )
                }
                .distinct()
                .forEach { date ->
                    if (date.isAfter(after)) return date
                }
        }
        candidateMonth = candidateMonth.plusMonths(1)
    }
    return after.plusMonths(step)
}

private fun RepeatRule.nextYearlyOccurrence(after: LocalDate, step: Long): LocalDate {
    if (months == null && monthDays == null && monthDay == null) return after.plusYears(step)

    val selectedMonths = selectedMonths()
    val selectedDays = selectedMonthDays()
    var candidateYear = after.year
    repeat(240) {
        val yearDistance = candidateYear.toLong() - anchorDate.year.toLong()
        if (yearDistance >= 0 && yearDistance % step == 0L) {
            selectedMonths.forEach { month ->
                val candidateMonth = YearMonth.of(candidateYear, month)
                selectedDays
                    .map { day ->
                        LocalDate.of(
                            candidateYear,
                            month,
                            day.coerceAtMost(candidateMonth.lengthOfMonth())
                        )
                    }
                    .distinct()
                    .forEach { date ->
                        if (date.isAfter(after)) return date
                    }
            }
        }
        candidateYear += 1
    }
    return after.plusYears(step)
}

private fun RepeatRule.selectedMonthDays(): List<Int> {
    val explicitDays = monthDays.toIntSet(1..31)
    val legacyDay = monthDay?.takeIf { it in 1..31 }?.let { setOf(it) }.orEmpty()
    return explicitDays
        .ifEmpty { legacyDay }
        .ifEmpty { setOf(anchorDate.dayOfMonth) }
        .sorted()
}

private fun RepeatRule.selectedMonths(): List<Int> {
    return months.toIntSet(1..12)
        .ifEmpty { setOf(anchorDate.monthValue) }
        .sorted()
}

private fun String?.toIntSet(range: IntRange): Set<Int> {
    return this
        ?.split(',')
        ?.mapNotNull { value ->
            value.trim().toIntOrNull()?.takeIf { it in range }
        }
        ?.toSet()
        .orEmpty()
}
