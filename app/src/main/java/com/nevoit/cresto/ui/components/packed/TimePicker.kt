package com.nevoit.cresto.ui.components.packed

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nevoit.cresto.R
import com.nevoit.cresto.theme.AppButtonColors
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.AppSpecs
import com.nevoit.cresto.ui.components.glasense.GlasenseModalTopBar
import com.nevoit.cresto.ui.components.glasense.GlasensePopup
import com.nevoit.cresto.ui.components.glasense.GlasenseWheelPicker
import com.nevoit.cresto.ui.components.glasense.PopupDirection
import com.nevoit.cresto.ui.components.glasense.PopupState
import com.nevoit.glasense.core.component.HDivider
import java.time.LocalTime
import java.util.Locale

@Composable
fun TimePicker(
    isVisible: Boolean,
    direction: PopupDirection = PopupDirection.Auto,
    anchorBounds: Rect,
    initialTime: LocalTime?,
    minTime: LocalTime? = null,
    maxTime: LocalTime? = null,
    onDismiss: () -> Unit,
    onTimeSelected: (LocalTime?) -> Unit
) {
    var selectedHour by remember { mutableIntStateOf(13) }
    var selectedMinute by remember { mutableIntStateOf(0) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            initialTime?.let { time ->
                selectedHour = time.hour
                selectedMinute = time.minute
            } ?: run {
                selectedHour = 13
                selectedMinute = 0
            }
        }
    }

    val effectiveMinTime = minTime?.plusMinutes(1)
    val effectiveMaxTime = maxTime?.minusMinutes(1)

    val rawMinHour = effectiveMinTime?.hour ?: 0
    val rawMaxHour = effectiveMaxTime?.hour ?: 23
    val minHour = minOf(rawMinHour, rawMaxHour)
    val maxHour = maxOf(rawMinHour, rawMaxHour)
    val validHours = remember(minHour, maxHour) { (minHour..maxHour).toList() }

    val coercedHour = selectedHour.coerceIn(minHour, maxHour)
    val rawMinMinute =
        if (effectiveMinTime != null && coercedHour == effectiveMinTime.hour) effectiveMinTime.minute else 0
    val rawMaxMinute =
        if (effectiveMaxTime != null && coercedHour == effectiveMaxTime.hour) effectiveMaxTime.minute else 59
    val minMinute = minOf(rawMinMinute, rawMaxMinute)
    val maxMinute = maxOf(rawMinMinute, rawMaxMinute)
    val validMinutes = remember(minMinute, maxMinute) { (minMinute..maxMinute).toList() }
    val coercedMinute = selectedMinute.coerceIn(minMinute, maxMinute)

    val locale = Locale.ROOT
    val hourOptions = remember(validHours) { validHours.map { String.format(locale, "%02d", it) } }
    val minuteOptions =
        remember(validMinutes) { validMinutes.map { String.format(locale, "%02d", it) } }

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
        GlasenseModalTopBar(
            leading = {
                Action(
                    icon = painterResource(id = R.drawable.ic_cross),
                    contentDescription = stringResource(R.string.cancel),
                    onClick = onDismiss
                )
            },
            title = stringResource(R.string.select_time),
            trailing = {
                Action(
                    icon = painterResource(id = R.drawable.ic_checkmark),
                    contentDescription = stringResource(R.string.done),
                    onClick = {
                        onTimeSelected(LocalTime.of(coercedHour, coercedMinute))
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
            }
            Row {
                GlasenseWheelPicker(
                    modifier = Modifier.weight(1f),
                    items = hourOptions,
                    indicator = false,
                    currentSelected = validHours.indexOf(coercedHour).coerceAtLeast(0)
                ) { index ->
                    selectedHour = validHours[index]
                }
                GlasenseWheelPicker(
                    modifier = Modifier.weight(1f),
                    items = minuteOptions,
                    indicator = false,
                    currentSelected = validMinutes.indexOf(coercedMinute).coerceAtLeast(0)
                ) { index ->
                    selectedMinute = validMinutes[index]
                }
            }
        }
    }
}