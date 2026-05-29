package com.nevoit.cresto.ui.components.packed

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.nativePaint
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.shapes.Capsule
import com.nevoit.cresto.R
import com.nevoit.cresto.data.todo.TodoReminderMode
import com.nevoit.cresto.theme.AppButtonColors
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.AppSpecs
import com.nevoit.cresto.ui.components.glasense.GlasenseModalTopBar
import com.nevoit.cresto.ui.components.glasense.GlasensePopup
import com.nevoit.cresto.ui.components.glasense.GlasenseWheelPicker
import com.nevoit.cresto.ui.components.glasense.PopupDirection
import com.nevoit.cresto.ui.components.glasense.PopupState
import com.nevoit.glasense.core.component.HDivider
import com.nevoit.glasense.core.component.Text
import com.nevoit.glasense.theme.tokens.Springs

private enum class ReminderCustomMode { Hour, Day }

data class TodoReminderConfig(
    val mode: TodoReminderMode,
    val offsetMinutes: Int? = null,
    val dayOffset: Int? = null,
    val time: java.time.LocalTime? = null,
    val persistent: Boolean = false,
    val strong: Boolean = false
)

@Composable
fun CustomReminderPopup(
    isVisible: Boolean,
    anchorBounds: Rect,
    isAllDayEnabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (TodoReminderConfig) -> Unit
) {
    var selectedMode by remember(isAllDayEnabled) {
        mutableStateOf(if (isAllDayEnabled) ReminderCustomMode.Day else ReminderCustomMode.Hour)
    }
    var selectedHourBefore by remember { mutableIntStateOf(1) }
    var selectedMinuteBefore by remember { mutableIntStateOf(0) }
    var selectedDayBefore by remember { mutableIntStateOf(0) }
    var selectedHour by remember { mutableIntStateOf(8) }
    var selectedMinute by remember { mutableIntStateOf(0) }

    val reminderDueDay = stringResource(R.string.reminder_due_day)
    val reminderDaysBeforeFormat = stringResource(R.string.reminder_days_before_format)
    val reminderHoursUnitFormat = stringResource(R.string.reminder_hours_unit_format)
    val reminderMinutesUnitFormat = stringResource(R.string.reminder_minutes_unit_format)
    val hourOptions = remember(reminderHoursUnitFormat) {
        (0..24).map { reminderHoursUnitFormat.format(it) }
    }
    val dayOptions = remember(reminderDueDay, reminderDaysBeforeFormat) {
        (0..30).map { day ->
            if (day == 0) reminderDueDay else reminderDaysBeforeFormat.format(day)
        }
    }
    val clockHourOptions = remember { (0..23).map { it.toString().padStart(2, '0') } }
    val clockMinuteOptions = remember { (0..59).map { it.toString().padStart(2, '0') } }
    val minuteOptions = remember(reminderMinutesUnitFormat) {
        (0..59).map { reminderMinutesUnitFormat.format(it) }
    }
    val shape = AppSpecs.cardShape
    val color = AppColors.scrimNormal

    GlasensePopup(
        popupState = PopupState(
            isVisible = isVisible,
            anchorBounds = anchorBounds
        ),
        onDismiss = onDismiss,
        width = LocalWindowInfo.current.containerDpSize.width - 24.dp,
        popupMargin = 12.dp,
        anchorGap = 12.dp,
        direction = PopupDirection.Down
    ) {
        GlasenseModalTopBar(
            leading = {
                Action(
                    icon = painterResource(id = R.drawable.ic_cross),
                    contentDescription = stringResource(R.string.cancel),
                    onClick = onDismiss
                )
            },
            title = stringResource(R.string.custom_reminder),
            trailing = {
                Action(
                    icon = painterResource(id = R.drawable.ic_checkmark),
                    contentDescription = stringResource(R.string.done),
                    onClick = {
                        val config = when (selectedMode) {
                            ReminderCustomMode.Hour -> TodoReminderConfig(
                                mode = TodoReminderMode.BeforeStart,
                                offsetMinutes = selectedHourBefore * 60 + selectedMinuteBefore
                            )

                            ReminderCustomMode.Day -> TodoReminderConfig(
                                mode = TodoReminderMode.BeforeDueDate,
                                dayOffset = selectedDayBefore,
                                time = java.time.LocalTime.of(selectedHour, selectedMinute)
                            )
                        }
                        onConfirm(config)
                    },
                    colors = AppButtonColors.primary(),
                    highlight = true
                )
            }
        )

        if (!isAllDayEnabled) {
            Spacer(modifier = Modifier.height(12.dp))
            CustomReminderSegmentedControl(
                selectedMode = selectedMode,
                onModeSelected = { selectedMode = it }
            )
        }

        Box {
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(40.dp)
                    .drawWithContent {
                        drawContent()
                        val outline = shape.createOutline(size, layoutDirection, this)
                        drawOutline(outline, color)
                    }
                    .padding(vertical = 8.dp)
            ) {
                if (selectedMode == ReminderCustomMode.Hour) {
                    Spacer(Modifier.weight(1f))
                    HDivider()
                    Spacer(Modifier.weight(1f))
                    HDivider()
                    Spacer(Modifier.weight(1f))
                } else {
                    Spacer(Modifier.weight(1f))
                    HDivider()
                    Spacer(Modifier.weight(1f))
                    HDivider()
                    Spacer(Modifier.weight(1f))
                }
            }

            when (selectedMode) {
                ReminderCustomMode.Hour -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.reminder_before_prefix),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        GlasenseWheelPicker(
                            modifier = Modifier.weight(1f),
                            items = hourOptions,
                            indicator = false,
                            currentSelected = selectedHourBefore
                        ) { index ->
                            selectedHourBefore = index
                        }
                        GlasenseWheelPicker(
                            modifier = Modifier.weight(1f),
                            items = minuteOptions,
                            indicator = false,
                            currentSelected = selectedMinuteBefore
                        ) { index ->
                            selectedMinuteBefore = index
                        }
                    }
                }

                ReminderCustomMode.Day -> {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        GlasenseWheelPicker(
                            modifier = Modifier.weight(1f),
                            items = dayOptions,
                            indicator = false,
                            currentSelected = selectedDayBefore
                        ) { index ->
                            selectedDayBefore = index
                        }
                        GlasenseWheelPicker(
                            modifier = Modifier.weight(1f),
                            items = clockHourOptions,
                            indicator = false,
                            currentSelected = selectedHour
                        ) { index ->
                            selectedHour = index
                        }
                        GlasenseWheelPicker(
                            modifier = Modifier.weight(1f),
                            items = clockMinuteOptions,
                            indicator = false,
                            currentSelected = selectedMinute
                        ) { index ->
                            selectedMinute = index
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomReminderSegmentedControl(
    selectedMode: ReminderCustomMode,
    onModeSelected: (ReminderCustomMode) -> Unit
) {
    val segmentSpacing = 4.dp
    val selectedIndex = if (selectedMode == ReminderCustomMode.Hour) 0f else 1f
    val animatedSelectedIndex by animateFloatAsState(
        targetValue = selectedIndex,
        animationSpec = Springs.smooth(
            durationMillis = 250,
            extraBounce = 0.1,
            visibilityThreshold = 0.0001f
        ),
        label = "CustomReminderSegmentedControlSelectedIndex"
    )

    val indicatorColor = AppColors.segmentedControlIndicator

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Capsule())
            .background(AppColors.segmentedControlBackground)
            .padding(4.dp)
            .drawWithContent {
                val shadowColor = Color.Black.copy(alpha = 0.08f)
                val spacing = segmentSpacing.toPx()
                val indicatorWidth = (size.width - spacing) / 2f
                val indicatorOffset = (indicatorWidth + spacing) * animatedSelectedIndex
                val shadowRadius = 8.dp.toPx()
                val shadowOffsetY = 4.dp.toPx()
                val outline = Capsule().createOutline(
                    size = Size(indicatorWidth, size.height),
                    layoutDirection = layoutDirection,
                    density = this
                )

                withTransform({ translate(left = indicatorOffset) }) {
                    withTransform({ translate(top = shadowOffsetY) }) {
                        drawContext.canvas.nativeCanvas.apply {
                            val paint = Paint().nativePaint.apply {
                                isAntiAlias = true
                                maskFilter =
                                    BlurMaskFilter(shadowRadius, BlurMaskFilter.Blur.NORMAL)
                                color = shadowColor.toArgb()
                            }
                            when (outline) {
                                is Outline.Rectangle -> {
                                    drawContext.canvas.nativeCanvas.drawRect(
                                        outline.rect.left,
                                        outline.rect.top,
                                        outline.rect.right,
                                        outline.rect.bottom,
                                        paint
                                    )
                                }

                                is Outline.Rounded -> {
                                    drawContext.canvas.nativeCanvas.drawRoundRect(
                                        outline.roundRect.left,
                                        outline.roundRect.top,
                                        outline.roundRect.right,
                                        outline.roundRect.bottom,
                                        outline.roundRect.bottomLeftCornerRadius.x,
                                        outline.roundRect.bottomLeftCornerRadius.y,
                                        paint
                                    )
                                }

                                is Outline.Generic -> {
                                    drawContext.canvas.nativeCanvas.drawPath(
                                        outline.path.asAndroidPath(),
                                        paint
                                    )
                                }
                            }
                        }
                    }
                    drawOutline(outline, indicatorColor)
                }
                drawContent()
            },
        horizontalArrangement = Arrangement.spacedBy(segmentSpacing)
    ) {
        CustomReminderSegment(
            text = stringResource(R.string.by_hour),
            selected = selectedMode == ReminderCustomMode.Hour,
            onClick = { onModeSelected(ReminderCustomMode.Hour) },
            modifier = Modifier.weight(1f)
        )
        CustomReminderSegment(
            text = stringResource(R.string.by_day),
            selected = selectedMode == ReminderCustomMode.Day,
            onClick = { onModeSelected(ReminderCustomMode.Day) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CustomReminderSegment(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fontColor =
        if (selected) AppColors.onSegmentedControlIndicator else AppColors.onSegmentedControlBackground

    val hapticController = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .clip(Capsule())
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    hapticController.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onClick()
                }
            )
            .defaultMinSize(minHeight = 32.dp)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            lineHeight = 14.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = fontColor
        )
    }
}
