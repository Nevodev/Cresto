package com.nevoit.cresto.ui.modifier

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import org.intellij.lang.annotations.Language

@Language("AGSL")
private val RIPPLE_SHADER = """
    uniform float2 iResolution;
    uniform float2 positions[10];
    uniform float progresses[10];
    uniform float maxRadius;

    half4 main(float2 fragCoord) {
        float3 finalColor = float3(0.0);
        
        for (int i = 0; i < 10; i++) {
            float progress = progresses[i];
            
            if (progress > 0.0 && progress < 1.0) {
                float2 pos = positions[i];
                
                float radius = progress * maxRadius;
                float dist = distance(fragCoord, pos);
                
                // Blurry ring effect
                float blur = maxRadius * 0.4;
                float thickness = maxRadius * 0.05;
                
                // Use a soft ring centered at 'radius'
                float ring = 1.0 - smoothstep(0.0, blur, abs(dist - radius) - thickness);
                
                float alpha = 0.1;
                if (progress < 0.2){
                    // Fade in with ease-out
                    float t = progress / 0.2;
                    float easeOut = 1.0 - pow(1.0 - t, 2.0);
                    alpha = 0.1 * easeOut;
                } else {
                    // Fade out with ease-in
                    float t = (progress - 0.2) / 0.8;
                    float easeIn = t * t;
                    alpha = 0.1 * (1.0 - easeIn);
                }
                float final = ring * alpha;
                finalColor += float3(final);
            }
        }
        
        return half4(finalColor, 1.0);
    }
""".trimIndent()

private class RippleInstance(
    val position: Offset,
    val animatable: Animatable<Float, *>
)

fun Modifier.shaderRipple(
    durationMillis: Int = 800,
    maxRadius: Float = 2000f
): Modifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    composed {
        val ripples = remember { mutableStateListOf<RippleInstance>() }
        val scope = rememberCoroutineScope()
        val shader = remember { RuntimeShader(RIPPLE_SHADER) }

        this
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Press) {
                            val change = event.changes.first()
                            val newRipple = RippleInstance(
                                position = change.position,
                                animatable = Animatable(0f)
                            )

                            if (ripples.size >= 10) {
                                ripples.removeAt(0)
                            }
                            ripples.add(newRipple)

                            scope.launch {
                                newRipple.animatable.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(durationMillis, easing = LinearEasing)
                                )
                                ripples.remove(newRipple)
                            }
                        }
                    }
                }
            }
            .drawWithCache {
                onDrawWithContent {
                    drawContent()

                    if (ripples.isNotEmpty()) {
                        shader.setFloatUniform("iResolution", size.width, size.height)
                        shader.setFloatUniform("maxRadius", maxRadius)

                        val positions = FloatArray(20)
                        val progresses = FloatArray(10)

                        for (i in 0 until 10) {
                            if (i < ripples.size) {
                                val ripple = ripples[i]
                                positions[i * 2] = ripple.position.x
                                positions[i * 2 + 1] = ripple.position.y
                                progresses[i] = ripple.animatable.value
                            } else {
                                positions[i * 2] = 0f
                                positions[i * 2 + 1] = 0f
                                progresses[i] = 0f
                            }
                        }

                        shader.setFloatUniform("positions", positions)
                        shader.setFloatUniform("progresses", progresses)

                        drawRect(
                            brush = ShaderBrush(shader),
                            blendMode = BlendMode.Plus
                        )
                    }
                }
            }
    }
} else {
    this
}
