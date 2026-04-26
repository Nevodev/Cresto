package com.nevoit.cresto.ui.components.glasense

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nevoit.cresto.R
import com.nevoit.cresto.theme.AppButtonColors
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.AppSpecs
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle

@Composable
fun DueDatePicker(
    isVisible: Boolean,
    direction: PopupDirection = PopupDirection.Auto,
    anchorBounds: Rect,
    initialDate: LocalDate?,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate?) -> Unit
) {
    var selectedYearIndex by remember { mutableIntStateOf(0) }
    var selectedMonth by remember { mutableIntStateOf(LocalDate.now().monthValue) }
    var selectedDay by remember { mutableIntStateOf(LocalDate.now().dayOfMonth) }

    val currentYear = LocalDate.now().year
    val noneString = stringResource(R.string.none)
    val yearOptions = remember(currentYear, noneString) {
        listOf(noneString) + ((currentYear - 1)..(currentYear + 20)).map { it.toString() }
    }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            initialDate?.let { date ->
                selectedYearIndex = (date.year - (currentYear - 1)) + 1
                selectedMonth = date.monthValue
                selectedDay = date.dayOfMonth
            } ?: run {
                selectedYearIndex = 0
                val now = LocalDate.now()
                selectedMonth = now.monthValue
                selectedDay = now.dayOfMonth
            }
        }
    }

    val locale = LocalLocale.current.platformLocale
    val monthOptions = remember(locale) {
        (1..12).map { month ->
            Month.of(month).getDisplayName(TextStyle.FULL, locale)
        }
    }

    val daysInMonth = remember(selectedYearIndex, selectedMonth) {
        val year =
            if (selectedYearIndex > 0) selectedYearIndex - 1 + (currentYear - 1) else currentYear
        YearMonth.of(year, selectedMonth).lengthOfMonth()
    }

    val dayOptions = remember(daysInMonth) {
        (1..daysInMonth).map { it.toString() }
    }

    LaunchedEffect(daysInMonth) {
        if (selectedDay > daysInMonth) {
            selectedDay = daysInMonth
        }
    }
    val showColumn by remember {
        derivedStateOf {
            selectedYearIndex > 0
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
        direction = direction
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlasenseButton(
                enabled = true,
                shape = CircleShape,
                onClick = onDismiss,
                modifier = Modifier
                    .width(48.dp)
                    .height(48.dp),
                colors = AppButtonColors.secondary(),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_cross),
                    contentDescription = stringResource(R.string.cancel),
                    modifier = Modifier.width(28.dp)
                )
            }
            Text(
                text = stringResource(R.string.select_due_date),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineSmall
            )
            GlasenseButton(
                enabled = true,
                shape = CircleShape,
                onClick = {
                    if (selectedYearIndex == 0) {
                        onDateSelected(null)
                    } else {
                        val year = selectedYearIndex - 1 + (currentYear - 1)
                        onDateSelected(LocalDate.of(year, selectedMonth, selectedDay))
                    }
                    onDismiss()
                },
                modifier = Modifier
                    .width(48.dp)
                    .height(48.dp),
                colors = AppButtonColors.primary()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .glasenseHighlight(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_checkmark),
                        contentDescription = stringResource(R.string.done),
                        modifier = Modifier.width(28.dp)
                    )
                }
            }
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
                Spacer(Modifier.weight(1f))
                ZeroWidthDivider()
                Spacer(Modifier.weight(1f))
                ZeroWidthDivider()
                Spacer(Modifier.weight(1f))
            }
            Row {
                GlasenseWheelPicker(
                    modifier = Modifier.weight(1f),
                    items = yearOptions,
                    indicator = false,
                    currentSelected = selectedYearIndex
                ) { index ->
                    selectedYearIndex = index
                }
                if (showColumn) {
                    GlasenseWheelPicker(
                        modifier = Modifier
                            .weight(1f),
                        items = monthOptions,
                        indicator = false,
                        currentSelected = (selectedMonth - 1).coerceAtLeast(0)
                    ) { index ->
                        selectedMonth = index + 1
                    }
                    GlasenseWheelPicker(
                        modifier = Modifier
                            .weight(1f),
                        items = dayOptions,
                        indicator = false,
                        currentSelected = (selectedDay - 1).coerceAtLeast(0)
                    ) { index ->
                        selectedDay = index + 1
                    }
                } else {
                    Spacer(modifier = Modifier.weight(2f))
                }

            }
        }
    }
}
