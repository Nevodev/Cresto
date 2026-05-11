package com.nevoit.cresto.ui.components.packed

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nevoit.cresto.R
import com.nevoit.cresto.theme.AppButtonColors
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.AppSpecs
import com.nevoit.cresto.ui.components.glasense.GlasenseButton
import com.nevoit.cresto.ui.components.glasense.GlasensePopup
import com.nevoit.cresto.ui.components.glasense.GlasenseWheelPicker
import com.nevoit.cresto.ui.components.glasense.PopupDirection
import com.nevoit.cresto.ui.components.glasense.PopupState
import com.nevoit.cresto.ui.components.glasense.ZeroWidthDivider
import com.nevoit.cresto.ui.components.glasense.glasenseHighlight
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun TimePicker(
    isVisible: Boolean,
    direction: PopupDirection = PopupDirection.Auto,
    anchorBounds: Rect,
    initialTime: LocalTime?,
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

    val hourOptions = remember { (0..23).map { String.format("%02d", it) } }
    val minuteOptions = remember { (0..59).map { String.format("%02d", it) } }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

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
                text = stringResource(R.string.select_time),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineSmall
            )
            GlasenseButton(
                enabled = true,
                shape = CircleShape,
                onClick = {
                    onTimeSelected(LocalTime.of(selectedHour, selectedMinute))
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
            }
            Row {
                GlasenseWheelPicker(
                    modifier = Modifier.weight(1f),
                    items = hourOptions,
                    indicator = false,
                    currentSelected = selectedHour
                ) { index ->
                    selectedHour = index
                }
                GlasenseWheelPicker(
                    modifier = Modifier.weight(1f),
                    items = minuteOptions,
                    indicator = false,
                    currentSelected = selectedMinute
                ) { index ->
                    selectedMinute = index
                }
            }
        }
    }
}