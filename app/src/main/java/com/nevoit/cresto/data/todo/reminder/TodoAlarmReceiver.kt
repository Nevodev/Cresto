package com.nevoit.cresto.data.todo.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.nevoit.cresto.R
import com.nevoit.cresto.data.todo.EXTRA_TODO_ID
import com.nevoit.cresto.feature.detail.DetailActivity

class TodoAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TODO_REMINDER) return

        val todoId = intent.getIntExtra(EXTRA_REMINDER_TODO_ID, -1)
        if (todoId <= 0) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        TodoReminderNotifications.createChannel(context)

        val title = intent.getStringExtra(EXTRA_REMINDER_TODO_TITLE)
            ?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.app_name)
        val notes = intent.getStringExtra(EXTRA_REMINDER_TODO_NOTES)
            ?.takeIf { it.isNotBlank() }
            ?: "待办事项提醒"
        val persistent = intent.getBooleanExtra(EXTRA_REMINDER_PERSISTENT, false)
        val strong = intent.getBooleanExtra(EXTRA_REMINDER_STRONG, false)

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

        val notification = NotificationCompat.Builder(context, TODO_REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(title)
            .setContentText(notes)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notes))
            .setContentIntent(openPendingIntent)
            .setAutoCancel(!persistent)
            .setOngoing(persistent)
            .setPriority(
                if (strong) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT
            )
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        NotificationManagerCompat.from(context).notify(todoId, notification)
    }
}

object TodoReminderNotifications {
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            TODO_REMINDER_CHANNEL_ID,
            "Todo reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "待办事项提醒"
        }

        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }
}

const val TODO_REMINDER_CHANNEL_ID = "todo_reminders"
