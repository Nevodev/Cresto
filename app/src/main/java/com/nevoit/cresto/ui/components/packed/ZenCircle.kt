package com.nevoit.cresto.ui.components.packed

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.toArgb
import org.intellij.lang.annotations.Language

// 1. 定义 AGSL Shader 代码 (加法混合版)
@Language("AGSL")
val ZEN_CIRCLES_ADDITIVE_SHADER = """
    uniform float2 iResolution;
    uniform float iTime;
    layout(color) uniform half4 iBackColor; // 背景颜色
    
    // Configurable Parameters
    uniform float scale;
    uniform float thickness;
    uniform float breathAmp;
    uniform float layerDelay;
    uniform float blur;
    uniform float layerGap;

    // --- Noise Functions (保持不变) ---
    float hash(float2 p) {
        return fract(sin(dot(p, float2(12.9898, 78.233))) * 43758.5453);
    }

    float noise(float2 p) {
        float2 i = floor(p);
        float2 f = fract(p);
        f = f * f * (3.0 - 2.0 * f);
        return mix(mix(hash(i + float2(0.0, 0.0)), hash(i + float2(1.0, 0.0)), f.x),
                   mix(hash(i + float2(0.0, 1.0)), hash(i + float2(1.0, 1.0)), f.x), f.y);
    }

    float fbm(float2 p) {
        float v = 0.0;
        float a = 0.5;
        mat2 rot = mat2(cos(0.5), sin(0.5), -sin(0.5), cos(0.5));
        for (int i = 0; i < 3; i++) {
            v += a * noise(p);
            p = rot * p * 2.0;
            a *= 0.5;
        }
        return v;
    }

    half4 main(float2 fragCoord) {
        float2 uv = (fragCoord * 2.0 - iResolution.xy) / iResolution.y;
        
        // Apply Scale
        uv /= scale;
        
        float t_noise = iTime * 0.15;
        float w = 0.628318;
        float layers = 5.0;
        
        // 【核心修改 1】：初始化为 0，准备累加光线
        vec3 totalLight = vec3(0.0);
        
        for (float i = 0.0; i < 5.0; i += 1.0) {
            float2 noiseUV = uv * 1.2 + float2(i * 7.0, t_noise * 0.4);
            
            float distortion = fbm(noiseUV);
            float wave = (distortion - 0.5) * 2.0; 
            
            float len = length(uv);
            float baseRadius = 0.3 + i * layerGap; 
            
            float phase = i * layerDelay;
            float breath = sin(iTime * w - phase) * breathAmp;
            
            float distToRing = abs(len - (baseRadius + breath) + wave * 0.12);
            
            // 形状计算保持不变
            float ringAlpha = smoothstep(thickness + blur, thickness, distToRing);
            
            // 颜色定义 (你可以根据喜好调整这里的发光色)
            vec3 colA = vec3(0.0, 0.9, 1.0); // 青色
            vec3 colB = vec3(0.6, 0.4, 1.0); // 紫色
            
            vec3 layerColor = mix(colA, colB, i / (layers - 1.0));
            
            // 【核心修改 2】：加法混合 (Additive Mixing)
            // 将当前层的颜色 * 透明度 累加到总光照中
            // 这里的 1.5 是强度系数，可以让光更亮一点
            totalLight += layerColor * ringAlpha * 1.5;
        }
        
        // Vignette (暗角)
        // 这是一个遮罩，让光线在边缘衰减，而不是让背景变色
        float vig = 1.0 - length(uv * 0.5);
        vig = smoothstep(0.0, 1.5, vig);
        
        // 应用暗角到光线上
        totalLight *= vig;

        // 【核心修改 3】：最终合成
        // 背景色 + 累加的光线 = 发光效果
        vec3 finalColor = iBackColor.rgb + totalLight;

        return half4(finalColor, 1.0);
    }
"""

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun ZenCirclesBreathing(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Black, // 默认改为黑色，加法混合在深色背景下效果最好
    scale: Float = 2.00f,
    thickness: Float = 0.001f,
    breathAmp: Float = 0.068f,
    layerDelay: Float = 0.42f,
    blur: Float = 0.11f,
    layerGap: Float = 0.007f
) {
    val shader = remember { RuntimeShader(ZEN_CIRCLES_ADDITIVE_SHADER) }
    var time by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        val startTime = System.nanoTime()
        while (true) {
            withInfiniteAnimationFrameMillis {
                time = (System.nanoTime() - startTime) / 1_000_000_000f
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        shader.setFloatUniform("iResolution", size.width, size.height)
        shader.setFloatUniform("iTime", time)
        shader.setColorUniform("iBackColor", backgroundColor.toArgb())

        shader.setFloatUniform("scale", scale)
        shader.setFloatUniform("thickness", thickness)
        shader.setFloatUniform("breathAmp", breathAmp)
        shader.setFloatUniform("layerDelay", layerDelay)
        shader.setFloatUniform("blur", blur)
        shader.setFloatUniform("layerGap", layerGap)

        drawRect(brush = ShaderBrush(shader))
    }
}