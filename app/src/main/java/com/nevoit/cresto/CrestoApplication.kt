package com.nevoit.cresto

import android.app.Application
import com.nevoit.cresto.data.todo.TodoDatabase
import com.nevoit.cresto.data.todo.TodoRepository
import com.tencent.mmkv.MMKV

/**
 * Application class for Cresto, responsible for initializing application-level components.
 */
class CrestoApplication : Application() {
    private val database by lazy { TodoDatabase.getDatabase(this) }
    val repository by lazy { TodoRepository(database.todoDao()) }

    override fun onCreate() {
        super.onCreate()
        // Initialize the MMKV key-value storage library.
        MMKV.initialize(this)
    }
}
