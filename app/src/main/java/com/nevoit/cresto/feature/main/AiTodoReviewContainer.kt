package com.nevoit.cresto.feature.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nevoit.cresto.R
import com.nevoit.cresto.data.todo.SubTodoItem
import com.nevoit.cresto.data.todo.TodoItem
import com.nevoit.cresto.data.todo.TodoItemWithSubTodos
import com.nevoit.cresto.data.todo.TodoReminderMode
import com.nevoit.cresto.data.utils.EventItem
import com.nevoit.cresto.feature.screenextract.PendingAiTodos
import com.nevoit.cresto.feature.settings.util.SettingsManager
import com.nevoit.cresto.theme.AppButtonColors
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.AppSpecs
import com.nevoit.cresto.ui.components.glasense.GlasenseButtonAlt
import com.nevoit.cresto.ui.components.glasense.rememberSwipeableListState
import com.nevoit.cresto.ui.components.glasense.glasenseHighlight
import com.nevoit.cresto.ui.components.packed.SwipeableTodoItem
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private data class ReviewTodoItem(
    val id: Int,
    val eventItem: EventItem
)

@Composable
fun AiTodoReviewContainer(
    pendingTodos: PendingAiTodos,
    onDismiss: () -> Unit,
    onInsert: (List<EventItem>) -> Unit
) {
    val reviewItems = remember(pendingTodos) {
        mutableStateListOf<ReviewTodoItem>().apply {
            addAll(
                pendingTodos.items.mapIndexed { index, eventItem ->
                    ReviewTodoItem(
                        id = -(index + 1),
                        eventItem = eventItem
                    )
                }
            )
        }
    }
    val swipeListState = rememberSwipeableListState()
    val isDueTodayMarkerEnabled = SettingsManager.isDueTodayMarkerState.value
    val isOverdueMarkerEnabled = SettingsManager.isOverdueMarkerState.value

    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 36.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .shadow(24.dp, AppSpecs.dialogShape)
                .clip(AppSpecs.dialogShape)
                .background(AppColors.cardBackground)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlasenseButtonAlt(
                    enabled = true,
                    shape = CircleShape,
                    onClick = onDismiss,
                    modifier = Modifier.size(44.dp),
                    colors = AppButtonColors.secondary()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_cross),
                        contentDescription = stringResource(R.string.ai_todo_review_discard),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = stringResource(R.string.ai_todo_review_title),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(AppColors.scrimNormal),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = reviewItems.size.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = AppColors.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (reviewItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.ai_todo_review_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.content.copy(alpha = 0.55f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(
                        items = reviewItems,
                        key = { _, item -> item.id }
                    ) { _, reviewItem ->
                        SwipeableTodoItem(
                            listState = swipeListState,
                            item = reviewItem.toTodoItemWithSubTodos(),
                            showDate = true,
                            isDueTodayMarkerEnabled = isDueTodayMarkerEnabled,
                            isOverdueMarkerEnabled = isOverdueMarkerEnabled,
                            onCheckedChange = { isCompleted ->
                                val itemIndex = reviewItems.indexOfFirst { it.id == reviewItem.id }
                                if (itemIndex >= 0) {
                                    reviewItems[itemIndex] = reviewItems[itemIndex].copy(
                                        eventItem = reviewItems[itemIndex].eventItem.copy(
                                            isCompleted = isCompleted
                                        )
                                    )
                                }
                            },
                            onDelete = {
                                val itemIndex = reviewItems.indexOfFirst { it.id == reviewItem.id }
                                if (itemIndex >= 0) {
                                    reviewItems.removeAt(itemIndex)
                                }
                            },
                            modifier = Modifier
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            GlasenseButtonAlt(
                enabled = reviewItems.isNotEmpty(),
                shape = AppSpecs.buttonShape,
                onClick = { onInsert(reviewItems.map { it.eventItem }) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .glasenseHighlight(AppSpecs.buttonCorner),
                colors = AppButtonColors.primary()
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_checkmark),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.ai_todo_review_insert),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private fun ReviewTodoItem.toTodoItemWithSubTodos(): TodoItemWithSubTodos {
    val todo = eventItem.toTodoItem(id)
    return TodoItemWithSubTodos(
        todoItem = todo,
        subTodos = eventItem.subTasks
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
            .mapIndexed { index, subTitle ->
                SubTodoItem(
                    id = id * 1000 - index,
                    parentId = id,
                    description = subTitle,
                    isCompleted = eventItem.isCompleted
                )
            }
    )
}

private fun EventItem.toTodoItem(id: Int): TodoItem {
    return TodoItem(
        id = id,
        title = title,
        dueDate = parseLocalDate(date) ?: LocalDate.now(),
        isCompleted = isCompleted,
        completedDateTime = if (isCompleted) LocalDateTime.now() else null,
        startTime = startTime?.let(::parseLocalTime),
        endTime = endTime?.let(::parseLocalTime),
        reminderMode = reminderMode?.let(::parseReminderMode),
        reminderOffsetMinutes = reminderOffsetMinutes,
        reminderDayOffset = reminderDayOffset,
        reminderTime = reminderTime?.let(::parseLocalTime)
    )
}

private fun parseLocalDate(value: String): LocalDate? {
    return try {
        LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE)
    } catch (_: Exception) {
        null
    }
}


private fun parseLocalTime(value: String): LocalTime? {
    return try {
        LocalTime.parse(value, DateTimeFormatter.ofPattern("HH:mm"))
    } catch (_: Exception) {
        null
    }
}

private fun parseReminderMode(value: String): TodoReminderMode? {
    return try {
        TodoReminderMode.valueOf(value)
    } catch (_: Exception) {
        null
    }
}