package com.nevoit.cresto.data.todo

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class RepeatRuleTest {

    @Test
    fun monthlyRule_usesPersistedMonthDays() {
        val rule = repeatRule(
            frequency = RepeatFrequency.Monthly,
            anchorDate = date("2026-06-10"),
            monthDays = "15,30"
        )

        assertEquals(date("2026-06-15"), rule.nextOccurrence(date("2026-06-10"), 0))
        assertEquals(date("2026-06-30"), rule.nextOccurrence(date("2026-06-15"), 0))
        assertEquals(date("2026-07-15"), rule.nextOccurrence(date("2026-06-30"), 0))
    }

    @Test
    fun yearlyRule_usesPersistedMonths() {
        val rule = repeatRule(
            frequency = RepeatFrequency.Yearly,
            anchorDate = date("2026-06-25"),
            months = "3,9"
        )

        assertEquals(date("2026-09-25"), rule.nextOccurrence(date("2026-06-25"), 0))
        assertEquals(date("2027-03-25"), rule.nextOccurrence(date("2026-09-25"), 0))
    }

    @Test
    fun simpleYearlyRule_keepsLegacyPlusYearsBehavior() {
        val rule = repeatRule(
            frequency = RepeatFrequency.Yearly,
            anchorDate = date("2026-06-25")
        )

        assertEquals(date("2027-06-25"), rule.nextOccurrence(date("2026-06-25"), 0))
    }

    private fun repeatRule(
        frequency: RepeatFrequency,
        anchorDate: LocalDate,
        monthDay: Int? = null,
        monthDays: String? = null,
        months: String? = null
    ): RepeatRule {
        return RepeatRule(
            id = "rule",
            seriesId = "series",
            frequency = frequency,
            monthDay = monthDay,
            monthDays = monthDays,
            months = months,
            anchorDate = anchorDate
        )
    }

    private fun date(value: String): LocalDate = LocalDate.parse(value)
}
