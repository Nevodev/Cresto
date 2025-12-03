package com.nevoit.cresto.ui.theme.glasense

import androidx.compose.animation.core.tween
import com.nevoit.cresto.ui.components.myFadeIn
import com.nevoit.cresto.ui.components.myFadeOut
import com.nevoit.cresto.ui.components.myScaleIn
import com.nevoit.cresto.ui.components.myScaleOut

val defaultEnterTransition = myFadeIn(animationSpec = tween(delayMillis = 100)) + myScaleIn(
    animationSpec = tween(delayMillis = 100),
    initialScale = 0.9f
)

val defaultExitTransition = myFadeOut(animationSpec = tween(durationMillis = 100)) + myScaleOut(
    animationSpec = tween(delayMillis = 100),
    targetScale = 0.9f
)