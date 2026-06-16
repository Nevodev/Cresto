package com.nevoit.glasense.core.interaction.overscroll

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.OverscrollFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope

@Composable
fun rememberOffsetOverscrollFactory(
    animationSpec: AnimationSpec<Float> = OffsetOverscrollEffect.DefaultAnimationSpec,
    maxFraction: Float = 0.65f
): OverscrollFactory {
    val animationScope = rememberCoroutineScope()
    return remember(animationScope, animationSpec, maxFraction) {
        OffsetOverscrollFactory(
            animationScope = animationScope,
            animationSpec = animationSpec,
            maxFraction = maxFraction
        )
    }
}

internal data class OffsetOverscrollFactory(
    private val animationScope: CoroutineScope,
    private val animationSpec: AnimationSpec<Float> = OffsetOverscrollEffect.DefaultAnimationSpec,
    private val maxFraction: Float = 0.65f
) : OverscrollFactory {

    override fun createOverscrollEffect(): OverscrollEffect {
        return OffsetOverscrollEffect(
            animationScope = animationScope,
            animationSpec = animationSpec,
            maxFraction = maxFraction
        )
    }
}
