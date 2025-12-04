package com.nevoit.cresto.ui.theme.glasense

import androidx.compose.animation.core.EaseInSine
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.spring
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

val strongEnterTransition = myFadeIn(animationSpec = tween(delayMillis = 100)) + myScaleIn(
    animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f, visibilityThreshold = 0.0001f),
    initialScale = 0f
)

val strongExitTransition =
    myFadeOut(animationSpec = tween(durationMillis = 200, delayMillis = 100)) + myScaleOut(
        animationSpec = tween(durationMillis = 300, delayMillis = 0, easing = EaseInSine),
        targetScale = 0.8f
    )

val elegantEnterTransition = myFadeIn(animationSpec = tween(delayMillis = 100)) + myScaleIn(
    animationSpec = tween(durationMillis = 400, delayMillis = 0, easing = EaseOutCubic),
    initialScale = 0f
)