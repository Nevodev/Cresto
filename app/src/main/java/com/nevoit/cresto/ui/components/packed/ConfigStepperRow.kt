package com.nevoit.cresto.ui.components.packed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nevoit.cresto.R
import com.nevoit.cresto.theme.AppButtonColors
import com.nevoit.cresto.theme.AppSpecs
import com.nevoit.cresto.ui.components.glasense.GlasenseButton
import com.nevoit.glasense.core.component.Icon
import com.nevoit.glasense.core.component.Text
import com.nevoit.glasense.theme.GlasenseTheme

/**
 * A configuration row that edits an integer value with decrement/increment buttons and an
 * inline numeric field for precise input.
 *
 * Styling mirrors [ConfigTextField]: an optional title above a rounded card that holds the
 * editor. Values outside [minValue]..[maxValue] are never emitted through [onValueChange].
 *
 * @param title Label rendered above the card.
 * @param value Current value.
 * @param onValueChange Called with a clamped value whenever the user steps or types a valid one.
 * @param backgroundColor Card background, supplied by the caller to match the surrounding page.
 * @param minValue Inclusive lower bound; the decrement button disables at this value.
 * @param maxValue Inclusive upper bound; the increment button disables at this value.
 * @param step Amount added or removed per button press.
 * @param valueLabel Formats [value] for display, e.g. "30 s".
 * @param hint Optional helper text rendered under the card.
 */
@Composable
fun ConfigStepperRow(
    modifier: Modifier = Modifier,
    title: String? = null,
    backgroundColor: Color,
    value: Int,
    onValueChange: (Int) -> Unit,
    minValue: Int,
    maxValue: Int,
    step: Int,
    valueLabel: @Composable (Int) -> String,
    hint: String? = null
) {
    // Local buffer so the user can clear the field while typing without fighting the caller state.
    var text by remember { mutableStateOf(value.toString()) }
    var isFocused by remember { mutableStateOf(false) }

    // Only re-sync from the outside while the field is not being edited, so typing is not clobbered.
    LaunchedEffect(value) {
        if (!isFocused) {
            text = value.toString()
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (title != null) {
            Text(
                text = title,
                style = GlasenseTheme.type.subHeadline.copy(lineHeight = 14.sp),
                color = GlasenseTheme.colors.contentVariant,
                modifier = Modifier
                    .padding(
                        start = 12.dp,
                        top = 0.dp,
                        end = 12.dp,
                        bottom = 12.dp
                    )
                    .fillMaxWidth()
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .background(color = backgroundColor, shape = AppSpecs.cardShape)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = valueLabel(value),
                    style = GlasenseTheme.type.subHeadline.copy(lineHeight = 14.sp),
                    color = GlasenseTheme.colors.contentVariant,
                    modifier = Modifier.weight(1f)
                )

                GlasenseButton(
                    enabled = value > minValue,
                    shape = CircleShape,
                    onClick = { onValueChange((value - step).coerceAtLeast(minValue)) },
                    modifier = Modifier.size(36.dp),
                    colors = AppButtonColors.action()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_minus),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }

                BasicTextField(
                    value = text,
                    onValueChange = { raw ->
                        val digits = raw.filter(Char::isDigit).take(4)
                        text = digits
                        val parsed = digits.toIntOrNull() ?: return@BasicTextField
                        if (parsed in minValue..maxValue) {
                            onValueChange(parsed)
                        }
                    },
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .widthIn(min = 48.dp)
                        .onFocusChanged { focusState ->
                            if (!focusState.isFocused) {
                                isFocused = false
                                // Discard partial (out of range or empty) input when leaving the field.
                                val parsed = text.toIntOrNull()
                                if (parsed == null || parsed !in minValue..maxValue) {
                                    text = value.toString()
                                }
                            } else {
                                isFocused = true
                            }
                        },
                    cursorBrush = SolidColor(GlasenseTheme.colors.primary),
                    textStyle = TextStyle(
                        color = GlasenseTheme.colors.content,
                        fontSize = 16.sp
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions.Default,
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.Center) {
                            innerTextField()
                        }
                    }
                )

                GlasenseButton(
                    enabled = value < maxValue,
                    shape = CircleShape,
                    onClick = { onValueChange((value + step).coerceAtMost(maxValue)) },
                    modifier = Modifier.size(36.dp),
                    colors = AppButtonColors.action()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        if (hint != null) {
            Text(
                text = hint,
                color = GlasenseTheme.colors.contentVariant,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 12.dp, top = 8.dp, end = 12.dp)
            )
        }
    }
}
