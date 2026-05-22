package com.nevoit.cresto.data.todo

import androidx.room.Room
import com.nevoit.cresto.data.todo.reminder.TodoAlarmScheduler
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {

    single {
        Room.databaseBuilder(
            androidContext(),
            TodoDatabase::class.java,
            "todo_database"
        )
            .fallbackToDestructiveMigration(true)
            .build()
    }

    single { get<TodoDatabase>().todoDao() }
    single { TodoAlarmScheduler(androidContext()) }
    singleOf(::TodoRepository)
    viewModelOf(::TodoViewModel)
}