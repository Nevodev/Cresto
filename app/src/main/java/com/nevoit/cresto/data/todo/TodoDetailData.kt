package com.nevoit.cresto.data.todo

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

data class TodoDetailData(
    val item: TodoItemWithSubTodos?,
    val repeatRule: RepeatRule?,
    val groups: List<TodoGroup>,
    val groupCounts: Map<Int?, Int>,
)

@OptIn(ExperimentalCoroutinesApi::class)
internal fun observeTodoDetail(
    todo: Flow<TodoItemWithSubTodos?>,
    groups: Flow<List<TodoGroup>>,
    groupCounts: Flow<List<TodoGroupCount>>,
    repeatRule: (String) -> Flow<RepeatRule?>,
): Flow<TodoDetailData> = todo.flatMapLatest { item ->
    val rule = item?.todoItem?.repeatRuleId?.let(repeatRule) ?: flowOf(null)
    // An empty list from Room is ready; a StateFlow's initial empty placeholder is not.
    combine(rule, groups, groupCounts) { loadedRule, loadedGroups, loadedCounts ->
        TodoDetailData(
            item = item,
            repeatRule = loadedRule,
            groups = loadedGroups,
            groupCounts = loadedCounts.associate { it.groupId to it.count },
        )
    }
}
