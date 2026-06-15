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
    half sa = src.a;
    half da = dst.a;
    half sad = sa * da;
    half3 rgb = src.rgb + dst.rgb - min(src.rgb * da + dst.rgb * sa, half3(sad));
    half a = sa + da - sad;
    return half4(rgb, a);
}"""
            )
        }
    } else {
        null
    }
