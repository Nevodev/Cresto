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
import androidx.compose.foundation.lazy.LazyColumn
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
import com.nevoit.cresto.data.todo.TodoReminderMode
import com.nevoit.cresto.theme.AppButtonColors
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.AppSpecs
import com.nevoit.cresto.ui.components.glasense.GlasenseButton
import com.nevoit.cresto.ui.components.glasense.GlasenseMenuItem
import com.nevoit.cresto.ui.components.glasense.GlasenseSwitch
import com.nevoit.cresto.ui.components.glasense.MenuDivider
import com.nevoit.cresto.ui.components.glasense.MenuItemData
import com.nevoit.cresto.ui.components.glasense.extend.overscrollSpacer
import com.nevoit.cresto.ui.components.packed.ConfigItem
import com.nevoit.cresto.ui.components.packed.ConfigItemContainer
import com.nevoit.cresto.ui.components.packed.ConfigTextField
import com.nevoit.cresto.ui.components.packed.TodoReminderConfig
import com.nevoit.cresto.ui.components.packed.displayText
import com.nevoit.glasense.core.component.Icon
import com.nevoit.glasense.core.component.Text
import com.nevoit.glasense.core.component.VDivider
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
    showMenu: (anchorBounds: Rect, items: List<GlasenseMenuItem>) -> Unit,
    onRequestCustomDate: (Rect, LocalDate?, (LocalDate?) -> Unit) -> Unit,
    onRequestCustomTime: (Rect, LocalTime?, LocalTime?, LocalTime?, (LocalTime?) -> Unit) -> Unit,
    onRequestCustomReminder: (Rect) -> Unit,
    navigateToBasic: () -> Unit
) {
    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    var dateButtonBounds by remember { mutableStateOf(Rect.Zero) }
    var rangeStartTimeButtonBounds by remember { mutableStateOf(Rect.Zero) }
    var rangeEndTimeButtonBounds by remember { mutableStateOf(Rect.Zero) }
    var reminderButtonBounds by remember { mutableStateOf(Rect.Zero) }

    val noneText = stringResource(R.string.none)
    val customText = stringResource(R.string.custom)
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
    val reminderIcon = painterResource(R.drawable.ic_alarm)
    val noneReminderIcon = painterResource(R.drawable.ic_alarm_slash)

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
        isAllDayEnabled,
        noneText,
        customText,
        allDayMorningText,
        oneMinuteBeforeText,
        fiveMinutesBeforeText,
        thirtyMinutesBeforeText,
        oneHourBeforeText,
        twoHoursBeforeText,
        reminderIcon,
        noneReminderIcon
    ) {
        buildList {
            if (isAllDayEnabled) {
                add(
                    MenuItemData(
                        text = allDayMorningText,
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
                    MenuItemData(
                        text = oneMinuteBeforeText,
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
                    MenuItemData(
                        text = fiveMinutesBeforeText,
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
                    MenuItemData(
                        text = thirtyMinutesBeforeText,
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
                    MenuItemData(
                        text = oneHourBeforeText,
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
                    MenuItemData(
                        text = twoHoursBeforeText,
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
                MenuItemData(
                    text = customText,
                    onClick = {
                        onRequestCustomReminder(reminderButtonBounds)
                    }
                )
            )
            add(MenuDivider)
            add(
                MenuItemData(
                    text = noneText,
                    icon = noneReminderIcon,
                    onClick = { onReminderConfigChange(null) }
                )
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.elevatedPageBackground)
            .padding(horizontal = 12.dp)
    ) {
        val lazyListState = rememberLazyListState()

        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = navigationBarHeight)
        ) {
            item { Spacer(Modifier.height(48.dp + 12.dp + 12.dp)) }
            item {
                ConfigTextField(
                    value = notesText,
                    onValueChange = onNotesChange,
                    backgroundColor = AppColors.elevatedCardBackground,
                    singleLine = false,
                    decorateText = stringResource(R.string.notes),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Default
                    )
                )
                VGap()
            }
            item {
                CompositionLocalProvider(
                    LocalGlasenseContentColor provides AppColors.contentVariant
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = AppColors.elevatedCardBackground,
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
                VGap()
            }
            item {
                ConfigItemContainer(
                    backgroundColor = AppColors.elevatedCardBackground,
                    title = stringResource(R.string.time)
                ) {
                    Column {
                        ConfigItem(title = stringResource(R.string.all_day)) {
                            GlasenseSwitch(
                                backgroundColor = AppColors.elevatedCardBackground,
                                checked = isAllDayEnabled,
                                onCheckedChange = { checked ->
                                    onAllDayEnabledChange(checked)
                                    if (checked) onTimeRangeEnabledChange(false)
                                })
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        VDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        ConfigItem(title = stringResource(R.string.time_range)) {
                            GlasenseSwitch(
                                backgroundColor = AppColors.elevatedCardBackground,
                                checked = isTimeRangeEnabled,
                                onCheckedChange = { checked ->
                                    onTimeRangeEnabledChange(checked)
                                    if (checked) onAllDayEnabledChange(false)
                                }
                            )
                        }
                        val timeTextStyle = TextStyle(
                            fontFeatureSettings = "tnum",
                            fontWeight = FontWeight.Medium,
                            fontSize = 24.sp,
                            lineHeight = 24.sp,
                            color = AppColors.content
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
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
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                VGap()
            }
            item {
                ConfigItemContainer(
                    backgroundColor = AppColors.elevatedCardBackground,
                    title = stringResource(R.string.reminder)
                ) {
                    Column {
                        ConfigItem(title = stringResource(R.string.reminder_timing)) {
                            Row(
                                modifier = Modifier
                                    .onGloballyPositioned { coordinates ->
                                        reminderButtonBounds = coordinates.boundsInWindow()
                                    }
                                    .wrapContentSize()
                                    .clip(Capsule())
                                    .background(
                                        color = AppColors.scrimNormal
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = DimIndication()
                                    ) {
                                        showMenu(reminderButtonBounds, reminderMenuItems)
                                    }
                                    .padding(
                                        horizontal = 8.dp,
                                        vertical = 4.dp
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = reminderTimingText,
                                    fontWeight = FontWeight.Normal,
                                    color = AppColors.content
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        VDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        ConfigItem(title = stringResource(R.string.persistent_reminder)) {
                            GlasenseSwitch(
                                backgroundColor = AppColors.elevatedCardBackground,
                                checked = reminderPersistent,
                                onCheckedChange = onReminderPersistentChange
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        VDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        ConfigItem(title = stringResource(R.string.strong_reminder)) {
                            GlasenseSwitch(
                                backgroundColor = AppColors.elevatedCardBackground,
                                checked = reminderStrong,
                                onCheckedChange = onReminderStrongChange
                            )
                        }
                    }
                }
                VGap()
            }
//            item {
//                ConfigItemContainer(
//                    backgroundColor = AppColors.elevatedCardBackground,
//                    title = stringResource(R.string.repeat)
//                ) {
//                    Column {
//                        ConfigItem(title = stringResource(R.string.repeat_cycle)) {
//
//                        }
//                        Spacer(modifier = Modifier.height(8.dp))
//                        VDivider()
//                        Spacer(modifier = Modifier.height(8.dp))
//                        ConfigItem(title = stringResource(R.string.postpone_after_expiry)) {
//                            GlasenseSwitch(
//                                backgroundColor = AppColors.elevatedCardBackground,
//                                checked = false,
//                                onCheckedChange = { })
//                        }
//                    }
//                }
//            }
//            item {
//                VGap()
//            }
            overscrollSpacer(lazyListState)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
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
                style = GlasenseTheme.type.smallTitle
            )
        }
    }
}


