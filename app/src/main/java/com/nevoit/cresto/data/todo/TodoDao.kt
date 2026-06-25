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
import java.time.LocalDate
import java.time.LocalDateTime

// Data Access Object (DAO) for the todo_items table.
@Dao
interface TodoDao {
    // Inserts a todo item into the table, replacing it if it already exists.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(item: TodoItem): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRepeatRule(rule: RepeatRule)

    // Inserts a list of todo items, ignoring any that already exist.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<TodoItem>)

    // Updates an existing todo item.
    @Update
    suspend fun updateTodo(item: TodoItem)

    @Update
    suspend fun updateRepeatRule(rule: RepeatRule)

    // Deletes a todo item from the table.
    @Delete
    suspend fun deleteTodo(item: TodoItem)

    // Deletes all todo items from the table.
    @Query("DELETE FROM todo_items")
    suspend fun deleteAllTodos()

    @Query("DELETE FROM repeat_rules")
    suspend fun deleteAllRepeatRules()

    @Query("DELETE FROM repeat_rules WHERE id = :id")
    suspend fun deleteRepeatRuleById(id: String)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTodoGroup(group: TodoGroup): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTodoGroupForImport(group: TodoGroup): Long

    @Update
    suspend fun updateTodoGroup(group: TodoGroup)

    @Delete
    suspend fun deleteTodoGroup(group: TodoGroup)

    @Query("SELECT * FROM todo_groups ORDER BY sortOrder ASC, name COLLATE NOCASE ASC, id ASC")
    fun getTodoGroups(): Flow<List<TodoGroup>>

    @Query("SELECT * FROM todo_groups ORDER BY sortOrder ASC, name COLLATE NOCASE ASC, id ASC")
    suspend fun getAllTodoGroupsSnapshot(): List<TodoGroup>

    @Query("SELECT * FROM todo_groups WHERE name = :name LIMIT 1")
    suspend fun getTodoGroupByNameSnapshot(name: String): TodoGroup?

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM todo_groups")
    suspend fun getNextTodoGroupSortOrder(): Int

    @Query("UPDATE todo_items SET groupId = NULL WHERE groupId = :groupId")
    suspend fun clearTodoGroupId(groupId: Int)

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

    @Transaction
    @Query("SELECT * FROM todo_items WHERE dueDate = :date ORDER BY creationDateTime DESC")
    fun getTodosByDate(date: LocalDate): Flow<List<TodoItemWithSubTodos>>

    @Query("SELECT DISTINCT dueDate FROM todo_items WHERE dueDate IS NOT NULL")
    fun getDatesWithTodo(): Flow<List<LocalDate>>

    // Fetches a single todo item with its sub-todos by ID.
    @Transaction
    @Query("SELECT * FROM todo_items WHERE id = :id")
    fun getTodoWithSubTodosById(id: Int): Flow<TodoItemWithSubTodos?>

    @Transaction
    @Query("SELECT * FROM todo_items WHERE id = :id")
    suspend fun getTodoWithSubTodosByIdSnapshot(id: Int): TodoItemWithSubTodos?

    @Transaction
    @Query("SELECT * FROM todo_items WHERE id IN (:ids)")
    suspend fun getTodosWithSubTodosByIds(ids: List<Int>): List<TodoItemWithSubTodos>

    @Transaction
    @Query("SELECT * FROM todo_items WHERE id IN (:ids)")
    fun getTodosWithSubTodosByIdsFlow(ids: List<Int>): Flow<List<TodoItemWithSubTodos>>

    @Query("SELECT * FROM repeat_rules WHERE id = :id")
    suspend fun getRepeatRuleByIdSnapshot(id: String): RepeatRule?

    @Query("SELECT * FROM repeat_rules WHERE id = :id")
    fun getRepeatRuleById(id: String): Flow<RepeatRule?>

    @Query("SELECT * FROM repeat_rules ORDER BY id ASC")
    suspend fun getAllRepeatRulesSnapshot(): List<RepeatRule>

    @Query("SELECT * FROM todo_items WHERE seriesId = :seriesId AND occurrenceDate = :occurrenceDate LIMIT 1")
    suspend fun getTodoBySeriesOccurrenceSnapshot(
        seriesId: String,
        occurrenceDate: LocalDate
    ): TodoItem?

    @Query("SELECT COUNT(*) FROM todo_items WHERE seriesId = :seriesId")
    suspend fun countTodosBySeriesIdSnapshot(seriesId: String): Int

    @Query("SELECT * FROM todo_items WHERE generatedFromTodoId = :todoId ORDER BY occurrenceDate ASC LIMIT 1")
    suspend fun getGeneratedTodoFromSnapshot(todoId: Int): TodoItem?

    @Query("DELETE FROM todo_items WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM todo_items WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Int>)

    @Query(
        """
        UPDATE todo_items
        SET calendarEventId = :calendarEventId,
            calendarSyncedAt = :calendarSyncedAt
        WHERE id = :id
        """
    )
    suspend fun updateCalendarSyncState(
        id: Int,
        calendarEventId: Long?,
        calendarSyncedAt: LocalDateTime?
    )

    @Query(
        """
        UPDATE todo_items
        SET calendarEventId = NULL,
            calendarSyncedAt = NULL
        WHERE id = :id
        """
    )
    suspend fun clearCalendarSyncState(id: Int)

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
        SET isCompleted = 1,
            completedDateTime = COALESCE(completedDateTime, :completedDateTime)
        WHERE id = :id
        """
    )
    suspend fun markCompletedById(id: Int, completedDateTime: LocalDateTime)

    @Query(
        """
        UPDATE todo_items
        SET flag = :flag,
            occurrenceEditedAt = CASE
                WHEN generatedFromTodoId IS NOT NULL THEN COALESCE(occurrenceEditedAt, :editedAt)
                ELSE occurrenceEditedAt
            END
        WHERE id IN (:ids)
        """
    )
    suspend fun updateFlagByIds(ids: List<Int>, flag: Int, editedAt: LocalDateTime)

    @Query(
        """
        UPDATE todo_items
        SET occurrenceEditedAt = COALESCE(occurrenceEditedAt, :editedAt)
        WHERE id = :id AND generatedFromTodoId IS NOT NULL
        """
    )
    suspend fun markOccurrenceEdited(id: Int, editedAt: LocalDateTime)

    @Query("SELECT COUNT(*) FROM todo_items")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM todo_items WHERE isCompleted = true")
    fun getCompletedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM todo_items WHERE dueDate = :date")
    fun getTodoCountByDueDate(date: LocalDate): Flow<Int>

    @Query("SELECT COUNT(*) FROM todo_items WHERE dueDate = :date AND isCompleted = 1")
    fun getCompletedTodoCountByDueDate(date: LocalDate): Flow<Int>

    @Query("SELECT COUNT(*) FROM todo_items WHERE dueDate >= :startDate AND dueDate <= :endDate")
    fun getTodoCountByDueDateRange(startDate: LocalDate, endDate: LocalDate): Flow<Int>

    @Query("SELECT COUNT(*) FROM todo_items WHERE dueDate >= :startDate AND dueDate <= :endDate AND isCompleted = 1")
    fun getCompletedTodoCountByDueDateRange(startDate: LocalDate, endDate: LocalDate): Flow<Int>

    @Query("SELECT COUNT(*) FROM todo_items WHERE isCompleted = 0")
    fun getPendingTodoCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM todo_items WHERE dueDate < :today AND isCompleted = 0")
    fun getOverdueTodoCount(today: LocalDate): Flow<Int>

    @Query(
        """
        SELECT COUNT(*) FROM todo_items
        WHERE isCompleted = 0
            AND COALESCE(dueDate, substr(creationDateTime, 1, 10)) < :thresholdDate
    """
    )
    fun getStalePendingTodoCount(thresholdDate: LocalDate): Flow<Int>

    @Query(
        """
        SELECT MIN(COALESCE(dueDate, substr(creationDateTime, 1, 10)))
        FROM todo_items
        WHERE isCompleted = 0
    """
    )
    fun getOldestPendingReferenceDate(): Flow<LocalDate?>

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

    @Query(
        """
        SELECT substr(completedDateTime, 1, 10) as date, COUNT(*) as count
        FROM todo_items
        WHERE isCompleted = 1
            AND completedDateTime IS NOT NULL
            AND completedDateTime >= :startDateTime
            AND completedDateTime < :endDateTime
        GROUP BY substr(completedDateTime, 1, 10)
        ORDER BY date ASC
    """
    )
    fun getCompletedStatsBetween(
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime
    ): Flow<List<DailyStat>>


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

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRepeatRuleForImport(rule: RepeatRule)

    @Query("SELECT * FROM todo_items ORDER BY id ASC")
    suspend fun getAllTodosSnapshot(): List<TodoItem>

    @Query("SELECT * FROM todo_items WHERE isCompleted = 0 AND reminderMode IS NOT NULL")
    suspend fun getReminderTodosSnapshot(): List<TodoItem>

    @Transaction
    @Query("SELECT * FROM todo_items ORDER BY id ASC")
    suspend fun getAllTodosWithSubTodosSnapshot(): List<TodoItemWithSubTodos>

    @Query("SELECT * FROM sub_todo_items ORDER BY id ASC")
    suspend fun getAllSubTodosSnapshot(): List<SubTodoItem>

    @Transaction
    @Query(
        """
        SELECT * FROM todo_items
        WHERE (:query = '' OR title LIKE '%' || :query || '%' COLLATE NOCASE)
        ORDER BY id DESC
        """
    )
    fun searchTodosWithSubTodos(query: String): Flow<List<TodoItemWithSubTodos>>
}
