package com.nevoit.cresto

import android.app.Application
import com.nevoit.cresto.data.todo.appModule
import com.nevoit.cresto.data.todo.reminder.TodoReminderNotifications
import com.nevoit.cresto.feature.shareextract.ShareExtractNotifications
import com.nevoit.cresto.feature.screenextract.ScreenExtractNotifications
import com.nevoit.cresto.feature.settings.util.AppIconManager
import com.nevoit.cresto.feature.settings.util.SettingsManager
import com.tencent.mmkv.MMKV
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin
import rikka.shizuku.ShizukuProvider

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
        ShareExtractNotifications.createChannel(this)
        AppIconManager.setIcon(this, SettingsManager.appIcon)
        startKoin {
            androidContext(this@CrestoApplication)

            modules(appModule)
        }
    }
}
