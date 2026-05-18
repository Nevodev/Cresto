package com.nevoit.cresto.feature.shareextract

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.nevoit.cresto.MainActivity
import com.nevoit.cresto.R
import com.nevoit.cresto.feature.screenextract.ScreenExtractEvents

object ShareExtractNotifications {
    private const val CHANNEL_ID = "shared_extraction"
    private const val NOTIFICATION_ID = 260518

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.share_extract_notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.share_extract_notification_channel_description)
        }

        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    fun showProgress(context: Context, phase: ShareExtractPhase) {
        if (!canPostNotifications(context)) return
        createChannel(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_sparkle_viewfinder)
            .setContentTitle(context.getString(R.string.share_extract_notification_title))
            .setContentText(context.getString(phase.messageRes))
            .setProgress(ShareExtractPhase.PROGRESS_MAX, phase.progress, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()

        post(context, notification)
    }

    fun showSuccess(context: Context, count: Int) {
        if (!canPostNotifications(context)) return
        createChannel(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_sparkle_viewfinder)
            .setContentTitle(context.getString(R.string.share_extract_success_title))
            .setContentText(context.getString(R.string.share_extract_success, count))
            .setContentIntent(openCrestoIntent(context, requestCode = 1))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()

        post(context, notification)
    }

    fun showFailure(context: Context, message: String) {
        if (!canPostNotifications(context)) return
        createChannel(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_sparkle_viewfinder)
            .setContentTitle(context.getString(R.string.share_extract_failed))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(openCrestoIntent(context, requestCode = 2, errorMessage = message))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .build()

        post(context, notification)
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun post(context: Context, notification: Notification) {
        if (!canPostNotifications(context)) return
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Notification permission can change while extraction is running.
        }
    }

    private fun openCrestoIntent(
        context: Context,
        requestCode: Int,
        errorMessage: String? = null
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (errorMessage != null) {
                putExtra(ScreenExtractEvents.EXTRA_SHOW_ERROR_DIALOG, true)
                putExtra(ScreenExtractEvents.EXTRA_ERROR_MESSAGE, errorMessage)
            }
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun canPostNotifications(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
}

enum class ShareExtractPhase(
    val progress: Int,
    val messageRes: Int
) {
    Starting(5, R.string.share_extract_progress_starting),
    Reading(25, R.string.share_extract_progress_reading),
    Extracting(65, R.string.share_extract_progress_ai),
    Importing(90, R.string.share_extract_progress_importing);

    companion object {
        const val PROGRESS_MAX = 100
    }
}