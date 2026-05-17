package com.nevoit.cresto.feature.screenextract

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.widget.Toast
import com.nevoit.cresto.R
import com.nevoit.cresto.data.todo.TodoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

class ScreenExtractService : Service() {

    private val todoRepository: TodoRepository by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serviceScope.launch {
            try {
                Toast.makeText(
                    this@ScreenExtractService,
                    R.string.extract_screen_started,
                    Toast.LENGTH_SHORT
                ).show()
                val count = withContext(Dispatchers.IO) {
                    ScreenExtractRepository(todoRepository).captureExtractAndInsert()
                }
                Toast.makeText(
                    this@ScreenExtractService,
                    getString(R.string.extract_screen_success, count),
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@ScreenExtractService,
                    e.localizedMessage ?: getString(R.string.extract_screen_failed),
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
