package com.nevoit.cresto.ui.components.bottomsheet

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
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.nevoit.cresto.theme.AppButtonColors
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.AppSpecs
import com.nevoit.cresto.ui.components.glasense.DimIndication
import com.nevoit.cresto.ui.components.glasense.GlasenseButton
import com.nevoit.cresto.ui.components.glasense.GlasenseMenuItem
import com.nevoit.cresto.ui.components.glasense.GlasenseSwitch
import com.nevoit.cresto.ui.components.glasense.MenuDivider
import com.nevoit.cresto.ui.components.glasense.MenuItemData
import com.nevoit.cresto.ui.components.glasense.ZeroHeightDivider
import com.nevoit.cresto.ui.components.glasense.extend.overscrollSpacer
import com.nevoit.cresto.ui.components.packed.ConfigItem
import com.nevoit.cresto.ui.components.packed.ConfigItemContainer
import com.nevoit.cresto.ui.components.packed.ConfigTextField
import com.nevoit.cresto.ui.components.packed.VGap
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
    showMenu: (anchorBounds: Rect, items: List<GlasenseMenuItem>) -> Unit,
    onRequestCustomDate: (Rect, LocalDate?, (LocalDate?) -> Unit) -> Unit,
    onRequestCustomTime: (Rect, LocalTime?, LocalTime?, LocalTime?, (LocalTime?) -> Unit) -> Unit,
    navigateToBasic: () -> Unit
) {
    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    var dateButtonBounds by remember { mutableStateOf(Rect.Zero) }
    var rangeStartTimeButtonBounds by remember { mutableStateOf(Rect.Zero) }
    var rangeEndTimeButtonBounds by remember { mutableStateOf(Rect.Zero) }
    var reminderButtonBounds by remember { mutableStateOf(Rect.Zero) }
    var reminderTimingText by remember { mutableStateOf<String?>(null) }

    val noneText = stringResource(R.string.none)
    val customText = stringResource(R.string.custom)
    val allDayMorningText = stringResource(R.string.reminder_all_day_morning_8)
    val oneMinuteBeforeText = stringResource(R.string.reminder_before_1_minute)
    val fiveMinutesBeforeText = stringResource(R.string.reminder_before_5_minutes)
    val thirtyMinutesBeforeText = stringResource(R.string.reminder_before_30_minutes)
    val oneHourBeforeText = stringResource(R.string.reminder_before_1_hour)
    val twoHoursBeforeText = stringResource(R.string.reminder_before_2_hours)
    val reminderIcon = painterResource(R.drawable.ic_alarm)
    val noneReminderIcon = painterResource(R.drawable.ic_alarm_slash)

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
                        onClick = { reminderTimingText = allDayMorningText })
                )
            } else {
                add(
                    MenuItemData(
                        text = oneMinuteBeforeText,
                        onClick = { reminderTimingText = oneMinuteBeforeText })
                )
                add(
                    MenuItemData(
                        text = fiveMinutesBeforeText,
                        onClick = { reminderTimingText = fiveMinutesBeforeText })
                )
                add(
                    MenuItemData(
                        text = thirtyMinutesBeforeText,
                        onClick = { reminderTimingText = thirtyMinutesBeforeText })
                )
                add(
                    MenuItemData(
                        text = oneHourBeforeText,
                        onClick = { reminderTimingText = oneHourBeforeText })
                )
                add(
                    MenuItemData(
                        text = twoHoursBeforeText,
                        onClick = { reminderTimingText = twoHoursBeforeText })
                )
            }
            add(MenuDivider)
            add(MenuItemData(text = customText, onClick = { reminderTimingText = customText }))
            add(MenuDivider)
            add(
                MenuItemData(
                    text = noneText,
                    icon = noneReminderIcon,
                    onClick = { reminderTimingText = noneText }
                )
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.pageBackgroundElevated)
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
                    backgroundColor = AppColors.cardBackgroundElevated,
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
                    LocalContentColor provides AppColors.contentVariant
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = AppColors.cardBackgroundElevated,
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
                                fontSize = 16.sp,
                                lineHeight = 18.sp,
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
                                    fontSize = 16.sp,
                                    lineHeight = 18.sp,
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
                    backgroundColor = AppColors.cardBackgroundElevated,
                    title = stringResource(R.string.time)
                ) {
                    Column {
                        ConfigItem(title = stringResource(R.string.all_day)) {
                            GlasenseSwitch(
                                backgroundColor = AppColors.cardBackgroundElevated,
                                checked = isAllDayEnabled,
                                onCheckedChange = { checked ->
                                    onAllDayEnabledChange(checked)
                                    if (checked) onTimeRangeEnabledChange(false)
                                })
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        ZeroHeightDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        ConfigItem(title = stringResource(R.string.time_range)) {
                            GlasenseSwitch(
                                backgroundColor = AppColors.cardBackgroundElevated,
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
                    backgroundColor = AppColors.cardBackgroundElevated,
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
                                    text = reminderTimingText ?: noneText,
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
                                checked = false,
                                onCheckedChange = { })
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        ZeroHeightDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        ConfigItem(title = stringResource(R.string.strong_reminder)) {
                            GlasenseSwitch(
                                backgroundColor = AppColors.cardBackgroundElevated,
                                checked = false,
                                onCheckedChange = { })
                        }
                    }
                }
                VGap()
            }
            item {
                ConfigItemContainer(
                    backgroundColor = AppColors.cardBackgroundElevated,
                    title = stringResource(R.string.repeat)
                ) {
                    Column {
                        ConfigItem(title = stringResource(R.string.repeat_cycle)) {

                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        ZeroHeightDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        ConfigItem(title = stringResource(R.string.postpone_after_expiry)) {
                            GlasenseSwitch(
                                backgroundColor = AppColors.cardBackgroundElevated,
                                checked = false,
                                onCheckedChange = { })
                        }
                    }
                }
            }
            item {
                VGap()
            }
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
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}