package com.nevoit.cresto.ui.modifier

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.nevoit.glasense.theme.Springs
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import android.graphics.RenderEffect as AndroidRenderEffect

private const val PRESS_INDENT_SHADER = """
uniform shader composable;
uniform float2 resolution;
uniform float2 touch;
uniform float radius;
uniform float progress;
uniform float maxDepth;
uniform float chromaticStrength;

const int SAMPLES = 5;

half3 getSpectrumWeight(float t) {
    half r = max(0.0, 1.0 - abs(t - 0.0) * 2.0);
    half g = max(0.0, 1.0 - abs(t - 0.5) * 2.0);
    half b = max(0.0, 1.0 - abs(t - 1.0) * 2.0);
    return half3(r, g, b);
}

half4 main(float2 fragCoord) {
    float2 delta = fragCoord - touch;
    float dist = length(delta);
    float mask = smoothstep(radius, 0.0, dist) * progress;

    float depth = mask * abs(mask) * maxDepth;
    float maxChannelOffset = depth * chromaticStrength;

    half4 accumulatedColor = half4(0.0);
    half3 totalWeightVec = half3(0.0);
    float totalAlphaWeight = 0.0;

    for (int i = 0; i < SAMPLES; i++) {
        float t = float(i) / float(SAMPLES - 1);

        float currentOffset = mix(maxChannelOffset, -maxChannelOffset, t);

        float2 sampleCoord = touch + delta * (1.0 + depth + currentOffset);

        half4 sampled = composable.eval(sampleCoord);

        half3 weight = getSpectrumWeight(t);

        accumulatedColor.rgb += sampled.rgb * weight;
        accumulatedColor.a += sampled.a;
        
        totalWeightVec += weight;
        totalAlphaWeight += 1.0;
    }

    accumulatedColor.rgb /= totalWeightVec;
    accumulatedColor.a /= totalAlphaWeight;

    return accumulatedColor;
}
"""

fun Modifier.pressIndentShaderEffect(
    radiusDp: Float = 300f,
    maxDepth: Float = 0.3f,
    chromaticStrength: Float = 0.5f
): Modifier {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return this
    return pressIndentShaderEffectApi33(
        radiusDp = radiusDp,
        maxDepth = maxDepth,
        chromaticStrength = chromaticStrength
    )
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun Modifier.pressIndentShaderEffectApi33(
    radiusDp: Float,
    maxDepth: Float,
    chromaticStrength: Float
): Modifier = composed {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val shader = remember { RuntimeShader(PRESS_INDENT_SHADER) }
    val progress = remember { Animatable(0f) }
    var pressAnimationJob by remember { mutableStateOf<Job?>(null) }
    var releaseAnimationJob by remember { mutableStateOf<Job?>(null) }

    var size by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    var touch by remember { mutableStateOf(Offset.Unspecified) }

    val radiusPx = with(density) { radiusDp.dp.toPx() }

    val runtimeEffect =
        remember(size, touch, radiusPx, maxDepth, chromaticStrength, progress.value) {
            if (
                size.width == 0 ||
                size.height == 0 ||
                touch == Offset.Unspecified ||
                abs(progress.value) <= 0.001f
            ) {
                null
            } else {
                shader.setFloatUniform("resolution", size.width.toFloat(), size.height.toFloat())
                shader.setFloatUniform("touch", touch.x, touch.y)
                shader.setFloatUniform("radius", radiusPx)
                shader.setFloatUniform("progress", progress.value)
                shader.setFloatUniform("maxDepth", maxDepth)
                shader.setFloatUniform("chromaticStrength", chromaticStrength.coerceIn(0f, 0.5f))
                AndroidRenderEffect
                    .createRuntimeShaderEffect(shader, "composable")
                    .asComposeRenderEffect()
            }
        }

    DisposableEffect(Unit) {
        onDispose {
            pressAnimationJob?.cancel()
            releaseAnimationJob?.cancel()
        }
    }

    Modifier
        .onSizeChanged { size = it }
        .pointerInput(Unit) {
            while (true) {
                awaitPointerEventScope {
                    var pressed = false
                    while (!pressed) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        val down = event.changes.firstOrNull { it.changedToDownIgnoreConsumed() }
                        if (down != null) {
                            touch = down.position
                            releaseAnimationJob?.cancel()
                            pressAnimationJob?.cancel()
                            pressAnimationJob = scope.launch {
                                progress.snapTo(0f)
                                progress.animateTo(
                                    targetValue = 1f,
                                    animationSpec = Springs.smooth(durationMillis = 220)
                                )
                            }
                            pressed = true
                        }
                    }

                    while (pressed) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        event.changes.firstOrNull { it.pressed }?.let { touch = it.position }
                        pressed = event.changes.any { it.pressed }
                    }
                }

                pressAnimationJob?.cancel()
                releaseAnimationJob?.cancel()
                releaseAnimationJob = scope.launch {
                    progress.animateTo(
                        targetValue = 0f,
                        animationSpec = Springs.smooth(durationMillis = 400)
                    )
                }
            }
        }
        .graphicsLayer {
            renderEffect = runtimeEffect
        }
}