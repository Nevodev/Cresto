package com.nevoit.cresto.data.todo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class RecurrenceCalculatorTest {
    @Test
    fun dailyRule_usesInterval() {
        val next = RecurrenceCalculator.nextDate(
            rrule = "FREQ=DAILY;INTERVAL=2",
            after = LocalDate.parse("2026-04-28"),
            startDate = LocalDate.parse("2026-04-28")
        )

        assertEquals(LocalDate.parse("2026-04-30"), next)
    }

    @Test
    fun weeklyRule_usesByDay() {
        val next = RecurrenceCalculator.nextDate(
            rrule = "FREQ=WEEKLY;INTERVAL=1;BYDAY=MO,WE,FR",
            after = LocalDate.parse("2026-04-28"),
            startDate = LocalDate.parse("2026-04-27")
        )

        assertEquals(LocalDate.parse("2026-04-29"), next)
    }

    @Test
    fun monthlyRule_keepsStartDayOfMonth() {
        val next = RecurrenceCalculator.nextDate(
            rrule = "FREQ=MONTHLY;INTERVAL=1",
            after = LocalDate.parse("2026-04-15"),
            startDate = LocalDate.parse("2026-04-15")
        )

        assertEquals(LocalDate.parse("2026-05-15"), next)
    }

    @Test
    fun untilStopsFutureOccurrences() {
        val next = RecurrenceCalculator.nextDate(
            rrule = "FREQ=DAILY;INTERVAL=1;UNTIL=20260428",
            after = LocalDate.parse("2026-04-28"),
            startDate = LocalDate.parse("2026-04-28")
        )

        assertNull(next)
    }
}
