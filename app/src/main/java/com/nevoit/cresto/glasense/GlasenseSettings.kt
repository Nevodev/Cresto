package com.nevoit.cresto.glasense

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

@Immutable
data class GlasenseSettings(
    val liquidGlass: Boolean,
    val liteMode: Boolean
)

val LocalGlasenseSettings = staticCompositionLocalOf {
    GlasenseSettings(
        liquidGlass = false,
        liteMode = false
    )
}