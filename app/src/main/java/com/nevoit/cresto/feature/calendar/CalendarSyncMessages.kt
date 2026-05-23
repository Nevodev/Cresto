package com.nevoit.cresto.feature.calendar

import android.content.Context
import com.nevoit.cresto.R
import com.nevoit.cresto.data.todo.calendar.CalendarSyncSummary

fun CalendarSyncSummary.toToastMessage(context: Context): String {
    return when {
        total == 1 && skippedNoDueDate == 1 -> context.getString(R.string.calendar_sync_no_due_date)
        total == 1 && skippedNoPermission == 1 -> context.getString(R.string.calendar_sync_permission_required)
        total == 1 && skippedNoWritableCalendar == 1 -> context.getString(R.string.calendar_sync_no_writable_calendar)
        total == 1 && failed == 1 -> context.getString(R.string.calendar_sync_failed)
        skipped == 0 && failed == 0 -> context.getString(R.string.calendar_sync_completed, synced)
        else -> context.getString(R.string.calendar_sync_partial, synced, skipped, failed)
    }
}