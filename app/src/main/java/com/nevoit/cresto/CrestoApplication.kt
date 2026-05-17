package com.nevoit.cresto

import android.app.Application
import com.nevoit.cresto.data.todo.appModule
import com.nevoit.cresto.data.todo.reminder.TodoReminderNotifications
import com.nevoit.cresto.feature.screenextract.ScreenExtractNotifications
import com.tencent.mmkv.MMKV
import rikka.shizuku.ShizukuProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

/**
 * Application class for Cresto, responsible for initializing application-level components.
 */
class CrestoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ShizukuProvider.requestBinderForNonProviderProcess(this)
        MMKV.initialize(this)
        TodoReminderNotifications.createChannel(this)
        ScreenExtractNotifications.createChannel(this)
        startKoin {
            androidContext(this@CrestoApplication)

            modules(appModule)
        }
    }
}
