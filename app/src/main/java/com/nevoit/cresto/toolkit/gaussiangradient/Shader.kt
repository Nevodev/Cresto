package com.nevoit.cresto.toolkit.gaussiangradient

const val GAUSSIAN_COLOR_INTERPOLATION_SHADER = """
    uniform vec2 iResolution;
    uniform half4 startColor;
    uniform half4 endColor;
    uniform float center;
    uniform float sigma;

    float gaussian(float x, float a, float b, float c) {
        return a * exp(-(x - b) * (x - b) / (2.0 * c * c));
    }
    float smoothstep_quadratic(float edge0, float edge1, float x) {
        float t = (x - edge0) / (edge1 - edge0);
        t = clamp(t, 0.0, 1.0);

        float result_if_true = 2.0 * t * t;
        float result_if_false = -2.0 * t * t + 4.0 * t - 1.0;

        float factor = step(0.5, t);

        return mix(result_if_true, result_if_false, factor);
    }
    float hash(vec2 p) {
        return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453123);
    }
    half4 main(vec2 fragCoord) {
        vec2 uv = fragCoord.xy / iResolution.xy;
        float t = smoothstep_quadratic(center, center + sigma, uv.y);
        
        half3 mixed_linear = mix(startColor.rgb, endColor.rgb, half(t));
        
        half mixed_alpha = mix(startColor.a, endColor.a, half(t));
        
        half3 final_rgb = mixed_linear;
        
        float dither_strength = 1.0 / 255.0;
        float dither = (hash(fragCoord.xy) - 0.5) * dither_strength;
        
        // final_rgb += dither;
        mixed_alpha += half(dither) * 4.0;
        
        final_rgb *= mixed_alpha; 
        
        return clamp(half4(half3(final_rgb), mixed_alpha), 0.0, 1.0);
    }
"""