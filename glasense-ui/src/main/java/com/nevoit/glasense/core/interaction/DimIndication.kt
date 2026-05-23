package com.nevoit.glasense.core.interaction

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
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
import androidx.compose.ui.util.fastCoerceIn
import com.nevoit.glasense.theme.LocalGlasenseContentColor
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

object DimIndicationDefaults {
    const val MAX_ALPHA = 0.1f
    val PressIn = tween<Float>(150)
    val ReleaseOut = tween<Float>(200)
    val ReleaseOutFast = tween<Float>(100)
}

data class DimIndication(
    val color: Color = Color.Unspecified,
    val maxAlpha: Float = DimIndicationDefaults.MAX_ALPHA,
    val shape: Shape = RectangleShape,
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode {
        return DimIndicationNode(interactionSource, color, maxAlpha, shape)
    }
}

private class DimIndicationNode(
    private val interactionSource: InteractionSource,
    private val color: Color,
    maxAlpha: Float,
    private val shape: Shape,
) : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {

    private val alphaAnimatable = Animatable(0f)
    private val targetAlpha = maxAlpha.fastCoerceIn(0f, 1f)

    override fun onAttach() {
        super.onAttach()
        coroutineScope.launch {
            interactionSource.interactions.collectLatest { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        alphaAnimatable.animateTo(targetAlpha, DimIndicationDefaults.PressIn)
                    }

                    is PressInteraction.Release -> {
                        val currentAlpha = alphaAnimatable.value

                        if (currentAlpha < targetAlpha * 0.5f) {
                            alphaAnimatable.animateTo(
                                targetAlpha * 0.5f,
                                DimIndicationDefaults.ReleaseOutFast
                            )
                        }

                        alphaAnimatable.animateTo(0f, DimIndicationDefaults.ReleaseOut)
                    }

                    is PressInteraction.Cancel -> {
                        alphaAnimatable.animateTo(0f, DimIndicationDefaults.ReleaseOut)
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
                currentValueOf(LocalGlasenseContentColor)
            }

            val outline = shape.createOutline(size, layoutDirection, this)
            drawOutline(outline, color = targetColor.copy(alpha = alphaAnimatable.value))
        }
    }
}