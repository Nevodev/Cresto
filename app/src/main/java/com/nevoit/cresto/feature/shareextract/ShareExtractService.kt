package com.nevoit.cresto.feature.shareextract

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import com.nevoit.cresto.R
import com.nevoit.cresto.data.todo.TodoRepository
import com.nevoit.cresto.data.todo.reminder.TodoAlarmScheduler
import com.nevoit.cresto.feature.screenextract.AiExtractSource
import com.nevoit.cresto.feature.screenextract.toScreenExtractErrorMessage
import com.nevoit.cresto.feature.screenextract.ScreenExtractEvents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import java.util.concurrent.atomic.AtomicBoolean

class ShareExtractService : Service() {

    private val todoRepository: TodoRepository by inject()
    private val alarmScheduler: TodoAlarmScheduler by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isExtracting.compareAndSet(false, true)) {
            ShareExtractNotifications.showProgress(this, ShareExtractPhase.Starting)
            stopSelf(startId)
            return START_NOT_STICKY
        }

        val sharedText = intent?.getStringExtra(EXTRA_SHARED_TEXT).orEmpty()
        val imageUris = intent?.getParcelableArrayListExtra(EXTRA_IMAGE_URIS, Uri::class.java)
            .orEmpty()

        serviceScope.launch {
            try {
                ShareExtractNotifications.showProgress(
                    this@ShareExtractService,
                    ShareExtractPhase.Starting
                )
                val repository = ShareExtractRepository(
                    context = this@ShareExtractService,
                    todoRepository = todoRepository,
                    alarmScheduler = alarmScheduler
                )
                val items = withContext(Dispatchers.IO) {
                    repository.extract(
                        sharedText = sharedText,
                        imageUris = imageUris
                    ) { phase ->
                        ShareExtractNotifications.showProgress(this@ShareExtractService, phase)
                    }
                }

                if (ScreenExtractEvents.isMainUiOpen() &&
                    ScreenExtractEvents.emitPendingTodos(items, AiExtractSource.Share)
                ) {
                    ShareExtractNotifications.cancel(this@ShareExtractService)
                } else {
                    ShareExtractNotifications.showProgress(
                        this@ShareExtractService,
                        ShareExtractPhase.Importing
                    )
                    val count = withContext(Dispatchers.IO) {
                        repository.insertExtractedTodos(items)
                    }
                    ShareExtractNotifications.showSuccess(this@ShareExtractService, count)
                }
            } catch (e: Exception) {
                val errorMessage = e.toScreenExtractErrorMessage(getString(R.string.share_extract_failed))
                ShareExtractNotifications.showFailure(this@ShareExtractService, errorMessage)
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

    companion object {
        const val EXTRA_SHARED_TEXT = "com.nevoit.cresto.extra.SHARED_TEXT"
        const val EXTRA_IMAGE_URIS = "com.nevoit.cresto.extra.IMAGE_URIS"

        private val isExtracting = AtomicBoolean(false)
    }
}