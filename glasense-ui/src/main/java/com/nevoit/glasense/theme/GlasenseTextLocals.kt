package com.nevoit.glasense.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.TextStyle

val LocalGlasenseContentColor = compositionLocalOf { GlasenseLightPalette.content }

val LocalGlasenseTextStyle = compositionLocalOf { GlasenseTypeStandard.body }

@Composable
fun ProvideGlasenseTextStyle(
    value: TextStyle,
    content: @Composable () -> Unit
) {
    val mergedStyle = LocalGlasenseTextStyle.current.merge(value)
    CompositionLocalProvider(
        LocalGlasenseTextStyle provides mergedStyle,
        content = content
    )
}