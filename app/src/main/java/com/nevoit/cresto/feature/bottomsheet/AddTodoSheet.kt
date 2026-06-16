package com.nevoit.cresto.feature.bottomsheet

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.shapes.Capsule
import com.nevoit.cresto.R
import com.nevoit.cresto.theme.AppButtonColors
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.AppSpecs
import com.nevoit.cresto.theme.defaultEnterTransition
import com.nevoit.cresto.theme.defaultExitTransition
import com.nevoit.cresto.theme.getFlagColor
import com.nevoit.cresto.ui.components.CustomAnimatedVisibility
import com.nevoit.cresto.ui.components.glasense.GlasenseButtonAlt
import com.nevoit.cresto.ui.components.glasense.GlasenseModalTopBar
import com.nevoit.cresto.ui.components.packed.HorizontalFlagPicker
import com.nevoit.cresto.ui.components.packed.HorizontalPresetDatePicker
import com.nevoit.glasense.core.component.Icon
import com.nevoit.glasense.core.component.Text
import com.nevoit.glasense.core.component.VGap
import com.nevoit.glasense.core.interaction.DimIndication
import com.nevoit.glasense.core.interaction.overscroll.rememberOffsetOverscrollFactory
import com.nevoit.glasense.theme.GlasenseTheme
import com.nevoit.glasense.theme.LocalGlasenseContentColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.time.Duration.Companion.milliseconds

/**
 * Enum to represent the currently selected button in the AddTodoSheet.
 */
enum class SelectedButton {
    DUE_DATE, FLAG, NONE
}

/**
 * A composable function that provides a sheet for adding a new todo item.
 *
 * @param onAddClick Callback function to be invoked when the add button is clicked.
 * It provides the todo text, selected flag index, and due date.
 * @param onClose Callback function to be invoked when the close button is clicked.
 */
@Composable
fun AddTodoSheet(
    modifier: Modifier = Modifier,
    finalDate: LocalDate?,
    onFinalDateChange: (LocalDate?) -> Unit,
    focusRequester: FocusRequester = remember { FocusRequester() },
    autoRequestFocus: Boolean = true,
    onAddClick: (String, Int, LocalDate?) -> Unit,
    onNavigate: () -> Unit,
    onClose: () -> Unit,
    onRequestCustomDate: (Rect, LocalDate?, (LocalDate?) -> Unit) -> Unit
) {
    // State for tracking the currently selected button (due date, flag, etc.)
    var selectedButton by remember { mutableStateOf(SelectedButton.NONE) }
    var title by remember { mutableStateOf("") }
    var selectedIndex by remember { mutableIntStateOf(0) }

    val keyboardController = LocalSoftwareKeyboardController.current

    val onAdd = {
        if (title.isNotBlank()) {
            keyboardController?.hide()
            onAddClick(title, selectedIndex, finalDate)
        }
    }

    val hapticController = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val overscrollFactory = rememberOffsetOverscrollFactory()
    // Main layout
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp, 0.dp, 12.dp, 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        VGap()
        GlasenseModalTopBar(
            leading = {
                Action(
                    icon = painterResource(id = R.drawable.ic_cross),
                    contentDescription = stringResource(R.string.cancel),
                    onClick = onClose
                )
            },
            title = stringResource(R.string.new_todo),
            trailing = {
                Action(
                    icon = painterResource(id = R.drawable.ic_checkmark),
                    contentDescription = stringResource(R.string.done),
                    onClick = onAdd,
                    colors = AppButtonColors.primary(),
                    highlight = true
                )
            }
        )
        VGap()
        // Text field
        Box(
            modifier = Modifier
                .height(48.dp)
                .background(
                    AppColors.scrimNormal,
                    AppSpecs.textFieldShape
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onAdd() }),
                textStyle = GlasenseTheme.type.body.copy(color = AppColors.content),
                cursorBrush = SolidColor(AppColors.primary),
                singleLine = true
            )
        }
        VGap()
        // Provide the custom overscroll factory to the composable tree.
        CompositionLocalProvider(
            LocalOverscrollFactory provides overscrollFactory
        ) {
            // Buttons for due date and flag.
            val progress by animateFloatAsState(
                targetValue = when (selectedButton) {
                    SelectedButton.DUE_DATE -> 1f
                    SelectedButton.FLAG -> -1f
                    SelectedButton.NONE -> 0f
                },
                animationSpec = spring(
                    dampingRatio = 0.7f,
                    stiffness = 300f
                ),
                label = "buttons_expansion"
            )

            Layout(
                content = {
                    // Due date button.
                    GlasenseButtonAlt(
                        enabled = true,
                        shape = Capsule(),
                        onClick = {
                            hapticController.performHapticFeedback(HapticFeedbackType.ContextClick)
                            selectedButton = if (selectedButton == SelectedButton.DUE_DATE) {
                                SelectedButton.NONE
                            } else {
                                SelectedButton.DUE_DATE
                            }
                        },
                        modifier = Modifier.height(48.dp),
                        colors = AppButtonColors.secondary()
                            .copy(containerColor = AppColors.scrimNormal),
                        indication = true
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            // Animated visibility for the due date icon.
                            CustomAnimatedVisibility(
                                visible = selectedButton != SelectedButton.DUE_DATE,
                                enter = defaultEnterTransition,
                                exit = defaultExitTransition
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_calendar),
                                    contentDescription = stringResource(R.string.due_date),
                                    modifier = Modifier.width(28.dp),
                                    tint = if (finalDate != null) {
                                        AppColors.primary
                                    } else {
                                        AppColors.content.copy(alpha = 0.5F)
                                    }
                                )
                            }
                            // Animated visibility for the date picker.
                            CustomAnimatedVisibility(
                                visible = selectedButton == SelectedButton.DUE_DATE,
                                enter = defaultEnterTransition,
                                exit = defaultExitTransition
                            ) {
                                HorizontalPresetDatePicker(
                                    initialDate = finalDate,
                                    onDateSelected = {
                                        onFinalDateChange(it)
                                        selectedButton = SelectedButton.NONE
                                    },
                                    onRequestCustomDate = { coordinates ->
                                        onRequestCustomDate(
                                            coordinates.boundsInWindow(),
                                            finalDate
                                        ) { newDate ->
                                            onFinalDateChange(newDate)
                                            scope.launch {
                                                delay(100.milliseconds)
                                                selectedButton = SelectedButton.NONE
                                            }

                                        }
                                    },
                                    hapticController = hapticController
                                )

                            }
                        }

                    }
                    // Flag button.
                    GlasenseButtonAlt(
                        enabled = true,
                        shape = Capsule(),
                        onClick = {
                            hapticController.performHapticFeedback(HapticFeedbackType.ContextClick)
                            selectedButton = if (selectedButton == SelectedButton.FLAG) {
                                SelectedButton.NONE
                            } else {
                                SelectedButton.FLAG
                            }
                        },
                        modifier = Modifier.height(48.dp),
                        colors = AppButtonColors.secondary()
                            .copy(containerColor = AppColors.scrimNormal),
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            // Animated visibility for the flag icon.
                            CustomAnimatedVisibility(
                                visible = selectedButton != SelectedButton.FLAG,
                                enter = defaultEnterTransition,
                                exit = defaultExitTransition
                            ) {
                                val displayColor = getFlagColor(selectedIndex)
                                Icon(
                                    painter = if (displayColor == Color.Transparent) {
                                        painterResource(id = R.drawable.ic_flag)
                                    } else {
                                        painterResource(id = R.drawable.ic_flag_fill)
                                    },
                                    contentDescription = stringResource(R.string.flag),
                                    modifier = Modifier.width(28.dp),
                                    tint = if (displayColor == Color.Transparent) {
                                        AppColors.content.copy(alpha = 0.5F)
                                    } else {
                                        displayColor
                                    }
                                )
                            }
                            // Animated visibility for the flag picker.
                            CustomAnimatedVisibility(
                                visible = selectedButton == SelectedButton.FLAG,
                                enter = defaultEnterTransition,
                                exit = defaultExitTransition
                            ) {
                                HorizontalFlagPicker(
                                    selectedIndex = selectedIndex,
                                    onIndexSelected = { newIndex ->
                                        selectedIndex = newIndex
                                        selectedButton = SelectedButton.NONE
                                    },
                                    hapticController = hapticController
                                )
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) { measurables, constraints ->
                val totalWidth = constraints.maxWidth
                val spacerPx = 12.dp.roundToPx()
                val collapsedPx = 48.dp.roundToPx()
                val availableWidth = totalWidth - spacerPx
                val defaultWidth = availableWidth / 2f
                val expandedWidth = availableWidth - collapsedPx

                val dueDateWidth: Int
                val flagWidth: Int

                if (progress >= 0) {
                    dueDateWidth =
                        (defaultWidth + (expandedWidth - defaultWidth) * progress).toInt()
                    flagWidth = (defaultWidth + (collapsedPx - defaultWidth) * progress).toInt()
                } else {
                    val p = -progress
                    dueDateWidth = (defaultWidth + (collapsedPx - defaultWidth) * p).toInt()
                    flagWidth = (defaultWidth + (expandedWidth - defaultWidth) * p).toInt()
                }

                val dueDatePlaceable = measurables[0].measure(
                    Constraints.fixed(dueDateWidth, constraints.maxHeight)
                )
                val flagPlaceable = measurables[1].measure(
                    Constraints.fixed(flagWidth, constraints.maxHeight)
                )

                layout(totalWidth, constraints.maxHeight) {
                    dueDatePlaceable.placeRelative(0, 0)
                    flagPlaceable.placeRelative(dueDateWidth + spacerPx, 0)
                }
            }
        }
        VGap()
        CompositionLocalProvider(
            LocalGlasenseContentColor provides AppColors.contentVariant
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AppSpecs.cardShape)
                    .background(
                        color = AppColors.scrimNormal
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = DimIndication()
                    ) {
                        onNavigate()
                    }
                    .padding(horizontal = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter =
                            painterResource(id = R.drawable.ic_sliders),
                        contentDescription = stringResource(id = R.string.advanced),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .width(28.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.advanced),
                        fontSize = 16.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        painter =
                            painterResource(id = R.drawable.ic_chevron_forward_compact),
                        contentDescription = stringResource(id = R.string.advanced),
                        modifier = Modifier.height(20.dp),
                        tint = AppColors.contentVariant.copy(alpha = 0.3f)
                    )
                }
            }
        }
        // Request focus for the text field when the sheet is launched.
        LaunchedEffect(autoRequestFocus) {
            if (autoRequestFocus) {
                focusRequester.requestFocus()
            }
        }
    }
}
