package com.nevoit.glasense.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

object GlasenseTheme {
    val colors: GlasenseColors
        @Composable get() = LocalGlasenseColors.current

    val specs: GlasenseSpecs
        @Composable get() = LocalGlasenseSpecs.current

    val type: GlasenseType
        @Composable get() = LocalGlasenseType.current

    val darkTheme: Boolean
        @Composable get() = LocalDarkTheme.current
}

@Composable
fun GlasenseTheme(
    darkTheme: Boolean,
    colors: GlasenseColors = if (darkTheme) GlasenseDarkPalette else GlasenseLightPalette,
    specs: GlasenseSpecs = GlasenseSpecsStandard,
    type: GlasenseType = GlasenseTypeStandard,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalGlasenseColors provides colors,
        LocalGlasenseSpecs provides specs,
        LocalGlasenseType provides type,
        LocalDarkTheme provides darkTheme
    ) {
        content()
    }
}