package com.nevoit.cresto.ui.components.packed

import com.nevoit.cresto.data.todo.TodoItem
import com.nevoit.cresto.data.todo.TodoReminderMode
import java.time.LocalTime
import java.time.format.DateTimeFormatter

fun TodoItem.toReminderConfig(): TodoReminderConfig? {
    val mode = reminderMode ?: return null
    return TodoReminderConfig(
        mode = mode,
        offsetMinutes = reminderOffsetMinutes,
        dayOffset = reminderDayOffset,
        time = reminderTime,
        persistent = reminderPersistent,
        strong = reminderStrong
    )
}

fun TodoItem.withReminderConfig(config: TodoReminderConfig?): TodoItem {
    return copy(
        reminderMode = config?.mode,
        reminderOffsetMinutes = config?.offsetMinutes,
        reminderDayOffset = config?.dayOffset,
        reminderTime = config?.time,
        reminderPersistent = config?.persistent ?: reminderPersistent,
        reminderStrong = config?.strong ?: reminderStrong
    )
}

fun TodoReminderConfig?.compatibleWithAllDay(isAllDayEnabled: Boolean): TodoReminderConfig? {
    return if (isAllDayEnabled && this?.mode == TodoReminderMode.BeforeStart) null else this
}

fun TodoReminderConfig.displayText(
    noneText: String,
    allDayMorningText: String,
    oneMinuteBeforeText: String,
    fiveMinutesBeforeText: String,
    thirtyMinutesBeforeText: String,
    oneHourBeforeText: String,
    twoHoursBeforeText: String,
    beforePrefix: String,
    dueDayText: String,
    daysBeforeFormat: String,
    hoursUnitFormat: String,
    minutesUnitFormat: String
): String {
    return when (mode) {
        TodoReminderMode.BeforeStart -> when (offsetMinutes) {
            1 -> oneMinuteBeforeText
            5 -> fiveMinutesBeforeText
            30 -> thirtyMinutesBeforeText
            60 -> oneHourBeforeText
            120 -> twoHoursBeforeText
            null -> noneText
            else -> {
                val hours = offsetMinutes / 60
                val minutes = offsetMinutes % 60
                buildList {
                    if (hours > 0) add(hoursUnitFormat.format(hours))
                    if (minutes > 0) add(minutesUnitFormat.format(minutes))
                }.joinToString(" ")
                    .takeIf { it.isNotBlank() }
                    ?.let { "$beforePrefix $it" }
                    ?: noneText
            }
        }

        TodoReminderMode.BeforeDueDate -> {
            val selectedTime = time ?: return noneText
            if (dayOffset == 0 && selectedTime == LocalTime.of(8, 0)) {
                allDayMorningText
            } else {
                val dayText = if (dayOffset == 0) dueDayText else daysBeforeFormat.format(dayOffset)
                "$dayText ${selectedTime.format(DateTimeFormatter.ofPattern("HH:mm"))}"
            }
        }
    }
}
