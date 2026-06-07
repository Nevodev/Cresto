package com.nevoit.cresto.feature.bottomsheet

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.nevoit.cresto.R
import com.nevoit.cresto.data.todo.RepeatFrequency
import com.nevoit.cresto.theme.AppButtonColors
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.AppSpecs
import com.nevoit.cresto.ui.components.glasense.GlasenseButtonCompact
import com.nevoit.cresto.ui.components.glasense.GlasenseMenuItem
import com.nevoit.cresto.ui.components.glasense.GlasenseModalTopBar
import com.nevoit.cresto.ui.components.glasense.GlasensePopup
import com.nevoit.cresto.ui.components.glasense.GlasenseWheelPicker
import com.nevoit.cresto.ui.components.glasense.PopupDirection
import com.nevoit.cresto.ui.components.glasense.PopupState
import com.nevoit.cresto.ui.components.glasense.SelectiveMenuItemData
import com.nevoit.cresto.ui.components.glasense.extend.overscrollSpacer
import com.nevoit.glasense.component.BottomSheet
import com.nevoit.glasense.component.ListColors
import com.nevoit.glasense.component.ListRowAccessory
import com.nevoit.glasense.component.ListStack
import com.nevoit.glasense.component.SectionScope
import com.nevoit.glasense.core.component.HDivider
import com.nevoit.glasense.core.component.Icon
import com.nevoit.glasense.core.component.Text
import com.nevoit.glasense.core.component.VDivider
import com.nevoit.glasense.core.component.VGap
import com.nevoit.glasense.theme.GlasenseTheme
import com.nevoit.glasense.theme.LocalGlasenseContentColor
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as DateTextStyle

enum class CustomRepeatEndMode {
    Never,
    OnDate,
    AfterCount
}

data class CustomRepeatConfig(
    val frequency: RepeatFrequency = RepeatFrequency.Weekly,
    val interval: Int = 1,
    val weekdays: Set<DayOfWeek> = setOf(LocalDate.now().dayOfWeek),
    val monthDays: Set<Int> = setOf(LocalDate.now().dayOfMonth),
    val months: Set<Int> = setOf(LocalDate.now().monthValue),
    val endMode: CustomRepeatEndMode = CustomRepeatEndMode.Never,
    val endDate: LocalDate? = null,
    val maxOccurrences: Int = 10
)

@Composable
fun CustomRepeatBottomSheet(
    initialDate: LocalDate?,
    initialConfig: CustomRepeatConfig?,
    showMenu: (anchorBounds: Rect, items: List<GlasenseMenuItem>) -> Unit,
    onConfirm: (CustomRepeatConfig) -> Unit,
    onDismissed: () -> Unit
) {
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    val anchorDate = initialDate ?: LocalDate.now()
    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val elevatedPageBackground = AppColors.elevatedPageBackground
    val elevatedCardBackground = AppColors.elevatedCardBackground
    val contentVariant = AppColors.contentVariant

    var frequency by remember(initialConfig, anchorDate) {
        mutableStateOf(initialConfig?.frequency ?: RepeatFrequency.Weekly)
    }
    var interval by remember(initialConfig) { mutableIntStateOf(initialConfig?.interval ?: 1) }
    var weekdays by remember(initialConfig, anchorDate) {
        mutableStateOf(initialConfig?.weekdays ?: setOf(anchorDate.dayOfWeek))
    }
    var monthDays by remember(initialConfig, anchorDate) {
        mutableStateOf(initialConfig?.monthDays ?: setOf(anchorDate.dayOfMonth))
    }
    var months by remember(initialConfig, anchorDate) {
        mutableStateOf(initialConfig?.months ?: setOf(anchorDate.monthValue))
    }
    var endMode by remember(initialConfig) {
        mutableStateOf(initialConfig?.endMode ?: CustomRepeatEndMode.Never)
    }
    var endDate by remember(initialConfig, anchorDate) {
        mutableStateOf(initialConfig?.endDate ?: anchorDate.plusMonths(1))
    }
    var maxOccurrences by remember(initialConfig) {
        mutableIntStateOf(initialConfig?.maxOccurrences ?: 10)
    }
    var frequencyButtonBounds by remember { mutableStateOf(Rect.Zero) }
    var endModeButtonBounds by remember { mutableStateOf(Rect.Zero) }
    var endDateButtonBounds by remember { mutableStateOf(Rect.Zero) }
    var isEndDatePickerVisible by remember { mutableStateOf(false) }
    var contentBounds by remember { mutableStateOf(Rect.Zero) }
    var intervalNumberInputBounds by remember { mutableStateOf(Rect.Zero) }
    var occurrencesNumberInputBounds by remember { mutableStateOf(Rect.Zero) }
    val endDateFormatter = remember { DateTimeFormatter.ofPattern("yyyy/M/d") }

    val frequencyLabels = mapOf(
        RepeatFrequency.Daily to stringResource(R.string.repeat_daily),
        RepeatFrequency.Weekly to stringResource(R.string.repeat_weekly),
        RepeatFrequency.Monthly to stringResource(R.string.repeat_monthly),
        RepeatFrequency.Yearly to stringResource(R.string.repeat_yearly)
    )

    val frequencyMenuItems = remember(frequency, frequencyLabels) {
        listOf(
            RepeatFrequency.Daily,
            RepeatFrequency.Weekly,
            RepeatFrequency.Monthly,
            RepeatFrequency.Yearly
        ).map { option ->
            SelectiveMenuItemData(
                text = frequencyLabels.getValue(option),
                isSelected = { frequency == option },
                onClick = { frequency = option }
            )
        }
    }

    val endModeLabels = mapOf(
        CustomRepeatEndMode.Never to stringResource(R.string.repeat_end_never),
        CustomRepeatEndMode.OnDate to stringResource(R.string.repeat_end_date),
        CustomRepeatEndMode.AfterCount to stringResource(R.string.repeat_end_count)
    )

    val endModeIcons = mapOf(
        CustomRepeatEndMode.Never to painterResource(id = R.drawable.ic_infinity),
        CustomRepeatEndMode.OnDate to painterResource(id = R.drawable.ic_calendar),
        CustomRepeatEndMode.AfterCount to painterResource(id = R.drawable.ic_arrow_forward_circle_dotted)
    )

    val endModeMenuItems = remember(endMode, endModeLabels) {
        listOf(
            CustomRepeatEndMode.Never,
            CustomRepeatEndMode.OnDate,
            CustomRepeatEndMode.AfterCount
        ).map { option ->
            SelectiveMenuItemData(
                text = endModeLabels.getValue(option),
                isSelected = { endMode == option },
                onClick = { endMode = option },
                icon = endModeIcons.getValue(option)
            )
        }
    }

    fun currentConfig(): CustomRepeatConfig {
        return CustomRepeatConfig(
            frequency = frequency,
            interval = interval,
            weekdays = weekdays.ifEmpty { setOf(anchorDate.dayOfWeek) },
            monthDays = monthDays.ifEmpty { setOf(anchorDate.dayOfMonth) },
            months = months.ifEmpty { setOf(anchorDate.monthValue) },
            endMode = endMode,
            endDate = if (endMode == CustomRepeatEndMode.OnDate) endDate else null,
            maxOccurrences = maxOccurrences
        )
    }

    fun saveAndClose(slideOut: () -> Unit) {
        onConfirm(currentConfig())
        slideOut()
    }

    BottomSheet(onDismissed = onDismissed) { slideOut ->
        BackHandler {
            saveAndClose(slideOut)
        }

        ListStack(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .onGloballyPositioned { coordinates ->
                    contentBounds = coordinates.boundsInWindow()
                }
                .pointerInput(
                    focusManager,
                    contentBounds,
                    intervalNumberInputBounds,
                    occurrencesNumberInputBounds
                ) {
                    awaitEachGesture {
                        val down = awaitFirstDown(
                            requireUnconsumed = false,
                            pass = PointerEventPass.Initial
                        )
                        val downPositionInWindow = Offset(
                            x = contentBounds.left + down.position.x,
                            y = contentBounds.top + down.position.y
                        )
                        val isNumberInputTouched =
                            intervalNumberInputBounds.contains(downPositionInWindow) ||
                                    occurrencesNumberInputBounds.contains(downPositionInWindow)
                        if (!isNumberInputTouched) {
                            focusManager.clearFocus()
                        }
                    }
                },
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
            Section(key = "frequency", topSpacing = 0.dp) {
                Row(
                    onClick = {
                        showMenu(
                            frequencyButtonBounds,
                            frequencyMenuItems
                        )
                    },
                    trailing = {
                        Text(
                            text = frequencyLabels.getValue(frequency),
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                frequencyButtonBounds = coordinates.boundsInWindow()
                            }
                        )
                    },
                    accessory = ListRowAccessory.SelectIndicator
                ) {
                    Text(stringResource(R.string.repeat_frequency))
                }
                Row(
                    trailing = {
                        NumberStepper(
                            value = interval,
                            minValue = 1,
                            maxValue = 99,
                            onPositioned = { intervalNumberInputBounds = it },
                            onValueChange = { interval = it }
                        )
                    }
                ) {
                    Text(stringResource(R.string.repeat_every))
                }
            }
            when (frequency) {
                RepeatFrequency.Weekly -> {
                    Section(
                        key = "weekly",
                        header = { stringResource(R.string.repeat_weekdays) }
                    ) {
                        WeekdayRows(
                            selected = weekdays,
                            onSelectedChange = { weekdays = it }
                        )
                    }
                }

                RepeatFrequency.Monthly -> {
                    NoPaddingSection(
                        key = "monthly",
                        header = { stringResource(R.string.repeat_month_days) }
                    ) {
                        Row {
                            NumberGrid(
                                values = (1..31).toList(),
                                selected = monthDays,
                                columns = 7,
                                onSelectedChange = { monthDays = it }
                            )
                        }
                    }
                }

                RepeatFrequency.Yearly -> {
                    NoPaddingSection(
                        key = "yearly",
                        header = { stringResource(R.string.repeat_months) }
                    ) {
                        Row {
                            NumberGrid(
                                values = (1..12).toList(),
                                selected = months,
                                columns = 4,
                                label = { monthLabel(it) },
                                onSelectedChange = { months = it }
                            )
                        }
                    }
                }

                RepeatFrequency.Daily -> Unit
            }
            Section(key = "end_mode") {
                Row(
                    onClick = {
                        showMenu(
                            endModeButtonBounds,
                            endModeMenuItems
                        )
                    },
                    trailing = {
                        Text(
                            text = endModeLabels.getValue(endMode),
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                endModeButtonBounds = coordinates.boundsInWindow()
                            }
                        )
                    },
                    accessory = ListRowAccessory.SelectIndicator
                ) {
                    Text(stringResource(R.string.repeat_end))
                }
                if (endMode == CustomRepeatEndMode.OnDate) {
                    Row(
                        onClick = { isEndDatePickerVisible = true },
                        trailing = {
                            Text(
                                text = endDate.format(endDateFormatter),
                                modifier = Modifier.onGloballyPositioned { coordinates ->
                                    endDateButtonBounds = coordinates.boundsInWindow()
                                }
                            )
                        },
                        accessory = ListRowAccessory.SelectIndicator
                    ) {
                        Text(stringResource(R.string.repeat_until_date))
                    }
                }
                if (endMode == CustomRepeatEndMode.AfterCount) {
                    Row(
                        trailing = {
                            NumberStepper(
                                value = maxOccurrences,
                                minValue = 1,
                                maxValue = 999,
                                onPositioned = { occurrencesNumberInputBounds = it },
                                onValueChange = { maxOccurrences = it }
                            )
                        }
                    ) {
                        Text(stringResource(R.string.repeat_occurrences))
                    }
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
                    onClick = { saveAndClose(slideOut) },
                    iconSize = 32.dp
                )
            },
            title = stringResource(R.string.custom_repeat),
            modifier = Modifier.padding(12.dp)
        )

    }

    RepeatEndDatePicker(
        isVisible = isEndDatePickerVisible,
        anchorBounds = endDateButtonBounds,
        minDate = anchorDate,
        initialDate = endDate,
        onDismiss = { isEndDatePickerVisible = false },
        onDateSelected = { date -> endDate = date }
    )
}

private fun SectionScope.WeekdayRows(
    selected: Set<DayOfWeek>,
    onSelectedChange: (Set<DayOfWeek>) -> Unit
) {
    DayOfWeek.entries.forEach { day ->
        val isSelected = day in selected
        Row(
            onClick = {
                val next = if (isSelected) selected - day else selected + day
                onSelectedChange(next.ifEmpty { setOf(day) })
            },
            trailing = {
                Icon(
                    painter = painterResource(R.drawable.ic_checkmark),
                    contentDescription = stringResource(R.string.done),
                    modifier = Modifier.size(24.dp),
                    tint = if (isSelected) AppColors.primary else Color.Transparent
                )
            }
        ) {
            Text(
                text = day.getDisplayName(
                    DateTextStyle.FULL_STANDALONE,
                    LocalLocale.current.platformLocale
                )
            )
        }
    }
}

@Composable
private fun NumberGrid(
    values: List<Int>,
    selected: Set<Int>,
    columns: Int,
    label: @Composable (Int) -> String = { it.toString() },
    onSelectedChange: (Set<Int>) -> Unit
) {
    Column {
        val chunks = values.chunked(columns)
        chunks.forEachIndexed { rowIndex, rowValues ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                repeat(columns) { index ->
                    if (index < rowValues.size) {
                        val value = rowValues[index]
                        val isSelected = value in selected
                        GridCell(
                            text = label(value),
                            selected = isSelected,
                            onClick = {
                                val next = if (isSelected) selected - value else selected + value
                                onSelectedChange(next.ifEmpty { setOf(value) })
                            },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    if (index < rowValues.size && index < columns - 1) {
                        HDivider(modifier = Modifier.zIndex(999f))
                    }
                }
            }
            if (rowIndex < chunks.lastIndex) {
                VDivider(modifier = Modifier.zIndex(999f))
            }
        }
    }
}

@Composable
private fun GridCell(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) AppColors.primary else Color.Transparent
    val contentColor = if (selected) AppColors.onPrimary else AppColors.content
    Box(
        modifier = modifier
            .height(48.dp)
            .background(containerColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun NumberInput(
    value: Int,
    minValue: Int,
    maxValue: Int,
    onPositioned: (Rect) -> Unit = {},
    onValueChange: (Int) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var text by remember(value) { mutableStateOf(value.toString()) }

    BasicTextField(
        value = text,
        onValueChange = { rawValue ->
            val digits = rawValue.filter(Char::isDigit).take(3)
            text = digits
            digits.toIntOrNull()
                ?.coerceIn(minValue, maxValue)
                ?.let(onValueChange)
        },
        modifier = Modifier
            .width(64.dp)
            .onGloballyPositioned { coordinates ->
                onPositioned(coordinates.boundsInWindow())
            }
            .defaultMinSize(minHeight = 32.dp)
            .clip(AppSpecs.cardShape)
            .background(AppColors.scrimNormal)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        textStyle = GlasenseTheme.type.body.copy(
            textAlign = TextAlign.Center,
            color = LocalGlasenseContentColor.current
        ),
        cursorBrush = SolidColor(AppColors.primary),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = { focusManager.clearFocus() }
        ),
        singleLine = true
    )
}

@Composable
private fun NumberStepper(
    value: Int,
    minValue: Int,
    maxValue: Int,
    onPositioned: (Rect) -> Unit = {},
    onValueChange: (Int) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepperButton(StepperType.Minus) {
            onValueChange((value - 1).coerceAtLeast(minValue))
        }
        NumberInput(
            value = value,
            minValue = minValue,
            maxValue = maxValue,
            onPositioned = onPositioned,
            onValueChange = onValueChange
        )
        StepperButton(StepperType.Plus) {
            onValueChange((value + 1).coerceAtMost(maxValue))
        }
    }
}

enum class StepperType {
    Minus,
    Plus
}

@Composable
private fun StepperButton(
    type: StepperType,
    onClick: () -> Unit
) {
    GlasenseButtonCompact(
        colors = AppButtonColors.secondary(),
        shape = CircleShape,
        onClick = onClick,
        modifier = Modifier.size(32.dp),
        padding = PaddingValues(4.dp)
    ) {
        Icon(
            painter = when (type) {
                StepperType.Minus -> painterResource(id = R.drawable.ic_minus)
                StepperType.Plus -> painterResource(id = R.drawable.ic_add)
            },
            contentDescription = null,
            tint = LocalGlasenseContentColor.current
        )
    }
}

@Composable
private fun RepeatEndDatePicker(
    isVisible: Boolean,
    anchorBounds: Rect,
    minDate: LocalDate,
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val maxYear = minDate.year + 20
    val locale = LocalLocale.current.platformLocale
    val yearOptions = remember(minDate.year, maxYear) {
        (minDate.year..maxYear).map { it.toString() }
    }
    val monthOptions = remember(locale) {
        (1..12).map { month ->
            Month.of(month).getDisplayName(DateTextStyle.FULL, locale)
        }
    }

    val selectedYear = remember { mutableIntStateOf(minDate.year) }
    val selectedMonth = remember { mutableIntStateOf(minDate.monthValue) }
    val selectedDay = remember { mutableIntStateOf(minDate.dayOfMonth) }

    fun selectedDate(): LocalDate = LocalDate.of(
        selectedYear.intValue,
        selectedMonth.intValue,
        selectedDay.intValue
    )

    LaunchedEffect(isVisible, initialDate, minDate) {
        if (isVisible) {
            val date = if (initialDate.isBefore(minDate)) minDate else initialDate
            selectedYear.intValue = date.year.coerceIn(minDate.year, maxYear)
            selectedMonth.intValue = date.monthValue
            selectedDay.intValue = date.dayOfMonth
        }
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
        direction = PopupDirection.Up
    ) {
        GlasenseModalTopBar(
            leading = {
                Action(
                    icon = painterResource(id = R.drawable.ic_cross),
                    contentDescription = stringResource(R.string.cancel),
                    onClick = onDismiss
                )
            },
            title = stringResource(R.string.repeat_until_date),
            trailing = {
                Action(
                    icon = painterResource(id = R.drawable.ic_checkmark),
                    contentDescription = stringResource(R.string.done),
                    onClick = {
                        val date = selectedDate()
                        onDateSelected(if (date.isBefore(minDate)) minDate else date)
                        onDismiss()
                    },
                    colors = AppButtonColors.primary(),
                    highlight = true
                )
            }
        )
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
                Spacer(Modifier.weight(1f))
                HDivider()
                Spacer(Modifier.weight(1f))
                HDivider()
                Spacer(Modifier.weight(1f))
            }
            Row {
                RepeatEndYearWheel(
                    modifier = Modifier.weight(1f),
                    minYear = minDate.year,
                    yearOptions = yearOptions,
                    selectedYear = selectedYear
                )
                RepeatEndMonthWheel(
                    modifier = Modifier.weight(1f),
                    monthOptions = monthOptions,
                    selectedMonth = selectedMonth
                )
                RepeatEndDayWheel(
                    modifier = Modifier.weight(1f),
                    selectedYear = selectedYear,
                    selectedMonth = selectedMonth,
                    selectedDay = selectedDay
                )
            }
        }
    }
}

@Composable
private fun RepeatEndYearWheel(
    modifier: Modifier,
    minYear: Int,
    yearOptions: List<String>,
    selectedYear: MutableIntState
) {
    GlasenseWheelPicker(
        modifier = modifier,
        items = yearOptions,
        indicator = false,
        currentSelected = (selectedYear.intValue - minYear).coerceAtLeast(0)
    ) { index ->
        selectedYear.intValue = minYear + index
    }
}

@Composable
private fun RepeatEndMonthWheel(
    modifier: Modifier,
    monthOptions: List<String>,
    selectedMonth: MutableIntState
) {
    GlasenseWheelPicker(
        modifier = modifier,
        items = monthOptions,
        indicator = false,
        currentSelected = (selectedMonth.intValue - 1).coerceAtLeast(0)
    ) { index ->
        selectedMonth.intValue = index + 1
    }
}

@Composable
private fun RepeatEndDayWheel(
    modifier: Modifier,
    selectedYear: MutableIntState,
    selectedMonth: MutableIntState,
    selectedDay: MutableIntState
) {
    val availableDays by remember {
        derivedStateOf {
            1..YearMonth.of(
                selectedYear.intValue,
                selectedMonth.intValue
            ).lengthOfMonth()
        }
    }

    LaunchedEffect(availableDays) {
        selectedDay.intValue = selectedDay.intValue.coerceIn(
            availableDays.first,
            availableDays.last
        )
    }

    GlasenseWheelPicker(
        modifier = modifier,
        items = availableDays.map { it.toString() },
        indicator = false,
        currentSelected = (selectedDay.intValue - availableDays.first).coerceAtLeast(0)
    ) { index ->
        selectedDay.intValue = availableDays.first + index
    }
}

@Composable
private fun monthLabel(monthValue: Int): String {
    return Month.of(monthValue)
        .getDisplayName(DateTextStyle.SHORT_STANDALONE, LocalLocale.current.platformLocale)
}
