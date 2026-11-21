package com.nevoit.cresto.data.todo

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TodoDetailDataTest {
    @Test
    fun waitsForEveryRealQueryIncludingAnEmptyRepeatRuleResult() = runBlocking {
        val todo = Channel<TodoItemWithSubTodos?>()
        val groups = Channel<List<TodoGroup>>()
        val counts = Channel<List<TodoGroupCount>>()
        val rules = Channel<RepeatRule?>()
        val results = Channel<TodoDetailData>(Channel.UNLIMITED)
        val item = TodoItemWithSubTodos(TodoItem(id = 7, title = "Loaded", repeatRuleId = "rule"), emptyList())
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            observeTodoDetail(
                todo = flow { for (value in todo) emit(value) },
                groups = flow { for (value in groups) emit(value) },
                groupCounts = flow { for (value in counts) emit(value) },
                repeatRule = { flow { for (value in rules) emit(value) } },
            ).collect { results.send(it) }
        }
        try {
            withTimeout(2_000) {
                todo.send(item)
                groups.send(emptyList())
                counts.send(emptyList())
                assertTrue(results.tryReceive().isFailure)
                rules.send(null)
                val loaded = results.receive()
                assertEquals(item, loaded.item)
                assertNull(loaded.repeatRule)
                assertTrue(loaded.groups.isEmpty())
                assertTrue(loaded.groupCounts.isEmpty())
            }
        } finally {
            job.cancel()
        }
    }

    @Test
    fun aDeletedTodoIsACompletedQueryInsteadOfWaitingForever() = runBlocking {
        val loaded = withTimeout(2_000) {
            observeTodoDetail(
                todo = MutableStateFlow(null),
                groups = MutableStateFlow(emptyList()),
                groupCounts = MutableStateFlow(emptyList()),
                repeatRule = { error("A missing todo has no repeat rule to load") },
            ).first()
        }
        assertNull(loaded.item)
        assertNull(loaded.repeatRule)
    }

    @Test
    fun changingRepeatSeriesCancelsTheOldQueryWithoutPublishingItsStaleData() = runBlocking {
        val initial = TodoItemWithSubTodos(TodoItem(id = 7, title = "Loaded", repeatRuleId = "old"), emptyList())
        val todo = MutableStateFlow<TodoItemWithSubTodos?>(initial)
        val queries = Channel<String>(Channel.UNLIMITED)
        val results = Channel<TodoDetailData>(Channel.UNLIMITED)
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            observeTodoDetail(
                todo = todo,
                groups = MutableStateFlow(emptyList()),
                groupCounts = MutableStateFlow(emptyList()),
                repeatRule = { id ->
                    queries.trySend(id)
                    emptyFlow()
                },
            ).collect { results.send(it) }
        }
        try {
            withTimeout(2_000) {
                assertEquals("old", queries.receive())
                todo.value = initial.copy(todoItem = initial.todoItem.copy(repeatRuleId = null))
                val loaded = results.receive()
                assertNull(loaded.repeatRule)
                assertNull(loaded.item?.todoItem?.repeatRuleId)
            }
        } finally {
            job.cancel()
        }
    }
}
