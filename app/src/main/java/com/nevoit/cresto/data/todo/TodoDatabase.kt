package com.nevoit.cresto.data.todo

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nevoit.cresto.data.todo.liveactivity.ActivityDao
import com.nevoit.cresto.data.todo.liveactivity.ActivityTypeConverters
import com.nevoit.cresto.data.todo.liveactivity.LiveActivityEntity
import com.nevoit.cresto.data.utils.Converters

@Database(
    entities = [TodoItem::class, SubTodoItem::class, LiveActivityEntity::class],
    version = 13,
    exportSchema = false
)

@TypeConverters(Converters::class, ActivityTypeConverters::class)
abstract class TodoDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao

    abstract fun activityDao(): ActivityDao
}