package com.nevoit.cresto.data.todo

import org.junit.Assert.assertEquals
import org.junit.Test

class TodoGroupOrderTest {

    @Test
    fun resolveTodoGroupOrder_usesRequestedOrder() {
        assertEquals(
            listOf(3, 1, 2),
            resolveTodoGroupOrder(
                currentGroupIds = listOf(1, 2, 3),
                orderedGroupIds = listOf(3, 1, 2)
            )
        )
    }

    @Test
    fun resolveTodoGroupOrder_ignoresUnknownAndDuplicateIds() {
        assertEquals(
            listOf(2, 1, 3),
            resolveTodoGroupOrder(
                currentGroupIds = listOf(1, 2, 3),
                orderedGroupIds = listOf(2, 99, 2, 1)
            )
        )
    }

    @Test
    fun resolveTodoGroupOrder_appendsGroupsCreatedAfterDragStarted() {
        assertEquals(
            listOf(2, 1, 3),
            resolveTodoGroupOrder(
                currentGroupIds = listOf(1, 2, 3),
                orderedGroupIds = listOf(2, 1)
            )
        )
    }
}
