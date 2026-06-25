package com.nevoit.cresto.feature.bottomsheet

import com.nevoit.cresto.data.todo.RepeatFrequency
import com.nevoit.cresto.data.todo.RepeatRule
import com.nevoit.cresto.data.todo.RepeatRuleConfig
import java.time.DayOfWeek
import java.time.LocalDate

fun CustomRepeatConfig.toPresetFrequency(anchorDate: LocalDate): RepeatFrequency? {
    if (interval != 1 || endMode != CustomRepeatEndMode.Never) return null
    return when (frequency) {
        RepeatFrequency.Daily -> RepeatFrequency.Daily
        RepeatFrequency.Weekly -> {
            if (weekdays == setOf(anchorDate.dayOfWeek)) RepeatFrequency.Weekly else null
        }

        RepeatFrequency.Monthly -> {
            if (monthDays == setOf(anchorDate.dayOfMonth)) RepeatFrequency.Monthly else null
        }

        RepeatFrequency.Yearly -> {
            if (months == setOf(anchorDate.monthValue)) RepeatFrequency.Yearly else null
        }
    }
}

fun RepeatFrequency.toCustomRepeatConfig(anchorDate: LocalDate): CustomRepeatConfig {
    return CustomRepeatConfig(
        frequency = this,
        weekdays = setOf(anchorDate.dayOfWeek),
        monthDays = setOf(anchorDate.dayOfMonth),
        months = setOf(anchorDate.monthValue)
    )
}

fun CustomRepeatConfig.toRepeatRuleConfig(): RepeatRuleConfig {
    val repeatMonthDays = when (frequency) {
        RepeatFrequency.Monthly -> monthDays
        else -> emptySet()
    }
    val repeatMonths = when (frequency) {
        RepeatFrequency.Yearly -> months
        else -> emptySet()
    }
    return RepeatRuleConfig(
        frequency = frequency,
        interval = interval,
        weekdays = if (frequency == RepeatFrequency.Weekly) weekdays else emptySet(),
        monthDay = repeatMonthDays.singleOrNull(),
        monthDays = repeatMonthDays,
        months = repeatMonths,
        endDate = if (endMode == CustomRepeatEndMode.OnDate) endDate else null,
        maxOccurrences = if (endMode == CustomRepeatEndMode.AfterCount) maxOccurrences else null
    )
}

fun RepeatRule.toCustomRepeatConfig(): CustomRepeatConfig {
    val weekdays = weekdays
        ?.split(',')
        ?.mapNotNull { runCatching { DayOfWeek.valueOf(it) }.getOrNull() }
        ?.toSet()
        ?.ifEmpty { null }
        ?: setOf(anchorDate.dayOfWeek)
    val endMode = when {
        endDate != null -> CustomRepeatEndMode.OnDate
        maxOccurrences != null -> CustomRepeatEndMode.AfterCount
        else -> CustomRepeatEndMode.Never
    }
    val selectedMonthDays = monthDays.toIntSet(1..31)
        .ifEmpty { monthDay?.takeIf { it in 1..31 }?.let { setOf(it) }.orEmpty() }
        .ifEmpty { setOf(anchorDate.dayOfMonth) }
    val selectedMonths = months.toIntSet(1..12)
        .ifEmpty { setOf(anchorDate.monthValue) }
    return CustomRepeatConfig(
        frequency = frequency,
        interval = interval,
        weekdays = weekdays,
        monthDays = selectedMonthDays,
        months = selectedMonths,
        endMode = endMode,
        endDate = endDate,
        maxOccurrences = maxOccurrences ?: 10
    )
}

fun RepeatRule.isSimpleFrequency(frequency: RepeatFrequency): Boolean {
    return this.frequency == frequency && !isCustomRepeatRule()
}

fun RepeatRule.isCustomRepeatRule(): Boolean {
    return interval != 1 ||
        weekdays != null ||
        monthDay != null ||
        monthDays != null ||
        months != null ||
        endDate != null ||
        maxOccurrences != null
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
