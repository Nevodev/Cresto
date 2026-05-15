package com.nevoit.cresto.ui.components.glasense.material

internal const val AGSL_CODE = """
uniform shader image;
uniform vec4 curvePoints;
uniform float intensity;
uniform float saturation;
uniform float brightness;

vec3 rgb2hsl(vec3 color) {
    float fmin = min(min(color.r, color.g), color.b);
    float fmax = max(max(color.r, color.g), color.b);
    float delta = fmax - fmin;

    float h = 0.0;
    float s = 0.0;
    float l = (fmax + fmin) / 2.0;

    if (delta > 0.004) {
        s = l < 0.5 ? delta / (fmax + fmin) : delta / (2.0 - fmax - fmin);

        float deltaR = fmax - color.r;
        float deltaG = fmax - color.g;

        if (deltaR < 0.001) {
            h = (color.g - color.b) / delta + (color.g < color.b ? 6.0 : 0.0);
        } else if (deltaG < 0.001) {
            h = (color.b - color.r) / delta + 2.0;
        } else {
            h = (color.r - color.g) / delta + 4.0;
        }
        h /= 6.0;
    }

    return vec3(h, s, l);
}


vec3 hsl2rgb(vec3 hsl) {
    vec3 rgb = clamp(abs(mod(hsl.x * 6.0 + vec3(0.0, 4.0, 2.0), 6.0) - 3.0) - 1.0, 0.0, 1.0);
    return hsl.z + hsl.y * (rgb - 0.5) * (1.0 - abs(2.0 * hsl.z - 1.0));
}

float bezierBrightness(float t, vec4 p) {
    float u = 1.0 - t; float tt = t * t; float uu = u * u;
    return (uu * u * p.x) + (3.0 * uu * t * p.y) + (3.0 * u * tt * p.z) + (tt * t * p.w);
}

half4 main(float2 fragCoord) {
    half4 color = image.eval(fragCoord);

    vec3 hsl = rgb2hsl(color.rgb);

    hsl.z = clamp(bezierBrightness(hsl.z, curvePoints), 0.0, 1.0);
    hsl.y = clamp(hsl.y * saturation, 0.0, 1.0);
    hsl.z = clamp(hsl.z * (1 + brightness), 0.0, 1.0);

    half3 mappedColor = half3(hsl2rgb(hsl));

    color.rgb = mix(color.rgb, mappedColor, intensity);
    
    return color;
}
"""