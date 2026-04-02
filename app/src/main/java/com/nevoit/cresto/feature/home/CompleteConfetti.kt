package com.nevoit.cresto.feature.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalWindowInfo
import com.nevoit.glasense.theme.Amber400
import com.nevoit.glasense.theme.Pink400
import com.nevoit.glasense.theme.Purple400
import com.nevoit.glasense.theme.Rose400
import io.github.vinceglb.confettikit.compose.ConfettiKit
import io.github.vinceglb.confettikit.core.Party
import io.github.vinceglb.confettikit.core.Position
import io.github.vinceglb.confettikit.core.emitter.Emitter
import kotlin.time.Duration.Companion.seconds

@Composable
fun CompleteConfettiOverlay(
    visible: Boolean,
    position: Offset
) {
    if (visible) {
        ConfettiKit(
            modifier = Modifier.fillMaxSize(),
            parties = listOf(
                Party(
                    speed = 0f,
                    maxSpeed = 60f,
                    damping = 0.9f,
                    spread = 270,
                    colors = listOf(
                        Amber400.toArgb(),
                        Purple400.toArgb(),
                        Rose400.toArgb(),
                        Pink400.toArgb()
                    ),
                    timeToLive = 400L,
                    emitter = Emitter(duration = 0.2.seconds).perSecond(400),
                    position = Position.Relative(
                        x = (position.x / LocalWindowInfo.current.containerSize.width).toDouble(),
                        y = (position.y / LocalWindowInfo.current.containerSize.height).toDouble()
                    )
                )
            )
        )
    }
}