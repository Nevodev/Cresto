package com.nevoit.cresto.data.todo

import androidx.room.Room
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
            .fallbackToDestructiveMigration(false)
            .build()
    }

    single { get<TodoDatabase>().todoDao() }
    singleOf(::TodoRepository)
    viewModelOf(::TodoViewModel)
}