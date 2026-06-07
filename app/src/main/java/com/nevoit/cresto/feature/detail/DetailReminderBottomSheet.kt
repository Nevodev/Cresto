package com.nevoit.cresto.feature.detail

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nevoit.cresto.R
import com.nevoit.cresto.data.todo.TodoReminderMode
import com.nevoit.cresto.feature.settings.CustomSwitchRow
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.AppSpecs
import com.nevoit.cresto.ui.components.glasense.GlasenseMenuItem
import com.nevoit.cresto.ui.components.glasense.GlasenseModalTopBar
import com.nevoit.cresto.ui.components.glasense.MenuDivider
import com.nevoit.cresto.ui.components.glasense.SelectiveMenuItemData
import com.nevoit.cresto.ui.components.glasense.extend.overscrollSpacer
import com.nevoit.cresto.ui.components.packed.TodoReminderConfig
import com.nevoit.cresto.ui.components.packed.displayText
import com.nevoit.glasense.component.BottomSheet
import com.nevoit.glasense.component.ListColors
import com.nevoit.glasense.component.ListRowAccessory
import com.nevoit.glasense.component.ListStack
import com.nevoit.glasense.core.component.Text
import com.nevoit.glasense.core.component.VGap
import java.time.LocalTime

@Composable
fun DetailReminderBottomSheet(
    reminderConfig: TodoReminderConfig?,
    reminderPersistent: Boolean,
    reminderStrong: Boolean,
    isAllDayEnabled: Boolean,
    onReminderConfigChange: (TodoReminderConfig?) -> Unit,
    onPersistentChange: (Boolean) -> Unit,
    onStrongChange: (Boolean) -> Unit,
    showMenu: (anchorBounds: Rect, items: List<GlasenseMenuItem>) -> Unit,
    onRequestCustomReminder: (Rect) -> Unit,
    onDismissed: () -> Unit
) {
    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val listState = rememberLazyListState()
    val elevatedPageBackground = AppColors.elevatedPageBackground
    val elevatedCardBackground = AppColors.elevatedCardBackground
    val contentVariant = AppColors.contentVariant

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
        reminderConfig,
        isAllDayEnabled,
        reminderPersistent,
        reminderStrong,
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
                    SelectiveMenuItemData(
                        text = allDayMorningText,
                        isSelected = { reminderConfig.isAllDayMorningReminder() },
                        onClick = {
                            onReminderConfigChange(
                                TodoReminderConfig(
                                    mode = TodoReminderMode.BeforeDueDate,
                                    dayOffset = 0,
                                    time = LocalTime.of(8, 0),
                                    persistent = reminderPersistent,
                                    strong = reminderStrong
                                )
                            )
                        }
                    )
                )
            } else {
                listOf(
                    oneMinuteBeforeText to 1,
                    fiveMinutesBeforeText to 5,
                    thirtyMinutesBeforeText to 30,
                    oneHourBeforeText to 60,
                    twoHoursBeforeText to 120
                ).forEach { (text, offsetMinutes) ->
                    add(
                        SelectiveMenuItemData(
                            text = text,
                            isSelected = { reminderConfig.isStartOffsetReminder(offsetMinutes) },
                            onClick = {
                                onReminderConfigChange(
                                    TodoReminderConfig(
                                        mode = TodoReminderMode.BeforeStart,
                                        offsetMinutes = offsetMinutes,
                                        persistent = reminderPersistent,
                                        strong = reminderStrong
                                    )
                                )
                            }
                        )
                    )
                }
            }
            add(MenuDivider)
            add(
                SelectiveMenuItemData(
                    text = customText,
                    isSelected = { reminderConfig.isCustomReminder(isAllDayEnabled) },
                    onClick = { onRequestCustomReminder(reminderButtonBounds) }
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

    BottomSheet(onDismissed = onDismissed) { slideOut ->
        ListStack(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            colors = ListColors(
                background = elevatedPageBackground,
                rowBackground = elevatedCardBackground,
                headerText = contentVariant,
                footerText = contentVariant.copy(alpha = .3f)
            ),
            cornerRadius = AppSpecs.cardCorner,
            contentPadding = PaddingValues(bottom = navigationBarHeight)
        ) {
            item { VGap(72.dp) }
            Section(topSpacing = 0.dp) {
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
                    onCheckedChange = onPersistentChange,
                    backgroundColor = elevatedCardBackground
                ) {
                    Text(stringResource(R.string.persistent_reminder))
                }
                CustomSwitchRow(
                    checked = reminderStrong,
                    onCheckedChange = onStrongChange,
                    backgroundColor = elevatedCardBackground
                ) {
                    Text(stringResource(R.string.strong_reminder))
                }
            }
            item { VGap() }
            overscrollSpacer(listState)
        }

        GlasenseModalTopBar(
            leading = {
                Action(
                    icon = painterResource(id = R.drawable.ic_forward_nav),
                    contentDescription = stringResource(R.string.back),
                    onClick = slideOut,
                    iconSize = 32.dp
                )
            },
            title = stringResource(R.string.reminder),
            modifier = Modifier.padding(12.dp)
        )
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
