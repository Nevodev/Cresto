package com.nevoit.cresto.feature.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.nevoit.cresto.MainActivity
import com.nevoit.cresto.R
import com.nevoit.cresto.data.todo.EXTRA_TODO_ID
import com.nevoit.cresto.data.todo.TodoItem
import com.nevoit.cresto.data.todo.TodoItemWithSubTodos
import com.nevoit.cresto.data.todo.TodoRepository
import com.nevoit.cresto.feature.detail.DetailActivity
import com.nevoit.cresto.feature.settings.util.SettingsManager
import com.nevoit.cresto.theme.getFlagColor
import com.nevoit.cresto.theme.resolveAppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import org.koin.core.context.GlobalContext
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.milliseconds
import androidx.glance.color.ColorProvider as DayNightColorProvider

private val TodoIdKey = ActionParameters.Key<Int>(EXTRA_TODO_ID)
private val PendingTodoIdsKey = stringSetPreferencesKey("pending_todo_ids")
private const val CHECKBOX_ANIMATION_MILLIS = 300L

private data class TodayTodoWidgetState(
    val todos: List<TodoItem>,
    val remainingCount: Int,
    val pendingTodoIds: Set<Int>
)

private data class TodayTodoWidgetColors(
    val background: ColorProvider,
    val content: ColorProvider,
    val scrimBold: ColorProvider,
    val primary: ColorProvider
)

/** A view of the incomplete todos due today. */
object TodayTodoWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(110.dp, 110.dp),
            DpSize(250.dp, 180.dp),
            DpSize(250.dp, 250.dp)
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = GlobalContext.get().get<TodoRepository>()
        val todayTodos = repository.getTodosByDate(LocalDate.now())
        val initialTodos = todayTodos.first()

        provideContent {
            val todos by todayTodos.collectAsState(initial = initialTodos)
            val pendingTodoIds = currentState(PendingTodoIdsKey)
                .orEmpty()
                .mapNotNull(String::toIntOrNull)
                .toSet()
            TodayTodoWidgetContent(
                context = context,
                state = todos.toWidgetState(pendingTodoIds)
            )
        }
    }
}

private fun List<TodoItemWithSubTodos>.toWidgetState(
    pendingTodoIds: Set<Int> = emptySet()
): TodayTodoWidgetState {
    val todos = asSequence()
        .map { it.todoItem }
        .filterNot { it.isCompleted }
        .sortedWith(compareByDescending<TodoItem> { it.isPinned }
            .thenByDescending { it.creationDateTime })
        .toList()
    return TodayTodoWidgetState(
        todos = todos,
        remainingCount = todos.size,
        pendingTodoIds = pendingTodoIds
    )
}

@Composable
private fun rememberTodayTodoWidgetColors(context: Context): TodayTodoWidgetColors {
    val dynamicColor = SettingsManager.isUseDynamicColorState.value
    val customPrimaryEnabled = SettingsManager.isCustomPrimaryColorEnabledState.value
    val themePrimaryColorArgb = SettingsManager.themePrimaryColorState.intValue

    return remember(context, dynamicColor, customPrimaryEnabled, themePrimaryColorArgb) {
        val lightColors = resolveAppColors(
            context = context,
            isDark = false,
            dynamicColor = dynamicColor,
            customPrimaryEnabled = customPrimaryEnabled,
            themePrimaryColorArgb = themePrimaryColorArgb
        )
        val darkColors = resolveAppColors(
            context = context,
            isDark = true,
            dynamicColor = dynamicColor,
            customPrimaryEnabled = customPrimaryEnabled,
            themePrimaryColorArgb = themePrimaryColorArgb
        )
        TodayTodoWidgetColors(
            background = DayNightColorProvider(
                day = lightColors.background,
                night = darkColors.background
            ),
            content = DayNightColorProvider(
                day = lightColors.content,
                night = darkColors.content
            ),
            scrimBold = DayNightColorProvider(
                day = lightColors.scrimBold,
                night = darkColors.scrimBold
            ),
            primary = DayNightColorProvider(
                day = lightColors.primary,
                night = darkColors.primary
            )
        )
    }
}

@Composable
private fun TodayTodoWidgetContent(
    context: Context,
    state: TodayTodoWidgetState
) {
    val colors = rememberTodayTodoWidgetColors(context)

    fun textStyle(
        color: ColorProvider = colors.content,
        size: Int = 14,
        weight: FontWeight? = null
    ) = TextStyle(
        color = color,
        fontSize = size.sp,
        fontWeight = weight
    )

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(colors.background)
            .cornerRadius(24.dp)
            .clickable(
                onClick = actionStartActivity<MainActivity>(),
                rippleOverride = R.drawable.widget_ripple
            )
            .padding(16.dp),
        verticalAlignment = Alignment.Vertical.Top
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Text(
                text = context.getString(R.string.today),
                style = textStyle(
                    color = colors.primary,
                    size = 16,
                    weight = FontWeight.Medium
                ),
                modifier = GlanceModifier.defaultWeight(),
                maxLines = 1
            )
            Text(
                text = state.remainingCount.toString(),
                style = textStyle(
                    color = colors.content,
                    size = 16,
                    weight = FontWeight.Bold
                ),
                maxLines = 1
            )
        }

        Spacer(modifier = GlanceModifier.height(10.dp))

        if (state.todos.isEmpty()) {
            Text(
                text = context.getString(R.string.today_widget_empty),
                style = textStyle(color = colors.primary, size = 13)
            )
        } else {
            LazyColumn(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
            ) {
                itemsIndexed(
                    items = state.todos,
                    itemId = { _, todo -> todo.id.toLong() }
                ) { index, todo ->
                    val isPending = todo.id in state.pendingTodoIds
                    val flagColor = getFlagColor(todo.flag)
                    Column(
                        modifier = GlanceModifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .clickable(
                                    onClick = actionStartActivity<DetailActivity>(
                                        actionParametersOf(TodoIdKey to todo.id)
                                    ),
                                    rippleOverride = R.drawable.widget_ripple
                                ),
                            verticalAlignment = Alignment.Vertical.CenterVertically
                        ) {
                            CheckBox(
                                checked = isPending,
                                onCheckedChange = if (isPending) {
                                    null
                                } else {
                                    actionRunCallback<CompleteTodayTodoAction>(
                                        actionParametersOf(TodoIdKey to todo.id)
                                    )
                                },
                                checkedColor = colors.primary,
                                uncheckedColor = colors.scrimBold
                            )
                            Spacer(modifier = GlanceModifier.width(8.dp))
                            Text(
                                text = todo.title,
                                style = textStyle(size = 14),
                                maxLines = 1,
                                modifier = GlanceModifier.defaultWeight()
                            )
                            if (flagColor != Color.Transparent) {
                                Spacer(modifier = GlanceModifier.width(4.dp))
                                Image(
                                    provider = ImageProvider(R.drawable.ic_flag_fill),
                                    contentDescription = context.getString(R.string.flag),
                                    colorFilter = ColorFilter.tint(
                                        DayNightColorProvider(
                                            day = flagColor,
                                            night = flagColor
                                        )
                                    ),
                                    modifier = GlanceModifier.size(20.dp)
                                )
                            }
                        }

                        if (index < state.todos.lastIndex) {
                            Spacer(modifier = GlanceModifier.height(4.dp))
                            Image(
                                provider = ImageProvider(R.drawable.widget_row_divider),
                                contentDescription = null,
                                contentScale = ContentScale.FillBounds,
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .padding(start = 32.dp)
                                    .height(2.dp)
                            )
                            Spacer(modifier = GlanceModifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}

class CompleteTodayTodoAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val todoId = parameters[TodoIdKey] ?: return
        val pendingId = todoId.toString()
        updateAppWidgetState(context, glanceId) { preferences ->
            preferences[PendingTodoIdsKey] = preferences[PendingTodoIdsKey].orEmpty() + pendingId
        }

        var completed = false
        try {
            delay(CHECKBOX_ANIMATION_MILLIS.milliseconds)
            GlobalContext.get().get<TodoRepository>().updateCompletedStatusByIds(
                ids = listOf(todoId),
                isCompleted = true,
                completedDateTime = LocalDateTime.now()
            )
            TodayTodoWidget.update(context, glanceId)
            completed = true
        } finally {
            updateAppWidgetState(context, glanceId) { preferences ->
                val remaining = preferences[PendingTodoIdsKey]
                    .orEmpty()
                    .filterNot { it == pendingId }
                    .toSet()
                if (remaining.isEmpty()) {
                    preferences.remove(PendingTodoIdsKey)
                } else {
                    preferences[PendingTodoIdsKey] = remaining
                }
            }
            if (!completed) {
                TodayTodoWidget.update(context, glanceId)
            }
        }
    }
}
