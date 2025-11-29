package com.nevoit.cresto.repository

import com.nevoit.cresto.data.SubTodoItem
import com.nevoit.cresto.data.TodoDao
import com.nevoit.cresto.data.TodoItem
import com.nevoit.cresto.data.TodoItemWithSubTodos
import kotlinx.coroutines.flow.Flow

/**
 * A repository that provides a single source of truth for all to-do data.
 * It abstracts the data source (in this case, a Room database) from the rest of the app.
 *
 * @param todoDao The Data Access Object for the to-do items.
 */
class TodoRepository(private val todoDao: TodoDao) {

    /**
     * A flow that emits a list of all to-do items with their sub-todos from the database.
     */
    val allTodos: Flow<List<TodoItemWithSubTodos>> = todoDao.getAllTodosWithSubTodos()

    /**
     * A flow that emits a list of all to-do items with their sub-todos, sorted by due date.
     */
    val allTodosSortedByDueDate: Flow<List<TodoItemWithSubTodos>> =
        todoDao.getAllTodosWithSubTodosSortedByDueDate()

    /**
     * Retrieves a single to-do item with its sub-todos by its ID.
     *
     * @param id The ID of the to-do item.
     */
    fun getTodoById(id: Int): Flow<TodoItemWithSubTodos?> {
        return todoDao.getTodoWithSubTodosById(id)
    }

    /**
     * Inserts a new to-do item into the database.
     *
     * @param item The to-do item to insert.
     */
    suspend fun insert(item: TodoItem) {
        todoDao.insertTodo(item)
    }

    /**
     * Inserts a list of to-do items into the database.
     *
     * @param items The list of to-do items to insert.
     */
    suspend fun insertAll(items: List<TodoItem>) {
        todoDao.insertAll(items)
    }

    /**
     * Updates an existing to-do item in the database.
     *
     * @param item The to-do item to update.
     */
    suspend fun update(item: TodoItem) {
        todoDao.updateTodo(item)
    }

    /**
     * Deletes a to-do item from the database.
     *
     * @param item The to-do item to delete.
     */
    suspend fun delete(item: TodoItem) {
        todoDao.deleteTodo(item)
    }

    // --- SubTodo Operations ---

    /**
     * Inserts a new sub-todo item into the database.
     *
     * @param item The sub-todo item to insert.
     */
    suspend fun insertSubTodo(item: SubTodoItem) {
        todoDao.insertSubTodo(item)
    }

    /**
     * Updates an existing sub-todo item in the database.
     *
     * @param item The sub-todo item to update.
     */
    suspend fun updateSubTodo(item: SubTodoItem) {
        todoDao.updateSubTodo(item)
    }

    /**
     * Deletes a sub-todo item from the database.
     *
     * @param item The sub-todo item to delete.
     */
    suspend fun deleteSubTodo(item: SubTodoItem) {
        todoDao.deleteSubTodo(item)
    }

    suspend fun deleteById(id: Int) {
        todoDao.deleteById(id)
    }
}
