package com.nevoit.cresto.feature.sharetodo

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.kyant.shapes.RoundedRectangle
import com.nevoit.cresto.R
import com.nevoit.cresto.data.todo.TodoItem
import com.nevoit.cresto.data.todo.TodoItemWithSubTodos
import com.nevoit.cresto.feature.settings.util.SettingsManager
import com.nevoit.cresto.theme.AppButtonColors
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.AppSpecs
import com.nevoit.cresto.theme.getFlagColor
import com.nevoit.cresto.ui.components.glasense.GlasenseBottomSheet
import com.nevoit.cresto.ui.components.glasense.GlasenseButton
import com.nevoit.cresto.ui.components.glasense.GlasenseButtonAlt
import com.nevoit.cresto.ui.components.glasense.extend.LineThroughText
import com.nevoit.cresto.ui.components.glasense.glasenseHighlight
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun TodoShareSheet(
    todos: List<TodoItemWithSubTodos>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()
    val shareTitle = stringResource(R.string.share_todos_title)
    val shareFailedTitle = stringResource(R.string.share_todo_failed)
    val shareImageFailed = stringResource(R.string.share_todo_image_failed)
    val shareTextFailed = stringResource(R.string.share_todo_text_failed)
    val isDueTodayMarkerEnabled by SettingsManager.isDueTodayMarkerState
    val isOverdueMarkerEnabled by SettingsManager.isOverdueMarkerState

    GlasenseBottomSheet(onDismissed = onDismiss) { slideOut ->
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .padding(
                    top = 72.dp,
                    bottom = WindowInsets.navigationBars.asPaddingValues()
                        .calculateBottomPadding() + 72.dp
                )
                .padding(horizontal = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layout { measurable, constraints ->
                        val childConstraints = constraints.copy(maxHeight = Constraints.Infinity)
                        val placeable = measurable.measure(childConstraints)
                        val availableHeight = constraints.maxHeight
                        val scale =
                            if (placeable.height > availableHeight && placeable.height > 0) {
                                availableHeight.toFloat() / placeable.height.toFloat()
                            } else {
                                1f
                            }
                        layout(constraints.maxWidth, availableHeight) {
                            placeable.placeWithLayer(0, 0) {
                                scaleX = scale
                                scaleY = scale
                                transformOrigin =
                                    TransformOrigin(0.5f, 0f)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                TodoShareCard(
                    todos = todos,
                    isDueTodayMarkerEnabled = isDueTodayMarkerEnabled,
                    isOverdueMarkerEnabled = isOverdueMarkerEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawWithContent {
                            graphicsLayer.record {
                                this@drawWithContent.drawContent()
                            }
                            drawLayer(graphicsLayer)
                        }
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .padding(horizontal = 12.dp)
                .height(48.dp)
        ) {
            GlasenseButton(
                enabled = true,
                shape = CircleShape,
                onClick = { slideOut() },
                modifier = Modifier
                    .width(48.dp)
                    .height(48.dp),
                colors = AppButtonColors.action(),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_forward_nav),
                    contentDescription = stringResource(R.string.back),
                    modifier = Modifier.width(32.dp)
                )
            }
            Text(
                text = stringResource(R.string.share_todos_title),
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.headlineSmall
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GlasenseButtonAlt(
                modifier = Modifier
                    .weight(1f),
                enabled = todos.isNotEmpty(),
                onClick = {
                    val result = runCatching {
                        context.shareText(
                            title = shareTitle,
                            text = formatTodosForPlainText(
                                context = context,
                                todos = todos,
                                isDueTodayMarkerEnabled = isDueTodayMarkerEnabled,
                                isOverdueMarkerEnabled = isOverdueMarkerEnabled
                            )
                        )
                    }
                    if (result.isFailure) {
                        Toast.makeText(context, shareTextFailed, Toast.LENGTH_SHORT).show()
                    } else {
                        slideOut()
                    }
                },
                colors = AppButtonColors.secondary(),
                shape = AppSpecs.buttonShape
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_document),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = stringResource(R.string.share_text), maxLines = 1)
                }
            }

            GlasenseButtonAlt(
                modifier = Modifier
                    .weight(1f)
                    .glasenseHighlight(AppSpecs.buttonCorner),
                enabled = todos.isNotEmpty(),
                onClick = {
                    scope.launch {
                        val result = runCatching {
                            val imageUri = context.writeShareBitmap(
                                bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap(),
                                fileName = "cresto_todos.png"
                            )
                            context.shareImage(title = shareTitle, imageUri = imageUri)
                        }
                        if (result.isFailure) {
                            Toast.makeText(
                                context,
                                "$shareFailedTitle: $shareImageFailed",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            slideOut()
                        }
                    }
                },
                colors = AppButtonColors.primary(),
                shape = AppSpecs.buttonShape
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_photo),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = stringResource(R.string.share_image), maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun TodoShareCard(
    todos: List<TodoItemWithSubTodos>,
    isDueTodayMarkerEnabled: Boolean,
    isOverdueMarkerEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val footer = stringResource(R.string.share_todo_card_footer)
    val emptyText = stringResource(R.string.share_todos_empty)
    val today = remember { LocalDate.now() }
    val selectedAppIcon by SettingsManager.appIconState

    Column(
        modifier = modifier
            .clip(AppSpecs.cardShape)
            .background(AppColors.pageBackground)
            .padding(12.dp)
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = stringResource(R.string.todos),
                    fontSize = 16.sp,
                    lineHeight = 18.sp,
                    color = AppColors.content
                )
                Text(
                    text = DateTimeFormatter.ofPattern("yyyy/MM/dd").format(today),
                    fontSize = 14.sp,
                    lineHeight = 16.sp,
                    color = AppColors.contentVariant
                )
            }
            Icon(
                painter = painterResource(selectedAppIcon.mipmapResId),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedRectangle(40.dp / 4))
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (todos.isEmpty()) {
            Text(
                text = emptyText,
                color = AppColors.contentVariant,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                todos.forEach { item ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(AppSpecs.cardShape)
                            .background(AppColors.cardBackground)
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        ShareTodoItem(
                            item = item,
                            isDueTodayMarkerEnabled = isDueTodayMarkerEnabled,
                            isOverdueMarkerEnabled = isOverdueMarkerEnabled
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = footer,
                fontSize = 14.sp,
                lineHeight = 14.sp,
                color = AppColors.contentVariant
            )
        }
    }
}

@Composable
private fun ShareTodoItem(
    item: TodoItemWithSubTodos,
    isDueTodayMarkerEnabled: Boolean,
    isOverdueMarkerEnabled: Boolean
) {
    val todo = item.todoItem
    val flagColor = getFlagColor(todo.flag)
    val contentColor = AppColors.content
    val errorColor = AppColors.error
    val highlightTextColor = AppColors.highlightText
    val today = remember { LocalDate.now() }
    val nowTime = remember { LocalTime.now() }
    val dueTodayText = stringResource(R.string.due_today_text)
    val overdueText = stringResource(R.string.overdue)
    val inProgressText = stringResource(R.string.todo_in_progress)
    val overdueWithDate = stringResource(R.string.overdue_with_date, "%s")
    val dueDateMeta = remember(
        todo,
        today,
        nowTime,
        dueTodayText,
        overdueText,
        inProgressText,
        overdueWithDate,
        isDueTodayMarkerEnabled,
        isOverdueMarkerEnabled,
        contentColor,
        errorColor,
        highlightTextColor
    ) {
        todo.shareDueDateMeta(
            today = today,
            nowTime = nowTime,
            dueTodayText = dueTodayText,
            overdueText = overdueText,
            inProgressText = inProgressText,
            overdueWithDateTemplate = overdueWithDate,
            isDueTodayMarkerEnabled = isDueTodayMarkerEnabled,
            isOverdueMarkerEnabled = isOverdueMarkerEnabled,
            contentColor = contentColor,
            errorColor = errorColor,
            highlightTextColor = highlightTextColor
        )
    }
    val todoTitleTextStyle = TextStyle(
        fontSize = 16.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShareCheckMark(isCompleted = todo.isCompleted)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            LineThroughText(
                text = todo.title,
                style = todoTitleTextStyle.copy(
                    color = contentColor
                ),
                lineThrough = todo.isCompleted,
                modifier = Modifier.fillMaxWidth()
            )
            if (dueDateMeta.text != null) {
                Spacer(modifier = Modifier.height(2.dp))
                ShareDueDateMeta(
                    text = dueDateMeta.text,
                    color = dueDateMeta.color
                )
            }

            if (item.subTodos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    item.subTodos.take(4).forEach { subTodo ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.alpha(AppColors.contentVariant.alpha)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(AppColors.content)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            LineThroughText(
                                text = subTodo.description,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                lineThrough = subTodo.isCompleted,
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    lineHeight = 16.sp,
                                    color = AppColors.content
                                ),
                                lineColor = AppColors.content
                            )
                        }
                    }
                    val remainingCount = item.subTodos.size - 4
                    if (remainingCount > 0) {
                        Text(
                            text = "+$remainingCount",
                            fontSize = 14.sp,
                            lineHeight = 16.sp,
                            color = AppColors.contentVariant,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }
        }
        if (flagColor != Color.Transparent) {
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                painter = painterResource(R.drawable.ic_flag_fill),
                contentDescription = null,
                tint = flagColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun ShareDueDateMeta(
    text: String,
    color: Color
) {
    val density = LocalDensity.current

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.ic_calendar),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(density.run { 18.sp.toDp() })
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 14.sp,
            color = color,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ShareCheckMark(isCompleted: Boolean) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .then(
                if (isCompleted) Modifier.background(AppColors.primary)
                else Modifier.border(2.dp, AppColors.scrimBold, CircleShape)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isCompleted) {
            Icon(
                painter = painterResource(R.drawable.ic_checkbox_checkmark),
                contentDescription = null,
                tint = AppColors.onPrimary,
                modifier = Modifier
                    .fillMaxSize()
                    .scale(1.1f)
            )
        }
    }
}

fun formatTodosForPlainText(
    context: Context,
    todos: List<TodoItemWithSubTodos>,
    isDueTodayMarkerEnabled: Boolean = SettingsManager.isDueTodayMarker,
    isOverdueMarkerEnabled: Boolean = SettingsManager.isOverdueMarker
): String {
    val title = context.getString(R.string.app_name)
    return buildString {
        appendLine(title)
        appendLine()
        todos.forEachIndexed { index, item ->
            val marker = if (item.todoItem.isCompleted) "[x]" else "[ ]"
            append(marker).append(' ').appendLine(item.todoItem.title)
            item.todoItem.shareDueDateText(
                context = context,
                isDueTodayMarkerEnabled = isDueTodayMarkerEnabled,
                isOverdueMarkerEnabled = isOverdueMarkerEnabled
            )?.let { dueDateText ->
                appendLine(dueDateText)
            }
            item.subTodos.forEach { subTodo ->
                val subMarker = if (subTodo.isCompleted) "[x]" else "[ ]"
                append("    ").append(subMarker).append(' ').appendLine(subTodo.description)
            }
            if (index < todos.lastIndex) appendLine()
        }
    }.trimEnd()
}

private data class ShareDueDateMeta(
    val text: String?,
    val color: Color
)

private fun TodoItem.shareDueDateText(
    context: Context,
    isDueTodayMarkerEnabled: Boolean,
    isOverdueMarkerEnabled: Boolean
): String? {
    val dueTodayText = context.getString(R.string.due_today_text)
    val overdueText = context.getString(R.string.overdue)
    val inProgressText = context.getString(R.string.todo_in_progress)
    val today = LocalDate.now()
    val nowTime = LocalTime.now()

    return shareDueDateText(
        today = today,
        nowTime = nowTime,
        dueTodayText = dueTodayText,
        overdueText = overdueText,
        inProgressText = inProgressText,
        isDueTodayMarkerEnabled = isDueTodayMarkerEnabled,
        isOverdueMarkerEnabled = isOverdueMarkerEnabled,
        overdueWithDate = { rawDate ->
            context.getString(R.string.overdue_with_date, rawDate)
        }
    )
}

private fun TodoItem.shareDueDateMeta(
    today: LocalDate,
    nowTime: LocalTime,
    dueTodayText: String,
    overdueText: String,
    inProgressText: String,
    overdueWithDateTemplate: String,
    isDueTodayMarkerEnabled: Boolean,
    isOverdueMarkerEnabled: Boolean,
    contentColor: Color,
    errorColor: Color,
    highlightTextColor: Color
): ShareDueDateMeta {
    val text = shareDueDateText(
        today = today,
        nowTime = nowTime,
        dueTodayText = dueTodayText,
        overdueText = overdueText,
        inProgressText = inProgressText,
        isDueTodayMarkerEnabled = isDueTodayMarkerEnabled,
        isOverdueMarkerEnabled = isOverdueMarkerEnabled,
        overdueWithDate = { rawDate -> overdueWithDateTemplate.format(rawDate) }
    )

    val isToday = dueDate == today
    val isExpired = dueDate?.let { it < today } == true
    val timeText = shareTimeText(startTime, endTime)
    val isOverdueTime = !isCompleted && timeText.isNotEmpty() && (
            (endTime != null && nowTime.isAfter(endTime)) ||
                    (endTime == null && startTime != null && nowTime.isAfter(startTime))
            )

    val color = when {
        dueDate == null -> contentColor.copy(alpha = 0.4f)
        isToday -> when {
            isOverdueTime -> errorColor
            isDueTodayMarkerEnabled && !isCompleted -> highlightTextColor
            else -> contentColor.copy(alpha = 0.4f)
        }

        isExpired -> if (isOverdueMarkerEnabled && !isCompleted) {
            errorColor
        } else {
            contentColor.copy(alpha = 0.4f)
        }

        else -> contentColor.copy(alpha = 0.4f)
    }

    return ShareDueDateMeta(text = text, color = color)
}

private fun TodoItem.shareDueDateText(
    today: LocalDate,
    nowTime: LocalTime,
    dueTodayText: String,
    overdueText: String,
    inProgressText: String,
    isDueTodayMarkerEnabled: Boolean,
    isOverdueMarkerEnabled: Boolean,
    overdueWithDate: (String) -> String
): String? {
    val dueDate = dueDate ?: return null
    val rawFormattedDate = if (dueDate.year == today.year) {
        dueDate.format(thisYearFormatter)
    } else {
        dueDate.format(otherYearFormatter)
    }
    val timeText = shareTimeText(startTime, endTime)
    val isToday = dueDate == today
    val isExpired = dueDate < today

    return when {
        isToday && !isCompleted -> {
            val baseText = if (isDueTodayMarkerEnabled) dueTodayText else rawFormattedDate
            if (timeText.isNotEmpty()) {
                val isOverdueTime =
                    (endTime != null && nowTime.isAfter(endTime)) ||
                            (endTime == null && startTime != null && nowTime.isAfter(startTime))
                val isInProgressTime =
                    startTime != null && endTime != null &&
                            !nowTime.isBefore(startTime) && !nowTime.isAfter(endTime)

                when {
                    isOverdueTime -> "$overdueText · $timeText"
                    isInProgressTime -> "$inProgressText · $timeText"
                    else -> timeText
                }
            } else {
                baseText
            }
        }

        isExpired && isOverdueMarkerEnabled && !isCompleted -> overdueWithDate(rawFormattedDate)
        else -> rawFormattedDate
    }
}

private fun shareTimeText(startTime: LocalTime?, endTime: LocalTime?): String {
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    return when {
        startTime != null && endTime != null -> {
            "${startTime.format(formatter)}-${endTime.format(formatter)}"
        }

        startTime != null -> startTime.format(formatter)
        endTime != null -> endTime.format(formatter)
        else -> ""
    }
}

private val thisYearFormatter = DateTimeFormatter.ofPattern("M/d")
private val otherYearFormatter = DateTimeFormatter.ofPattern("yyyy/M/d")

private fun Context.shareText(title: String, text: String) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    startActivity(Intent.createChooser(sendIntent, title))
}

private fun Context.shareImage(title: String, imageUri: Uri) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, imageUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = android.content.ClipData.newUri(contentResolver, title, imageUri)
    }
    startActivity(Intent.createChooser(sendIntent, title))
}

private fun Context.writeShareBitmap(bitmap: Bitmap, fileName: String): Uri {
    val shareDir = File(cacheDir, "shared_images").apply {
        mkdirs()
    }
    val file = File(shareDir, fileName)
    FileOutputStream(file).use { output ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
    }
    return FileProvider.getUriForFile(
        this,
        "$packageName.fileprovider",
        file
    )
}
