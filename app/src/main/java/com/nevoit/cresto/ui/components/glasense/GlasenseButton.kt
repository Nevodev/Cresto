package com.nevoit.cresto.ui.components.glasense

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.capsule.ContinuousCapsule

/**
 * A custom button with press animations.
 *
 * @param enabled Controls the enabled state of the button.
 * @param shape The shape of the button.
 * @param onClick The callback to be invoked when the button is clicked.
 * @param modifier The modifier to be applied to the button.
 * @param colors The colors for the button in different states.
 * @param animated Whether to enable press animations.
 * @param content The content to be displayed inside the button.
 */
@Composable
fun GlasenseButton(
    enabled: Boolean = true,
    shape: Shape = ContinuousCapsule,
    onClick: () -> Unit,
    modifier: Modifier,
    colors: ButtonColors,
    animated: Boolean = true,
    content: @Composable () -> Unit,
) {
    val contentColor = if (enabled) colors.contentColor else colors.disabledContentColor
    val backgroundColor = if (enabled) colors.containerColor else colors.disabledContainerColor
    val interactionSource = remember { MutableInteractionSource() }

    // Animate scale and alpha for press feedback.
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.2f else 1.0f,
        animationSpec = spring(0.5f, 300f, 0.0001f)
    )
    val alpha by animateFloatAsState(
        targetValue = if (isPressed) 0.2f else 0f,
        animationSpec = spring(0.5f, 300f, 0.001f)
    )
    Box(
        modifier = Modifier
            .then(modifier)
            // Apply scale animation for press effect.
            .then(
                if (animated) Modifier.graphicsLayer {
                    scaleY = scale
                    scaleX = scale
                    transformOrigin = TransformOrigin.Center
                } else Modifier
            )
            .then(if (animated) Modifier.clip(shape) else Modifier)
            // Handle click events.
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                onClick = { onClick() },
                indication = null,
                role = Role.Button
            )
            .height(48.dp)
            .background(color = backgroundColor, shape = shape)
            // Draw a white flash overlay on press.
            .then(
                if (animated) {
                    Modifier.drawBehind {
                        drawRect(
                            size = this.size,
                            color = Color.White,
                            alpha = alpha,
                            blendMode = BlendMode.Plus
                        )
                    }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

/**
 * An alternative, simpler version of [GlasenseButton] without press animations.
 *
 * @param enabled Controls the enabled state of the button.
 * @param shape The shape of the button.
 * @param onClick The callback to be invoked when the button is clicked.
 * @param modifier The modifier to be applied to the button.
 * @param colors The colors for the button in different states.
 * @param indication Whether to show a ripple indication on click.
 * @param content The content to be displayed inside the button.
 */
@Composable
fun GlasenseButtonAlt(
    enabled: Boolean = true,
    shape: Shape = ContinuousCapsule,
    onClick: () -> Unit,
    modifier: Modifier,
    colors: ButtonColors,
    indication: Boolean = true,
    content: @Composable () -> Unit,
) {
    val contentColor = if (enabled) colors.contentColor else colors.disabledContentColor
    val backgroundColor = if (enabled) colors.containerColor else colors.disabledContainerColor
    val interactionSource = remember { MutableInteractionSource() }

    // Animatable for the press feedback effect's alpha.
    val alphaAni = remember { Animatable(0f) }
    // Observe interactions to animate the press feedback.
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    alphaAni.animateTo(1f, tween(100))
                }

                is PressInteraction.Release -> {
                    alphaAni.animateTo(0f, tween(200))
                }

                is PressInteraction.Cancel -> {
                    alphaAni.animateTo(0f, tween(200))
                }
            }
        }
    }

    Box(
        modifier = Modifier
            // Handle click events.
            .clickable(
                interactionSource = interactionSource,
                onClick = { onClick() },
                indication = null,
                role = Role.Button
            )
            .then(modifier)
            .defaultMinSize(minHeight = 48.dp)
            .clip(shape)
            .background(color = backgroundColor, shape = shape),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
        if (indication) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = alphaAni.value }
                    .background(Color.Black.copy(.1f))
            )
        }
    }
}

/**
 * A version of [GlasenseButton] with an adaptable size.
 *
 * @param width A lambda that returns the width of the button.
 * @param height A lambda that returns the height of the button.
 * @param padding Padding to be applied around the button.
 * @param tint An optional tint color for the content.
 * @param enabled Controls the enabled state of the button.
 * @param shape The shape of the button.
 * @param onClick The callback to be invoked when the button is clicked.
 * @param modifier The modifier to be applied to the button.
 * @param colors The colors for the button in different states.
 * @param animated Whether to enable press animations.
 * @param content The content to be displayed inside the button.
 */
@Composable
fun GlasenseButtonAdaptable(
    width: () -> Dp,
    height: () -> Dp,
    padding: PaddingValues,
    tint: Color? = null,
    enabled: Boolean = true,
    shape: Shape = ContinuousCapsule,
    onClick: () -> Unit,
    modifier: Modifier,
    colors: ButtonColors,
    animated: Boolean = true,
    content: @Composable () -> Unit,
) {
    val contentColor = tint ?: if (enabled) colors.contentColor else colors.disabledContentColor
    val backgroundColor = if (enabled) colors.containerColor else colors.disabledContainerColor
    val interactionSource = remember { MutableInteractionSource() }

    // Animate scale and alpha for press feedback.
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.2f else 1.0f,
        animationSpec = spring(0.5f, 300f, 0.0001f)
    )
    val alpha by animateFloatAsState(
        targetValue = if (isPressed) 0.2f else 0f,
        animationSpec = spring(0.5f, 300f, 0.001f)
    )
    Box(
        modifier = Modifier
            .padding(padding)
            .width(width())
            .height(height())
            // Apply scale animation for press effect.
            .then(
                if (animated) Modifier.graphicsLayer {
                    scaleY = scale
                    scaleX = scale
                    transformOrigin = TransformOrigin.Center
                } else Modifier
            )
            .then(modifier)
            .then(if (animated) Modifier.clip(shape) else Modifier)
            // Handle click events.
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                onClick = { onClick() },
                indication = null,
                role = Role.Button
            )
            .height(48.dp)
            .background(color = backgroundColor, shape = shape)
            // Draw a white flash overlay on press.
            .then(
                if (animated) {
                    Modifier.drawBehind {
                        drawRect(
                            size = this.size,
                            color = Color.White,
                            alpha = alpha,
                            blendMode = BlendMode.Plus
                        )
                    }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}
