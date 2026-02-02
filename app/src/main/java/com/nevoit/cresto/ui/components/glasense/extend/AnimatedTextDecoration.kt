package com.nevoit.cresto.ui.components.glasense.extend

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nevoit.cresto.ui.theme.glasense.AppColors

@Composable
fun LineThroughText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    lineColor: Color = AppColors.primary,
    strokeWidth: Dp = 2.dp,
    animationDuration: Int = 300,
    lineThrough: Boolean = false
) {
    val strokeWidth = with(LocalDensity.current) {
        strokeWidth.toPx()
    }

    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val progress = remember { Animatable(if (lineThrough) 1f else 0f) }
    val lineAlpha = remember { Animatable(if (lineThrough) 1f else 0f) }

    LaunchedEffect(lineThrough) {
        if (lineThrough) {
            if (progress.value == 1f && lineAlpha.value == 1f) {
                return@LaunchedEffect
            }
            
            lineAlpha.snapTo(1f)

            if (progress.value >= 1f) {
                progress.snapTo(0f)
            }

            progress.animateTo(
                1f,
                animationSpec = tween(
                    animationDuration,
                    easing = CubicBezierEasing(.2f, .2f, 0f, 1f)
                )
            )
        } else {
            lineAlpha.animateTo(0f, tween(100))
            progress.snapTo(0f)
        }
    }

    Text(
        text = text,
        style = style,
        onTextLayout = { textLayoutResult = it },
        modifier = modifier.drawWithContent() {
            drawContent()

            val layout = textLayoutResult ?: return@drawWithContent

            val totalTextWidth = (0 until layout.lineCount).sumOf {
                layout.multiParagraph.getLineWidth(it).toDouble()
            }.toFloat()

            val currentAnimatedLength = totalTextWidth * progress.value
            var accumulatedLength = 0f

            for (lineIndex in 0 until layout.lineCount) {
                val lineLeft = layout.getLineLeft(lineIndex)
                val lineWidth = layout.multiParagraph.getLineWidth(lineIndex)

                val lineY = layout.getLineTop(lineIndex) +
                        (layout.getLineBottom(lineIndex) - layout.getLineTop(lineIndex)) / 2

                if (accumulatedLength >= currentAnimatedLength) break

                val remainingNeeded = currentAnimatedLength - accumulatedLength
                val drawWidthInThisLine = minOf(lineWidth, remainingNeeded)

                if (drawWidthInThisLine > 0) {
                    drawLine(
                        color = lineColor,
                        start = Offset(x = lineLeft, y = lineY),
                        end = Offset(x = lineLeft + drawWidthInThisLine, y = lineY),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                        alpha = lineAlpha.value
                    )
                }
                accumulatedLength += lineWidth
            }
        }
    )
}

@Composable
fun LineThroughBasicTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    lineThrough: Boolean = false,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    lineColor: Color = AppColors.primary,
    strokeWidth: Dp = 2.dp,
    animationDuration: Int = 300,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActionHandler? = null,
    decorator: TextFieldDecorator? = null
) {
    val strokeWidth = with(LocalDensity.current) {
        strokeWidth.toPx()
    }

    var layoutResultProvider by remember { mutableStateOf<(() -> TextLayoutResult?)?>(null) }


    val progress = remember { Animatable(if (lineThrough) 1f else 0f) }
    val lineAlpha = remember { Animatable(if (lineThrough) 1f else 0f) }

    LaunchedEffect(lineThrough) {
        if (lineThrough) {
            if (progress.value == 1f && lineAlpha.value == 1f) {
                return@LaunchedEffect
            }

            lineAlpha.snapTo(1f)

            if (progress.value >= 1f) {
                progress.snapTo(0f)
            }

            progress.animateTo(
                1f,
                animationSpec = tween(
                    animationDuration,
                    easing = CubicBezierEasing(.2f, .2f, 0f, 1f)
                )
            )
        } else {
            lineAlpha.animateTo(0f, tween(100))
            progress.snapTo(0f)
        }
    }

    BasicTextField(
        state = state,
        textStyle = textStyle,
        cursorBrush = SolidColor(AppColors.primary),
        onTextLayout = { layoutResultProvider = it },
        keyboardOptions = keyboardOptions,
        onKeyboardAction = onKeyboardAction,
        decorator = decorator,
        modifier = modifier.drawWithContent {
            drawContent()

            if (!lineThrough && progress.value == 0f) return@drawWithContent

            val layout = layoutResultProvider?.invoke() ?: return@drawWithContent

            val totalTextWidth = (0 until layout.lineCount).sumOf {
                layout.multiParagraph.getLineWidth(it).toDouble()
            }.toFloat()

            val currentAnimatedLength = totalTextWidth * progress.value
            var accumulatedLength = 0f

            for (lineIndex in 0 until layout.lineCount) {
                val lineWidth = layout.multiParagraph.getLineWidth(lineIndex)
                val lineLeft = layout.getLineLeft(lineIndex)

                val lineY = layout.getLineTop(lineIndex) +
                        (layout.getLineBottom(lineIndex) - layout.getLineTop(lineIndex)) / 2

                if (accumulatedLength >= currentAnimatedLength) break

                val remainingNeeded = currentAnimatedLength - accumulatedLength
                val drawWidthInThisLine = minOf(lineWidth, remainingNeeded)

                if (drawWidthInThisLine > 0) {
                    drawLine(
                        color = lineColor,
                        start = Offset(x = lineLeft, y = lineY),
                        end = Offset(x = lineLeft + drawWidthInThisLine, y = lineY),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                        alpha = lineAlpha.value
                    )
                }
                accumulatedLength += lineWidth
            }
        }
    )
}