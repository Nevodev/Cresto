package com.nevoit.cresto.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseOutExpo
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun <D : Any> CrossfadeNavContainer(
    destination: D,
    destinations: List<D>,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(D) -> Unit,
) {
    val saveableStateHolder = rememberSaveableStateHolder()
    Box(modifier = modifier.fillMaxSize()) {
        destinations.forEach { pageDestination ->
            key(pageDestination) {
                ManualAnimatedVisibility(
                    visible = destination == pageDestination,
                ) {
                    saveableStateHolder.SaveableStateProvider(key = pageDestination) {
                        content(pageDestination)
                    }
                }
            }
        }
    }
}

@Composable
fun ManualAnimatedVisibility(
    visible: Boolean,
    content: @Composable BoxScope.() -> Unit,
) {
    val alpha = remember { Animatable(if (visible) 1f else 0f, 0.000001f) }
    val scale = remember { Animatable(if (visible) 1f else 0.95f, 0.000001f) }

    LaunchedEffect(visible) {
        if (visible) {
            launch {
                delay(100.milliseconds)
                alpha.animateTo(1f, tween(200))
            }
            launch {
                delay(100.milliseconds)
                scale.animateTo(1f, tween(400, easing = EaseOutExpo))
            }
        } else {
            launch {
                alpha.animateTo(0f, tween(200))
            }
            launch {
                scale.animateTo(0.95f, tween(600, easing = CubicBezierEasing(.2f, .2f, .0f, 1f)))
            }
        }
    }

    if (visible || alpha.value > 0f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // Scale the composed page rather than transforming each child's clip.
                    compositingStrategy = if (scale.value != 1f) {
                        CompositingStrategy.Offscreen
                    } else {
                        CompositingStrategy.Auto
                    }
                    this.alpha = alpha.value
                    scaleX = scale.value
                    scaleY = scale.value
                }
        ) {
            content()
        }
    }
}
