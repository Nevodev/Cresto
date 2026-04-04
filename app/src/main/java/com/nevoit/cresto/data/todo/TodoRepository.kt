package com.nevoit.cresto.data.todo

import androidx.room.withTransaction
import com.nevoit.cresto.data.statistics.DailyStat
import com.nevoit.cresto.data.todo.backup.SubTodoBackupDto
import com.nevoit.cresto.data.todo.backup.TodoBackupDto
import com.nevoit.cresto.data.todo.backup.TodoBackupFile
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
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
    private val todoDatabase: TodoDatabase
) {

    val allTodos: Flow<List<TodoItemWithSubTodos>> = todoDao.getAllTodosWithSubTodos()

    fun getTodoById(id: Int): Flow<TodoItemWithSubTodos?> {
        return todoDao.getTodoWithSubTodosById(id)
    }

    suspend fun insert(item: TodoItem) {
        todoDao.insertTodo(item)
    }

    suspend fun insertAll(items: List<TodoItem>) {
        todoDao.insertAll(items)
    }

    suspend fun update(item: TodoItem) {
        todoDao.updateTodo(item)
    }

    suspend fun delete(item: TodoItem) {
        todoDao.deleteTodo(item)
    }

    suspend fun insertSubTodo(item: SubTodoItem) {
        todoDao.insertSubTodo(item)
    }

    suspend fun updateSubTodo(item: SubTodoItem) {
        todoDao.updateSubTodo(item)
    }

    suspend fun deleteSubTodo(item: SubTodoItem) {
        todoDao.deleteSubTodo(item)
    }

    suspend fun deleteById(id: Int) {
        todoDao.deleteById(id)
    }

    suspend fun deleteByIds(ids: List<Int>) {
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

    suspend fun duplicateByIds(ids: List<Int>): Int {
        if (ids.isEmpty()) return 0

        return todoDatabase.withTransaction {
            val sourceTodosById = todoDao.getTodosWithSubTodosByIds(ids)
                .associateBy { it.todoItem.id }
            val orderedSourceTodos = ids.mapNotNull(sourceTodosById::get).asReversed()
            if (orderedSourceTodos.isEmpty()) return@withTransaction 0

            val now = LocalDateTime.now()
            val todoCopies = orderedSourceTodos.mapIndexed { index, source ->
                source.todoItem.copy(
                    id = 0,
                    creationDateTime = now.plusNanos(index * 1000000L),
                    isCompleted = false,
                    completedDateTime = null
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

            newTodoIds.size
        }
    }

    suspend fun mergeByIdsAsSubTodos(ids: List<Int>, newTodoTitle: String): Int {
        if (ids.isEmpty()) return 0

        return todoDatabase.withTransaction {
            val sourceTodosById = todoDao.getTodosWithSubTodosByIds(ids)
                .associateBy { it.todoItem.id }
            val orderedSourceTodos = ids.mapNotNull(sourceTodosById::get)
            if (orderedSourceTodos.isEmpty()) return@withTransaction 0

            val newTodoId = todoDao.insertTodoForMerge(
                TodoItem(
                    id = 0,
                    title = newTodoTitle,
                    creationDateTime = LocalDateTime.now()
                )
            ).toInt()

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

    suspend fun deleteAll() {
        todoDao.deleteAllTodos()
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
                    completedDateTime = it.completedDateTime?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
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
                    dueDate = todoDto.dueDate?.let(java.time.LocalDate::parse),
                    creationDateTime = LocalDateTime.parse(todoDto.creationDateTime),
                    isCompleted = todoDto.isCompleted,
                    flag = todoDto.flag,
                    completedDateTime = todoDto.completedDateTime?.let(LocalDateTime::parse)
                )
            ).toInt()

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
}