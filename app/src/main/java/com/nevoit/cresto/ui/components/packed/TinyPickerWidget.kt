package com.nevoit.cresto.ui.components.packed

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nevoit.cresto.R
import com.nevoit.cresto.ui.theme.glasense.Blue500
import com.nevoit.cresto.ui.theme.glasense.Gray500
import com.nevoit.cresto.ui.theme.glasense.Green500
import com.nevoit.cresto.ui.theme.glasense.Orange500
import com.nevoit.cresto.ui.theme.glasense.Purple500
import com.nevoit.cresto.ui.theme.glasense.Red500
import com.nevoit.cresto.ui.theme.glasense.Yellow500
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * A horizontal picker for selecting a color flag.
 *
 * @param selectedIndex The index of the currently selected color.
 * @param onIndexSelected Callback for when a color is selected.
 */
@Composable
fun HorizontalFlagPicker(
    selectedIndex: Int,
    onIndexSelected: (Int) -> Unit,
) {
    val colors = listOf(
        Color.Transparent,
        Red500,
        Orange500,
        Yellow500,
        Green500,
        Blue500,
        Purple500,
        Gray500
    )
    Box(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.align(Alignment.Center)
        ) {
            itemsIndexed(colors) { index, color ->
                if (index == 0) {
                    SelectorBox(
                        text = "无",
                        isSelected = (selectedIndex == 0),
                        onClick = { onIndexSelected(index) }
                    )
                } else {
                    // 其他项目显示颜色圆圈
                    ColorCircle(
                        color = color,
                        isSelected = (selectedIndex == index),
                        onClick = { onIndexSelected(index) }
                    )
                }
            }
        }
    }
}

/**
 * A single color circle item for the picker.
 *
 * @param color The color to display.
 * @param isSelected Whether the circle is currently selected.
 * @param onClick Callback for when the circle is clicked.
 */
@Composable
fun ColorCircle(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val scale = remember { Animatable(1f) }

    val interactionSource = remember { MutableInteractionSource() }

    var isPressed by remember { mutableStateOf(false) }

    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    val strokeColor =
        if (onSurfaceColor == Color.Black) onSurfaceColor.copy(alpha = 0.2F) else onSurfaceColor.copy(
            alpha = 0.4F
        )

    // Handle press and release animations.
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    isPressed = true
                    scale.animateTo(
                        targetValue = 1.4f,
                        animationSpec = tween(100)
                    )
                }

                is PressInteraction.Release -> {
                    isPressed = true
                    coroutineScope {
                        launch {
                            scale.animateTo(
                                targetValue = 0.8f,
                                animationSpec = tween(300)
                            )
                        }
                        launch {
                            delay(100)
                            onClick()
                        }
                    }
                }

                is PressInteraction.Cancel -> {
                    isPressed = false
                    scale.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .size(20.dp)
            .background(color, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { // Empty clickable for interactionSource
            }
            .then(
                // Draw a stroke if selected or pressed.
                if (isSelected || isPressed) {
                    Modifier.drawBehind {
                        drawCircle(
                            radius = size.minDimension / 2.0f - 1.dp.toPx(),
                            color = strokeColor,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                } else {
                    Modifier
                }
            )
    )
}

/**
 * Helper to get the preset type for a given date.
 */
private fun getPresetTypeForDate(date: LocalDate?): Int {
    if (date == null) {
        return 0 // None
    }
    val today = LocalDate.now()
    return when (date) {
        today -> 1 // Today
        today.plusDays(1) -> 2 // Tomorrow
        today.plusWeeks(1) -> 3 // Next Week
        else -> 4 // Custom
    }
}

/**
 * A horizontal picker for preset dates like "Today" or "Tomorrow".
 *
 * @param initialDate The initially selected date.
 * @param onDateSelected Callback for when a date is selected.
 */
@Composable
fun HorizontalPresetDatePicker(
    initialDate: LocalDate?,
    onDateSelected: (LocalDate?) -> Unit
) {
    var selectedDate by remember { mutableStateOf(initialDate) }
    var selectedPreset by remember { mutableStateOf(getPresetTypeForDate(initialDate)) }
    var showDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()
            ?.toEpochMilli()
    )

    val presets = listOf(
        stringResource(R.string.none),
        stringResource(R.string.today),
        stringResource(R.string.tomorrow),
        stringResource(R.string.next_week)
    )
    // Update state if the initial date changes.
    LaunchedEffect(initialDate) {
        selectedDate = initialDate
        selectedPreset = getPresetTypeForDate(initialDate)
    }
    Box(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            modifier = Modifier
                .align(Alignment.Center)
                .clickable( // Prevent clicks from passing through.
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {},
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
        ) {
            itemsIndexed(presets) { index, text ->
                SelectorBox(
                    text = text,
                    isSelected = (selectedPreset == index),
                    onClick = {
                        val newDate = when (index) {
                            0 -> null
                            1 -> LocalDate.now()
                            2 -> LocalDate.now().plusDays(1)
                            3 -> LocalDate.now().plusWeeks(1)
                            else -> selectedDate
                        }
                        selectedPreset = index
                        selectedDate = newDate
                        onDateSelected(newDate)
                    }
                )
            }
            item {
                // Divider
                Spacer(
                    modifier = Modifier
                        .width(1.5.dp)
                        .height(20.dp)
                        .background(
                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                            RoundedCornerShape(1.dp)
                        )
                )
            }
            item {
                val isCustomDateSelected = (selectedPreset == 4)
                val buttonText = if (isCustomDateSelected) {
                    selectedDate?.format(DateTimeFormatter.ofPattern("MM/dd"))
                } else {
                    stringResource(R.string.custom)
                }

                DateSelectorBox(
                    text = buttonText ?: stringResource(R.string.custom),
                    isSelected = isCustomDateSelected,
                    onClick = { showDatePicker = true }
                )
            }
        }
    }

    // Show the date picker dialog when needed.
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                        datePickerState.selectedDateMillis?.let { millis ->
                            val chosenDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            selectedDate = chosenDate
                            // selectedPreset = 4 // Mark as custom date
                            onDateSelected(chosenDate)
                        }
                    },
                    enabled = datePickerState.selectedDateMillis != null
                ) {
                    Text(stringResource(R.string.done))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/**
 * A selectable text box for the date picker presets.
 */
@Composable
private fun SelectorBox(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale = remember { Animatable(1f) }
    val interactionSource = remember { MutableInteractionSource() }
    var isPressed by remember { mutableStateOf(false) }

    // Handle press and release animations.
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    isPressed = true
                    scale.animateTo(
                        targetValue = 1.2f,
                        animationSpec = tween(100)
                    )
                }

                is PressInteraction.Release -> {
                    isPressed = true
                    coroutineScope {
                        launch {
                            scale.animateTo(
                                targetValue = 0.8f,
                                animationSpec = tween(300)
                            )
                        }
                        launch {
                            delay(100)
                            onClick()
                        }
                    }
                }

                is PressInteraction.Cancel -> {
                    isPressed = false
                    scale.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .clickable(interactionSource = interactionSource, indication = null) {}
            .then(
                // Adjust alpha based on selection/press state.
                if (isSelected || isPressed) {
                    Modifier.alpha(1f)
                } else {
                    Modifier.alpha(0.5f)
                }
            )
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * A selectable box specifically for the "Custom" date button.
 */
@Composable
private fun DateSelectorBox(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale = remember { Animatable(1f) }
    val interactionSource = remember { MutableInteractionSource() }
    var isPressed by remember { mutableStateOf(false) }

    // Handle press and release animations.
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Release -> {
                    isPressed = true
                    onClick()
                }

                is PressInteraction.Cancel -> {
                    isPressed = false
                    scale.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .clickable(interactionSource = interactionSource, indication = null) {}
            .then(
                // Adjust alpha based on selection/press state.
                if (isSelected || isPressed) {
                    Modifier.alpha(1f)
                } else {
                    Modifier.alpha(0.5f)
                }
            )
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
