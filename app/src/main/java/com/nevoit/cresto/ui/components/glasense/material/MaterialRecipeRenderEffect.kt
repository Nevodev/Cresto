package com.nevoit.cresto.ui.components.glasense.material

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asComposeRenderEffect

@Composable
fun rememberMaterialRenderEffect(recipe: MaterialRecipe): androidx.compose.ui.graphics.RenderEffect {
    return remember(recipe) {
        recipe.toRenderEffect()
    }
}

fun MaterialRecipe.toRenderEffect(): androidx.compose.ui.graphics.RenderEffect {
    val shader = RuntimeShader(AGSL_CODE)

    shader.setFloatUniform(
        "curvePoints",
        luminanceMapCurve.p0,
        luminanceMapCurve.p1,
        luminanceMapCurve.p2,
        luminanceMapCurve.p3
    )
    shader.setFloatUniform("intensity", luminanceMapIntensity)
    shader.setFloatUniform("saturation", saturation)
    shader.setFloatUniform("brightness", extraBrightness)

    return RenderEffect.createRuntimeShaderEffect(
        shader,
        "image"
    ).asComposeRenderEffect()
}