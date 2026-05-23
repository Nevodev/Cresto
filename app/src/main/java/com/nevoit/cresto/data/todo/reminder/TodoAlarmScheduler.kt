package com.nevoit.cresto.data.todo.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.nevoit.cresto.R
import com.nevoit.cresto.data.todo.TodoItem
import com.nevoit.cresto.data.todo.TodoReminderMode
import com.nevoit.cresto.data.todo.reminderDateTime
import com.nevoit.cresto.feature.detail.DetailActivity
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.ZoneId

class TodoAlarmScheduler(
    private val context: Context
) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    fun schedule(todo: TodoItem) {
        if (todo.id <= 0 || todo.isCompleted) return

        val reminderDateTime = todo.reminderDateTime() ?: return
        val triggerAtMillis = reminderDateTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        scheduleAt(todo, triggerAtMillis)
    }

    fun snooze(todo: TodoItem, delayMillis: Long = DEFAULT_SNOOZE_DELAY_MILLIS) {
        if (todo.id <= 0 || todo.isCompleted) return
        scheduleAt(todo, System.currentTimeMillis() + delayMillis)
    }

    private fun scheduleAt(todo: TodoItem, triggerAtMillis: Long) {

        if (triggerAtMillis <= System.currentTimeMillis()) return

        val pendingIntent = createPendingIntent(todo)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            try {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(
                        triggerAtMillis,
                        createOpenPendingIntent(todo.id)
                    ),
                    pendingIntent
                )
                return
            } catch (_: SecurityException) {
                // Fall back to the best non-exact alarm Android allows without exact alarm access.
            }

            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
            return
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }

    fun cancel(todo: TodoItem) {
        cancel(todo.id)
    }

    fun cancel(todoId: Int) {
        if (todoId <= 0) return
        findPendingIntent(todoId)?.let { pendingIntent ->
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
        NotificationManagerCompat.from(appContext).cancel(todoId)
    }

    fun cancelAll(todoIds: Iterable<Int>) {
        todoIds.forEach(::cancel)
    }

    fun hasNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun createPendingIntent(todo: TodoItem): PendingIntent {
        val intent = Intent(appContext, TodoAlarmReceiver::class.java).apply {
            setAction(ACTION_TODO_REMINDER)
            putExtra(EXTRA_REMINDER_TODO_ID, todo.id)
            putExtra(EXTRA_REMINDER_TODO_TITLE, todo.title)
            putExtra(EXTRA_REMINDER_TODO_NOTES, todo.notes)
            putExtra(EXTRA_REMINDER_FALLBACK_TEXT, todo.buildReminderFallbackText())
            putExtra(EXTRA_REMINDER_PERSISTENT, todo.reminderPersistent)
            putExtra(EXTRA_REMINDER_STRONG, todo.reminderStrong)
        }

        return PendingIntent.getBroadcast(
            appContext,
            todo.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun findPendingIntent(todoId: Int): PendingIntent? {
        val intent = Intent(appContext, TodoAlarmReceiver::class.java).apply {
            setAction(ACTION_TODO_REMINDER)
        }

        return PendingIntent.getBroadcast(
            appContext,
            todoId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createOpenPendingIntent(todoId: Int): PendingIntent {
        val intent = Intent(appContext, DetailActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(com.nevoit.cresto.data.todo.EXTRA_TODO_ID, todoId)
        }

        return PendingIntent.getActivity(
            appContext,
            todoId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun TodoItem.buildReminderFallbackText(): String {
        return when (reminderMode) {
            TodoReminderMode.BeforeStart -> buildStartReminderText()
            TodoReminderMode.BeforeDueDate -> buildDueDateReminderText()
            null -> appContext.getString(R.string.reminder_notification_default_content)
        }
    }

    private fun TodoItem.buildStartReminderText(): String {
        val start = startTime ?: return appContext.getString(R.string.reminder_notification_default_content)
        val startText = formatDateTime(dueDate, start)
        val endText = endTime?.format(REMINDER_TIME_FORMATTER)

        return if (endText == null) {
            appContext.getString(R.string.reminder_notification_start_time, startText)
        } else {
            appContext.getString(R.string.reminder_notification_time_range, startText, endText)
        }
    }

    private fun TodoItem.buildDueDateReminderText(): String {
        val date = dueDate
        val dayOffset = reminderDayOffset

        return when {
            date != null && dayOffset != null && dayOffset > 0 -> {
                appContext.getString(
                    R.string.reminder_notification_days_until_due,
                    formatDate(date),
                    dayOffset
                )
            }

            date != null -> appContext.getString(
                R.string.reminder_notification_due_date,
                formatDate(date)
            )

            reminderTime != null -> appContext.getString(
                R.string.reminder_notification_reminder_time,
                reminderTime.format(REMINDER_TIME_FORMATTER)
            )

            else -> appContext.getString(R.string.reminder_notification_default_content)
        }
    }

    private fun formatDateTime(date: LocalDate?, time: LocalTime): String {
        val timeText = time.format(REMINDER_TIME_FORMATTER)
        return if (date == null) timeText else "${formatDate(date)} $timeText"
    }

    private fun formatDate(date: LocalDate): String {
        val today = LocalDate.now()
        return when (date) {
            today -> appContext.getString(R.string.today)
            today.plusDays(1) -> appContext.getString(R.string.tomorrow)
            else -> appContext.getString(
                R.string.reminder_notification_date_format,
                date.monthValue,
                date.dayOfMonth
            )
        }
    }
}

const val ACTION_TODO_REMINDER = "com.nevoit.cresto.action.TODO_REMINDER"
const val EXTRA_REMINDER_TODO_ID = "extra_reminder_todo_id"
const val EXTRA_REMINDER_TODO_TITLE = "extra_reminder_todo_title"
const val EXTRA_REMINDER_TODO_NOTES = "extra_reminder_todo_notes"
const val EXTRA_REMINDER_FALLBACK_TEXT = "extra_reminder_fallback_text"
const val EXTRA_REMINDER_PERSISTENT = "extra_reminder_persistent"
const val EXTRA_REMINDER_STRONG = "extra_reminder_strong"

private const val DEFAULT_SNOOZE_DELAY_MILLIS = 10 * 60 * 1000L
private val REMINDER_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
