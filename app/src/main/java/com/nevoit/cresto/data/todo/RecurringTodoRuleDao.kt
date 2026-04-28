package com.nevoit.cresto.data.todo

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface RecurringTodoRuleDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(rule: RecurringTodoRule): Long

    @Update
    suspend fun update(rule: RecurringTodoRule)

    @Query("SELECT * FROM recurring_todo_rules WHERE id = :id")
    suspend fun getById(id: Int): RecurringTodoRule?

    @Query("SELECT * FROM recurring_todo_rules ORDER BY id ASC")
    suspend fun getAllSnapshot(): List<RecurringTodoRule>
}
