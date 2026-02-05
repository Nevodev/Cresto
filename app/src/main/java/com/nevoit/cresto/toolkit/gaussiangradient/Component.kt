package com.nevoit.cresto.toolkit.gaussiangradient

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush

/**
 * A Composable function that applies a smooth gradient mask to a Modifier.
 * This is achieved using a Gaussian color interpolation shader.
 *
 * @param startColor The starting color of the gradient.
 * @param endColor The ending color of the gradient.
 * @param center The center point of the Gaussian distribution.
 * @param sigma The standard deviation of the Gaussian distribution, controlling the smoothness of the gradient.
 * @param alpha The alpha transparency to apply to the drawn rect.
 * @return A Modifier with the gradient mask applied.
 */
@Composable
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun Modifier.smoothGradientMask(
    startColor: Color,
    endColor: Color,
    center: Float,
    sigma: Float,
    alpha: Float
): Modifier {
    val shader = remember { RuntimeShader(GAUSSIAN_COLOR_INTERPOLATION_SHADER) }
    val brush = remember { ShaderBrush(shader) }

    // Apply the drawing logic behind the content of the composable.
    return this.drawBehind {
        shader.setFloatUniform(
            "startColor",
            startColor.red,
            startColor.green,
            startColor.blue,
            startColor.alpha
        )
        shader.setFloatUniform(
            "endColor",
            endColor.red,
            endColor.green,
            endColor.blue,
            endColor.alpha
        )
        shader.setFloatUniform(
            "iResolution",
            this.size.width,
            this.size.height
        )

        shader.setFloatUniform("center", center)
        shader.setFloatUniform("sigma", sigma)

        // Draw a rectangle covering the entire drawing area with the shader brush.
        drawRect(brush = brush, alpha = alpha)
    }
}