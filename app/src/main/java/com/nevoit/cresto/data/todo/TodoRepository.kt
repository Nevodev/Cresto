package com.nevoit.cresto.data.todo

import androidx.room.withTransaction
import com.nevoit.cresto.data.statistics.DailyStat
import com.nevoit.cresto.data.todo.backup.SubTodoBackupDto
import com.nevoit.cresto.data.todo.backup.TodoBackupDto
import com.nevoit.cresto.data.todo.backup.TodoBackupFile
import com.nevoit.cresto.data.todo.calendar.CalendarSyncResult
import com.nevoit.cresto.data.todo.calendar.CalendarSyncStatus
import com.nevoit.cresto.data.todo.calendar.CalendarSyncSummary
import com.nevoit.cresto.data.todo.calendar.TodoCalendarSyncManager
import com.nevoit.cresto.feature.settings.util.SettingsManager
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class DuplicatePolicy {
    SKIP_DUPLICATES,
    IMPORT_ALL
}

data class ImportResult(
    val total: Int,
    val imported: Int,
    val skipped: Int
)

/**
 * A repository that provides a single source of truth for all to-do data.
 * It abstracts the data source (in this case, a Room database) from the rest of the app.
 *
 * @param todoDao The Data Access Object for the to-do items.
 */
class TodoRepository(
    private val todoDao: TodoDao,
    private val todoDatabase: TodoDatabase,
    private val calendarSyncManager: TodoCalendarSyncManager
) {

    val allTodos: Flow<List<TodoItemWithSubTodos>> = todoDao.getAllTodosWithSubTodos()

    fun getTodosByDate(date: LocalDate): Flow<List<TodoItemWithSubTodos>> {
        return todoDao.getTodosByDate(date)
    }

    fun getDatesWithTodo(): Flow<List<LocalDate>> {
        return todoDao.getDatesWithTodo()
    }

    fun getTodoById(id: Int): Flow<TodoItemWithSubTodos?> {
        return todoDao.getTodoWithSubTodosById(id)
    }

    suspend fun insert(item: TodoItem): Long {
        val id = todoDao.insertTodo(item)
        syncTodoByIdIfAutoEnabled(id.toInt())
        return id
    }

    suspend fun insertAll(items: List<TodoItem>) {
        todoDao.insertAll(items)
    }

    suspend fun update(item: TodoItem) {
        val existingCalendarState = todoDao.getTodoWithSubTodosByIdSnapshot(item.id)?.todoItem
        val itemToPersist = item.copy(
            calendarEventId = item.calendarEventId ?: existingCalendarState?.calendarEventId,
            calendarSyncedAt = item.calendarSyncedAt ?: existingCalendarState?.calendarSyncedAt
        )
        todoDao.updateTodo(itemToPersist)
        syncTodoByIdIfAutoEnabled(itemToPersist.id)
    }

    suspend fun delete(item: TodoItem) {
        deleteCalendarEventIfPresent(item)
        todoDao.deleteTodo(item)
    }

    suspend fun insertSubTodo(item: SubTodoItem) {
        todoDao.insertSubTodo(item)
        syncTodoByIdIfAutoEnabled(item.parentId)
    }

    suspend fun insertAiGeneratedTodosWithSubTasks(aiItems: List<com.nevoit.cresto.data.utils.EventItem>): List<TodoItem> {
        if (aiItems.isEmpty()) return emptyList()

        val insertedTodos = todoDatabase.withTransaction {
            aiItems.map { eventItem ->
                val todo = TodoItem(
                    title = eventItem.title,
                    isCompleted = eventItem.isCompleted,
                    completedDateTime = if (eventItem.isCompleted) LocalDateTime.now() else null,
                    dueDate = try {
                        LocalDate.parse(eventItem.date, DateTimeFormatter.ISO_LOCAL_DATE)
                    } catch (_: Exception) {
                        LocalDate.now()
                    },
                    startTime = eventItem.startTime?.let {
                        try {
                            LocalTime.parse(it, DateTimeFormatter.ofPattern("HH:mm"))
                        } catch (_: Exception) {
                            null
                        }
                    },
                    endTime = eventItem.endTime?.let {
                        try {
                            LocalTime.parse(it, DateTimeFormatter.ofPattern("HH:mm"))
                        } catch (_: Exception) {
                            null
                        }
                    },
                    reminderMode = eventItem.reminderMode?.let {
                        try {
                            TodoReminderMode.valueOf(it)
                        } catch (_: Exception) {
                            null
                        }
                    },
                    reminderOffsetMinutes = eventItem.reminderOffsetMinutes,
                    reminderDayOffset = eventItem.reminderDayOffset,
                    reminderTime = eventItem.reminderTime?.let {
                        try {
                            LocalTime.parse(it, DateTimeFormatter.ofPattern("HH:mm"))
                        } catch (_: Exception) {
                            null
                        }
                    }
                )
                val insertedTodo = todo.copy(
                    id = todoDao.insertTodoForImport(todo).toInt()
                )

                val subTodos = eventItem.subTasks
                    .map(String::trim)
                    .filter(String::isNotEmpty)
                    .distinct()
                    .map { subTitle ->
                        SubTodoItem(
                            parentId = insertedTodo.id,
                            description = subTitle,
                            isCompleted = eventItem.isCompleted
                        )
                    }

                subTodos.forEach { subTodo ->
                    todoDao.insertSubTodoForImport(subTodo)
                }

                insertedTodo
            }
        }

        syncTodoIdsIfAutoEnabled(insertedTodos.map { it.id })
        return insertedTodos
    }

    suspend fun updateSubTodo(item: SubTodoItem) {
        todoDao.updateSubTodo(item)
        syncTodoByIdIfAutoEnabled(item.parentId)
    }

    suspend fun deleteSubTodo(item: SubTodoItem) {
        todoDao.deleteSubTodo(item)
        syncTodoByIdIfAutoEnabled(item.parentId)
    }

    suspend fun deleteById(id: Int) {
        todoDao.getTodoWithSubTodosByIdSnapshot(id)?.let { deleteCalendarEventIfPresent(it.todoItem) }
        todoDao.deleteById(id)
    }

    suspend fun deleteByIds(ids: List<Int>) {
        todoDao.getTodosWithSubTodosByIds(ids)
            .map { it.todoItem }
            .forEach { deleteCalendarEventIfPresent(it) }
        todoDao.deleteByIds(ids)
    }

    suspend fun updateCompletedStatusByIds(
        ids: List<Int>,
        isCompleted: Boolean,
        completedDateTime: LocalDateTime?
    ) {
        todoDao.updateCompletedStatusByIds(ids, isCompleted, completedDateTime)
    }

    suspend fun getCompletedCountByIds(ids: List<Int>): Int {
        return todoDao.getCompletedCountByIds(ids)
    }

    suspend fun updateFlagByIds(ids: List<Int>, flag: Int) {
        todoDao.updateFlagByIds(ids, flag)
    }

    suspend fun duplicateByIds(ids: List<Int>): List<TodoItem> {
        if (ids.isEmpty()) return emptyList()

        val insertedTodos = todoDatabase.withTransaction {
            val sourceTodosById = todoDao.getTodosWithSubTodosByIds(ids)
                .associateBy { it.todoItem.id }
            val orderedSourceTodos = ids.mapNotNull(sourceTodosById::get).asReversed()
            if (orderedSourceTodos.isEmpty()) return@withTransaction emptyList()

            val now = LocalDateTime.now()
            val todoCopies = orderedSourceTodos.mapIndexed { index, source ->
                source.todoItem.copy(
                    id = 0,
                    creationDateTime = now.plusNanos(index * 1000000L),
                    isCompleted = false,
                    completedDateTime = null,
                    calendarEventId = null,
                    calendarSyncedAt = null
                )
            }

            val newTodoIds = todoDao.insertTodosForDuplicate(todoCopies)
                .map(Long::toInt)

            val subTodoCopies =
                orderedSourceTodos.zip(newTodoIds).flatMap { (source, newTodoId) ->
                    source.subTodos.map { subTodo ->
                        subTodo.copy(
                            id = 0,
                            parentId = newTodoId
                        )
                    }
                }

            if (subTodoCopies.isNotEmpty()) {
                todoDao.insertSubTodosForDuplicate(subTodoCopies)
            }

            todoCopies.zip(newTodoIds).map { (todo, newTodoId) ->
                todo.copy(id = newTodoId)
            }
        }

        syncTodoIdsIfAutoEnabled(insertedTodos.map { it.id })
        return insertedTodos
    }

    suspend fun mergeByIdsAsSubTodos(ids: List<Int>, newTodoTitle: String): Int {
        if (ids.isEmpty()) return 0

        var sourceTodosForCalendar = emptyList<TodoItem>()
        var mergedTodoId = 0
        val mergedSubTodoCount = todoDatabase.withTransaction {
            val sourceTodosById = todoDao.getTodosWithSubTodosByIds(ids)
                .associateBy { it.todoItem.id }
            val orderedSourceTodos = ids.mapNotNull(sourceTodosById::get)
            if (orderedSourceTodos.isEmpty()) return@withTransaction 0
            sourceTodosForCalendar = orderedSourceTodos.map { it.todoItem }

            val latestDueDate = orderedSourceTodos
                .mapNotNull { it.todoItem.dueDate }
                .maxOrNull()

            val newTodoId = todoDao.insertTodoForMerge(
                TodoItem(
                    id = 0,
                    title = newTodoTitle,
                    creationDateTime = LocalDateTime.now(),
                    dueDate = latestDueDate
                )
            ).toInt()
            mergedTodoId = newTodoId

            val mergedSubTodos = orderedSourceTodos.flatMap { source ->
                buildList {
                    add(
                        SubTodoItem(
                            id = 0,
                            parentId = newTodoId,
                            description = source.todoItem.title,
                            isCompleted = source.todoItem.isCompleted
                        )
                    )
                    addAll(
                        source.subTodos.map { subTodo ->
                            subTodo.copy(
                                id = 0,
                                parentId = newTodoId
                            )
                        }
                    )
                }
            }

            if (mergedSubTodos.isNotEmpty()) {
                todoDao.insertSubTodosForMerge(mergedSubTodos)
            }

            todoDao.deleteByIds(orderedSourceTodos.map { it.todoItem.id })

            mergedSubTodos.size
        }

        sourceTodosForCalendar.forEach { deleteCalendarEventIfPresent(it) }
        syncTodoByIdIfAutoEnabled(mergedTodoId)
        return mergedSubTodoCount
    }

    fun getTotalCount(): Flow<Int> {
        return todoDao.getTotalCount()
    }

    fun getCompletedCount(): Flow<Int> {
        return todoDao.getCompletedCount()
    }

    fun getDailyStatistics(): Flow<List<DailyStat>> {
        return todoDao.getDailyStats()
    }

    fun getTodoCountByDueDate(date: LocalDate): Flow<Int> {
        return todoDao.getTodoCountByDueDate(date)
    }

    fun getCompletedTodoCountByDueDate(date: LocalDate): Flow<Int> {
        return todoDao.getCompletedTodoCountByDueDate(date)
    }

    fun getTodoCountByDueDateRange(startDate: LocalDate, endDate: LocalDate): Flow<Int> {
        return todoDao.getTodoCountByDueDateRange(startDate, endDate)
    }

    fun getCompletedTodoCountByDueDateRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<Int> {
        return todoDao.getCompletedTodoCountByDueDateRange(startDate, endDate)
    }

    fun getPendingTodoCount(): Flow<Int> {
        return todoDao.getPendingTodoCount()
    }

    fun getOverdueTodoCount(today: LocalDate): Flow<Int> {
        return todoDao.getOverdueTodoCount(today)
    }

    fun getStalePendingTodoCount(thresholdDate: LocalDate): Flow<Int> {
        return todoDao.getStalePendingTodoCount(thresholdDate)
    }

    fun getOldestPendingReferenceDate(): Flow<LocalDate?> {
        return todoDao.getOldestPendingReferenceDate()
    }

    fun getCompletedStatisticsBetween(
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime
    ): Flow<List<DailyStat>> {
        return todoDao.getCompletedStatsBetween(startDateTime, endDateTime)
    }

    suspend fun deleteAll() {
        todoDao.getAllTodosSnapshot().forEach { deleteCalendarEventIfPresent(it) }
        todoDao.deleteAllTodos()
    }

    suspend fun getReminderTodosSnapshot(): List<TodoItem> {
        return todoDao.getReminderTodosSnapshot()
    }

    private data class SubTodoFingerprint(
        val description: String,
        val isCompleted: Boolean
    )

    private data class TodoFingerprint(
        val title: String,
        val dueDate: String?,
        val creationDateTime: String,
        val isCompleted: Boolean,
        val flag: Int,
        val completedDateTime: String?,
        val startTime: String?,
        val endTime: String?,
        val reminderMode: String?,
        val reminderOffsetMinutes: Int?,
        val reminderDayOffset: Int?,
        val reminderTime: String?,
        val reminderPersistent: Boolean,
        val reminderStrong: Boolean,
        val subTodos: List<SubTodoFingerprint>
    )

    private val backupJson = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    suspend fun exportToJson(): String {
        val todos = todoDao.getAllTodosSnapshot()
        val subTodos = todoDao.getAllSubTodosSnapshot()

        val backup = TodoBackupFile(
            schemaVersion = 1,
            exportedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            todos = todos.map {
                TodoBackupDto(
                    id = it.id,
                    title = it.title,
                    dueDate = it.dueDate?.toString(),
                    creationDateTime = it.creationDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    isCompleted = it.isCompleted,
                    flag = it.flag,
                    completedDateTime = it.completedDateTime?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    startTime = it.startTime?.format(DateTimeFormatter.ISO_LOCAL_TIME),
                    endTime = it.endTime?.format(DateTimeFormatter.ISO_LOCAL_TIME),
                    reminderMode = it.reminderMode?.name,
                    reminderOffsetMinutes = it.reminderOffsetMinutes,
                    reminderDayOffset = it.reminderDayOffset,
                    reminderTime = it.reminderTime?.format(DateTimeFormatter.ISO_LOCAL_TIME),
                    reminderPersistent = it.reminderPersistent,
                    reminderStrong = it.reminderStrong
                )
            },
            subTodos = subTodos.map {
                SubTodoBackupDto(
                    id = it.id,
                    parentId = it.parentId,
                    description = it.description,
                    isCompleted = it.isCompleted
                )
            }
        )

        return backupJson.encodeToString(backup)
    }

    suspend fun importFromJson(
        json: String,
        policy: DuplicatePolicy
    ): ImportResult {
        val backup = backupJson.decodeFromString<TodoBackupFile>(json)

        val subTodosByParent = backup.subTodos.groupBy { it.parentId }

        val existingFingerprints = todoDao.getAllTodosWithSubTodosSnapshot()
            .map { it.toFingerprint() }
            .toMutableSet()

        var imported = 0
        var skipped = 0
        val importedTodoIds = mutableListOf<Int>()

        for (todoDto in backup.todos) {
            val relatedSubDtos = subTodosByParent[todoDto.id].orEmpty()
            val fp = buildFingerprint(todoDto, relatedSubDtos)

            if (policy == DuplicatePolicy.SKIP_DUPLICATES && fp in existingFingerprints) {
                skipped++
                continue
            }

            val newTodoId = todoDao.insertTodoForImport(
                TodoItem(
                    id = 0, // auto-generate
                    title = todoDto.title,
                    dueDate = todoDto.dueDate?.let(LocalDate::parse),
                    creationDateTime = LocalDateTime.parse(todoDto.creationDateTime),
                    isCompleted = todoDto.isCompleted,
                    flag = todoDto.flag,
                    completedDateTime = todoDto.completedDateTime?.let(LocalDateTime::parse),
                    startTime = todoDto.startTime?.let(LocalTime::parse),
                    endTime = todoDto.endTime?.let(LocalTime::parse),
                    reminderMode = todoDto.reminderMode?.let(TodoReminderMode::valueOf),
                    reminderOffsetMinutes = todoDto.reminderOffsetMinutes,
                    reminderDayOffset = todoDto.reminderDayOffset,
                    reminderTime = todoDto.reminderTime?.let(LocalTime::parse),
                    reminderPersistent = todoDto.reminderPersistent,
                    reminderStrong = todoDto.reminderStrong
                )
            ).toInt()
            importedTodoIds += newTodoId

            relatedSubDtos.forEach { subDto ->
                todoDao.insertSubTodoForImport(
                    SubTodoItem(
                        id = 0,
                        parentId = newTodoId,
                        description = subDto.description,
                        isCompleted = subDto.isCompleted
                    )
                )
            }

            imported++
            if (policy == DuplicatePolicy.SKIP_DUPLICATES) {
                existingFingerprints.add(fp)
            }
        }

        syncTodoIdsIfAutoEnabled(importedTodoIds)

        return ImportResult(
            total = backup.todos.size,
            imported = imported,
            skipped = skipped
        )
    }

    private fun TodoItemWithSubTodos.toFingerprint(): TodoFingerprint {
        return TodoFingerprint(
            title = todoItem.title,
            dueDate = todoItem.dueDate?.toString(),
            creationDateTime = todoItem.creationDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            isCompleted = todoItem.isCompleted,
            flag = todoItem.flag,
            completedDateTime = todoItem.completedDateTime?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            startTime = todoItem.startTime?.format(DateTimeFormatter.ISO_LOCAL_TIME),
            endTime = todoItem.endTime?.format(DateTimeFormatter.ISO_LOCAL_TIME),
            reminderMode = todoItem.reminderMode?.name,
            reminderOffsetMinutes = todoItem.reminderOffsetMinutes,
            reminderDayOffset = todoItem.reminderDayOffset,
            reminderTime = todoItem.reminderTime?.format(DateTimeFormatter.ISO_LOCAL_TIME),
            reminderPersistent = todoItem.reminderPersistent,
            reminderStrong = todoItem.reminderStrong,
            subTodos = subTodos
                .map { SubTodoFingerprint(it.description, it.isCompleted) }
                .sortedWith(
                    compareBy(
                        SubTodoFingerprint::description,
                        SubTodoFingerprint::isCompleted
                    )
                )
        )
    }


    private fun buildFingerprint(
        todo: TodoBackupDto,
        subTodos: List<SubTodoBackupDto>
    ): TodoFingerprint {
        return TodoFingerprint(
            title = todo.title,
            dueDate = todo.dueDate,
            creationDateTime = todo.creationDateTime,
            isCompleted = todo.isCompleted,
            flag = todo.flag,
            completedDateTime = todo.completedDateTime,
            startTime = todo.startTime,
            endTime = todo.endTime,
            reminderMode = todo.reminderMode,
            reminderOffsetMinutes = todo.reminderOffsetMinutes,
            reminderDayOffset = todo.reminderDayOffset,
            reminderTime = todo.reminderTime,
            reminderPersistent = todo.reminderPersistent,
            reminderStrong = todo.reminderStrong,
            subTodos = subTodos
                .map { SubTodoFingerprint(it.description, it.isCompleted) }
                .sortedWith(
                    compareBy(
                        SubTodoFingerprint::description,
                        SubTodoFingerprint::isCompleted
                    )
                )
        )
    }

    data class ImportPreviewResult(
        val total: Int,
        val duplicate: Int,
        val unique: Int
    )

    suspend fun previewImportDuplicates(json: String): ImportPreviewResult {
        val backup = backupJson.decodeFromString<TodoBackupFile>(json)
        val subTodosByParent = backup.subTodos.groupBy { it.parentId }

        val existing = todoDao.getAllTodosWithSubTodosSnapshot()
            .map { it.toFingerprint() }
            .toMutableSet()

        var duplicateCount = 0
        var uniqueCount = 0

        val seenInThisBackup = mutableSetOf<TodoFingerprint>()

        for (todoDto in backup.todos) {
            val fp = buildFingerprint(todoDto, subTodosByParent[todoDto.id].orEmpty())

            val isDuplicate = fp in existing || fp in seenInThisBackup
            if (isDuplicate) {
                duplicateCount++
            } else {
                uniqueCount++
                seenInThisBackup.add(fp)
            }
        }

        return ImportPreviewResult(
            total = backup.todos.size,
            duplicate = duplicateCount,
            unique = uniqueCount
        )
    }

    fun searchTodos(query: String): Flow<List<TodoItemWithSubTodos>> {
        return todoDao.searchTodosWithSubTodos(query.trim())
    }

    suspend fun syncTodoToCalendar(todoId: Int): CalendarSyncResult {
        val todo = todoDao.getTodoWithSubTodosByIdSnapshot(todoId)
            ?: return CalendarSyncResult(todoId, CalendarSyncStatus.Failed)
        return syncTodoToCalendar(todo)
    }

    suspend fun syncTodosToCalendar(todoIds: List<Int>): CalendarSyncSummary {
        if (todoIds.isEmpty()) return CalendarSyncSummary.from(emptyList())

        val todosById = todoDao.getTodosWithSubTodosByIds(todoIds)
            .associateBy { it.todoItem.id }
        val results = todoIds
            .mapNotNull(todosById::get)
            .map { syncTodoToCalendar(it) }

        return CalendarSyncSummary.from(results)
    }

    private suspend fun syncTodoToCalendar(todo: TodoItemWithSubTodos): CalendarSyncResult {
        val result = calendarSyncManager.sync(todo)
        if (result.status == CalendarSyncStatus.Synced && result.calendarEventId != null) {
            todoDao.updateCalendarSyncState(
                id = todo.todoItem.id,
                calendarEventId = result.calendarEventId,
                calendarSyncedAt = LocalDateTime.now()
            )
        }
        return result
    }

    private suspend fun syncTodoByIdIfAutoEnabled(todoId: Int) {
        if (!SettingsManager.isAutoAddToSystemCalendar) return
        val todo = todoDao.getTodoWithSubTodosByIdSnapshot(todoId) ?: return
        if (todo.todoItem.dueDate == null) {
            deleteCalendarEventAndClearSyncState(todo.todoItem)
            return
        }
        syncTodoToCalendar(todo)
    }

    private suspend fun syncTodoIdsIfAutoEnabled(todoIds: List<Int>) {
        if (!SettingsManager.isAutoAddToSystemCalendar || todoIds.isEmpty()) return
        todoIds.forEach { syncTodoByIdIfAutoEnabled(it) }
    }

    private suspend fun deleteCalendarEventAndClearSyncState(todo: TodoItem) {
        if (todo.calendarEventId == null) return
        if (calendarSyncManager.deleteEvent(todo)) {
            todoDao.clearCalendarSyncState(todo.id)
        }
    }

    private suspend fun deleteCalendarEventIfPresent(todo: TodoItem) {
        if (todo.calendarEventId != null) {
            calendarSyncManager.deleteEvent(todo)
        }
    }
}