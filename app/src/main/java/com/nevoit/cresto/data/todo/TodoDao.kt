package com.nevoit.cresto.data.todo

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.nevoit.cresto.data.statistics.DailyStat
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

// Data Access Object (DAO) for the todo_items table.
@Dao
interface TodoDao {
    // Inserts a todo item into the table, replacing it if it already exists.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(item: TodoItem)

    // Inserts a list of todo items, ignoring any that already exist.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<TodoItem>)

    // Updates an existing todo item.
    @Update
    suspend fun updateTodo(item: TodoItem)

    // Deletes a todo item from the table.
    @Delete
    suspend fun deleteTodo(item: TodoItem)

    // Deletes all todo items from the table.
    @Query("DELETE FROM todo_items")
    suspend fun deleteAllTodos()

    // --- New operations for SubTodoItem ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubTodo(item: SubTodoItem)

    @Update
    suspend fun updateSubTodo(item: SubTodoItem)

    @Delete
    suspend fun deleteSubTodo(item: SubTodoItem)

    // --- New queries to include sub-todos ---

    // Fetches all todo items with their sub-todos, ordered by ID in descending order.
    @Transaction
    @Query("SELECT * FROM todo_items ORDER BY id DESC")
    fun getAllTodosWithSubTodos(): Flow<List<TodoItemWithSubTodos>>

    // Fetches all todo items with their sub-todos, ordered by due date.
    @Transaction
    @Query("SELECT * FROM todo_items ORDER BY dueDate IS NULL, dueDate ASC")
    fun getAllTodosWithSubTodosSortedByDueDate(): Flow<List<TodoItemWithSubTodos>>

    // Fetches a single todo item with its sub-todos by ID.
    @Transaction
    @Query("SELECT * FROM todo_items WHERE id = :id")
    fun getTodoWithSubTodosById(id: Int): Flow<TodoItemWithSubTodos?>

    @Transaction
    @Query("SELECT * FROM todo_items WHERE id IN (:ids)")
    suspend fun getTodosWithSubTodosByIds(ids: List<Int>): List<TodoItemWithSubTodos>

    @Query("DELETE FROM todo_items WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM todo_items WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Int>)

    @Query(
        """
        UPDATE todo_items
        SET isCompleted = :isCompleted,
            completedDateTime = CASE
                WHEN :isCompleted = 1 THEN COALESCE(completedDateTime, :completedDateTime)
                ELSE NULL
            END
        WHERE id IN (:ids)
        """
    )
    suspend fun updateCompletedStatusByIds(
        ids: List<Int>,
        isCompleted: Boolean,
        completedDateTime: LocalDateTime?
    )

    @Query("SELECT COUNT(*) FROM todo_items WHERE id IN (:ids) AND isCompleted = 1")
    suspend fun getCompletedCountByIds(ids: List<Int>): Int

    @Query(
        """
        UPDATE todo_items
        SET flag = :flag
        WHERE id IN (:ids)
        """
    )
    suspend fun updateFlagByIds(ids: List<Int>, flag: Int)

    @Query("SELECT COUNT(*) FROM todo_items")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM todo_items WHERE isCompleted = true")
    fun getCompletedCount(): Flow<Int>

    @Query(
        """
        SELECT substr(completedDateTime, 1, 10) as date, COUNT(*) as count 
        FROM todo_items 
        WHERE isCompleted = 1 AND completedDateTime IS NOT NULL 
        GROUP BY substr(completedDateTime, 1, 10) 
        ORDER BY date DESC
    """
    )
    fun getDailyStats(): Flow<List<DailyStat>>


    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTodoForImport(item: TodoItem): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSubTodoForImport(item: SubTodoItem): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTodosForDuplicate(items: List<TodoItem>): List<Long>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSubTodosForDuplicate(items: List<SubTodoItem>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTodoForMerge(item: TodoItem): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSubTodosForMerge(items: List<SubTodoItem>)

    @Query("SELECT * FROM todo_items ORDER BY id ASC")
    suspend fun getAllTodosSnapshot(): List<TodoItem>

    @Transaction
    @Query("SELECT * FROM todo_items ORDER BY id ASC")
    suspend fun getAllTodosWithSubTodosSnapshot(): List<TodoItemWithSubTodos>

    @Query("SELECT * FROM sub_todo_items ORDER BY id ASC")
    suspend fun getAllSubTodosSnapshot(): List<SubTodoItem>

}
