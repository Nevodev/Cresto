package com.nevoit.cresto.ui.components.bottomsheet

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush.Companion.verticalGradient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import com.nevoit.cresto.R
import com.nevoit.cresto.toolkit.overscroll.OffsetOverscrollFactory
import com.nevoit.cresto.ui.components.CustomAnimatedVisibility
import com.nevoit.cresto.ui.components.glasense.GlasenseButton
import com.nevoit.cresto.ui.components.glasense.GlasenseButtonAlt
import com.nevoit.cresto.ui.components.packed.HorizontalFlagPicker
import com.nevoit.cresto.ui.components.packed.HorizontalPresetDatePicker
import com.nevoit.cresto.ui.theme.glasense.AppButtonColors
import com.nevoit.cresto.ui.theme.glasense.AppColors
import com.nevoit.cresto.ui.theme.glasense.defaultEnterTransition
import com.nevoit.cresto.ui.theme.glasense.defaultExitTransition
import com.nevoit.cresto.ui.theme.glasense.getFlagColor
import com.nevoit.cresto.util.g2
import java.time.LocalDate

/**
 * Enum to represent the currently selected button in the AddTodoSheet.
 */
enum class SelectedButton {
    DUE_DATE, FLAG, HASHTAG, NONE
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
    onAddClick: (String, Int, LocalDate?) -> Unit,
    onClose: () -> Unit
) {
    // State for tracking the currently selected button (due date, flag, etc.)
    var selectedButton by remember { mutableStateOf(SelectedButton.NONE) }

    val state = rememberTextFieldState()
    // Focus requester to programmatically request focus for the text field.
    val focusRequester = remember { FocusRequester() }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var finalDate by remember { mutableStateOf<LocalDate?>(null) }

    val keyboardController = LocalSoftwareKeyboardController.current

    val onAdd = {
        val text = state.text.toString()
        if (text.isNotBlank()) {
            keyboardController?.hide()
            onAddClick(text, selectedIndex, finalDate)
        }
    }

    val animationScope = rememberCoroutineScope()
    // Create a custom overscroll factory.
    val overscrollFactory = remember {
        OffsetOverscrollFactory(
            orientation = Orientation.Horizontal,
            animationScope = animationScope,
        )
    }

    // Main layout
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp, 0.dp, 12.dp, 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        // Header row with close, title, and add buttons.
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // Close button.
            GlasenseButton(
                enabled = true,
                shape = CircleShape,
                onClick = { onClose() },
                modifier = Modifier
                    .width(48.dp)
                    .height(48.dp),
                colors = AppButtonColors.secondary(),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_cross),
                    contentDescription = stringResource(R.string.done),
                    modifier = Modifier.width(28.dp)
                )
            }
            // Title text.
            Text(
                text = stringResource(R.string.new_todo),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineSmall
            )
            // Add button with a custom border.
            GlasenseButton(
                enabled = true,
                shape = CircleShape,
                onClick = onAdd,
                modifier = Modifier
                    .width(48.dp)
                    .height(48.dp),
                colors = AppButtonColors.primary()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            val outline = CircleShape.createOutline(
                                size = this.size,
                                layoutDirection = this.layoutDirection,
                                density = this,
                            )
                            val gradientBrush = verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.White.copy(alpha = 0.2f),
                                    1.0f to Color.White.copy(alpha = 0.02f)
                                )
                            )
                            drawOutline(
                                outline = outline,
                                brush = gradientBrush,
                                style = Stroke(width = 3.dp.toPx()),
                                blendMode = BlendMode.Plus
                            )
                        }, contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_checkmark),
                        contentDescription = stringResource(R.string.done),
                        modifier = Modifier.width(28.dp)
                    )
                }

            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        // Text field
        Box(
            modifier = Modifier
                .height(48.dp)
                .background(
                    AppColors.content.copy(alpha = 0.05F),
                    ContinuousRoundedRectangle(12.dp)
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                state = state,
                modifier = Modifier
                    .padding(16.dp, 0.dp, 16.dp, 0.dp)
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                onKeyboardAction = { onAdd() },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = AppColors.content),
                cursorBrush = SolidColor(AppColors.primary)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        // Provide the custom overscroll factory to the composable tree.
        CompositionLocalProvider(
            LocalOverscrollFactory provides overscrollFactory
        ) {
            // Buttons for due date, flag, and hashtag.
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                val totalWidth = this.maxWidth

                // Calculate the width of the buttons based on the selected state.
                val collapsedSize = 48.dp
                val spacerSize = 12.dp
                val expandedWidth = totalWidth - (collapsedSize * 1) - (spacerSize * 1)
                val defaultWidth = (totalWidth - (spacerSize * 1)) / 2

                // Animate the width of the due date button.
                val dueDateWidth by animateDpAsState(
                    targetValue = when (selectedButton) {
                        SelectedButton.DUE_DATE -> expandedWidth
                        SelectedButton.NONE -> defaultWidth
                        else -> collapsedSize
                    },
                    animationSpec = spring(
                        dampingRatio = 0.7f,
                        stiffness = 300f
                    )
                )
                // Animate the width of the flag button.
                val flagWidth by animateDpAsState(
                    targetValue = when (selectedButton) {
                        SelectedButton.FLAG -> expandedWidth
                        SelectedButton.NONE -> defaultWidth
                        else -> collapsedSize
                    },
                    animationSpec = spring(
                        dampingRatio = 0.7f,
                        stiffness = 300f
                    )
                )
                // Animate the width of the hashtag button.
                val hashtagWidth by animateDpAsState(
                    targetValue = when (selectedButton) {
                        SelectedButton.HASHTAG -> expandedWidth
                        SelectedButton.NONE -> defaultWidth
                        else -> collapsedSize
                    },
                    animationSpec = spring(
                        dampingRatio = 0.7f,
                        stiffness = 300f
                    )
                )
                Row(
                    modifier = Modifier
                        .width(totalWidth)
                        .height(48.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Due date button.
                    GlasenseButtonAlt(
                        enabled = true,
                        shape = ContinuousCapsule(g2),
                        onClick = {
                            selectedButton = if (selectedButton == SelectedButton.DUE_DATE) {
                                SelectedButton.NONE
                            } else {
                                SelectedButton.DUE_DATE
                            }
                        },
                        modifier = Modifier
                            .height(48.dp)
                            .width(dueDateWidth),
                        colors = AppButtonColors.secondary(),
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
                                    modifier = Modifier.width(28.dp)
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
                                        finalDate = it
                                        selectedButton = SelectedButton.NONE
                                    }
                                )

                            }
                        }

                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    // Flag button.
                    GlasenseButtonAlt(
                        enabled = true,
                        shape = ContinuousCapsule(g2),
                        onClick = {
                            selectedButton = if (selectedButton == SelectedButton.FLAG) {
                                SelectedButton.NONE
                            } else {
                                SelectedButton.FLAG
                            }
                        },
                        modifier = Modifier
                            .height(48.dp)
                            .width(flagWidth),
                        colors = AppButtonColors.secondary(),
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
                                    }
                                )
                            }
                        }
                    }

                    /*pacer(modifier = Modifier.width(12.dp))
                    Button(
                        enabled = true,
                        shape = ContinuousCapsule(g2),
                        onClick = {
                            selectedButton = if (selectedButton == SelectedButton.HASHTAG) {
                                SelectedButton.NONE
                            } else {
                                SelectedButton.HASHTAG
                            }
                        },
                        modifier = Modifier
                            .height(48.dp)
                            .width(hashtagWidth),
                        colors = AppButtonColors.secondary(),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_hashtag),
                            contentDescription = "Tag",
                            modifier = Modifier.width(28.dp)
                        )
                    }*/

                }

            }
        }
        // Request focus for the text field when the sheet is launched.
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}