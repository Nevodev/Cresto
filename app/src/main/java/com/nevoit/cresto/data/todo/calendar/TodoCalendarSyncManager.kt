package com.nevoit.cresto.data.todo.calendar

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.nevoit.cresto.data.todo.TodoItem
import com.nevoit.cresto.data.todo.TodoItemWithSubTodos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.TimeZone

enum class CalendarSyncStatus {
    Synced,
    SkippedNoDueDate,
    SkippedNoPermission,
    SkippedNoWritableCalendar,
    Failed
}

data class CalendarSyncResult(
    val todoId: Int,
    val status: CalendarSyncStatus,
    val calendarEventId: Long? = null,
    val errorMessage: String? = null
)

data class CalendarSyncSummary(
    val total: Int,
    val synced: Int,
    val skippedNoDueDate: Int,
    val skippedNoPermission: Int,
    val skippedNoWritableCalendar: Int,
    val failed: Int
) {
    val skipped: Int
        get() = skippedNoDueDate + skippedNoPermission + skippedNoWritableCalendar

    companion object {
        fun from(results: List<CalendarSyncResult>): CalendarSyncSummary {
            return CalendarSyncSummary(
                total = results.size,
                synced = results.count { it.status == CalendarSyncStatus.Synced },
                skippedNoDueDate = results.count { it.status == CalendarSyncStatus.SkippedNoDueDate },
                skippedNoPermission = results.count { it.status == CalendarSyncStatus.SkippedNoPermission },
                skippedNoWritableCalendar = results.count { it.status == CalendarSyncStatus.SkippedNoWritableCalendar },
                failed = results.count { it.status == CalendarSyncStatus.Failed }
            )
        }
    }
}

class TodoCalendarSyncManager(context: Context) {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver

    suspend fun sync(todo: TodoItemWithSubTodos): CalendarSyncResult = withContext(Dispatchers.IO) {
        val todoItem = todo.todoItem
        if (!hasCalendarPermissions(appContext)) {
            return@withContext CalendarSyncResult(todoItem.id, CalendarSyncStatus.SkippedNoPermission)
        }

        val calendarId = findWritableCalendarId()
            ?: return@withContext CalendarSyncResult(
                todoItem.id,
                CalendarSyncStatus.SkippedNoWritableCalendar
            )
        val timing = todoItem.toEventTiming()
            ?: return@withContext CalendarSyncResult(todoItem.id, CalendarSyncStatus.SkippedNoDueDate)
        val values = todo.toEventValues(calendarId, timing)

        runCatching {
            val existingEventId = todoItem.calendarEventId
                ?.takeIf { eventExists(it) }
                ?.takeIf { updateEvent(it, values) }

            val eventId = existingEventId ?: insertEvent(values)
            CalendarSyncResult(
                todoId = todoItem.id,
                status = CalendarSyncStatus.Synced,
                calendarEventId = eventId
            )
        }.getOrElse { error ->
            CalendarSyncResult(
                todoId = todoItem.id,
                status = CalendarSyncStatus.Failed,
                errorMessage = error.localizedMessage
            )
        }
    }

    suspend fun deleteEvent(todo: TodoItem): Boolean = withContext(Dispatchers.IO) {
        val eventId = todo.calendarEventId ?: return@withContext true
        if (!hasCalendarPermissions(appContext)) return@withContext false
        runCatching { !eventExists(eventId) || deleteEventById(eventId) }.getOrDefault(false)
    }

    private fun findWritableCalendarId(): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        )
        val selection = "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?"
        val selectionArgs = arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString())

        return resolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
            val primaryIndex = cursor.getColumnIndex(CalendarContract.Calendars.IS_PRIMARY)
            var firstWritableCalendarId: Long? = null

            while (cursor.moveToNext()) {
                val calendarId = cursor.getLong(idIndex)
                if (firstWritableCalendarId == null) {
                    firstWritableCalendarId = calendarId
                }
                if (primaryIndex >= 0 && cursor.getInt(primaryIndex) == 1) {
                    return@use calendarId
                }
            }

            firstWritableCalendarId
        }
    }

    private fun eventExists(eventId: Long): Boolean {
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        return resolver.query(uri, arrayOf(CalendarContract.Events._ID), null, null, null)
            ?.use { it.moveToFirst() } == true
    }

    private fun updateEvent(eventId: Long, values: ContentValues): Boolean {
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        return resolver.update(uri, values, null, null) > 0
    }

    private fun insertEvent(values: ContentValues): Long {
        val uri = resolver.insert(CalendarContract.Events.CONTENT_URI, values)
            ?: error("Calendar event insert failed")
        return ContentUris.parseId(uri)
    }

    private fun deleteEventById(eventId: Long): Boolean {
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        return resolver.delete(uri, null, null) > 0
    }

    private fun TodoItemWithSubTodos.toEventValues(
        calendarId: Long,
        timing: EventTiming
    ): ContentValues {
        return ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, todoItem.title)
            put(CalendarContract.Events.DESCRIPTION, buildDescription())
            put(CalendarContract.Events.DTSTART, timing.startMillis)
            put(CalendarContract.Events.DTEND, timing.endMillis)
            put(CalendarContract.Events.ALL_DAY, if (timing.allDay) 1 else 0)
            put(CalendarContract.Events.EVENT_TIMEZONE, timing.timeZoneId)
        }
    }

    private fun TodoItemWithSubTodos.buildDescription(): String {
        val notes = todoItem.notes.trim()
        val tasks = subTodos
            .map { it.description.trim() }
            .filter { it.isNotEmpty() }

        return buildString {
            if (notes.isNotEmpty()) {
                append(notes)
            }
            if (tasks.isNotEmpty()) {
                if (isNotEmpty()) append("\n\n")
                append("Tasks:")
                tasks.forEach { task ->
                    append("\n- ")
                    append(task)
                }
            }
        }
    }

    private fun TodoItem.toEventTiming(): EventTiming? {
        val date = dueDate ?: return null
        if (startTime == null && endTime == null) {
            val startMillis = date.atStartOfDay().toInstantMillis(ZoneOffset.UTC)
            val endMillis = date.plusDays(1).atStartOfDay().toInstantMillis(ZoneOffset.UTC)
            return EventTiming(
                startMillis = startMillis,
                endMillis = endMillis,
                allDay = true,
                timeZoneId = "UTC"
            )
        }

        val start = startTime ?: endTime?.minusHours(1) ?: LocalTime.of(9, 0)
        val rawEnd = endTime ?: start.plusHours(1)
        val endDate = if (rawEnd.isAfter(start)) date else date.plusDays(1)
        val zoneId = ZoneId.systemDefault()

        return EventTiming(
            startMillis = date.atTime(start).toInstantMillis(zoneId),
            endMillis = endDate.atTime(rawEnd).toInstantMillis(zoneId),
            allDay = false,
            timeZoneId = TimeZone.getDefault().id
        )
    }

    private fun LocalDateTime.toInstantMillis(zoneId: ZoneId): Long {
        return atZone(zoneId).toInstant().toEpochMilli()
    }

    private data class EventTiming(
        val startMillis: Long,
        val endMillis: Long,
        val allDay: Boolean,
        val timeZoneId: String
    )

    companion object {
        val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR
        )

        fun hasCalendarPermissions(context: Context): Boolean {
            return REQUIRED_PERMISSIONS.all { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        }
    }
}