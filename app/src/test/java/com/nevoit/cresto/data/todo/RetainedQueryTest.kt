package com.nevoit.cresto.data.todo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class RetainedQueryTest {
    @Test
    fun recreatingAReaderReusesTheQueryAndReceivesChangesWhileThePageIsGone() {
        val owner = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val database = MutableStateFlow(listOf("before"))
        var starts = 0
        val query = RetainedQuery<Int, List<String>>(owner) {
            starts++
            database
        }
        try {
            val state = query.stateFor(7)
            val reader = owner.launch { state.collect {} }
            reader.cancel()
            database.value = listOf("after")

            assertSame(state, query.stateFor(7))
            assertEquals(listOf("after"), state.value)
            assertEquals(1, starts)
            assertEquals(1, database.subscriptionCount.value)
        } finally {
            owner.cancel()
        }
    }

    @Test
    fun changingTheDateOrTodoReleasesTheOldQueryButPreservesItsOutgoingSnapshot() {
        val owner = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val first = MutableStateFlow(listOf("first"))
        val second = MutableStateFlow(listOf("second"))
        val query = RetainedQuery<Int, List<String>>(owner) { if (it == 1) first else second }
        try {
            val outgoing = query.stateFor(1)
            val incoming = query.stateFor(2)
            first.value = listOf("changed after cancellation")

            assertEquals(0, first.subscriptionCount.value)
            assertEquals(1, second.subscriptionCount.value)
            assertEquals(listOf("first"), outgoing.value)
            assertEquals(listOf("second"), incoming.value)
        } finally {
            owner.cancel()
        }
    }

    @Test
    fun equalDatabaseResultsKeepTheExistingStateValue() = runBlocking {
        val owner = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val original = listOf("same")
        val database = MutableSharedFlow<List<String>>(replay = 1)
        database.emit(original)
        val query = RetainedQuery<Int, List<String>>(owner) { database }
        try {
            val state = query.stateFor(1)
            database.emit(listOf("same"))
            assertSame(original, state.value)
        } finally {
            owner.cancel()
        }
    }
}
