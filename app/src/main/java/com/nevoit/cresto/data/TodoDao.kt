package com.nevoit.cresto.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

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

    // Fetches all todo items from the table, ordered by ID in descending order.
    @Query("SELECT * FROM todo_items ORDER BY id DESC")
    fun getAllTodos(): Flow<List<TodoItem>>

    // Fetches all todo items, ordered by due date. Items with no due date are last.
    @Query("SELECT * FROM todo_items ORDER BY dueDate IS NULL, dueDate ASC")
    fun getAllTodosSortedByDueDate(): Flow<List<TodoItem>>

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

    @Query("DELETE FROM todo_items WHERE id = :id")
    suspend fun deleteById(id: Int)
}
