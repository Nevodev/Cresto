package com.nevoit.cresto.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nevoit.cresto.R
import com.nevoit.cresto.feature.settings.CustomSwitchRow
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.AppSpecs
import com.nevoit.cresto.ui.components.glasense.GlasenseModalTopBar
import com.nevoit.glasense.component.BottomSheet
import com.nevoit.glasense.component.ListColors
import com.nevoit.glasense.component.ListStack
import com.nevoit.glasense.core.component.Text
import com.nevoit.glasense.core.component.VGap
import com.nevoit.glasense.core.interaction.DimIndication
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun DetailTimeBottomSheet(
    startTime: LocalTime?,
    endTime: LocalTime?,
    onTimeChange: (LocalTime?, LocalTime?) -> Unit,
    onDismissed: () -> Unit,
    onRequestCustomTime: (Rect, LocalTime?, LocalTime?, LocalTime?, (LocalTime?) -> Unit) -> Unit
) {
    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val listState = rememberLazyListState()
    val elevatedPageBackground = AppColors.elevatedPageBackground
    val elevatedCardBackground = AppColors.elevatedCardBackground
    val contentVariant = AppColors.contentVariant

    val isAllDayEnabled = startTime == null && endTime == null
    val isTimeRangeEnabled = startTime != null && endTime != null
    val rangeStartTime = startTime ?: LocalTime.of(9, 0)
    val rangeEndTime = endTime ?: defaultRangeEndTime(rangeStartTime)
    var rangeStartTimeButtonBounds by remember { mutableStateOf(Rect.Zero) }
    var rangeEndTimeButtonBounds by remember { mutableStateOf(Rect.Zero) }

    BottomSheet(
        onDismissed = onDismissed
    ) { slideOut ->
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
                CustomSwitchRow(
                    checked = isAllDayEnabled,
                    onCheckedChange = { checked ->
                        if (checked) {
                            onTimeChange(null, null)
                        } else {
                            onTimeChange(rangeStartTime, null)
                        }
                    },
                    backgroundColor = elevatedCardBackground
                ) {
                    Text(stringResource(R.string.all_day))
                }
                CustomSwitchRow(
                    checked = isTimeRangeEnabled,
                    onCheckedChange = { checked ->
                        if (checked) {
                            onTimeChange(rangeStartTime, defaultRangeEndTime(rangeStartTime))
                        } else {
                            onTimeChange(rangeStartTime, null)
                        }
                    },
                    backgroundColor = elevatedCardBackground
                ) {
                    Text(stringResource(R.string.time_range))
                }
                Row(separator = false) {
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
                        TimeButton(
                            time = rangeStartTime,
                            textStyle = timeTextStyle,
                            enabled = !isAllDayEnabled,
                            onPositioned = { rangeStartTimeButtonBounds = it },
                            onClick = {
                                onRequestCustomTime(
                                    rangeStartTimeButtonBounds,
                                    rangeStartTime,
                                    null,
                                    if (isTimeRangeEnabled) rangeEndTime else null
                                ) { newTime ->
                                    if (newTime != null) {
                                        onTimeChange(
                                            newTime,
                                            if (isTimeRangeEnabled) rangeEndTime else null
                                        )
                                    }
                                }
                            }
                        )
                        if (isTimeRangeEnabled) {
                            TimeButton(
                                time = rangeEndTime,
                                textStyle = timeTextStyle,
                                enabled = true,
                                onPositioned = { rangeEndTimeButtonBounds = it },
                                onClick = {
                                    onRequestCustomTime(
                                        rangeEndTimeButtonBounds,
                                        rangeEndTime,
                                        rangeStartTime,
                                        null
                                    ) { newTime ->
                                        if (newTime != null) {
                                            onTimeChange(rangeStartTime, newTime)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
            item { VGap() }
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
            title = stringResource(R.string.time),
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
private fun TimeButton(
    time: LocalTime,
    textStyle: TextStyle,
    enabled: Boolean,
    onPositioned: (Rect) -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .onGloballyPositioned { coordinates ->
                onPositioned(coordinates.boundsInWindow())
            }
            .clip(AppSpecs.cardShape)
            .background(color = AppColors.scrimNormal)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = DimIndication(),
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = time.format(DateTimeFormatter.ofPattern("HH:mm")),
            modifier = Modifier.align(Alignment.Center),
            style = textStyle
        )
    }
}

private fun defaultRangeEndTime(startTime: LocalTime): LocalTime {
    return if (startTime.hour >= 23) {
        LocalTime.of(23, 59)
    } else {
        startTime.plusHours(1)
    }
}
