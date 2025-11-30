package com.nevoit.cresto.ui.components.glasense

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.nevoit.cresto.R
import com.nevoit.cresto.ui.theme.glasense.CalculatedColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val ANIMATION_DURATION_SIZE = 200
private const val ANIMATION_DURATION_ALPHA_IN = 100
private const val ANIMATION_DURATION_ALPHA_OUT = 150
private val SizeEasing = CubicBezierEasing(0.2f, 0.81f, 0.34f, 1.0f)

@Composable
fun GlasenseCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    role: Role = Role.Checkbox
) {
    val density = LocalDensity.current
    val dimensions = remember(density) {
        CheckboxDimensions(
            defaultRadius = with(density) { 11.dp.toPx() },
            strokeWidth = with(density) { 2.dp.toPx() },
            checkedRadius = with(density) { 12.dp.toPx() },
            uncheckedRadius = with(density) { 6.dp.toPx() }
        )
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val strokeColor = CalculatedColor.onSurfaceContainerBold

    val sizeAnim =
        remember { Animatable(if (checked) dimensions.checkedRadius else dimensions.uncheckedRadius) }
    val alphaAnim = remember { Animatable(if (checked) 1f else 0f) }

    LaunchedEffect(checked) {
        if (checked) {
            launch {
                sizeAnim.animateTo(
                    targetValue = dimensions.checkedRadius,
                    animationSpec = tween(
                        durationMillis = ANIMATION_DURATION_SIZE,
                        easing = SizeEasing
                    )
                )
            }
            launch {
                alphaAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = ANIMATION_DURATION_ALPHA_IN)
                )
            }
        } else {
            launch {
                alphaAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = ANIMATION_DURATION_ALPHA_OUT)
                )
            }
            sizeAnim.snapTo(dimensions.checkedRadius)
            delay(150)
            sizeAnim.snapTo(dimensions.uncheckedRadius)
        }
    }

    val previousChecked = remember { mutableStateOf(checked) }
    var currentIconRes by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(checked) {
        if (previousChecked.value != checked) {
            currentIconRes = if (checked) {
                R.drawable.avd_checkmark_check
            } else {
                R.drawable.avd_checkmark_uncheck
            }
            previousChecked.value = checked
        }
    }

    Box(
        modifier = modifier
            .size(24.dp)
            .backgroundAnimation(
                sizeAnim = sizeAnim,
                alphaAnim = alphaAnim,
                dimensions = dimensions,
                fillColor = primaryColor,
                strokeColor = strokeColor
            )
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                role = role,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ),
        contentAlignment = Alignment.Center
    ) {
        CheckmarkContent(
            iconRes = currentIconRes,
            shouldShowStatic = previousChecked.value
        )
    }
}

@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
private fun CheckmarkContent(
    @DrawableRes iconRes: Int?,
    shouldShowStatic: Boolean
) {
    if (iconRes != null) {
        key(iconRes) {
            val avd = AnimatedImageVector.animatedVectorResource(id = iconRes)
            var atEnd by remember { mutableStateOf(false) }
            val painter = rememberAnimatedVectorPainter(animatedImageVector = avd, atEnd = atEnd)
            LaunchedEffect(Unit) { atEnd = true }
            Image(painter = painter, contentDescription = null)
        }
    } else {
        if (shouldShowStatic) {
            Image(
                modifier = Modifier.scale(1.1f),
                painter = painterResource(id = R.drawable.ic_checkbox_checkmark_animation_ready),
                contentDescription = null
            )
        }
    }
}

private fun Modifier.backgroundAnimation(
    sizeAnim: Animatable<Float, *>,
    alphaAnim: Animatable<Float, *>,
    dimensions: CheckboxDimensions,
    fillColor: Color,
    strokeColor: Color
): Modifier = this.drawBehind {
    val currentSize = sizeAnim.value
    val currentAlpha = alphaAnim.value

    if (currentAlpha > 0f) {
        drawCircle(
            color = fillColor,
            radius = currentSize,
            alpha = currentAlpha
        )
    }

    val strokeAlpha = 1f - currentAlpha
    if (strokeAlpha > 0f) {
        drawCircle(
            color = strokeColor,
            radius = dimensions.defaultRadius,
            alpha = strokeAlpha,
            style = Stroke(width = dimensions.strokeWidth)
        )
    }
}

private data class CheckboxDimensions(
    val defaultRadius: Float,
    val strokeWidth: Float,
    val checkedRadius: Float,
    val uncheckedRadius: Float
)

@Stable
class CheckBoxState<T>(initialSelection: T? = null) {
    var selectedValue by mutableStateOf(initialSelection)
        private set

    fun onSelectionChange(value: T) {
        if (selectedValue != value) {
            selectedValue = value
        }
    }

    fun select(value: T) {
        selectedValue = value
    }

    companion object {
        fun <T : Any> getSaver(): Saver<CheckBoxState<T>, T> {
            return Saver(
                save = { it.selectedValue },
                restore = { CheckBoxState(it) }
            )
        }
    }
}

@Composable
fun <T : Any> rememberCheckBoxState(initialSelection: T? = null): CheckBoxState<T> {
    return rememberSaveable(saver = CheckBoxState.getSaver()) {
        CheckBoxState(initialSelection)
    }
}

@Composable
fun <T> GlasenseCheckbox(
    state: CheckBoxState<T>,
    value: T,
    modifier: Modifier = Modifier
) {
    val isChecked = state.selectedValue == value

    GlasenseCheckbox(
        checked = isChecked,
        onCheckedChange = {
            state.onSelectionChange(value)
        },
        modifier = modifier,
        role = Role.RadioButton
    )
}