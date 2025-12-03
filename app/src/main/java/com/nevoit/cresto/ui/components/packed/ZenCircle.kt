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

// AGSL: 智能混合版 (暗色叠加，亮色遮盖)
@Language("AGSL")
val ZEN_CIRCLES_HYBRID_SHADER = """
    uniform float2 iResolution;
    uniform float iTime;
    layout(color) uniform half4 iBackColor;
    layout(color) uniform half4 iColorA;
    layout(color) uniform half4 iColorB;
    
    uniform float scale;
    uniform float thickness;
    uniform float breathAmp;
    uniform float layerDelay;
    uniform float blur;
    uniform float layerGap;
    uniform float intensity; // 强度控制

    // --- Noise Functions ---
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
        uv /= scale;
        
        float t_noise = iTime * 0.15;
        float w = 0.628318;
        float layers = 5.0;
        
        // 1. 计算背景亮度 (Luminance)
        // 接近 0.0 = 黑底，接近 1.0 = 白底
        float bgLum = dot(iBackColor.rgb, vec3(0.299, 0.587, 0.114));
        
        // 初始颜色
        vec3 finalColor = iBackColor.rgb;
        
        for (float i = 0.0; i < 5.0; i += 1.0) {
            float2 noiseUV = uv * 1.2 + float2(i * 7.0, t_noise * 0.4);
            
            float distortion = fbm(noiseUV);
            float wave = (distortion - 0.5) * 2.0; 
            
            float len = length(uv);
            float baseRadius = 0.3 + i * layerGap; 
            float phase = i * layerDelay;
            float breath = sin(iTime * w - phase) * breathAmp;
            
            float distToRing = abs(len - (baseRadius + breath) + wave * 0.12);
            
            float ringAlpha = smoothstep(thickness + blur, thickness, distToRing);
            // 稍作锐化，保证颜色扎实
            ringAlpha = clamp(pow(ringAlpha, 0.8), 0.0, 1.0);
            
            vec3 layerColor = mix(iColorA.rgb, iColorB.rgb, i / (layers - 1.0));
            
            // --- 【核心逻辑：智能混合算法】 ---
            
            // 混合系数：
            // 如果 bgLum 是 1 (白底)，bgMixFactor = (1.0 - ringAlpha)。这是标准的 Alpha 混合公式：Result = BG*(1-A) + Color*A。
            // 如果 bgLum 是 0 (黑底)，bgMixFactor = 1.0。这是加法混合公式：Result = BG + Color*A。
            // 这样在黑底上，颜色会越叠越亮，不会变灰。
            float bgMixFactor = mix(1.0, 1.0 - ringAlpha, bgLum);
            
            finalColor = finalColor * bgMixFactor + layerColor * ringAlpha * intensity;
        }
        
        // Vignette (暗角)
        // 边缘渐变回背景色 (无论黑白，边缘都应该融合进背景)
        float dist = length(uv);
        float vignetteStrength = smoothstep(0.6, 1.4, dist); 
        finalColor = mix(finalColor, iBackColor.rgb, vignetteStrength);

        return half4(finalColor, 1.0);
    }
"""

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun ZenCirclesBreathing(
    modifier: Modifier = Modifier,
    // 参数配置
    backgroundColor: Color = Color.Black, // 试着改成 Color.White 对比效果
    colorA: Color = Color(0xFF00E6FF),    // 青色
    colorB: Color = Color(0xFF9980FF),    // 紫色
    scale: Float = 2.00f,
    thickness: Float = 0.001f,
    breathAmp: Float = 0.068f,
    layerDelay: Float = 0.42f,
    blur: Float = 0.11f,
    layerGap: Float = 0.007f,
    intensity: Float = 1.0f // 颜色强度，黑底如果不满意可以调大到 1.5
) {
    val shader = remember { RuntimeShader(ZEN_CIRCLES_HYBRID_SHADER) }
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
        shader.setColorUniform("iColorA", colorA.toArgb())
        shader.setColorUniform("iColorB", colorB.toArgb())

        shader.setFloatUniform("scale", scale)
        shader.setFloatUniform("thickness", thickness)
        shader.setFloatUniform("breathAmp", breathAmp)
        shader.setFloatUniform("layerDelay", layerDelay)
        shader.setFloatUniform("blur", blur)
        shader.setFloatUniform("layerGap", layerGap)
        shader.setFloatUniform("intensity", intensity)

        drawRect(brush = ShaderBrush(shader))
    }
}