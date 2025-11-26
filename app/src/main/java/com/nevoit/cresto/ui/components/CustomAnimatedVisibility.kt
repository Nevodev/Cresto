package com.nevoit.cresto.ui.components

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Defines a fade animation.
data class Fade(val animationSpec: FiniteAnimationSpec<Float>)

// Defines a scale animation.
data class Scale(
    val animationSpec: FiniteAnimationSpec<Float>,
    val scale: Float
)

// Defines a blur animation.
data class Blur(
    val animationSpec: FiniteAnimationSpec<Dp>,
    val radius: Dp
)

/**
 * Represents the entering transition for CustomAnimatedVisibility.
 */
data class MyEnterTransition(
    val fade: Fade? = null,
    val scale: Scale? = null,
    val blur: Blur? = null
) {
    operator fun plus(other: MyEnterTransition): MyEnterTransition {
        return MyEnterTransition(
            fade = other.fade ?: this.fade,
            scale = other.scale ?: this.scale,
            blur = other.blur ?: this.blur
        )
    }
}

/**
 * Represents the exiting transition for CustomAnimatedVisibility.
 */
data class MyExitTransition(
    val fade: Fade? = null,
    val scale: Scale? = null,
    val blur: Blur? = null
) {
    operator fun plus(other: MyExitTransition): MyExitTransition {
        return MyExitTransition(
            fade = other.fade ?: this.fade,
            scale = other.scale ?: this.scale,
            blur = other.blur ?: this.blur
        )
    }
}

// Creates a fade-in enter transition.
fun myFadeIn(animationSpec: FiniteAnimationSpec<Float> = tween(300)): MyEnterTransition =
    MyEnterTransition(fade = Fade(animationSpec = animationSpec))

// Creates a scale-in enter transition.
fun myScaleIn(
    animationSpec: FiniteAnimationSpec<Float> = spring(stiffness = Spring.StiffnessMedium),
    initialScale: Float = 0.9f
): MyEnterTransition =
    MyEnterTransition(scale = Scale(animationSpec = animationSpec, scale = initialScale))

// Creates a blur-in enter transition.
fun myBlurIn(
    animationSpec: FiniteAnimationSpec<Dp> = tween(300),
    initialRadius: Dp = 8.dp
): MyEnterTransition =
    MyEnterTransition(blur = Blur(animationSpec = animationSpec, radius = initialRadius))

// Creates a fade-out exit transition.
fun myFadeOut(animationSpec: FiniteAnimationSpec<Float> = tween(300)): MyExitTransition =
    MyExitTransition(fade = Fade(animationSpec = animationSpec))

// Creates a scale-out exit transition.
fun myScaleOut(
    animationSpec: FiniteAnimationSpec<Float> = spring(stiffness = Spring.StiffnessMedium),
    targetScale: Float = 0.9f
): MyExitTransition =
    MyExitTransition(scale = Scale(animationSpec = animationSpec, scale = targetScale))

// Creates a blur-out exit transition.
fun myBlurOut(
    animationSpec: FiniteAnimationSpec<Dp> = tween(300),
    targetRadius: Dp = 8.dp
): MyExitTransition =
    MyExitTransition(blur = Blur(animationSpec = animationSpec, radius = targetRadius))

/**
 * A custom animated visibility composable that animates content in and out.
 * This version uses a MutableTransitionState to control visibility.
 */
@Composable
fun CustomAnimatedVisibility(
    visibleState: MutableTransitionState<Boolean>,
    modifier: Modifier = Modifier,
    enter: MyEnterTransition,
    exit: MyExitTransition,
    content: @Composable () -> Unit
) {
    val transition = rememberTransition(visibleState, label = "MyAnimatedVisibility")

    // Only compose the content if it's visible or animating.
    if (visibleState.currentState || visibleState.targetState) {

        val hiddenStateScale = when {
            visibleState.targetState -> enter.scale?.scale ?: 1f
            else -> exit.scale?.scale ?: 1f
        }

        val hiddenStateBlur = when {
            visibleState.targetState -> enter.blur?.radius ?: 0.dp
            else -> exit.blur?.radius ?: 0.dp
        }

        // Animate alpha property.
        val alpha by transition.animateFloat(
            label = "alpha",
            transitionSpec = {
                if (targetState) enter.fade?.animationSpec ?: tween()
                else exit.fade?.animationSpec ?: tween()
            }
        ) { isVisible -> if (isVisible) 1f else 0f }

        // Animate scale property.
        val scale by transition.animateFloat(
            label = "scale",
            transitionSpec = {
                if (targetState) enter.scale?.animationSpec ?: spring()
                else exit.scale?.animationSpec ?: spring()
            }
        ) { isVisible ->
            if (isVisible) 1f else hiddenStateScale
        }

        // Animate blur radius property.
        val blurRadius by transition.animateDp(
            label = "blur",
            transitionSpec = {
                if (targetState) enter.blur?.animationSpec ?: tween()
                else exit.blur?.animationSpec ?: tween()
            }
        ) { isVisible ->
            if (isVisible) 0.dp else hiddenStateBlur
        }

        // Apply animations to the content box.
        Box(
            modifier = modifier
                .then(if (blurRadius > 0.dp) Modifier.blur(radius = blurRadius) else Modifier)
                .graphicsLayer {
                    this.alpha = alpha
                    this.scaleX = scale
                    this.scaleY = scale
                }
        ) {
            content()
        }
    }
}

/**
 * A custom animated visibility composable that animates content in and out.
 * This version uses a simple Boolean to control visibility.
 */
@Composable
fun CustomAnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: MyEnterTransition,
    exit: MyExitTransition,
    content: @Composable () -> Unit
) {
    val visibleState = remember { MutableTransitionState(initialState = visible) }

    LaunchedEffect(visible) {
        visibleState.targetState = visible
    }

    CustomAnimatedVisibility(visibleState, modifier, enter, exit, content)
}
