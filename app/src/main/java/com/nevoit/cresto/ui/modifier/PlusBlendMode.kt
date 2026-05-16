package com.nevoit.cresto.ui.modifier

import android.graphics.Paint
import android.graphics.RuntimeXfermode
import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas

fun Modifier.plusBlendMode(darker: Boolean = false): Modifier =
    if (!darker) {
        this.graphicsLayer(
            blendMode = BlendMode.Plus
        )
    } else {
        if (PlusDarkerPaint != null) {
            this.drawWithContent {
                val canvas = drawContext.canvas.nativeCanvas
                val saveCount = canvas.saveLayer(0f, 0f, size.width, size.height, PlusDarkerPaint)
                drawContent()
                canvas.restoreToCount(saveCount)
            }
        } else {
            this
        }
    }

private val PlusDarkerPaint =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
        Paint().apply {
            xfermode = RuntimeXfermode(
                """
half4 main(half4 src, half4 dst) {
    half3 Co = max(half3(0.0), src.rgb + dst.rgb - src.a);
    half Ao = saturate(src.a + dst.a);
    return half4(Co, 1.0) * Ao;
}"""
            )
        }
    } else {
        null
    }
