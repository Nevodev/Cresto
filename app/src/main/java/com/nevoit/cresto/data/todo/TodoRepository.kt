package com.nevoit.cresto.data.todo

import androidx.room.withTransaction
import androidx.sqlite.db.SimpleSQLiteQuery
import com.nevoit.cresto.data.statistics.DailyStat
import com.nevoit.cresto.data.todo.backup.RepeatRuleBackupDto
import com.nevoit.cresto.data.todo.backup.SubTodoBackupDto
import com.nevoit.cresto.data.todo.backup.TodoBackupDto
import com.nevoit.cresto.data.todo.backup.TodoBackupFile
import com.nevoit.cresto.data.todo.calendar.CalendarSyncResult
import com.nevoit.cresto.data.todo.calendar.CalendarSyncStatus
import com.nevoit.cresto.data.todo.calendar.CalendarSyncSummary
import com.nevoit.cresto.data.todo.calendar.TodoCalendarSyncManager
import com.nevoit.cresto.feature.settings.util.SettingsManager
import com.nevoit.cresto.feature.settings.util.SortOption
import com.nevoit.cresto.feature.settings.util.SortOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

enum class DuplicatePolicy {
    SKIP_DUPLICATES,
    IMPORT_ALL
}

data class ImportResult(
    val total: Int,
    val imported: Int,
    val skipped: Int
)

data class RepeatCompletionResult(
    val updatedTodos: List<TodoItem> = emptyList(),
    val insertedTodos: List<TodoItem> = emptyList(),
    val deletedTodos: List<TodoItem> = emptyList()
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

    fun getHomeTodos(
        query: String,
        sortOption: SortOption,
        sortOrder: SortOrder,
        limit: Int
    ): Flow<List<TodoItemWithSubTodos>> {
        val searchQuery = query.trim()
        return todoDao.getHomeTodosWithSubTodos(
            SimpleSQLiteQuery(
                buildHomeTodosSql(sortOption, sortOrder),
                arrayOf<Any>(
                    searchQuery,
                    searchQuery,
                    limit.coerceAtLeast(1)
                )
            )
        )
    }

    fun getHomeTodoCount(query: String): Flow<Int> {
        val searchQuery = query.trim()
        return todoDao.getHomeTodoCount(
            SimpleSQLiteQuery(
                HOME_TODO_COUNT_SQL,
                arrayOf<Any>(searchQuery, searchQuery)
            )
        )
    }

    fun getTodosByDate(date: LocalDate): Flow<List<TodoItemWithSubTodos>> {
        return todoDao.getTodosByDate(date)
    }

    fun getDatesWithTodo(): Flow<List<LocalDate>> {
        return todoDao.getDatesWithTodo()
    }

    fun getTodoById(id: Int): Flow<TodoItemWithSubTodos?> {
        return todoDao.getTodoWithSubTodosById(id)
    }

    fun getRepeatRuleById(id: String): Flow<RepeatRule?> {
        return todoDao.getRepeatRuleById(id)
    }

    suspend fun getTodoByIdSnapshot(id: Int): TodoItem? {
        return todoDao.getTodoWithSubTodosByIdSnapshot(id)?.todoItem
    }

    fun getTodosByIds(ids: List<Int>): Flow<List<TodoItemWithSubTodos>> {
        return todoDao.getTodosWithSubTodosByIdsFlow(ids)
    }

    suspend fun insert(
        item: TodoItem,
        repeatFrequency: RepeatFrequency? = null,
        repeatRuleConfig: RepeatRuleConfig? = null
    ): Long {
        val id = todoDatabase.withTransaction {
            val repeatConfig = repeatRuleConfig ?: repeatFrequency?.let { frequency ->
                RepeatRuleConfig(frequency = frequency)
            }
            if (repeatConfig == null) {
                return@withTransaction todoDao.insertTodo(item)
            }

            val occurrenceDate = item.dueDate ?: LocalDate.now()
            val seriesId = UUID.randomUUID().toString()
            val ruleId = UUID.randomUUID().toString()
            val rule = RepeatRule(
                id = ruleId,
                seriesId = seriesId,
                frequency = repeatConfig.frequency,
                interval = repeatConfig.interval.coerceAtLeast(1),
                weekdays = repeatConfig.weekdays
                    .takeIf { it.isNotEmpty() }
                    ?.joinToString(",") { it.name },
                monthDay = repeatConfig.monthDay,
                endDate = repeatConfig.endDate,
                maxOccurrences = repeatConfig.maxOccurrences,
                anchorDate = occurrenceDate
            )
            todoDao.insertRepeatRule(rule)
            todoDao.insertTodo(
                item.copy(
                    repeatRuleId = ruleId,
                    seriesId = seriesId,
                    occurrenceDate = occurrenceDate
                )
            )
        }
        syncTodoByIdIfAutoEnabled(id.toInt())
        return id
    }

    suspend fun update(item: TodoItem) {
        val existingCalendarState = todoDao.getTodoWithSubTodosByIdSnapshot(item.id)?.todoItem
        val occurrenceWasEdited = existingCalendarState?.generatedFromTodoId != null &&
                existingCalendarState.userEditableSignature() != item.userEditableSignature()
        val itemToPersist = item.copy(
            calendarEventId = item.calendarEventId ?: existingCalendarState?.calendarEventId,
            calendarSyncedAt = item.calendarSyncedAt ?: existingCalendarState?.calendarSyncedAt,
            occurrenceEditedAt = when {
                item.occurrenceEditedAt != null -> item.occurrenceEditedAt
                occurrenceWasEdited -> LocalDateTime.now()
                else -> existingCalendarState?.occurrenceEditedAt
            }
        )
        todoDao.updateTodo(itemToPersist)
        syncTodoByIdIfAutoEnabled(itemToPersist.id)
    }

    suspend fun updateRepeatRuleForTodo(item: TodoItem, config: RepeatRuleConfig?) {
        val updatedItem = todoDatabase.withTransaction {
            val existingRuleId = item.repeatRuleId
            if (config == null) {
                if (existingRuleId != null) todoDao.deleteRepeatRuleById(existingRuleId)
                val nextItem = item.copy(
                    repeatRuleId = null,
                    seriesId = null,
                    occurrenceDate = null,
                    generatedFromTodoId = null,
                    occurrenceEditedAt = null
                )
                todoDao.updateTodo(nextItem)
                return@withTransaction nextItem
            }

            val occurrenceDate = item.occurrenceDate ?: item.dueDate ?: LocalDate.now()
            val seriesId = item.seriesId ?: UUID.randomUUID().toString()
            val ruleId = existingRuleId ?: UUID.randomUUID().toString()
            val rule = RepeatRule(
                id = ruleId,
                seriesId = seriesId,
                frequency = config.frequency,
                interval = config.interval.coerceAtLeast(1),
                weekdays = config.weekdays
                    .takeIf { it.isNotEmpty() }
                    ?.joinToString(",") { it.name },
                monthDay = config.monthDay,
                endDate = config.endDate,
                maxOccurrences = config.maxOccurrences,
                anchorDate = occurrenceDate
            )
            if (existingRuleId == null) {
                todoDao.insertRepeatRule(rule)
            } else {
                todoDao.updateRepeatRule(rule)
            }
            val nextItem = item.copy(
                repeatRuleId = ruleId,
                seriesId = seriesId,
                occurrenceDate = occurrenceDate
            )
            todoDao.updateTodo(nextItem)
            nextItem
        }
        syncTodoByIdIfAutoEnabled(updatedItem.id)
    }

    suspend fun delete(item: TodoItem) {
        deleteCalendarEventIfPresent(item)
        todoDao.deleteTodo(item)
    }

    suspend fun insertSubTodo(item: SubTodoItem) {
        todoDao.insertSubTodo(item)
        todoDao.markOccurrenceEdited(item.parentId, LocalDateTime.now())
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
        todoDao.markOccurrenceEdited(item.parentId, LocalDateTime.now())
        syncTodoByIdIfAutoEnabled(item.parentId)
    }

    suspend fun deleteSubTodo(item: SubTodoItem) {
        todoDao.deleteSubTodo(item)
        todoDao.markOccurrenceEdited(item.parentId, LocalDateTime.now())
        syncTodoByIdIfAutoEnabled(item.parentId)
    }

    suspend fun deleteById(id: Int) {
        todoDao.getTodoWithSubTodosByIdSnapshot(id)
            ?.let { deleteCalendarEventIfPresent(it.todoItem) }
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
    ): RepeatCompletionResult {
        if (ids.isEmpty()) return RepeatCompletionResult()

        val resolvedCompletedDateTime = if (isCompleted) {
            completedDateTime ?: LocalDateTime.now()
        } else {
            null
        }
        val result = todoDatabase.withTransaction {
            val todosById = todoDao.getTodosWithSubTodosByIds(ids)
                .associateBy { it.todoItem.id }
            val orderedTodos = ids.mapNotNull(todosById::get)
            val result = RepeatCompletionResultBuilder()
            val simpleTodos = orderedTodos
                .filterNot { it.todoItem.requiresRepeatCompletionHandling() }
            val simpleIds = simpleTodos.map { it.todoItem.id }

            if (simpleIds.isNotEmpty()) {
                todoDao.updateCompletedStatusByIds(
                    ids = simpleIds,
                    isCompleted = isCompleted,
                    completedDateTime = resolvedCompletedDateTime
                )
            }

            orderedTodos.forEach { itemWithSubTodos ->
                val todo = itemWithSubTodos.todoItem
                if (!todo.requiresRepeatCompletionHandling()) {
                    result.updatedTodos += todo.copy(
                        isCompleted = isCompleted,
                        completedDateTime = if (isCompleted) {
                            todo.completedDateTime ?: resolvedCompletedDateTime
                        } else {
                            null
                        }
                    )
                } else {
                    if (isCompleted) {
                        completeTodoInTransaction(
                            itemWithSubTodos = itemWithSubTodos,
                            completedDateTime = resolvedCompletedDateTime ?: LocalDateTime.now(),
                            result = result
                        )
                    } else {
                        reopenTodoInTransaction(todo, result)
                    }
                }
            }

            result.build()
        }
        result.deletedTodos.forEach { deleteCalendarEventIfPresent(it) }
        syncTodoIdsIfAutoEnabled(result.insertedTodos.map { it.id })
        return result
    }

    suspend fun getCompletedCountByIds(ids: List<Int>): Int {
        return todoDao.getCompletedCountByIds(ids)
    }

    suspend fun updateFlagByIds(ids: List<Int>, flag: Int) {
        todoDao.updateFlagByIds(ids, flag, LocalDateTime.now())
    }

    suspend fun markCompletedById(
        id: Int,
        completedDateTime: LocalDateTime
    ): RepeatCompletionResult {
        return updateCompletedStatusByIds(listOf(id), true, completedDateTime)
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
                    calendarSyncedAt = null,
                    repeatRuleId = null,
                    seriesId = null,
                    occurrenceDate = null,
                    generatedFromTodoId = null,
                    occurrenceEditedAt = null
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
        todoDao.deleteAllRepeatRules()
    }

    private class RepeatCompletionResultBuilder {
        val updatedTodos = mutableListOf<TodoItem>()
        val insertedTodos = mutableListOf<TodoItem>()
        val deletedTodos = mutableListOf<TodoItem>()

        fun build(): RepeatCompletionResult {
            return RepeatCompletionResult(
                updatedTodos = updatedTodos,
                insertedTodos = insertedTodos,
                deletedTodos = deletedTodos
            )
        }
    }

    private suspend fun completeTodoInTransaction(
        itemWithSubTodos: TodoItemWithSubTodos,
        completedDateTime: LocalDateTime,
        result: RepeatCompletionResultBuilder
    ) {
        val todo = itemWithSubTodos.todoItem
        val completedTodo = todo.copy(
            isCompleted = true,
            completedDateTime = todo.completedDateTime ?: completedDateTime
        )
        todoDao.updateTodo(completedTodo)
        result.updatedTodos += completedTodo

        val ruleId = completedTodo.repeatRuleId ?: return
        val seriesId = completedTodo.seriesId ?: return
        val occurrenceDate = completedTodo.occurrenceDate ?: completedTodo.dueDate ?: return
        val rule = todoDao.getRepeatRuleByIdSnapshot(ruleId) ?: return
        val occurrenceCount = todoDao.countTodosBySeriesIdSnapshot(seriesId)
        val nextDate = rule.nextOccurrence(occurrenceDate, occurrenceCount) ?: return
        if (todoDao.getTodoBySeriesOccurrenceSnapshot(seriesId, nextDate) != null) return

        val nextTodo = completedTodo.copy(
            id = 0,
            dueDate = if (completedTodo.dueDate != null) nextDate else null,
            creationDateTime = LocalDateTime.now(),
            isCompleted = false,
            completedDateTime = null,
            calendarEventId = null,
            calendarSyncedAt = null,
            occurrenceDate = nextDate,
            generatedFromTodoId = completedTodo.id,
            occurrenceEditedAt = null
        )
        val nextTodoId = todoDao.insertTodo(nextTodo).toInt()
        itemWithSubTodos.subTodos.forEach { subTodo ->
            todoDao.insertSubTodoForImport(
                subTodo.copy(
                    id = 0,
                    parentId = nextTodoId,
                    isCompleted = false
                )
            )
        }
        result.insertedTodos += nextTodo.copy(id = nextTodoId)
    }

    private suspend fun reopenTodoInTransaction(
        todo: TodoItem,
        result: RepeatCompletionResultBuilder
    ) {
        val reopenedTodo = todo.copy(
            isCompleted = false,
            completedDateTime = null
        )
        todoDao.updateTodo(reopenedTodo)
        result.updatedTodos += reopenedTodo

        val generatedTodo = todoDao.getGeneratedTodoFromSnapshot(todo.id) ?: return
        if (!generatedTodo.canDeleteOnReopen()) return
        todoDao.deleteById(generatedTodo.id)
        result.deletedTodos += generatedTodo
    }

    private fun TodoItem.canDeleteOnReopen(): Boolean {
        return !isCompleted && occurrenceEditedAt == null
    }

    private fun TodoItem.requiresRepeatCompletionHandling(): Boolean {
        return repeatRuleId != null || seriesId != null
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
        val repeatRuleId: String?,
        val seriesId: String?,
        val occurrenceDate: String?,
        val generatedFromTodoId: Int?,
        val occurrenceEditedAt: String?,
        val subTodos: List<SubTodoFingerprint>
    )

    private val backupJson = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    suspend fun exportToJson(): String {
        val todos = todoDao.getAllTodosSnapshot()
        val subTodos = todoDao.getAllSubTodosSnapshot()
        val repeatRules = todoDao.getAllRepeatRulesSnapshot()

        val backup = TodoBackupFile(
            schemaVersion = 2,
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
                    reminderStrong = it.reminderStrong,
                    repeatRuleId = it.repeatRuleId,
                    seriesId = it.seriesId,
                    occurrenceDate = it.occurrenceDate?.toString(),
                    generatedFromTodoId = it.generatedFromTodoId,
                    occurrenceEditedAt = it.occurrenceEditedAt?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                )
            },
            subTodos = subTodos.map {
                SubTodoBackupDto(
                    id = it.id,
                    parentId = it.parentId,
                    description = it.description,
                    isCompleted = it.isCompleted
                )
            },
            repeatRules = repeatRules.map {
                RepeatRuleBackupDto(
                    id = it.id,
                    seriesId = it.seriesId,
                    frequency = it.frequency.name,
                    interval = it.interval,
                    weekdays = it.weekdays,
                    monthDay = it.monthDay,
                    endDate = it.endDate?.toString(),
                    maxOccurrences = it.maxOccurrences,
                    anchorDate = it.anchorDate.toString(),
                    createNextOnCompletion = it.createNextOnCompletion
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
        val repeatRuleIdMap = backup.repeatRules.associate { it.id to UUID.randomUUID().toString() }
        val seriesIdMap =
            backup.repeatRules.associate { it.seriesId to UUID.randomUUID().toString() }
        val importedRuleIds = mutableSetOf<String>()
        val todoIdMap = mutableMapOf<Int, Int>()

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

            val mappedRepeatRuleId = todoDto.repeatRuleId?.let(repeatRuleIdMap::get)
            val mappedSeriesId = todoDto.seriesId?.let(seriesIdMap::get)
            if (todoDto.repeatRuleId != null && mappedRepeatRuleId != null && mappedRepeatRuleId !in importedRuleIds) {
                backup.repeatRules.firstOrNull { it.id == todoDto.repeatRuleId }?.let { ruleDto ->
                    todoDao.insertRepeatRuleForImport(
                        ruleDto.toRepeatRule(
                            mappedRepeatRuleId,
                            mappedSeriesId ?: UUID.randomUUID().toString()
                        )
                    )
                    importedRuleIds += mappedRepeatRuleId
                }
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
                    reminderStrong = todoDto.reminderStrong,
                    repeatRuleId = mappedRepeatRuleId,
                    seriesId = mappedSeriesId,
                    occurrenceDate = todoDto.occurrenceDate?.let(LocalDate::parse),
                    generatedFromTodoId = todoDto.generatedFromTodoId?.let(todoIdMap::get),
                    occurrenceEditedAt = todoDto.occurrenceEditedAt?.let(LocalDateTime::parse)
                )
            ).toInt()
            importedTodoIds += newTodoId
            todoIdMap[todoDto.id] = newTodoId

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
            repeatRuleId = todoItem.repeatRuleId,
            seriesId = todoItem.seriesId,
            occurrenceDate = todoItem.occurrenceDate?.toString(),
            generatedFromTodoId = todoItem.generatedFromTodoId,
            occurrenceEditedAt = todoItem.occurrenceEditedAt?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
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
            repeatRuleId = todo.repeatRuleId,
            seriesId = todo.seriesId,
            occurrenceDate = todo.occurrenceDate,
            generatedFromTodoId = todo.generatedFromTodoId,
            occurrenceEditedAt = todo.occurrenceEditedAt,
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

    private fun buildHomeTodosSql(
        sortOption: SortOption,
        sortOrder: SortOrder
    ): String {
        val direction = if (sortOrder == SortOrder.ASCENDING) "ASC" else "DESC"
        val baseDateExpr =
            "CASE WHEN isCompleted = 0 THEN creationDateTime ELSE completedDateTime END"
        val baseDateFallbackClauses = listOf(
            "CASE WHEN $baseDateExpr IS NULL THEN 1 ELSE 0 END ASC",
            "$baseDateExpr $direction",
            "id $direction"
        )
        val orderClauses = when (sortOption) {
            SortOption.DEFAULT -> baseDateFallbackClauses
            SortOption.DUE_DATE -> listOf(
                "CASE WHEN dueDate IS NULL THEN 1 ELSE 0 END ASC",
                "dueDate $direction"
            ) + baseDateFallbackClauses

            SortOption.FLAG -> {
                val flagDirection = if (sortOrder == SortOrder.ASCENDING) "DESC" else "ASC"
                listOf(
                    "CASE WHEN flag = 0 THEN 1 ELSE 0 END ASC",
                    "flag $flagDirection"
                ) + baseDateFallbackClauses
            }

            SortOption.TITLE -> listOf(
                "title COLLATE NOCASE $direction"
            ) + baseDateFallbackClauses
        }

        return """
            SELECT * FROM todo_items
            WHERE (? = '' OR title LIKE '%' || ? || '%' COLLATE NOCASE)
            ORDER BY isCompleted ASC, ${orderClauses.joinToString(", ")}
            LIMIT ?
        """.trimIndent()
    }

    private fun RepeatRuleBackupDto.toRepeatRule(newId: String, newSeriesId: String): RepeatRule {
        return RepeatRule(
            id = newId,
            seriesId = newSeriesId,
            frequency = RepeatFrequency.valueOf(frequency),
            interval = interval,
            weekdays = weekdays,
            monthDay = monthDay,
            endDate = endDate?.let(LocalDate::parse),
            maxOccurrences = maxOccurrences,
            anchorDate = LocalDate.parse(anchorDate),
            createNextOnCompletion = createNextOnCompletion
        )
    }

    private data class TodoEditableSignature(
        val title: String,
        val dueDate: LocalDate?,
        val flag: Int,
        val notes: String,
        val startTime: LocalTime?,
        val endTime: LocalTime?,
        val reminderMode: TodoReminderMode?,
        val reminderOffsetMinutes: Int?,
        val reminderDayOffset: Int?,
        val reminderTime: LocalTime?,
        val reminderPersistent: Boolean,
        val reminderStrong: Boolean
    )

    private fun TodoItem.userEditableSignature(): TodoEditableSignature {
        return TodoEditableSignature(
            title = title,
            dueDate = dueDate,
            flag = flag,
            notes = notes,
            startTime = startTime,
            endTime = endTime,
            reminderMode = reminderMode,
            reminderOffsetMinutes = reminderOffsetMinutes,
            reminderDayOffset = reminderDayOffset,
            reminderTime = reminderTime,
            reminderPersistent = reminderPersistent,
            reminderStrong = reminderStrong
        )
    }

    private companion object {
        private val HOME_TODO_COUNT_SQL = """
            SELECT COUNT(*) FROM todo_items
            WHERE (? = '' OR title LIKE '%' || ? || '%' COLLATE NOCASE)
        """.trimIndent()
    }
}
