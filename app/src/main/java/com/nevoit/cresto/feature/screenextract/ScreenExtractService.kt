package com.nevoit.cresto.feature.screenextract

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.nevoit.cresto.R
import com.nevoit.cresto.data.todo.TodoRepository
import com.nevoit.cresto.data.todo.reminder.TodoAlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import java.util.concurrent.atomic.AtomicBoolean

class ScreenExtractService : Service() {

    private val todoRepository: TodoRepository by inject()
    private val alarmScheduler: TodoAlarmScheduler by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isExtracting.compareAndSet(false, true)) {
            ScreenExtractNotifications.showProgress(this, ScreenExtractPhase.Starting)
            stopSelf(startId)
            return START_NOT_STICKY
        }

        serviceScope.launch {
            try {
                ScreenExtractNotifications.showProgress(
                    this@ScreenExtractService,
                    ScreenExtractPhase.Starting
                )
                val repository = ScreenExtractRepository(todoRepository, alarmScheduler)
                val items = withContext(Dispatchers.IO) {
                    repository.captureAndExtract { phase ->
                        ScreenExtractNotifications.showProgress(this@ScreenExtractService, phase)
                    }
                }

                if (ScreenExtractEvents.isMainUiOpen() &&
                    ScreenExtractEvents.emitPendingTodos(items, AiExtractSource.Screen)
                ) {
                    ScreenExtractNotifications.cancel(this@ScreenExtractService)
                } else {
                    ScreenExtractNotifications.showProgress(
                        this@ScreenExtractService,
                        ScreenExtractPhase.Importing
                    )
                    val count = withContext(Dispatchers.IO) {
                        repository.insertExtractedTodos(items)
                    }
                    ScreenExtractNotifications.showSuccess(this@ScreenExtractService, count)
                }
            } catch (e: Exception) {
                val errorMessage = e.toScreenExtractErrorMessage(getString(R.string.extract_screen_failed))
                ScreenExtractNotifications.showFailure(this@ScreenExtractService, errorMessage)
            } finally {
                isExtracting.set(false)
                stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private companion object {
        val isExtracting = AtomicBoolean(false)
    }
}
