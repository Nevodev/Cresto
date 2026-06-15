package com.nevoit.glasense.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Indication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.nevoit.glasense.core.interaction.DimIndication
import com.nevoit.glasense.theme.GlasenseTheme
import com.nevoit.glasense.theme.LocalGlasenseContentColor

enum class ButtonRole {
    Destructive,
    Cancel
}

@Stable
interface ButtonStyle {
    @Composable
    fun Content(configuration: ButtonStyleConfiguration)
}

class ButtonStyleConfiguration internal constructor(
    val action: () -> Unit,
    val modifier: Modifier,
    val enabled: Boolean,
    val role: ButtonRole?,
    val interactionSource: MutableInteractionSource,
    val content: @Composable RowScope.() -> Unit
)

val LocalButtonStyle = staticCompositionLocalOf<ButtonStyle> { DefaultButtonStyle }

@Composable
fun Button(
    action: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    role: ButtonRole? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    style: ButtonStyle = LocalButtonStyle.current,
    content: @Composable RowScope.() -> Unit
) {
    style.Content(
        ButtonStyleConfiguration(
            action = action,
            modifier = modifier,
            enabled = enabled,
            role = role,
            interactionSource = interactionSource,
            content = content
        )
    )
}

@Immutable
data class ButtonColors(
    val containerColor: Color,
    val contentColor: Color,
    val destructiveContainerColor: Color,
    val destructiveContentColor: Color,
    val cancelContainerColor: Color,
    val cancelContentColor: Color
) {
    fun containerColor(role: ButtonRole?): Color {
        return when (role) {
            ButtonRole.Destructive -> destructiveContainerColor
            ButtonRole.Cancel -> cancelContainerColor
            else -> containerColor
        }
    }

    fun contentColor(role: ButtonRole?): Color {
        return when (role) {
            ButtonRole.Destructive -> destructiveContentColor
            ButtonRole.Cancel -> cancelContentColor
            else -> contentColor
        }
    }
}

@Stable
sealed interface ButtonPressEffect {
    data object None : ButtonPressEffect
    data object Dim : ButtonPressEffect

    data class Scale(
        val pressedScale: Float = 1.2f,
        val overlayAlpha: Float = 0.2f
    ) : ButtonPressEffect
}

object ButtonDefaults {
    val MinHeight = 48.dp
    val ContentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)

    @Composable
    fun filledStyle(
        colors: ButtonColors = filledColors(),
        shape: Shape = GlasenseTheme.specs.buttonShape,
        minHeight: Dp = MinHeight,
        contentPadding: PaddingValues = ContentPadding,
        pressEffect: ButtonPressEffect = ButtonPressEffect.Scale(),
        disabledAlpha: Float = DISABLED_ALPHA
    ): ButtonStyle {
        return remember(
            colors,
            shape,
            minHeight,
            contentPadding,
            pressEffect,
            disabledAlpha
        ) {
            SurfaceButtonStyle(
                colors = colors,
                shape = shape,
                minHeight = minHeight,
                contentPadding = contentPadding,
                pressEffect = pressEffect,
                disabledAlpha = disabledAlpha
            )
        }
    }

    @Composable
    fun plainStyle(
        colors: ButtonColors = plainColors(),
        shape: Shape = GlasenseTheme.specs.buttonShape,
        minHeight: Dp = MinHeight,
        contentPadding: PaddingValues = ContentPadding,
        pressEffect: ButtonPressEffect = ButtonPressEffect.Scale(),
        disabledAlpha: Float = DISABLED_ALPHA
    ): ButtonStyle {
        return remember(
            colors,
            shape,
            minHeight,
            contentPadding,
            pressEffect,
            disabledAlpha
        ) {
            SurfaceButtonStyle(
                colors = colors,
                shape = shape,
                minHeight = minHeight,
                contentPadding = contentPadding,
                pressEffect = pressEffect,
                disabledAlpha = disabledAlpha
            )
        }
    }

    @Composable
    fun filledColors(
        containerColor: Color = GlasenseTheme.colors.primary,
        contentColor: Color = GlasenseTheme.colors.onPrimary,
        destructiveContainerColor: Color = GlasenseTheme.colors.error,
        destructiveContentColor: Color = GlasenseTheme.colors.onError,
        cancelContainerColor: Color = GlasenseTheme.colors.scrimNormal,
        cancelContentColor: Color = GlasenseTheme.colors.content
    ): ButtonColors {
        return ButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            destructiveContainerColor = destructiveContainerColor,
            destructiveContentColor = destructiveContentColor,
            cancelContainerColor = cancelContainerColor,
            cancelContentColor = cancelContentColor
        )
    }

    @Composable
    fun plainColors(
        contentColor: Color = GlasenseTheme.colors.primary,
        destructiveContentColor: Color = GlasenseTheme.colors.error,
        cancelContentColor: Color = GlasenseTheme.colors.contentVariant
    ): ButtonColors {
        return ButtonColors(
            containerColor = Color.Transparent,
            contentColor = contentColor,
            destructiveContainerColor = Color.Transparent,
            destructiveContentColor = destructiveContentColor,
            cancelContainerColor = Color.Transparent,
            cancelContentColor = cancelContentColor
        )
    }

    private const val DISABLED_ALPHA = 0.5f
}

private object DefaultButtonStyle : ButtonStyle {
    @Composable
    override fun Content(configuration: ButtonStyleConfiguration) {
        ButtonDefaults.filledStyle().Content(configuration)
    }
}

private class SurfaceButtonStyle(
    private val colors: ButtonColors,
    private val shape: Shape,
    private val minHeight: Dp,
    private val contentPadding: PaddingValues,
    private val pressEffect: ButtonPressEffect,
    private val disabledAlpha: Float
) : ButtonStyle {

    @Composable
    override fun Content(configuration: ButtonStyleConfiguration) {
        val containerColor = colors.containerColor(
            role = configuration.role
        )
        val contentColor = colors.contentColor(
            role = configuration.role
        )

        when (val effect = pressEffect) {
            is ButtonPressEffect.Scale -> {
                val isPressed by configuration.interactionSource.collectIsPressedAsState()
                val animatePress = configuration.enabled && isPressed
                val scale by animateFloatAsState(
                    targetValue = if (animatePress) effect.pressedScale else 1f,
                    animationSpec = spring(
                        dampingRatio = 0.5f,
                        stiffness = 300f,
                        visibilityThreshold = 0.0001f
                    )
                )
                val overlayAlpha by animateFloatAsState(
                    targetValue = if (animatePress) effect.overlayAlpha else 0f,
                    animationSpec = spring(
                        dampingRatio = 0.5f,
                        stiffness = 300f,
                        visibilityThreshold = 0.001f
                    )
                )

                ButtonSurface(
                    configuration = configuration,
                    containerColor = containerColor,
                    contentColor = contentColor,
                    pressModifier = Modifier
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = TransformOrigin.Center
                        }
                        .drawBehind {
                            if (overlayAlpha > 0f) {
                                drawRect(
                                    size = size,
                                    color = Color.White,
                                    alpha = overlayAlpha,
                                    blendMode = BlendMode.Plus
                                )
                            }
                        }
                )
            }

            ButtonPressEffect.Dim -> {
                ButtonSurface(
                    configuration = configuration,
                    containerColor = containerColor,
                    contentColor = contentColor,
                    indication = DimIndication()
                )
            }

            ButtonPressEffect.None -> {
                ButtonSurface(
                    configuration = configuration,
                    containerColor = containerColor,
                    contentColor = contentColor
                )
            }
        }
    }

    @Composable
    private fun ButtonSurface(
        configuration: ButtonStyleConfiguration,
        containerColor: Color,
        contentColor: Color,
        pressModifier: Modifier = Modifier,
        indication: Indication? = null
    ) {
        Box(
            modifier = configuration.modifier
                .alpha(if (configuration.enabled) 1f else disabledAlpha)
                .then(pressModifier)
                .defaultMinSize(minHeight = minHeight)
                .clip(shape)
                .background(
                    color = containerColor,
                    shape = shape
                )
                .clickable(
                    enabled = configuration.enabled,
                    interactionSource = configuration.interactionSource,
                    indication = indication,
                    role = Role.Button,
                    onClick = configuration.action
                )
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(
                LocalGlasenseContentColor provides contentColor
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    content = configuration.content
                )
            }
        }
    }
}
