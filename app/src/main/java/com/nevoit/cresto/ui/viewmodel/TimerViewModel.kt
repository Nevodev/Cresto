package com.nevoit.cresto.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

class ModeTimerViewModel : ViewModel() {
    var setupMinutes by mutableIntStateOf(25)

    var currentSeconds by mutableIntStateOf(0)
        private set

    var isTimerMode by mutableStateOf(false)
        private set

    var isPaused by mutableStateOf(false)
        private set

    var isFinished by mutableStateOf(false)
        private set

    var isStopwatchMode by mutableStateOf(false)
        private set

    val isRunning: Boolean
        get() = isTimerMode && !isFinished

    private var timerJob: Job? = null

    fun updateSetupTime(minutes: Int) {
        setupMinutes = minutes
    }

    fun startTimer() {
        if (setupMinutes == 0) {
            isStopwatchMode = true
            currentSeconds = 0
        } else {
            isStopwatchMode = false
            currentSeconds = setupMinutes * 60
        }

        isTimerMode = true
        isPaused = false
        isFinished = false

        startCountingLoop()
    }

    private fun startCountingLoop() {
        timerJob?.cancel()

        timerJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val initialSeconds = currentSeconds

            while (isActive) {
                val elapsedTime = System.currentTimeMillis() - startTime
                val delayToNextSecond = 1000L - (elapsedTime % 1000L)

                delay(delayToNextSecond)

                val now = System.currentTimeMillis()
                val deltaSeconds = ((now - startTime) / 1000L).toInt()

                if (isStopwatchMode) {
                    currentSeconds = initialSeconds + deltaSeconds
                } else {
                    val newSeconds = initialSeconds - deltaSeconds

                    if (newSeconds > 0) {
                        currentSeconds = newSeconds
                    } else {
                        currentSeconds = 0
                        isFinished = true
                        this.cancel()
                    }
                }
            }
        }
    }

    fun pauseTimer() {
        isPaused = true
        timerJob?.cancel()
        timerJob = null
    }

    fun resumeTimer() {
        if (isFinished) return

        isPaused = false
        startCountingLoop()
    }

    fun exitTimerMode() {
        timerJob?.cancel()
        timerJob = null
        isTimerMode = false
        isPaused = false
        isFinished = false
        currentSeconds = 0
    }

    fun confirmFinish() {
        exitTimerMode()
    }

    val formattedTime: String
        get() {
            val hours = currentSeconds / 3600
            val minutes = (currentSeconds % 3600) / 60
            val seconds = currentSeconds % 60

            return if (hours > 0) {
                String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(Locale.US, "%02d:%02d", minutes, seconds)
            }
        }
}