package com.nevoit.cresto.feature.calendar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nevoit.cresto.theme.AppColors
import com.nevoit.glasense.theme.Springs
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import kotlin.math.roundToInt
import java.time.format.TextStyle as TimeTextStyle

@Composable
fun MonthlyPagerCalendar(
    modifier: Modifier = Modifier,
    selectedDate: LocalDate,
    datesWithTodo: Set<LocalDate> = emptySet(),
    onDateSelected: (LocalDate) -> Unit,
    onMonthChanged: (YearMonth) -> Unit,
    collapseFractionProvider: () -> Float,
    initialDate: LocalDate = LocalDate.now()
) {
    val metrics = rememberCalendarMetrics()
    val density = LocalDensity.current

    val pageCount = 1000
    val initialPage = pageCount / 2

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { pageCount }
    )

    LaunchedEffect(selectedDate) {
        val targetMonth = YearMonth.from(selectedDate)
        val currentMonthOffset = pagerState.currentPage - initialPage
        val currentDisplayMonth =
            YearMonth.from(initialDate).plusMonths(currentMonthOffset.toLong())

        if (targetMonth != currentDisplayMonth) {
            val monthDifference = (targetMonth.year - currentDisplayMonth.year) * 12 +
                    (targetMonth.monthValue - currentDisplayMonth.monthValue)
            pagerState.animateScrollToPage(
                page = pagerState.currentPage + monthDifference,
                animationSpec = Springs.smooth(durationMillis = 300)
            )
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                val monthOffset = page - initialPage
                val newDisplayMonth = YearMonth.from(initialDate).plusMonths(monthOffset.toLong())
                onMonthChanged(newDisplayMonth)
            }
    }

    val selectedRowIndex by remember(selectedDate) {
        derivedStateOf {
            val currentMonthOffset = pagerState.currentPage - initialPage
            val currentDisplayMonth =
                YearMonth.from(initialDate).plusMonths(currentMonthOffset.toLong())

            calculateRowIndex(currentDisplayMonth, selectedDate)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .layout { measurable, constraints ->
                val fraction = collapseFractionProvider()
                val currentGridHeightPx =
                    metrics.gridTotalHeightPx - (metrics.maxCollapseOffsetPx * fraction)
                val heightPx = currentGridHeightPx.roundToInt()

                val placeable = measurable.measure(
                    constraints.copy(
                        minHeight = heightPx,
                        maxHeight = heightPx
                    )
                )
                layout(placeable.width, placeable.height) {
                    placeable.placeRelative(0, 0)
                }
            }
            .clipToBounds()
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .topAlignedFixedHeight(with(density) { metrics.gridTotalHeightPx.toDp() })
                .graphicsLayer {
                    val fraction = collapseFractionProvider()
                    translationY = -(selectedRowIndex * metrics.singleRowHeightPx * fraction)
                },
            contentPadding = PaddingValues(horizontal = 12.dp),
            pageSpacing = 12.dp * 2
        ) { page ->
            val monthOffset = page - initialPage
            val pageMonth = YearMonth.from(initialDate).plusMonths(monthOffset.toLong())

            MonthGrid(
                yearMonth = pageMonth,
                selectedDate = selectedDate,
                datesWithTodo = datesWithTodo,
                onDayClick = onDateSelected
            )
        }
    }
}

@Composable
internal fun WeekDaysIndicator() {
    val daysOfWeek = remember {
        DayOfWeek.entries.map { day ->
            day.getDisplayName(TimeTextStyle.NARROW, Locale.getDefault())
        }
    }

    val textMeasurer = rememberTextMeasurer()
    val spacing = 8.dp
    val textColor = AppColors.contentVariant
    val textStyle = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .height(24.dp)
    ) {
        val spacingPx = spacing.toPx()
        val cellWidth = (size.width - spacingPx * 6) / 7f

        daysOfWeek.forEachIndexed { col, day ->
            val textLayoutResult = textMeasurer.measure(
                text = day,
                style = textStyle.copy(color = textColor)
            )

            val x = col * (cellWidth + spacingPx)
            val centerY = size.height / 2f

            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(
                    x + cellWidth / 2f - textLayoutResult.size.width / 2f,
                    centerY - textLayoutResult.size.height / 2f
                )
            )
        }
    }
}

@Composable
private fun MonthGrid(
    yearMonth: YearMonth,
    selectedDate: LocalDate,
    datesWithTodo: Set<LocalDate>,
    onDayClick: (LocalDate) -> Unit
) {
    val daysInCurrentMonth = yearMonth.lengthOfMonth()
    val firstDayOfWeek = yearMonth.atDay(1).dayOfWeek.value

    val emptyDaysBefore = firstDayOfWeek - 1

    val previousMonth = yearMonth.minusMonths(1)
    val nextMonth = yearMonth.plusMonths(1)
    val daysInPreviousMonth = previousMonth.lengthOfMonth()

    val textMeasurer = rememberTextMeasurer()
    val spacing = 8.dp

    val today = LocalDate.now()

    val selectedBgColor = AppColors.primary
    val todayBgColor = AppColors.scrimNormal

    val selectedTextColor = AppColors.onPrimary
    val currentMonthTextColor = AppColors.content
    val otherMonthTextColor = AppColors.contentVariant

    val textStyle = TextStyle(
        fontSize = 14.sp,
        lineHeight = 12.sp,
        fontWeight = FontWeight.Normal
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(7f / 6f)
            .pointerInput(yearMonth, selectedDate) {
                detectTapGestures { offset ->
                    val spacingPx = spacing.toPx()
                    val cellWidth = (size.width - spacingPx * 6) / 7f
                    val cellHeight = (size.height - spacingPx * 5) / 6f

                    val col = (offset.x / (cellWidth + spacingPx)).toInt().coerceIn(0, 6)
                    val row = (offset.y / (cellHeight + spacingPx)).toInt().coerceIn(0, 5)

                    val cellIndex = row * 7 + col
                    val date = when {
                        cellIndex < emptyDaysBefore -> {
                            val day = daysInPreviousMonth - emptyDaysBefore + cellIndex + 1
                            previousMonth.atDay(day)
                        }

                        cellIndex < emptyDaysBefore + daysInCurrentMonth -> {
                            val day = cellIndex - emptyDaysBefore + 1
                            yearMonth.atDay(day)
                        }

                        else -> {
                            val day = cellIndex - emptyDaysBefore - daysInCurrentMonth + 1
                            nextMonth.atDay(day)
                        }
                    }
                    onDayClick(date)
                }
            }
    ) {
        val spacingPx = spacing.toPx()
        val cellWidth = (size.width - spacingPx * 6) / 7f
        val cellHeight = cellWidth

        for (row in 0 until 6) {
            for (col in 0 until 7) {
                val cellIndex = row * 7 + col
                val date: LocalDate
                val isCurrentMonth: Boolean

                when {
                    cellIndex < emptyDaysBefore -> {
                        val day = daysInPreviousMonth - emptyDaysBefore + cellIndex + 1
                        date = previousMonth.atDay(day)
                        isCurrentMonth = false
                    }

                    cellIndex < emptyDaysBefore + daysInCurrentMonth -> {
                        val day = cellIndex - emptyDaysBefore + 1
                        date = yearMonth.atDay(day)
                        isCurrentMonth = true
                    }

                    else -> {
                        val day = cellIndex - emptyDaysBefore - daysInCurrentMonth + 1
                        date = nextMonth.atDay(day)
                        isCurrentMonth = false
                    }
                }

                val isSelected = date == selectedDate
                val isToday = date == today
                val hasTodo = datesWithTodo.contains(date)

                val x = col * (cellWidth + spacingPx)
                val y = row * (cellHeight + spacingPx)
                val cellCenter = Offset(x + cellWidth / 2f, y + cellHeight / 2f)

                if (isSelected || isToday) {
                    val bgColor = if (isSelected) selectedBgColor else todayBgColor
                    drawCircle(
                        color = bgColor,
                        radius = cellWidth / 2f - 2.dp.toPx(),
                        center = cellCenter
                    )
                }

                val textColor = when {
                    isSelected -> selectedTextColor
                    isCurrentMonth -> currentMonthTextColor
                    else -> otherMonthTextColor
                }

                val text = date.dayOfMonth.toString()
                val textLayoutResult = textMeasurer.measure(
                    text = text,
                    style = textStyle.copy(color = textColor)
                )

                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(
                        cellCenter.x - textLayoutResult.size.width / 2f,
                        cellCenter.y - textLayoutResult.size.height / 2f
                    )
                )

                if (hasTodo && !isSelected) {
                    drawCircle(
                        color = selectedBgColor,
                        radius = 2.dp.toPx(),
                        center = Offset(
                            cellCenter.x,
                            cellCenter.y + textLayoutResult.size.height / 2f + 4.dp.toPx()
                        )
                    )
                }
            }
        }
    }
}

private fun calculateRowIndex(pageMonth: YearMonth, selectedDate: LocalDate): Int {
    val daysInCurrentMonth = pageMonth.lengthOfMonth()
    val firstDayOfWeek = pageMonth.atDay(1).dayOfWeek.value
    val emptyDaysBefore = firstDayOfWeek - 1

    val previousMonth = pageMonth.minusMonths(1)
    val nextMonth = pageMonth.plusMonths(1)
    val daysInPreviousMonth = previousMonth.lengthOfMonth()

    for (row in 0 until 6) {
        for (col in 0 until 7) {
            val cellIndex = row * 7 + col
            val date = when {
                cellIndex < emptyDaysBefore -> {
                    val day = daysInPreviousMonth - emptyDaysBefore + cellIndex + 1
                    previousMonth.atDay(day)
                }

                cellIndex < emptyDaysBefore + daysInCurrentMonth -> {
                    val day = cellIndex - emptyDaysBefore + 1
                    pageMonth.atDay(day)
                }

                else -> {
                    val day = cellIndex - emptyDaysBefore - daysInCurrentMonth + 1
                    nextMonth.atDay(day)
                }
            }

            if (date == selectedDate) {
                return row
            }
        }
    }

    return 0
}

@Stable
class CalendarMetrics(
    val singleRowHeightPx: Float,
    val gridTotalHeightPx: Float,
    val maxCollapseOffsetPx: Float
)

@Composable
fun rememberCalendarMetrics(
    horizontalPadding: Dp = 12.dp,
    spacing: Dp = 8.dp
): CalendarMetrics {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current

    return remember(windowInfo.containerDpSize.width, density.density) {
        with(density) {
            val availableWidthPx =
                (windowInfo.containerDpSize.width - (horizontalPadding * 2)).roundToPx()
            val spacingPx = spacing.roundToPx()

            val totalWeightPx = availableWidthPx - (spacingPx * 6)

            var maxCellWidthPx = 0
            var remainingPx = totalWeightPx
            for (weight in 7 downTo 1) {
                val cellPx = (remainingPx / weight.toFloat()).roundToInt()
                if (cellPx > maxCellWidthPx) {
                    maxCellWidthPx = cellPx
                }
                remainingPx -= cellPx
            }

            val singleRowPx = maxCellWidthPx.toFloat() + spacingPx
            val totalPx = (maxCellWidthPx * 6f) + (spacingPx * 5)

            CalendarMetrics(
                singleRowHeightPx = singleRowPx,
                gridTotalHeightPx = totalPx,
                maxCollapseOffsetPx = singleRowPx * 5
            )
        }
    }
}

fun Modifier.topAlignedFixedHeight(height: Dp) = layout { measurable, constraints ->
    val heightPx = height.roundToPx()

    val childConstraints = constraints.copy(
        minHeight = heightPx,
        maxHeight = heightPx
    )
    val placeable = measurable.measure(childConstraints)

    val reportedWidth = constraints.constrainWidth(placeable.width)
    val reportedHeight = constraints.constrainHeight(heightPx)

    layout(reportedWidth, reportedHeight) {
        placeable.placeRelative(0, 0)
    }
}