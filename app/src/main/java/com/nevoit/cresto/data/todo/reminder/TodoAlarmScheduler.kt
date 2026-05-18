package com.nevoit.cresto.data.todo.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.nevoit.cresto.data.todo.TodoItem
import com.nevoit.cresto.data.todo.reminderDateTime
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

        if (triggerAtMillis <= System.currentTimeMillis()) return

        val pendingIntent = createPendingIntent(todo)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
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
        alarmManager.cancel(createPendingIntent(todoId))
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
            action = ACTION_TODO_REMINDER
            putExtra(EXTRA_REMINDER_TODO_ID, todo.id)
            putExtra(EXTRA_REMINDER_TODO_TITLE, todo.title)
            putExtra(EXTRA_REMINDER_TODO_NOTES, todo.notes)
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

    private fun createPendingIntent(todoId: Int): PendingIntent {
        val intent = Intent(appContext, TodoAlarmReceiver::class.java).apply {
            action = ACTION_TODO_REMINDER
        }

        return PendingIntent.getBroadcast(
            appContext,
            todoId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

const val ACTION_TODO_REMINDER = "com.nevoit.cresto.action.TODO_REMINDER"
const val EXTRA_REMINDER_TODO_ID = "extra_reminder_todo_id"
const val EXTRA_REMINDER_TODO_TITLE = "extra_reminder_todo_title"
const val EXTRA_REMINDER_TODO_NOTES = "extra_reminder_todo_notes"
const val EXTRA_REMINDER_PERSISTENT = "extra_reminder_persistent"
const val EXTRA_REMINDER_STRONG = "extra_reminder_strong"
