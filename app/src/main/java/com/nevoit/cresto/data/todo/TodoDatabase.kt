package com.nevoit.cresto.data.todo

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nevoit.cresto.data.utils.Converters

val MIGRATION_25_26 = object : Migration(25, 26) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE todo_items ADD COLUMN repeatRuleId TEXT")
        db.execSQL("ALTER TABLE todo_items ADD COLUMN seriesId TEXT")
        db.execSQL("ALTER TABLE todo_items ADD COLUMN occurrenceDate TEXT")
        db.execSQL("ALTER TABLE todo_items ADD COLUMN generatedFromTodoId INTEGER")
        db.execSQL("ALTER TABLE todo_items ADD COLUMN occurrenceEditedAt TEXT")

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_todo_items_repeatRuleId` ON `todo_items` (`repeatRuleId`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_todo_items_seriesId_occurrenceDate` ON `todo_items` (`seriesId`, `occurrenceDate`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_todo_items_generatedFromTodoId` ON `todo_items` (`generatedFromTodoId`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `repeat_rules` (
                `id` TEXT NOT NULL, 
                `anchorDate` TEXT NOT NULL, 
                `createNextOnCompletion` INTEGER NOT NULL, 
                `endDate` TEXT, 
                `frequency` TEXT NOT NULL, 
                `interval` INTEGER NOT NULL, 
                `maxOccurrences` INTEGER, 
                `monthDay` INTEGER, 
                `seriesId` TEXT NOT NULL, 
                `weekdays` TEXT, 
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_repeat_rules_seriesId` ON `repeat_rules` (`seriesId`)")
    }
}

@Database(
    entities = [TodoItem::class, SubTodoItem::class, RepeatRule::class],
    version = 26,
    exportSchema = true
)

@TypeConverters(Converters::class)
abstract class TodoDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao
}