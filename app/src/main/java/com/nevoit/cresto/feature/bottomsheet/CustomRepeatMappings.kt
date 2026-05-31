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
    return RepeatRuleConfig(
        frequency = frequency,
        interval = interval,
        weekdays = weekdays,
        monthDay = monthDays.minOrNull(),
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
    return CustomRepeatConfig(
        frequency = frequency,
        interval = interval,
        weekdays = weekdays,
        monthDays = monthDay?.let { setOf(it) } ?: setOf(anchorDate.dayOfMonth),
        months = setOf(anchorDate.monthValue),
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
        endDate != null ||
        maxOccurrences != null
}
