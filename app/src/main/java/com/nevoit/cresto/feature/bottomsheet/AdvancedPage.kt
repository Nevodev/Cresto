package com.nevoit.cresto.feature.bottomsheet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.shapes.Capsule
import com.nevoit.cresto.R
import com.nevoit.cresto.data.todo.RepeatFrequency
import com.nevoit.cresto.data.todo.TodoReminderMode
import com.nevoit.cresto.feature.settings.CustomSwitchRow
import com.nevoit.cresto.theme.AppButtonColors
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.AppSpecs
import com.nevoit.cresto.ui.components.glasense.GlasenseButton
import com.nevoit.cresto.ui.components.glasense.GlasenseMenuItem
import com.nevoit.cresto.ui.components.glasense.MenuDivider
import com.nevoit.cresto.ui.components.glasense.SelectiveMenuItemData
import com.nevoit.cresto.ui.components.glasense.extend.overscrollSpacer
import com.nevoit.cresto.ui.components.packed.ConfigTextField
import com.nevoit.cresto.ui.components.packed.TodoReminderConfig
import com.nevoit.cresto.ui.components.packed.displayText
import com.nevoit.glasense.component.ListColors
import com.nevoit.glasense.component.ListRowAccessory
import com.nevoit.glasense.component.ListStack
import com.nevoit.glasense.core.component.Icon
import com.nevoit.glasense.core.component.Text
import com.nevoit.glasense.core.component.VGap
import com.nevoit.glasense.core.interaction.DimIndication
import com.nevoit.glasense.theme.GlasenseTheme
import com.nevoit.glasense.theme.LocalGlasenseContentColor
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun AdvancedPage(
    modifier: Modifier = Modifier,
    notesText: String,
    onNotesChange: (String) -> Unit,
    finalDate: LocalDate?,
    onFinalDateChange: (LocalDate?) -> Unit,
    isTimeRangeEnabled: Boolean,
    isAllDayEnabled: Boolean,
    rangeStartTime: LocalTime,
    rangeEndTime: LocalTime,
    onTimeRangeEnabledChange: (Boolean) -> Unit,
    onAllDayEnabledChange: (Boolean) -> Unit,
    onRangeStartTimeChange: (LocalTime?) -> Unit,
    onRangeEndTimeChange: (LocalTime?) -> Unit,
    reminderConfig: TodoReminderConfig?,
    onReminderConfigChange: (TodoReminderConfig?) -> Unit,
    reminderPersistent: Boolean,
    reminderStrong: Boolean,
    onReminderPersistentChange: (Boolean) -> Unit,
    onReminderStrongChange: (Boolean) -> Unit,
    repeatFrequency: RepeatFrequency?,
    customRepeatConfig: CustomRepeatConfig?,
    onRepeatFrequencyChange: (RepeatFrequency?) -> Unit,
    onRequestCustomRepeat: () -> Unit,
    showMenu: (anchorBounds: Rect, items: List<GlasenseMenuItem>) -> Unit,
    onRequestCustomDate: (Rect, LocalDate?, (LocalDate?) -> Unit) -> Unit,
    onRequestCustomTime: (Rect, LocalTime?, LocalTime?, LocalTime?, (LocalTime?) -> Unit) -> Unit,
    onRequestCustomReminder: (Rect) -> Unit,
    navigateToBasic: () -> Unit
) {
    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val elevatedPageBackground = AppColors.elevatedPageBackground
    val elevatedCardBackground = AppColors.elevatedCardBackground
    val contentVariant = AppColors.contentVariant

    var dateButtonBounds by remember { mutableStateOf(Rect.Zero) }
    var rangeStartTimeButtonBounds by remember { mutableStateOf(Rect.Zero) }
    var rangeEndTimeButtonBounds by remember { mutableStateOf(Rect.Zero) }
    var reminderButtonBounds by remember { mutableStateOf(Rect.Zero) }
    var repeatButtonBounds by remember { mutableStateOf(Rect.Zero) }

    val noneText = stringResource(R.string.none)
    val customText = stringResource(R.string.custom)
    val repeatDailyText = stringResource(R.string.repeat_daily)
    val repeatWeeklyText = stringResource(R.string.repeat_weekly)
    val repeatMonthlyText = stringResource(R.string.repeat_monthly)
    val repeatYearlyText = stringResource(R.string.repeat_yearly)
    val allDayMorningText = stringResource(R.string.reminder_all_day_morning_8)
    val oneMinuteBeforeText = stringResource(R.string.reminder_before_1_minute)
    val fiveMinutesBeforeText = stringResource(R.string.reminder_before_5_minutes)
    val thirtyMinutesBeforeText = stringResource(R.string.reminder_before_30_minutes)
    val oneHourBeforeText = stringResource(R.string.reminder_before_1_hour)
    val twoHoursBeforeText = stringResource(R.string.reminder_before_2_hours)
    val reminderDueDay = stringResource(R.string.reminder_due_day)
    val reminderDaysBeforeFormat = stringResource(R.string.reminder_days_before_format)
    val reminderBeforePrefix = stringResource(R.string.reminder_before_prefix)
    val reminderHoursUnitFormat = stringResource(R.string.reminder_hours_unit_format)
    val reminderMinutesUnitFormat = stringResource(R.string.reminder_minutes_unit_format)

    val reminderTimingText = remember(
        reminderConfig,
        noneText,
        allDayMorningText,
        oneMinuteBeforeText,
        fiveMinutesBeforeText,
        thirtyMinutesBeforeText,
        oneHourBeforeText,
        twoHoursBeforeText,
        reminderDueDay,
        reminderDaysBeforeFormat,
        reminderBeforePrefix,
        reminderHoursUnitFormat,
        reminderMinutesUnitFormat
    ) {
        reminderConfig?.displayText(
            noneText = noneText,
            allDayMorningText = allDayMorningText,
            oneMinuteBeforeText = oneMinuteBeforeText,
            fiveMinutesBeforeText = fiveMinutesBeforeText,
            thirtyMinutesBeforeText = thirtyMinutesBeforeText,
            oneHourBeforeText = oneHourBeforeText,
            twoHoursBeforeText = twoHoursBeforeText,
            beforePrefix = reminderBeforePrefix,
            dueDayText = reminderDueDay,
            daysBeforeFormat = reminderDaysBeforeFormat,
            hoursUnitFormat = reminderHoursUnitFormat,
            minutesUnitFormat = reminderMinutesUnitFormat
        ) ?: noneText
    }

    val reminderMenuItems = remember(
        reminderConfig,
        isAllDayEnabled,
        noneText,
        customText,
        allDayMorningText,
        oneMinuteBeforeText,
        fiveMinutesBeforeText,
        thirtyMinutesBeforeText,
        oneHourBeforeText,
        twoHoursBeforeText
    ) {
        buildList {
            if (isAllDayEnabled) {
                add(
                    SelectiveMenuItemData(
                        text = allDayMorningText,
                        isSelected = { reminderConfig.isAllDayMorningReminder() },
                        onClick = {
                            onReminderConfigChange(
                                TodoReminderConfig(
                                    mode = TodoReminderMode.BeforeDueDate,
                                    dayOffset = 0,
                                    time = LocalTime.of(8, 0)
                                )
                            )
                        })
                )
            } else {
                add(
                    SelectiveMenuItemData(
                        text = oneMinuteBeforeText,
                        isSelected = { reminderConfig.isStartOffsetReminder(1) },
                        onClick = {
                            onReminderConfigChange(
                                TodoReminderConfig(
                                    mode = TodoReminderMode.BeforeStart,
                                    offsetMinutes = 1
                                )
                            )
                        })
                )
                add(
                    SelectiveMenuItemData(
                        text = fiveMinutesBeforeText,
                        isSelected = { reminderConfig.isStartOffsetReminder(5) },
                        onClick = {
                            onReminderConfigChange(
                                TodoReminderConfig(
                                    mode = TodoReminderMode.BeforeStart,
                                    offsetMinutes = 5
                                )
                            )
                        })
                )
                add(
                    SelectiveMenuItemData(
                        text = thirtyMinutesBeforeText,
                        isSelected = { reminderConfig.isStartOffsetReminder(30) },
                        onClick = {
                            onReminderConfigChange(
                                TodoReminderConfig(
                                    mode = TodoReminderMode.BeforeStart,
                                    offsetMinutes = 30
                                )
                            )
                        })
                )
                add(
                    SelectiveMenuItemData(
                        text = oneHourBeforeText,
                        isSelected = { reminderConfig.isStartOffsetReminder(60) },
                        onClick = {
                            onReminderConfigChange(
                                TodoReminderConfig(
                                    mode = TodoReminderMode.BeforeStart,
                                    offsetMinutes = 60
                                )
                            )
                        })
                )
                add(
                    SelectiveMenuItemData(
                        text = twoHoursBeforeText,
                        isSelected = { reminderConfig.isStartOffsetReminder(120) },
                        onClick = {
                            onReminderConfigChange(
                                TodoReminderConfig(
                                    mode = TodoReminderMode.BeforeStart,
                                    offsetMinutes = 120
                                )
                            )
                        })
                )
            }
            add(MenuDivider)
            add(
                SelectiveMenuItemData(
                    text = customText,
                    isSelected = { reminderConfig.isCustomReminder(isAllDayEnabled) },
                    onClick = {
                        onRequestCustomReminder(reminderButtonBounds)
                    }
                )
            )
            add(MenuDivider)
            add(
                SelectiveMenuItemData(
                    text = noneText,
                    isSelected = { reminderConfig == null },
                    onClick = { onReminderConfigChange(null) }
                )
            )
        }
    }

    val repeatText = remember(
        repeatFrequency,
        customRepeatConfig,
        noneText,
        customText,
        repeatDailyText,
        repeatWeeklyText,
        repeatMonthlyText,
        repeatYearlyText
    ) {
        if (customRepeatConfig != null) {
            customText
        } else {
            repeatFrequency.displayText(
                noneText = noneText,
                dailyText = repeatDailyText,
                weeklyText = repeatWeeklyText,
                monthlyText = repeatMonthlyText,
                yearlyText = repeatYearlyText
            )
        }
    }

    val repeatMenuItems = remember(
        repeatFrequency,
        customRepeatConfig,
        noneText,
        customText,
        repeatDailyText,
        repeatWeeklyText,
        repeatMonthlyText,
        repeatYearlyText
    ) {
        listOf(
            SelectiveMenuItemData(
                text = repeatDailyText,
                isSelected = { repeatFrequency == RepeatFrequency.Daily },
                onClick = { onRepeatFrequencyChange(RepeatFrequency.Daily) }
            ),
            SelectiveMenuItemData(
                text = repeatWeeklyText,
                isSelected = { repeatFrequency == RepeatFrequency.Weekly },
                onClick = { onRepeatFrequencyChange(RepeatFrequency.Weekly) }
            ),
            SelectiveMenuItemData(
                text = repeatMonthlyText,
                isSelected = { repeatFrequency == RepeatFrequency.Monthly },
                onClick = { onRepeatFrequencyChange(RepeatFrequency.Monthly) }
            ),
            SelectiveMenuItemData(
                text = repeatYearlyText,
                isSelected = { repeatFrequency == RepeatFrequency.Yearly },
                onClick = { onRepeatFrequencyChange(RepeatFrequency.Yearly) }
            ),
            MenuDivider,
            SelectiveMenuItemData(
                text = customText,
                isSelected = { customRepeatConfig != null },
                onClick = onRequestCustomRepeat
            ),
            MenuDivider,
            SelectiveMenuItemData(
                text = noneText,
                isSelected = { repeatFrequency == null && customRepeatConfig == null },
                onClick = { onRepeatFrequencyChange(null) }
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(elevatedPageBackground)
    ) {
        val lazyListState = rememberLazyListState()

        ListStack(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            colors = ListColors(
                background = elevatedPageBackground,
                rowBackground = elevatedCardBackground,
                headerText = contentVariant,
                footerText = contentVariant.copy(alpha = .3f)
            ),
            cornerRadius = AppSpecs.cardCorner,
            contentPadding = PaddingValues(bottom = navigationBarHeight)
        ) {
            item { Spacer(Modifier.height(48.dp + 12.dp + 12.dp)) }
            item {
                ConfigTextField(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    value = notesText,
                    onValueChange = onNotesChange,
                    backgroundColor = elevatedCardBackground,
                    singleLine = false,
                    decorateText = stringResource(R.string.notes),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Default
                    )
                )
                VGap(24.dp)
            }
            item {
                CompositionLocalProvider(
                    LocalGlasenseContentColor provides contentVariant
                ) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .fillMaxWidth()
                            .background(
                                color = elevatedCardBackground,
                                shape = AppSpecs.cardShape
                            )
                            .padding(horizontal = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter =
                                    painterResource(id = R.drawable.ic_calendar),
                                contentDescription = stringResource(R.string.due_date),
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .width(28.dp)
                            )
                            Text(
                                text = stringResource(R.string.due_date),
                                fontWeight = FontWeight.Normal,
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .onGloballyPositioned { coordinates ->
                                        dateButtonBounds = coordinates.boundsInWindow()
                                    }
                                    .align(Alignment.CenterVertically)
                                    .wrapContentSize()
                                    .clip(Capsule())
                                    .background(
                                        color = AppColors.scrimNormal
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = DimIndication()
                                    ) {
                                        onRequestCustomDate(
                                            dateButtonBounds,
                                            finalDate
                                        ) { newDate ->
                                            onFinalDateChange(newDate)
                                        }
                                    }
                            ) {
                                Text(
                                    text = finalDate?.format(DateTimeFormatter.ofPattern("yyyy/M/d"))
                                        ?: stringResource(R.string.none),
                                    fontWeight = FontWeight.Normal,
                                    modifier = Modifier.padding(
                                        horizontal = 8.dp,
                                        vertical = 4.dp
                                    ),
                                    color = AppColors.content
                                )
                            }
                        }
                    }
                }
            }
            Section(
                header = { stringResource(R.string.time) },
                topSpacing = 24.dp
            ) {
                CustomSwitchRow(
                    checked = isAllDayEnabled,
                    onCheckedChange = { checked ->
                        onAllDayEnabledChange(checked)
                        if (checked) onTimeRangeEnabledChange(false)
                    },
                    backgroundColor = elevatedCardBackground
                ) {
                    Text(stringResource(R.string.all_day))
                }
                CustomSwitchRow(
                    checked = isTimeRangeEnabled,
                    onCheckedChange = { checked ->
                        onTimeRangeEnabledChange(checked)
                        if (checked) onAllDayEnabledChange(false)
                    },
                    backgroundColor = elevatedCardBackground
                ) {
                    Text(stringResource(R.string.time_range))
                }
                Row {
                    val timeTextStyle = TextStyle(
                        fontFeatureSettings = "tnum",
                        fontWeight = FontWeight.Medium,
                        fontSize = 24.sp,
                        lineHeight = 24.sp,
                        color = AppColors.content
                    )
                    Row(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .graphicsLayer {
                                alpha = if (isAllDayEnabled) 0.5f else 1f
                            }
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(
                            12.dp,
                            Alignment.CenterHorizontally
                        )
                    ) {
                        if (isTimeRangeEnabled) {
                            Box(
                                modifier = Modifier
                                    .onGloballyPositioned { coordinates ->
                                        rangeStartTimeButtonBounds =
                                            coordinates.boundsInWindow()
                                    }
                                    .clip(AppSpecs.cardShape)
                                    .background(color = AppColors.scrimNormal)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = DimIndication(),
                                        enabled = !isAllDayEnabled
                                    ) {
                                        onRequestCustomTime(
                                            rangeStartTimeButtonBounds,
                                            rangeStartTime,
                                            null,
                                            rangeEndTime
                                        ) { newTime ->
                                            onRangeStartTimeChange(newTime)
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = rangeStartTime.format(
                                        DateTimeFormatter.ofPattern(
                                            "HH:mm"
                                        )
                                    ),
                                    modifier = Modifier.align(Alignment.Center),
                                    style = timeTextStyle
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .onGloballyPositioned { coordinates ->
                                        rangeEndTimeButtonBounds =
                                            coordinates.boundsInWindow()
                                    }
                                    .clip(AppSpecs.cardShape)
                                    .background(color = AppColors.scrimNormal)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = DimIndication(),
                                        enabled = !isAllDayEnabled
                                    ) {
                                        onRequestCustomTime(
                                            rangeEndTimeButtonBounds,
                                            rangeEndTime,
                                            rangeStartTime,
                                            null
                                        ) { newTime ->
                                            onRangeEndTimeChange(newTime)
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = rangeEndTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                                    modifier = Modifier.align(Alignment.Center),
                                    style = timeTextStyle
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .onGloballyPositioned { coordinates ->
                                        rangeStartTimeButtonBounds =
                                            coordinates.boundsInWindow()
                                    }
                                    .clip(AppSpecs.cardShape)
                                    .background(color = AppColors.scrimNormal)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = DimIndication(),
                                        enabled = !isAllDayEnabled
                                    ) {
                                        onRequestCustomTime(
                                            rangeStartTimeButtonBounds,
                                            rangeStartTime,
                                            null,
                                            null
                                        ) { newTime ->
                                            onRangeStartTimeChange(newTime)
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = rangeStartTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                                    modifier = Modifier.align(Alignment.Center),
                                    style = timeTextStyle
                                )
                            }
                        }
                    }
                }
            }
            Section(
                header = { stringResource(R.string.reminder) }
            ) {
                Row(
                    onClick = { showMenu(reminderButtonBounds, reminderMenuItems) },
                    trailing = {
                        Text(
                            text = reminderTimingText,
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                reminderButtonBounds = coordinates.boundsInWindow()
                            }
                        )
                    },
                    accessory = ListRowAccessory.SelectIndicator
                ) {
                    Text(stringResource(R.string.reminder_timing))
                }
                CustomSwitchRow(
                    checked = reminderPersistent,
                    onCheckedChange = onReminderPersistentChange,
                    backgroundColor = elevatedCardBackground
                ) {
                    Text(stringResource(R.string.persistent_reminder))
                }
                CustomSwitchRow(
                    checked = reminderStrong,
                    onCheckedChange = onReminderStrongChange,
                    backgroundColor = elevatedCardBackground
                ) {
                    Text(stringResource(R.string.strong_reminder))
                }
            }
            Section(
                header = { stringResource(R.string.repeat) }
            ) {
                Row(
                    onClick = { showMenu(repeatButtonBounds, repeatMenuItems) },
                    trailing = {
                        Text(
                            text = repeatText,
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                repeatButtonBounds = coordinates.boundsInWindow()
                            }
                        )
                    },
                    accessory = ListRowAccessory.SelectIndicator
                ) {
                    Text(stringResource(R.string.repeat_cycle))
                }
            }
            item { VGap() }
            overscrollSpacer(lazyListState)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, start = 12.dp, end = 12.dp)
                .height(48.dp)
        ) {
            GlasenseButton(
                enabled = true,
                shape = CircleShape,
                onClick = { navigateToBasic() },
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
                text = stringResource(R.string.advanced),
                modifier = Modifier.align(Alignment.Center),
                style = GlasenseTheme.type.headline
            )
        }
    }
}

private fun RepeatFrequency?.displayText(
    noneText: String,
    dailyText: String,
    weeklyText: String,
    monthlyText: String,
    yearlyText: String
): String {
    return when (this) {
        RepeatFrequency.Daily -> dailyText
        RepeatFrequency.Weekly -> weeklyText
        RepeatFrequency.Monthly -> monthlyText
        RepeatFrequency.Yearly -> yearlyText
        null -> noneText
    }
}

private fun TodoReminderConfig?.isAllDayMorningReminder(): Boolean {
    return this?.mode == TodoReminderMode.BeforeDueDate &&
            dayOffset == 0 &&
            time == LocalTime.of(8, 0)
}

private fun TodoReminderConfig?.isStartOffsetReminder(offsetMinutes: Int): Boolean {
    return this?.mode == TodoReminderMode.BeforeStart && this.offsetMinutes == offsetMinutes
}

private fun TodoReminderConfig?.isCustomReminder(isAllDayEnabled: Boolean): Boolean {
    if (this == null) return false
    return if (isAllDayEnabled) {
        !isAllDayMorningReminder()
    } else {
        !listOf(1, 5, 30, 60, 120).any { isStartOffsetReminder(it) }
    }
}


