package com.nevoit.cresto.feature.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInQuad
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
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
import com.nevoit.cresto.theme.gradientColorsDark
import com.nevoit.cresto.theme.isAppInDarkTheme
import com.nevoit.cresto.ui.components.glasense.GlasenseButton
import com.nevoit.cresto.ui.components.glasense.RotatingGlow
import com.nevoit.cresto.ui.components.glasense.RotatingGlowBorder
import com.nevoit.cresto.ui.components.glasense.rememberSwipeableListState
import com.nevoit.cresto.ui.components.packed.SwipeableTodoItem
import com.nevoit.glasense.theme.Springs
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private data class ReviewTodoItem(
    val id: Int,
    val eventItem: EventItem
)

@Composable
fun BoxScope.AiTodoReviewContainer(
    backdrop: Backdrop,
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

    val availbaleHeight =
        LocalWindowInfo.current.containerDpSize.height - WindowInsets.statusBars.asPaddingValues()
            .calculateTopPadding() - WindowInsets.navigationBars.asPaddingValues()
            .calculateBottomPadding()

    var isReady by remember { mutableStateOf(false) }

    val rotationY = remember { Animatable(1f) }
    val blurRadius = remember { Animatable(0f) }
    val scale = remember { Animatable(0.2f) }
    val alpha = remember { Animatable(0f) }
    val scrimAlpha = remember { Animatable(0f) }
    val buttonScale = remember { Animatable(1.4f) }

    LaunchedEffect(isReady) {
        if (isReady) {
            launch {
                blurRadius.animateTo(
                    targetValue = 1f,
                    animationSpec = Springs.smooth(300, 0.0, 0.0001f)
                )
            }
            launch {
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = Springs.bouncy(500, 0.0, 0.0001f)
                )
            }
            launch {
                scrimAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(300)
                )
            }
            launch {
                buttonScale.animateTo(
                    targetValue = 1f,
                    animationSpec = Springs.smooth(500, 0.0, 0.0001f)
                )
            }
            launch {
                alpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(300)
                )
            }
            rotationY.animateTo(
                targetValue = 0f,
                animationSpec = Springs.bouncy(600, 0.0, 0.0001f)
            )
        }
    }

    val darkTheme = isAppInDarkTheme()
    val scope = rememberCoroutineScope()

    BackHandler { }
    fun dismiss() {
        scope.launch {
            blurRadius.animateTo(
                targetValue = 0f,
                animationSpec = Springs.smooth(300, 0.0, 0.0001f)
            )
            onDismiss()
        }
        scope.launch {
            scale.animateTo(
                targetValue = 1.4f,
                animationSpec = tween(200, 0, EaseInQuad)
            )
        }
        scope.launch {
            buttonScale.animateTo(
                targetValue = 1.4f,
                animationSpec = tween(200, 0, EaseInQuad)
            )
        }
        scope.launch {
            alpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(200)
            )
        }
        scope.launch {
            scrimAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(300)
            )
        }
    }

    fun insert() {
        scope.launch {
            blurRadius.animateTo(
                targetValue = 0f,
                animationSpec = Springs.smooth(300, 0.0, 0.0001f)
            )
            onInsert(reviewItems.map { it.eventItem })
        }
        scope.launch {
            scale.animateTo(
                targetValue = 1.4f,
                animationSpec = tween(200, 0, EaseInQuad)
            )
        }
        scope.launch {
            buttonScale.animateTo(
                targetValue = 1.4f,
                animationSpec = tween(200, 0, EaseInQuad)
            )
        }
        scope.launch {
            alpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(200)
            )
        }
        scope.launch {
            scrimAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(300)
            )
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
    )
    Box(
        modifier = Modifier
            // performance is bad
//            .centerWaveShaderEffect(
//                durationMillis = 700,
//                waveWidthMultiplier = 4f,
//                chromaticStrength = 0.5f,
//                centerY = 1f,
//                intensity = 1f
//            )
            .fillMaxSize()
            .drawPlainBackdrop(
                backdrop = backdrop,
                shape = { RectangleShape },
                effects = { blur(blurRadius.value * 32.dp.toPx()) }, onDrawSurface = {
                    drawRect(color = Color.Black.copy(alpha = scrimAlpha.value * 0.4f))
                })
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    this.transformOrigin = TransformOrigin(0.5f, 0.5f)
                    this.rotationY = rotationY.value * -90f
                    this.cameraDistance = 16 * density
                    this.scaleX = scale.value
                    this.scaleY = scale.value
                }
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            RotatingGlow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(availbaleHeight * 0.7f)
                    .graphicsLayer {
                        this.alpha = 0.8f * alpha.value
                    },
                blurRadius = 32.dp,
                shape = AppSpecs.dialogShape,
                colors = gradientColorsDark,
                timeMillis = 5000
            )
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        this.alpha = alpha.value
                    }
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(availbaleHeight * 0.7f)
                    .clip(AppSpecs.dialogShape)
                    .drawBehind {
                        drawRect(
                            color = if (darkTheme) Color.Black.copy(.8f) else Color.White.copy(
                                .8f
                            )
                        )
                    }
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        if (!isReady) {
                            isReady = true
                        }
                        layout(placeable.width, placeable.height) {
                            placeable.place(0, 0)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                RotatingGlowBorder(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            this.alpha = 0.5f
                        },
                    blurRadius = 24.dp,
                    shape = AppSpecs.dialogShape,
                    colors = gradientColorsDark,
                    timeMillis = 5000,
                    strokeWidth = 8.dp,
                    blendMode = if (darkTheme) BlendMode.Plus else BlendMode.ColorDodge,
                    edgeTreatment = BlurredEdgeTreatment.Rectangle
                )
                if (reviewItems.isEmpty()) {
                    Text(
                        text = stringResource(R.string.ai_todo_review_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.content
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        itemsIndexed(
                            items = reviewItems,
                            key = { _, item -> item.id }
                        ) { _, reviewItem ->
                            SwipeableTodoItem(
                                backgroundColor = if (darkTheme) Color.White.copy(.1f) else AppColors.cardBackground.copy(
                                    .5f
                                ),
                                listState = swipeListState,
                                item = reviewItem.toTodoItemWithSubTodos(),
                                showDate = true,
                                isDueTodayMarkerEnabled = isDueTodayMarkerEnabled,
                                isOverdueMarkerEnabled = isOverdueMarkerEnabled,
                                onCheckedChange = { isCompleted ->
                                    val itemIndex =
                                        reviewItems.indexOfFirst { it.id == reviewItem.id }
                                    if (itemIndex >= 0) {
                                        reviewItems[itemIndex] =
                                            reviewItems[itemIndex].copy(
                                                eventItem = reviewItems[itemIndex].eventItem.copy(
                                                    isCompleted = isCompleted
                                                )
                                            )
                                    }
                                },
                                onDelete = {
                                    val itemIndex =
                                        reviewItems.indexOfFirst { it.id == reviewItem.id }
                                    if (itemIndex >= 0) {
                                        reviewItems.removeAt(itemIndex)
                                    }
                                },
                                modifier = Modifier
                            )
                        }
                    }
                }
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                this.scaleX = buttonScale.value
                this.scaleY = buttonScale.value
            }) {
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 12.dp)
                .align(Alignment.TopCenter)
        ) {
            GlasenseButton(
                enabled = true,
                shape = CircleShape,
                onClick = { dismiss() },
                modifier = Modifier.size(48.dp),
                colors = AppButtonColors.action().copy(
                    containerColor = Color.White.copy(alpha = 0.1f),
                    contentColor = Color.White
                ),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_cross),
                    contentDescription = stringResource(R.string.back),
                    modifier = Modifier.width(28.dp)
                )
            }
            GlasenseButton(
                enabled = true,
                shape = CircleShape,
                onClick = { insert() },
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.CenterEnd),
                colors = AppButtonColors.action().copy(
                    containerColor = Color.White.copy(alpha = 0.1f),
                    contentColor = Color.White
                ),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_checkmark),
                    contentDescription = stringResource(R.string.ai_todo_review_insert),
                    modifier = Modifier.width(28.dp)
                )
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