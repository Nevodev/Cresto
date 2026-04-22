package com.nevoit.cresto.feature.home

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import com.nevoit.cresto.R
import com.nevoit.cresto.feature.settings.util.SettingsManager

internal class HomeCompletionSoundPlayer(context: Context) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(1)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val completeSoundId = soundPool.load(appContext, R.raw.complete, 1)

    @Volatile
    private var isLoaded = false

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (sampleId == completeSoundId && status == 0) {
                isLoaded = true
            }
        }
    }

    fun playIfAllowed() {
        if (!SettingsManager.isCompletionSoundEnabled) return
        if (!isLoaded) return
        if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
        if (audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION) <= 0) return

        val interruptionFilter = runCatching { notificationManager.currentInterruptionFilter }
            .getOrDefault(NotificationManager.INTERRUPTION_FILTER_UNKNOWN)
        if (interruptionFilter == NotificationManager.INTERRUPTION_FILTER_NONE) return

        soundPool.play(completeSoundId, 1f, 1f, 1, 0, 1f)
    }

    fun release() {
        soundPool.release()
    }
}

