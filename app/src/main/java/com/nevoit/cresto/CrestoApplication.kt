package com.nevoit.cresto

import android.app.Application
import com.nevoit.cresto.data.todo.appModule
import com.tencent.mmkv.MMKV
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

/**
 * Application class for Cresto, responsible for initializing application-level components.
 */
class CrestoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MMKV.initialize(this)
        startKoin {
            androidContext(this@CrestoApplication)

            modules(appModule)
        }
    }
}
