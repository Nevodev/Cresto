package com.nevoit.cresto.ui.components.glasense.material

import org.intellij.lang.annotations.Language

@Language("AGSL")
internal const val AGSL_CODE = """
uniform shader image;
uniform float p0, p1, p2, p3;
uniform float mapIntensity;
uniform float saturation;
uniform float brightness;

const vec3 LUMA_WEIGHTS = vec3(0.2126, 0.7152, 0.0722);

float bezierMap(float x) {
    float invX = 1.0 - x;
    float invX2 = invX * invX;
    float invX3 = invX2 * invX;
    float x2 = x * x;
    float x3 = x2 * x;
return invX3 * p0 + 3.0 * invX2 * x * p1 + 3.0 * invX * x2 * p2 + x3 * p3;
}

vec4 main(vec2 fragCoord) {
    vec3 rgb = image.eval(fragCoord).rgb;
    float luma1 = dot(rgb, LUMA_WEIGHTS);
    float mappedLuma = bezierMap(luma1);
    vec3 colorMapped = clamp(mix(rgb, vec3(mappedLuma), mapIntensity),0.0,1.0);
    float luma2 = dot(colorMapped, LUMA_WEIGHTS);
    vec3 colorSaturated = clamp(mix(vec3(luma2), colorMapped, saturation),0.0,1.0);
    vec3 finalColor = colorSaturated + vec3(brightness);
return vec4(clamp(finalColor, 0.0, 1.0), 1.0);
}
"""