package com.nevoit.cresto.ui.components.packed

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * A composable that represents a generic configuration item in a settings screen.
 * It consists of a title on the left and a content slot on the right, typically for a control like a switch or a button.
 *
 * @param title The main text label for the configuration item.
 * @param color The color of the title text. Defaults to `Color.Unspecified`, which means it will use the color from the current `LocalContentColor`.
 * @param content A composable lambda that defines the content to be placed at the end of the row (e.g., a switch, a button, or another custom composable).
 */
@Composable
fun ConfigItem(
    title: String,
    color: Color = Color.Unspecified,
    clickable: Boolean = false,
    indication: Boolean = false,
    onClick: () -> Unit = {},
    content: @Composable () -> Unit
) {
    // Interaction source to track press state.
    val interactionSource = remember { MutableInteractionSource() }

    // Animatable for the press feedback effect's alpha.
    val alphaAni = remember { Animatable(1f) }
    // Observe interactions to animate the press feedback.
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    alphaAni.animateTo(0.5f, tween(100))
                }

                is PressInteraction.Release -> {
                    alphaAni.animateTo(1f, tween(200))
                }

                is PressInteraction.Cancel -> {
                    alphaAni.animateTo(1f, tween(200))
                }
            }
        }
    }

    // A Row layout to arrange the title and content horizontally.
    Row(
        modifier = Modifier
            .then(
                if (clickable && indication) Modifier.graphicsLayer {
                    alpha = alphaAni.value
                } else Modifier)
            .fillMaxWidth()
            .defaultMinSize(minHeight = 32.dp)
            .then(
                if (clickable) Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) { onClick() } else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // The text label for the configuration item.
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically),
            color = color
        )
        Spacer(modifier = Modifier.width(12.dp))
        // The composable content provided to the function, placed at the end of the row.
        content()
    }
}
