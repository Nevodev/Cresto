package com.nevoit.cresto.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.shapes.Capsule
import com.nevoit.cresto.R
import com.nevoit.cresto.data.todo.TodoReminderMode
import com.nevoit.cresto.theme.AppButtonColors
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.ui.components.glasense.DimIndication
import com.nevoit.cresto.ui.components.glasense.GlasenseBottomSheet
import com.nevoit.cresto.ui.components.glasense.GlasenseButton
import com.nevoit.cresto.ui.components.glasense.GlasenseMenuItem
import com.nevoit.cresto.ui.components.glasense.GlasenseSwitch
import com.nevoit.cresto.ui.components.glasense.MenuDivider
import com.nevoit.cresto.ui.components.glasense.MenuItemData
import com.nevoit.cresto.ui.components.glasense.ZeroHeightDivider
import com.nevoit.cresto.ui.components.packed.ConfigItem
import com.nevoit.cresto.ui.components.packed.ConfigItemContainer
import com.nevoit.cresto.ui.components.packed.TodoReminderConfig
import com.nevoit.cresto.ui.components.packed.displayText
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
                        MenuItemData(
                            text = text,
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
                MenuItemData(
                    text = customText,
                    onClick = { onRequestCustomReminder(reminderButtonBounds) }
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

    GlasenseBottomSheet(onDismissed = onDismissed) { slideOut ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(top = 12.dp, bottom = navigationBarHeight + 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                GlasenseButton(
                    enabled = true,
                    shape = CircleShape,
                    onClick = slideOut,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.TopStart),
                    colors = AppButtonColors.action()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_forward_nav),
                        contentDescription = stringResource(R.string.back),
                        modifier = Modifier.width(32.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.reminder),
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            ConfigItemContainer(
                backgroundColor = AppColors.cardBackgroundElevated
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
                                .background(color = AppColors.scrimNormal)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = DimIndication()
                                ) {
                                    showMenu(reminderButtonBounds, reminderMenuItems)
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = reminderTimingText,
                                fontSize = 16.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.Normal,
                                color = AppColors.content
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    ZeroHeightDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    ConfigItem(title = stringResource(R.string.persistent_reminder)) {
                        GlasenseSwitch(
                            backgroundColor = AppColors.cardBackgroundElevated,
                            checked = reminderPersistent,
                            onCheckedChange = onPersistentChange
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    ZeroHeightDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    ConfigItem(title = stringResource(R.string.strong_reminder)) {
                        GlasenseSwitch(
                            backgroundColor = AppColors.cardBackgroundElevated,
                            checked = reminderStrong,
                            onCheckedChange = onStrongChange
                        )
                    }
                }
            }
        }
    }
}
