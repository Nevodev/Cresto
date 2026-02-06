package com.nevoit.cresto.ui.components.glasense

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.currentValueOf
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class DimIndication(
    val color: Color = Color.Unspecified,
    val maxAlpha: Float = 0.1f,
    val shape: Shape = RectangleShape,
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode {
        return DimIndicationNode(interactionSource, color, maxAlpha, shape)
    }
}

private class DimIndicationNode(
    private val interactionSource: InteractionSource,
    private val color: Color,
    private val maxAlpha: Float,
    private val shape: Shape,
) : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {

    private val alphaAnimatable = Animatable(0f)

    override fun onAttach() {
        super.onAttach()
        coroutineScope.launch {
            interactionSource.interactions.collectLatest { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        alphaAnimatable.animateTo(maxAlpha, tween(150))
                    }

                    is PressInteraction.Release -> {
                        val currentAlpha = alphaAnimatable.value

                        if (currentAlpha < maxAlpha * 0.5f) {
                            alphaAnimatable.animateTo(maxAlpha * 0.5f, tween(100))
                        }

                        alphaAnimatable.animateTo(0f, tween(200))
                    }

                    is PressInteraction.Cancel -> {
                        alphaAnimatable.animateTo(0f, tween(200))
                    }
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        if (alphaAnimatable.value > 0f) {
            val targetColor = if (color != Color.Unspecified) {
                color
            } else {
                currentValueOf(LocalContentColor)
            }

            val outline = shape.createOutline(size, layoutDirection, this)
            drawOutline(outline, color = targetColor.copy(alpha = alphaAnimatable.value))
        }
    }
}