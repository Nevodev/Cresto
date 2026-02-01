package com.nevoit.cresto.ui.components.glasense

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class DimIndication(
    val color: Color = Color.Black,
    val maxAlpha: Float = 0.1f,
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode {
        return DimIndicationNode(interactionSource, color, maxAlpha)
    }
}

private class DimIndicationNode(
    private val interactionSource: InteractionSource,
    private val color: Color,
    private val maxAlpha: Float
) : Modifier.Node(), DrawModifierNode {

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
            drawRect(color = color.copy(alpha = alphaAnimatable.value))
        }
    }
}