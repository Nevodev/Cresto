package com.nevoit.cresto.data.todo.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nevoit.cresto.data.todo.TodoDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

class TodoBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val koin = GlobalContext.getOrNull()
                val database = koin?.get<TodoDatabase>() ?: return@launch
                val scheduler = TodoAlarmScheduler(context)

                database.todoDao()
                    .getReminderTodosSnapshot()
                    .forEach(scheduler::schedule)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
