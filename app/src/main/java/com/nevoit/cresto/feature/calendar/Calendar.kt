package com.nevoit.cresto.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nevoit.cresto.theme.AppColors
import com.nevoit.glasense.theme.Springs
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

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

private val daysOfWeek = listOf("一", "二", "三", "四", "五", "六", "日")

@Composable
internal fun WeekDaysIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        daysOfWeek.forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                color = AppColors.contentVariant,
                fontWeight = FontWeight.Medium
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

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in 0 until 6) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                    val isToday = date == LocalDate.now()
                    val hasTodo = datesWithTodo.contains(date)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        DayCell(
                            date = date,
                            isSelected = isSelected,
                            isToday = isToday,
                            isCurrentMonth = isCurrentMonth,
                            hasTodo = hasTodo,
                            onClick = { onDayClick(date) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    isCurrentMonth: Boolean,
    hasTodo: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isSelected -> AppColors.primary
        isToday -> AppColors.scrimNormal
        else -> Color.Transparent
    }

    val textColor = when {
        isSelected -> AppColors.onPrimary
        isCurrentMonth -> AppColors.content
        else -> AppColors.contentVariant
    }

    val interactionSource = remember { MutableInteractionSource() }

    Layout(
        modifier = Modifier
            .fillMaxSize()
            .padding(2.dp)
            .then(
                if (isSelected || isToday) Modifier.background(
                    backgroundColor,
                    CircleShape
                ) else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() },
        measurePolicy = centerFirstColumnMeasurePolicy,
        content = {
            Text(
                text = date.dayOfMonth.toString(),
                fontSize = 14.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Normal,
                color = textColor
            )

            if (hasTodo && !isSelected) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(AppColors.primary, CircleShape)
                )
            }
        }
    )
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

val centerFirstColumnMeasurePolicy = MeasurePolicy { measurables, constraints ->
    val childConstraints = constraints.copy(
        minWidth = 0,
        minHeight = 0,
        maxHeight = Constraints.Infinity
    )

    val placeables = measurables.map { it.measure(childConstraints) }

    val layoutWidth =
        if (constraints.hasBoundedWidth) constraints.maxWidth else (placeables.firstOrNull()?.width
            ?: 0)
    val layoutHeight =
        if (constraints.hasBoundedHeight) constraints.maxHeight else (placeables.firstOrNull()?.height
            ?: 0)

    layout(layoutWidth, layoutHeight) {
        if (placeables.isEmpty()) return@layout

        val firstPlaceable = placeables[0]

        val firstX = (layoutWidth - firstPlaceable.width) / 2
        val firstY = (layoutHeight - firstPlaceable.height) / 2

        firstPlaceable.placeRelative(x = firstX, y = firstY)

        var currentY = firstY + firstPlaceable.height

        for (i in 1 until placeables.size) {
            val placeable = placeables[i]

            val x = (layoutWidth - placeable.width) / 2

            placeable.placeRelative(x = x, y = currentY)

            currentY += placeable.height
        }
    }
}