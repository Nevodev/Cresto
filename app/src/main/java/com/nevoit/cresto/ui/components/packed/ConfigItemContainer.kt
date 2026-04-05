package com.nevoit.cresto.ui.components.packed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.AppSpecs

/**
 * A container for configuration items.
 *
 * @param title An optional title for the container.
 * @param backgroundColor The background color of the container.
 * @param content The content of the container.
 */
@Composable
fun ConfigItemContainer(
    title: String? = null,
    backgroundColor: Color,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Display the title if it's provided.
        if (title != null) {
            Text(
                text = title,
                fontSize = 14.sp,
                lineHeight = 14.sp,
                color = AppColors.contentVariant,
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
        // The main container box with background and shape.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = backgroundColor, shape = AppSpecs.cardShape)
        ) {
            // Inner box with padding for the content.
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun ConfigTextField(
    title: String? = null,
    backgroundColor: Color,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    decorateText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Display the title if it's provided.
        if (title != null) {
            Text(
                text = title,
                fontSize = 14.sp,
                lineHeight = 14.sp,
                color = AppColors.contentVariant,
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
        // The main container box with background and shape.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentSize()
                .defaultMinSize(minHeight = 48.dp)
                .background(color = backgroundColor, shape = AppSpecs.cardShape)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .align(Alignment.CenterStart)
                    .fillMaxSize(),
                cursorBrush = SolidColor(AppColors.primary),
                textStyle = TextStyle(
                    color = AppColors.content,
                    fontSize = 16.sp
                ),
                singleLine = singleLine,
                minLines = minLines,
                maxLines = maxLines,
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (decorateText != null && value.isEmpty()) {
                            Text(
                                text = decorateText,
                                color = AppColors.contentVariant,
                                fontSize = 16.sp
                            )
                        }
                        innerTextField()
                    }
                },
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions
            )
        }
    }
}
