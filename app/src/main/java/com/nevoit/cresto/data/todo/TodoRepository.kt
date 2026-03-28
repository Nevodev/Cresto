package com.nevoit.cresto.data.todo

import com.nevoit.cresto.data.statistics.DailyStat
import com.nevoit.cresto.data.todo.backup.SubTodoBackupDto
import com.nevoit.cresto.data.todo.backup.TodoBackupDto
import com.nevoit.cresto.data.todo.backup.TodoBackupFile
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import java.time.LocalDate
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
class TodoRepository(private val todoDao: TodoDao) {

    val allTodos: Flow<List<TodoItemWithSubTodos>> = todoDao.getAllTodosWithSubTodos()

    val allTodosSortedByDueDate: Flow<List<TodoItemWithSubTodos>> =
        todoDao.getAllTodosWithSubTodosSortedByDueDate()

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
        completedDate: LocalDate?
    ) {
        todoDao.updateCompletedStatusByIds(ids, isCompleted, completedDate)
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
        val creationDate: String,
        val isCompleted: Boolean,
        val hashtag: String?,
        val flag: Int,
        val completedDate: String?,
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
                    creationDate = it.creationDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    isCompleted = it.isCompleted,
                    hashtag = it.hashtag,
                    flag = it.flag,
                    completedDate = it.completedDate?.toString()
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
                    id = 0, // 强制新 id，避免与现有冲突
                    title = todoDto.title,
                    dueDate = todoDto.dueDate?.let(LocalDate::parse),
                    creationDate = LocalDateTime.parse(todoDto.creationDate),
                    isCompleted = todoDto.isCompleted,
                    hashtag = todoDto.hashtag,
                    flag = todoDto.flag,
                    completedDate = todoDto.completedDate?.let(LocalDate::parse)
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
            creationDate = todoItem.creationDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            isCompleted = todoItem.isCompleted,
            hashtag = todoItem.hashtag,
            flag = todoItem.flag,
            completedDate = todoItem.completedDate?.toString(),
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
            creationDate = todo.creationDate,
            isCompleted = todo.isCompleted,
            hashtag = todo.hashtag,
            flag = todo.flag,
            completedDate = todo.completedDate,
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
}