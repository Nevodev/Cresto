package com.nevoit.cresto.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
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
import com.nevoit.cresto.theme.AppButtonColors
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.AppSpecs
import com.nevoit.cresto.ui.components.glasense.DimIndication
import com.nevoit.cresto.ui.components.glasense.GlasenseBottomSheet
import com.nevoit.cresto.ui.components.glasense.GlasenseButton
import com.nevoit.cresto.ui.components.glasense.GlasenseSwitch
import com.nevoit.cresto.ui.components.glasense.ZeroHeightDivider
import com.nevoit.cresto.ui.components.packed.ConfigItem
import com.nevoit.cresto.ui.components.packed.ConfigItemContainer
import com.nevoit.glasense.core.component.Icon
import com.nevoit.glasense.core.component.Text
import com.nevoit.glasense.theme.GlasenseTheme
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
    val isAllDayEnabled = startTime == null && endTime == null
    val isTimeRangeEnabled = startTime != null && endTime != null
    val rangeStartTime = startTime ?: LocalTime.of(9, 0)
    val rangeEndTime = endTime ?: defaultRangeEndTime(rangeStartTime)

    GlasenseBottomSheet(
        onDismissed = onDismissed
    ) { slideOut ->
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
                    text = stringResource(R.string.time),
                    modifier = Modifier.align(Alignment.Center),
                    style = GlasenseTheme.type.smallTitle
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            TimeConfigSection(
                isAllDayEnabled = isAllDayEnabled,
                isTimeRangeEnabled = isTimeRangeEnabled,
                rangeStartTime = rangeStartTime,
                rangeEndTime = rangeEndTime,
                onAllDayEnabledChange = { checked ->
                    if (checked) {
                        onTimeChange(null, null)
                    } else {
                        onTimeChange(rangeStartTime, null)
                    }
                },
                onTimeRangeEnabledChange = { checked ->
                    if (checked) {
                        onTimeChange(rangeStartTime, defaultRangeEndTime(rangeStartTime))
                    } else {
                        onTimeChange(rangeStartTime, null)
                    }
                },
                onRangeStartTimeChange = { newTime ->
                    if (newTime != null) {
                        onTimeChange(newTime, if (isTimeRangeEnabled) rangeEndTime else null)
                    }
                },
                onRangeEndTimeChange = { newTime ->
                    if (newTime != null) {
                        onTimeChange(rangeStartTime, newTime)
                    }
                },
                onRequestCustomTime = onRequestCustomTime
            )
        }
    }
}

@Composable
private fun TimeConfigSection(
    isAllDayEnabled: Boolean,
    isTimeRangeEnabled: Boolean,
    rangeStartTime: LocalTime,
    rangeEndTime: LocalTime,
    onAllDayEnabledChange: (Boolean) -> Unit,
    onTimeRangeEnabledChange: (Boolean) -> Unit,
    onRangeStartTimeChange: (LocalTime?) -> Unit,
    onRangeEndTimeChange: (LocalTime?) -> Unit,
    onRequestCustomTime: (Rect, LocalTime?, LocalTime?, LocalTime?, (LocalTime?) -> Unit) -> Unit
) {
    var rangeStartTimeButtonBounds by remember { mutableStateOf(Rect.Zero) }
    var rangeEndTimeButtonBounds by remember { mutableStateOf(Rect.Zero) }

    ConfigItemContainer(
        backgroundColor = AppColors.elevatedCardBackground
    ) {
        Column {
            ConfigItem(title = stringResource(R.string.all_day)) {
                GlasenseSwitch(
                    backgroundColor = AppColors.elevatedCardBackground,
                    checked = isAllDayEnabled,
                    onCheckedChange = onAllDayEnabledChange
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            ZeroHeightDivider()
            Spacer(modifier = Modifier.height(8.dp))
            ConfigItem(title = stringResource(R.string.time_range)) {
                GlasenseSwitch(
                    backgroundColor = AppColors.elevatedCardBackground,
                    checked = isTimeRangeEnabled,
                    onCheckedChange = onTimeRangeEnabledChange
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
                            if (isTimeRangeEnabled) rangeEndTime else null,
                            onRangeStartTimeChange
                        )
                    }
                )
                if (isTimeRangeEnabled) {
                    TimeButton(
                        time = rangeEndTime,
                        textStyle = timeTextStyle,
                        enabled = !isAllDayEnabled,
                        onPositioned = { rangeEndTimeButtonBounds = it },
                        onClick = {
                            onRequestCustomTime(
                                rangeEndTimeButtonBounds,
                                rangeEndTime,
                                rangeStartTime,
                                null,
                                onRangeEndTimeChange
                            )
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
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
