package com.nevoit.cresto.toolkit.gradientmapping

import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.core.graphics.createBitmap

private const val GRADIENT_MAP_AGSL = """
    uniform shader content;
    uniform shader gradient;
    uniform float gradientWidth;
    uniform float offset;
    
    const half3 kLuma = half3(0.2126, 0.7152, 0.0722);
    
    half4 main(float2 coord) {
        half4 inputColor = content.eval(coord);
        float luma = dot(inputColor.rgb, kLuma);

        float shiftedLuma = fract(luma + offset); 
        
        float mapX = shiftedLuma * gradientWidth;
        
        half4 mappedColor = gradient.eval(float2(mapX, 0.5));
        return half4(mappedColor.rgb, inputColor.a);
    }
"""

@Composable
fun GradientMappedImage(
    modifier: Modifier = Modifier,
    id: Int,
    gradientColors: List<Color>,
    offset: Float = 0f,
    contentScale: ContentScale = ContentScale.Crop
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return
    }

    val shader = remember { RuntimeShader(GRADIENT_MAP_AGSL) }

    val gradientShader = remember(gradientColors) {
        createLinearGradientShader(gradientColors)
    }

    Image(
        painter = painterResource(id = id),
        contentDescription = "Gradient Mapped Image",
        contentScale = contentScale,
        modifier = modifier.graphicsLayer {
            shader.setFloatUniform("gradientWidth", 256f)
            shader.setFloatUniform("offset", offset)
            shader.setInputShader("gradient", gradientShader)

            renderEffect = android.graphics.RenderEffect
                .createRuntimeShaderEffect(shader, "content")
                .asComposeRenderEffect()

            clip = true
        }
    )
}

private fun createLinearGradientShader(colors: List<Color>, width: Int = 256): Shader {
    val intColors = colors.map { it.toArgb() }.toIntArray()

    val bitmap = createBitmap(width, 1)
    val canvas = Canvas(bitmap)

    val paint = Paint()
    paint.shader = LinearGradient(
        0f, 0f, width.toFloat(), 0f,
        intColors,
        null,
        Shader.TileMode.CLAMP
    )

    canvas.drawRect(0f, 0f, width.toFloat(), 1f, paint)

    return BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
}