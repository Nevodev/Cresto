package com.nevoit.cresto.data.todo.reminder

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.nevoit.cresto.R
import com.nevoit.cresto.data.todo.EXTRA_TODO_ID
import com.nevoit.cresto.data.todo.TodoDatabase
import com.nevoit.cresto.feature.detail.DetailActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext
import java.time.LocalDateTime

class TodoAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: if (intent.hasExtra(EXTRA_REMINDER_TODO_ID)) {
            ACTION_TODO_REMINDER
        } else {
            return
        }
        if (action != ACTION_TODO_REMINDER &&
            action != ACTION_TODO_REMINDER_COMPLETE &&
            action != ACTION_TODO_REMINDER_SNOOZE
        ) {
            return
        }

        val todoId = intent.getIntExtra(EXTRA_REMINDER_TODO_ID, -1)
        if (todoId <= 0) return

        when (action) {
            ACTION_TODO_REMINDER_COMPLETE -> {
                completeTodo(context, todoId)
                return
            }

            ACTION_TODO_REMINDER_SNOOZE -> {
                snoozeTodo(context, todoId)
                return
            }
        }

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val title = intent.getStringExtra(EXTRA_REMINDER_TODO_TITLE)
            ?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.app_name)
        val contentText = intent.getStringExtra(EXTRA_REMINDER_TODO_NOTES)
            ?.takeIf { it.isNotBlank() }
            ?: intent.getStringExtra(EXTRA_REMINDER_FALLBACK_TEXT)
                ?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.reminder_notification_default_content)
        val persistent = intent.getBooleanExtra(EXTRA_REMINDER_PERSISTENT, false)
        val strong = intent.getBooleanExtra(EXTRA_REMINDER_STRONG, false)
        val channelId = if (strong) TODO_STRONG_REMINDER_CHANNEL_ID else TODO_REMINDER_CHANNEL_ID
        val soundUri = RingtoneManager.getDefaultUri(
            if (strong) RingtoneManager.TYPE_ALARM else RingtoneManager.TYPE_NOTIFICATION
        )
        val vibrationPattern = if (strong) {
            TODO_STRONG_REMINDER_VIBRATION_PATTERN
        } else {
            TODO_REMINDER_VIBRATION_PATTERN
        }

        TodoReminderNotifications.createChannels(context)

        val openIntent = Intent(context, DetailActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TODO_ID, todoId)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            todoId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            todoId + COMPLETE_REQUEST_CODE_OFFSET,
            Intent(context, TodoAlarmReceiver::class.java).apply {
                setAction(ACTION_TODO_REMINDER_COMPLETE)
                putExtra(EXTRA_REMINDER_TODO_ID, todoId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            todoId + SNOOZE_REQUEST_CODE_OFFSET,
            Intent(context, TodoAlarmReceiver::class.java).apply {
                setAction(ACTION_TODO_REMINDER_SNOOZE)
                putExtra(EXTRA_REMINDER_TODO_ID, todoId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setContentIntent(openPendingIntent)
            .setAutoCancel(!persistent)
            .setOngoing(persistent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setSound(soundUri)
            .setVibrate(vibrationPattern)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSilent(false)
            .setPriority(
                if (strong) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH
            )
            .setCategory(
                if (strong) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER
            )
            .addAction(
                R.drawable.ic_checkmark_circle,
                context.getString(R.string.reminder_notification_action_complete),
                completePendingIntent
            )
            .addAction(
                R.drawable.ic_clock_cycle,
                context.getString(R.string.reminder_notification_action_snooze),
                snoozePendingIntent
            )
            .build()

        try {
            NotificationManagerCompat.from(context).notify(todoId, notification)
        } catch (_: SecurityException) {
            // Notification permission can change while a reminder alarm is already scheduled.
        }
    }

    private fun completeTodo(context: Context, todoId: Int) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val koin = GlobalContext.getOrNull() ?: return@launch
                val database = koin.get<TodoDatabase>()
                val scheduler = koin.get<TodoAlarmScheduler>()

                database.todoDao().markCompletedById(todoId, LocalDateTime.now())
                scheduler.cancel(todoId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun snoozeTodo(context: Context, todoId: Int) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val koin = GlobalContext.getOrNull() ?: return@launch
                val database = koin.get<TodoDatabase>()
                val scheduler = koin.get<TodoAlarmScheduler>()
                val todo = database.todoDao().getTodoWithSubTodosByIdSnapshot(todoId)?.todoItem
                    ?: return@launch

                NotificationManagerCompat.from(context).cancel(todoId)
                if (todo.isCompleted) return@launch
                scheduler.snooze(todo)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val COMPLETE_REQUEST_CODE_OFFSET = 100_000
        const val SNOOZE_REQUEST_CODE_OFFSET = 200_000
    }
}

object TodoReminderNotifications {
    fun createChannel(context: Context) {
        createChannels(context)
    }

    fun createChannels(context: Context) {
        val notificationSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val alarmSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val notificationAudioAttributes =
            buildAudioAttributes(AudioAttributes.USAGE_NOTIFICATION_EVENT)
        val alarmAudioAttributes = buildAudioAttributes(AudioAttributes.USAGE_ALARM)

        val channel = NotificationChannel(
            TODO_REMINDER_CHANNEL_ID,
            context.getString(R.string.reminder_notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.reminder_notification_channel_description)
            setSound(notificationSoundUri, notificationAudioAttributes)
            enableVibration(true)
            vibrationPattern = TODO_REMINDER_VIBRATION_PATTERN
            enableLights(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val strongChannel = NotificationChannel(
            TODO_STRONG_REMINDER_CHANNEL_ID,
            context.getString(R.string.strong_reminder_notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description =
                context.getString(R.string.strong_reminder_notification_channel_description)
            setSound(alarmSoundUri, alarmAudioAttributes)
            enableVibration(true)
            vibrationPattern = TODO_STRONG_REMINDER_VIBRATION_PATTERN
            enableLights(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannels(listOf(channel, strongChannel))
    }

    private fun buildAudioAttributes(usage: Int): AudioAttributes {
        return AudioAttributes.Builder()
            .setUsage(usage)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
    }
}

private val TODO_REMINDER_VIBRATION_PATTERN = longArrayOf(0L, 300L, 150L, 300L)
private val TODO_STRONG_REMINDER_VIBRATION_PATTERN = longArrayOf(0L, 450L, 120L, 450L, 120L, 650L)
const val TODO_REMINDER_CHANNEL_ID = "todo_reminder_alerts_v2"
const val TODO_STRONG_REMINDER_CHANNEL_ID = "todo_strong_reminder_alerts_v1"
const val ACTION_TODO_REMINDER_COMPLETE = "com.nevoit.cresto.action.TODO_REMINDER_COMPLETE"
const val ACTION_TODO_REMINDER_SNOOZE = "com.nevoit.cresto.action.TODO_REMINDER_SNOOZE"
