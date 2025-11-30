package com.nevoit.cresto.toolkit.overscroll

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.OverscrollFactory
import androidx.compose.foundation.gestures.Orientation
import com.nevoit.cresto.util.ProgressConverter
import kotlinx.coroutines.CoroutineScope

data class OffsetOverscrollFactory(
    private val orientation: Orientation,
    private val animationScope: CoroutineScope,
    private val animationSpec: AnimationSpec<Float> = OffsetOverscrollEffect.DefaultAnimationSpec,
    private val progressConverter: ProgressConverter = ProgressConverter.Default
) : OverscrollFactory {

    override fun createOverscrollEffect(): OverscrollEffect {
        return OffsetOverscrollEffect(
            orientation = orientation,
            animationScope = animationScope,
            animationSpec = animationSpec,
            progressConverter = progressConverter
        )
    }
}