package com.nevoit.cresto.ui.modifier

import android.graphics.RuntimeShader
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.IntSize
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
const float PI = 3.14159265359;

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

private const val CENTER_WAVE_SHADER = """
uniform shader composable;
uniform float2 resolution;
uniform float progress;
uniform float intensity;
uniform float chromaticStrength;
uniform float waveWidthMultiplier;
uniform float2 centerFraction;

const int SAMPLES = 5;
const float PI = 3.14159265359;

half3 getWaveSpectrumWeight(float spectrumPosition) {
    half redWeight = max(0.0, 1.0 - abs(spectrumPosition - 0.0) * 2.0);
    half greenWeight = max(0.0, 1.0 - abs(spectrumPosition - 0.5) * 2.0);
    half blueWeight = max(0.0, 1.0 - abs(spectrumPosition - 1.0) * 2.0);
    return half3(redWeight, greenWeight, blueWeight);
}

half4 main(float2 fragCoord) {
    half4 baseColor = composable.eval(fragCoord);
    float2 center = resolution * centerFraction;
    float2 delta = fragCoord - center;
    float distanceFromCenter = length(delta);
    float maxDistance = max(
        max(length(center), length(resolution - center)),
        max(length(float2(resolution.x - center.x, center.y)), length(float2(center.x, resolution.y - center.y)))
    );
    float waveFront = progress * maxDistance * 1.15;
    float waveWidth = max(18.0, maxDistance * 0.07) * waveWidthMultiplier;

    float distanceToFront = abs(distanceFromCenter - waveFront);
    float wavePosition = clamp(1.0 - distanceToFront / waveWidth, 0.0, 1.0);
    float waveMask = 0.5 - 0.5 * cos(wavePosition * PI);
    waveMask *= waveMask;
    float animationFadeIn = smoothstep(0.0, 0.08, progress);
    float animationFadeOut = 1.0 - smoothstep(0.78, 1.0, progress);
    float wave = waveMask * animationFadeIn * animationFadeOut;

    float2 direction = distanceFromCenter > 0.001 ? delta / distanceFromCenter : float2(0.0, 0.0);
    float intensityFadeIn = smoothstep(0.0, 0.1, progress);
    float currentIntensity = intensity * intensityFadeIn;
    float maxDisplacement = min(maxDistance * 0.035, 42.0) * currentIntensity;
    float maxChannelOffset = min(maxDistance * 0.018, 18.0) * currentIntensity * chromaticStrength;
    float displacement = wave * maxDisplacement;
    float chromaticOffset = abs(wave) * maxChannelOffset;

    half3 accumulatedColor = half3(0.0);
    half3 totalWeightVec = half3(0.0);

    for (int sampleIndex = 0; sampleIndex < SAMPLES; sampleIndex++) {
        float spectrumPosition = float(sampleIndex) / float(SAMPLES - 1);
        float channelOffset = mix(chromaticOffset, -chromaticOffset, spectrumPosition);
        float2 sampleCoord = clamp(
            fragCoord - direction * (displacement + channelOffset),
            float2(0.0),
            max(resolution - float2(1.0), float2(0.0))
        );

        half4 sampled = composable.eval(sampleCoord);
        half3 weight = getWaveSpectrumWeight(spectrumPosition);

        accumulatedColor += sampled.rgb * weight;
        totalWeightVec += weight;
    }

    half3 refractedColor = accumulatedColor / totalWeightVec;

    return half4(refractedColor, baseColor.a);
}
"""

fun Modifier.pressIndentShaderEffect(
    radiusDp: Float = 300f,
    maxDepth: Float = 0.3f,
    chromaticStrength: Float = 0.5f
): Modifier = composed {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val shader = remember { RuntimeShader(PRESS_INDENT_SHADER) }
    val progress = remember { Animatable(0f) }
    var pressAnimationJob by remember { mutableStateOf<Job?>(null) }
    var releaseAnimationJob by remember { mutableStateOf<Job?>(null) }

    var size by remember { mutableStateOf(IntSize.Zero) }
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

fun Modifier.centerWaveShaderEffect(
    durationMillis: Int = 900,
    intensity: Float = 0.7f,
    chromaticStrength: Float = 0.45f,
    waveWidthMultiplier: Float = 1.5f,
    centerX: Float = 0.5f,
    centerY: Float = 0.5f
): Modifier = composed {
    val shader = remember { RuntimeShader(CENTER_WAVE_SHADER) }
    val progress = remember { Animatable(0f) }
    var size by remember { mutableStateOf(IntSize.Zero) }

    val safeDurationMillis = durationMillis.coerceAtLeast(1)
    val safeIntensity = intensity.coerceAtLeast(0f)
    val safeChromaticStrength = chromaticStrength.coerceIn(0f, 1f)
    val safeWaveWidthMultiplier = waveWidthMultiplier.coerceAtLeast(0.1f)
    val safeCenterX = centerX.coerceIn(0f, 1f)
    val safeCenterY = centerY.coerceIn(0f, 1f)

    LaunchedEffect(safeDurationMillis) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = safeDurationMillis,
                easing = LinearEasing
            )
        )
    }

    Modifier
        .onSizeChanged { size = it }
        .graphicsLayer {
            val currentProgress = progress.value
            if (
                size.width == 0 ||
                size.height == 0 ||
                safeIntensity <= 0f ||
                currentProgress <= 0.001f ||
                currentProgress >= 0.999f
            ) {
                renderEffect = null
            } else {
                shader.setFloatUniform("resolution", size.width.toFloat(), size.height.toFloat())
                shader.setFloatUniform("progress", currentProgress)
                shader.setFloatUniform("intensity", safeIntensity)
                shader.setFloatUniform("chromaticStrength", safeChromaticStrength)
                shader.setFloatUniform("waveWidthMultiplier", safeWaveWidthMultiplier)
                shader.setFloatUniform("centerFraction", safeCenterX, safeCenterY)
                renderEffect = AndroidRenderEffect
                    .createRuntimeShaderEffect(shader, "composable")
                    .asComposeRenderEffect()
            }
        }
}