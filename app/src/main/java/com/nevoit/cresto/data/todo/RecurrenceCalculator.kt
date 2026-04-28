package com.nevoit.cresto.data.todo

import java.time.DayOfWeek
import java.time.LocalDate

object RecurrenceCalculator {
    private const val MAX_SEARCH_DAYS = 366 * 20

    fun nextDate(
        rrule: String,
        after: LocalDate,
        startDate: LocalDate,
        endDate: LocalDate? = null
    ): LocalDate? {
        val rule = ParsedRRule.parse(rrule) ?: return null
        val effectiveUntil = listOfNotNull(rule.until, endDate).minOrNull()
        val firstCandidate = after.plusDays(1)

        return when (rule.frequency) {
            Frequency.DAILY -> nextDaily(firstCandidate, startDate, rule.interval, effectiveUntil)
            Frequency.WEEKLY -> nextWeekly(firstCandidate, startDate, rule.interval, rule.byDays, effectiveUntil)
            Frequency.MONTHLY -> nextMonthly(firstCandidate, startDate, rule.interval, effectiveUntil)
            Frequency.YEARLY -> nextYearly(firstCandidate, startDate, rule.interval, effectiveUntil)
        }
    }

    private fun nextDaily(
        firstCandidate: LocalDate,
        startDate: LocalDate,
        interval: Long,
        until: LocalDate?
    ): LocalDate? {
        val daysSinceStart = (firstCandidate.toEpochDay() - startDate.toEpochDay()).coerceAtLeast(0)
        val remainder = daysSinceStart % interval
        val daysToAdd = if (remainder == 0L) 0 else interval - remainder
        return firstCandidate.plusDays(daysToAdd).takeIfAllowed(until)
    }

    private fun nextWeekly(
        firstCandidate: LocalDate,
        startDate: LocalDate,
        interval: Long,
        byDays: Set<DayOfWeek>,
        until: LocalDate?
    ): LocalDate? {
        val allowedDays = byDays.ifEmpty { setOf(startDate.dayOfWeek) }
        var candidate = firstCandidate
        repeat(MAX_SEARCH_DAYS) {
            if (candidate.takeIfAllowed(until) == null) return null
            val weeksSinceStart = java.time.temporal.ChronoUnit.WEEKS.between(
                startDate.with(DayOfWeek.MONDAY),
                candidate.with(DayOfWeek.MONDAY)
            )
            if (weeksSinceStart >= 0 && weeksSinceStart % interval == 0L && candidate.dayOfWeek in allowedDays) {
                return candidate
            }
            candidate = candidate.plusDays(1)
        }
        return null
    }

    private fun nextMonthly(
        firstCandidate: LocalDate,
        startDate: LocalDate,
        interval: Long,
        until: LocalDate?
    ): LocalDate? {
        var candidate = firstCandidate
        repeat(MAX_SEARCH_DAYS) {
            if (candidate.takeIfAllowed(until) == null) return null
            val monthsSinceStart = java.time.temporal.ChronoUnit.MONTHS.between(
                startDate.withDayOfMonth(1),
                candidate.withDayOfMonth(1)
            )
            if (monthsSinceStart >= 0 && monthsSinceStart % interval == 0L && candidate.dayOfMonth == startDate.dayOfMonth) {
                return candidate
            }
            candidate = candidate.plusDays(1)
        }
        return null
    }

    private fun nextYearly(
        firstCandidate: LocalDate,
        startDate: LocalDate,
        interval: Long,
        until: LocalDate?
    ): LocalDate? {
        var candidate = firstCandidate
        repeat(MAX_SEARCH_DAYS) {
            if (candidate.takeIfAllowed(until) == null) return null
            val yearsSinceStart = candidate.year - startDate.year
            if (yearsSinceStart >= 0 && yearsSinceStart % interval == 0L &&
                candidate.month == startDate.month && candidate.dayOfMonth == startDate.dayOfMonth
            ) {
                return candidate
            }
            candidate = candidate.plusDays(1)
        }
        return null
    }

    private fun LocalDate.takeIfAllowed(until: LocalDate?): LocalDate? {
        return if (until == null || !isAfter(until)) this else null
    }

    private data class ParsedRRule(
        val frequency: Frequency,
        val interval: Long,
        val until: LocalDate?,
        val byDays: Set<DayOfWeek>
    ) {
        companion object {
            fun parse(rrule: String): ParsedRRule? {
                val parts = rrule.split(';')
                    .mapNotNull { part ->
                        val keyValue = part.split('=', limit = 2)
                        if (keyValue.size == 2) keyValue[0].uppercase() to keyValue[1].uppercase() else null
                    }
                    .toMap()

                val frequency = when (parts["FREQ"]) {
                    "DAILY" -> Frequency.DAILY
                    "WEEKLY" -> Frequency.WEEKLY
                    "MONTHLY" -> Frequency.MONTHLY
                    "YEARLY" -> Frequency.YEARLY
                    else -> return null
                }
                val interval = parts["INTERVAL"]?.toLongOrNull()?.takeIf { it > 0 } ?: 1L
                val until = parts["UNTIL"]?.let(::parseUntil)
                val byDays = parts["BYDAY"]
                    ?.split(',')
                    ?.mapNotNull(::parseDayOfWeek)
                    ?.toSet()
                    .orEmpty()

                return ParsedRRule(frequency, interval, until, byDays)
            }

            private fun parseUntil(value: String): LocalDate? {
                return runCatching {
                    val datePart = value.substringBefore('T')
                    LocalDate.parse(
                        if (datePart.length == 8) {
                            "${datePart.substring(0, 4)}-${datePart.substring(4, 6)}-${datePart.substring(6, 8)}"
                        } else {
                            datePart
                        }
                    )
                }.getOrNull()
            }

            private fun parseDayOfWeek(value: String): DayOfWeek? = when (value) {
                "MO" -> DayOfWeek.MONDAY
                "TU" -> DayOfWeek.TUESDAY
                "WE" -> DayOfWeek.WEDNESDAY
                "TH" -> DayOfWeek.THURSDAY
                "FR" -> DayOfWeek.FRIDAY
                "SA" -> DayOfWeek.SATURDAY
                "SU" -> DayOfWeek.SUNDAY
                else -> null
            }
        }
    }

    private enum class Frequency {
        DAILY,
        WEEKLY,
        MONTHLY,
        YEARLY
    }
}
