package com.nevoit.cresto.data.todo

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nevoit.cresto.data.utils.Converters

@Database(
    entities = [TodoItem::class, SubTodoItem::class, RecurringTodoRule::class],
    version = 21,
    exportSchema = false
)

@TypeConverters(Converters::class)
abstract class TodoDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao
    abstract fun recurringTodoRuleDao(): RecurringTodoRuleDao
}