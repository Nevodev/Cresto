package com.nevoit.cresto.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

// Annotates the class as a Room Database with tables for TodoItem and SubTodoItem.
@Database(entities = [TodoItem::class, SubTodoItem::class], version = 4, exportSchema = false)
@TypeConverters(LocalDateConverter::class)
// Abstract class that represents the Room database for the application.
abstract class TodoDatabase : RoomDatabase() {
    // Abstract method to get the Data Access Object (DAO) for the TodoItem entity.
    abstract fun todoDao(): TodoDao

    // Companion object to hold the singleton instance of the database.
    companion object {
        // Volatile annotation ensures that the INSTANCE variable is always up-to-date and the same to all execution threads.
        @Volatile
        private var INSTANCE: TodoDatabase? = null

        // Method to get the singleton instance of the database.
        fun getDatabase(context: Context): TodoDatabase {
            // Return the existing instance if it's not null, otherwise create a new one.
            return INSTANCE ?: synchronized(this) {
                // Create a new database instance using Room's databaseBuilder.
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TodoDatabase::class.java,
                    "todo_database"
                )
                    // Build the database instance.
                    .build()
                // Assign the newly created instance to the INSTANCE variable.
                INSTANCE = instance
                // Return the new instance.
                instance
            }
        }
    }
}
