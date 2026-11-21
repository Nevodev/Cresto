package com.nevoit.cresto.data.todo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class RetainedQuery<K : Any, T : Any>(
    private val scope: CoroutineScope,
    private val query: (K) -> Flow<T>,
) {
    private var current: Entry<K, T>? = null

    fun stateFor(key: K): StateFlow<T?> {
        current?.takeIf { it.key == key }?.let { return it.state }
        current?.job?.cancel()
        val value = MutableStateFlow<T?>(null)
        val state = value.asStateFlow()
        val job = scope.launch { query(key).collect { value.value = it } }
        // Keep one live query. An outgoing page can still render its previous state's last value.
        current = Entry(key, state, job)
        return state
    }

    private class Entry<K, T>(val key: K, val state: StateFlow<T?>, val job: Job)
}
