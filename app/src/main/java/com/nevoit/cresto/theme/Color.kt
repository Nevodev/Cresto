package com.nevoit.cresto.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.blend.Blend
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.hct.Hct
import com.materialkolor.scheme.DynamicScheme
import com.materialkolor.scheme.SchemeTonalSpot
import com.nevoit.cresto.feature.settings.util.SettingsManager
import com.nevoit.glasense.theme.values.Blue500
import com.nevoit.glasense.theme.values.Gray500
import com.nevoit.glasense.theme.values.Green500
import com.nevoit.glasense.theme.values.Orange500
import com.nevoit.glasense.theme.values.Purple500
import com.nevoit.glasense.theme.values.Red500
import com.nevoit.glasense.theme.values.Yellow500

@Composable
fun getFlagColor(flag: Int): Color {
    return when (flag) {
        0 -> Color.Transparent
        1 -> Red500
        2 -> Orange500
        3 -> Yellow500
        4 -> Green500
        5 -> Blue500
        6 -> Purple500
        7 -> Gray500
        else -> Gray500
    }
}

@Composable
fun harmonize(color: Color): Color {
    val isUseDynamicColor by SettingsManager.isUseDynamicColorState
    val primary = AppColors.primary

    return if (isUseDynamicColor) {
        color.harmonizeWith(primary)
    } else {
        color
    }
}


fun Color.adjustSaturationInOklab(factor: Float): Color {
    val oklabColor = this.convert(ColorSpaces.Oklab)

    val l = oklabColor.red
    val a = oklabColor.green
    val b = oklabColor.blue

    val newA = a * factor
    val newB = b * factor

    val newOklabColor = Color(
        red = l,
        green = newA,
        blue = newB,
        alpha = this.alpha,
        colorSpace = ColorSpaces.Oklab
    )

    return newOklabColor.convert(ColorSpaces.Srgb)
}

@Immutable
class ColorScheme(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val inversePrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val surfaceTint: Color,
    val inverseSurface: Color,
    val inverseOnSurface: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val outline: Color,
    val outlineVariant: Color,
    val scrim: Color,
    val surfaceBright: Color,
    val surfaceDim: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainerLowest: Color,
    val primaryFixed: Color,
    val primaryFixedDim: Color,
    val onPrimaryFixed: Color,
    val onPrimaryFixedVariant: Color,
    val secondaryFixed: Color,
    val secondaryFixedDim: Color,
    val onSecondaryFixed: Color,
    val onSecondaryFixedVariant: Color,
    val tertiaryFixed: Color,
    val tertiaryFixedDim: Color,
    val onTertiaryFixed: Color,
    val onTertiaryFixedVariant: Color,
) {
    fun copy(
        primary: Color = this.primary,
        onPrimary: Color = this.onPrimary,
        primaryContainer: Color = this.primaryContainer,
        onPrimaryContainer: Color = this.onPrimaryContainer,
        inversePrimary: Color = this.inversePrimary,
        secondary: Color = this.secondary,
        onSecondary: Color = this.onSecondary,
        secondaryContainer: Color = this.secondaryContainer,
        onSecondaryContainer: Color = this.onSecondaryContainer,
        tertiary: Color = this.tertiary,
        onTertiary: Color = this.onTertiary,
        tertiaryContainer: Color = this.tertiaryContainer,
        onTertiaryContainer: Color = this.onTertiaryContainer,
        background: Color = this.background,
        onBackground: Color = this.onBackground,
        surface: Color = this.surface,
        onSurface: Color = this.onSurface,
        surfaceVariant: Color = this.surfaceVariant,
        onSurfaceVariant: Color = this.onSurfaceVariant,
        surfaceTint: Color = this.surfaceTint,
        inverseSurface: Color = this.inverseSurface,
        inverseOnSurface: Color = this.inverseOnSurface,
        error: Color = this.error,
        onError: Color = this.onError,
        errorContainer: Color = this.errorContainer,
        onErrorContainer: Color = this.onErrorContainer,
        outline: Color = this.outline,
        outlineVariant: Color = this.outlineVariant,
        scrim: Color = this.scrim,
        surfaceBright: Color = this.surfaceBright,
        surfaceDim: Color = this.surfaceDim,
        surfaceContainer: Color = this.surfaceContainer,
        surfaceContainerHigh: Color = this.surfaceContainerHigh,
        surfaceContainerHighest: Color = this.surfaceContainerHighest,
        surfaceContainerLow: Color = this.surfaceContainerLow,
        surfaceContainerLowest: Color = this.surfaceContainerLowest,
        primaryFixed: Color = this.primaryFixed,
        primaryFixedDim: Color = this.primaryFixedDim,
        onPrimaryFixed: Color = this.onPrimaryFixed,
        onPrimaryFixedVariant: Color = this.onPrimaryFixedVariant,
        secondaryFixed: Color = this.secondaryFixed,
        secondaryFixedDim: Color = this.secondaryFixedDim,
        onSecondaryFixed: Color = this.onSecondaryFixed,
        onSecondaryFixedVariant: Color = this.onSecondaryFixedVariant,
        tertiaryFixed: Color = this.tertiaryFixed,
        tertiaryFixedDim: Color = this.tertiaryFixedDim,
        onTertiaryFixed: Color = this.onTertiaryFixed,
        onTertiaryFixedVariant: Color = this.onTertiaryFixedVariant,
    ): ColorScheme =
        ColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            inversePrimary = inversePrimary,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            surfaceTint = surfaceTint,
            inverseSurface = inverseSurface,
            inverseOnSurface = inverseOnSurface,
            error = error,
            onError = onError,
            errorContainer = errorContainer,
            onErrorContainer = onErrorContainer,
            outline = outline,
            outlineVariant = outlineVariant,
            scrim = scrim,
            surfaceBright = surfaceBright,
            surfaceDim = surfaceDim,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerHighest = surfaceContainerHighest,
            surfaceContainerLow = surfaceContainerLow,
            surfaceContainerLowest = surfaceContainerLowest,
            primaryFixed = primaryFixed,
            primaryFixedDim = primaryFixedDim,
            onPrimaryFixed = onPrimaryFixed,
            onPrimaryFixedVariant = onPrimaryFixedVariant,
            secondaryFixed = secondaryFixed,
            secondaryFixedDim = secondaryFixedDim,
            onSecondaryFixed = onSecondaryFixed,
            onSecondaryFixedVariant = onSecondaryFixedVariant,
            tertiaryFixed = tertiaryFixed,
            tertiaryFixedDim = tertiaryFixedDim,
            onTertiaryFixed = onTertiaryFixed,
            onTertiaryFixedVariant = onTertiaryFixedVariant,
        )
}

private fun argbColor(argb: Int): Color = Color(argb)

private fun Color.harmonizeWith(other: Color): Color =
    Color(Blend.harmonize(toArgb(), other.toArgb()))

private fun tonalSpotScheme(seedColor: Color, isDark: Boolean): DynamicScheme =
    SchemeTonalSpot(
        sourceColorHct = Hct.fromInt(seedColor.toArgb()),
        isDark = isDark,
        contrastLevel = 0.0,
        platform = DynamicScheme.Platform.PHONE,
        specVersion = ColorSpec.SpecVersion.SPEC_2021
    )

fun dynamicColorScheme(seedColor: Color, isDark: Boolean): ColorScheme {
    val scheme = tonalSpotScheme(seedColor, isDark)
    return ColorScheme(
        primary = argbColor(scheme.primary),
        onPrimary = argbColor(scheme.onPrimary),
        primaryContainer = argbColor(scheme.primaryContainer),
        onPrimaryContainer = argbColor(scheme.onPrimaryContainer),
        inversePrimary = argbColor(scheme.inversePrimary),
        secondary = argbColor(scheme.secondary),
        onSecondary = argbColor(scheme.onSecondary),
        secondaryContainer = argbColor(scheme.secondaryContainer),
        onSecondaryContainer = argbColor(scheme.onSecondaryContainer),
        tertiary = argbColor(scheme.tertiary),
        onTertiary = argbColor(scheme.onTertiary),
        tertiaryContainer = argbColor(scheme.tertiaryContainer),
        onTertiaryContainer = argbColor(scheme.onTertiaryContainer),
        background = argbColor(scheme.background),
        onBackground = argbColor(scheme.onBackground),
        surface = argbColor(scheme.surface),
        onSurface = argbColor(scheme.onSurface),
        surfaceVariant = argbColor(scheme.surfaceVariant),
        onSurfaceVariant = argbColor(scheme.onSurfaceVariant),
        surfaceTint = argbColor(scheme.surfaceTint),
        inverseSurface = argbColor(scheme.inverseSurface),
        inverseOnSurface = argbColor(scheme.inverseOnSurface),
        error = argbColor(scheme.error),
        onError = argbColor(scheme.onError),
        errorContainer = argbColor(scheme.errorContainer),
        onErrorContainer = argbColor(scheme.onErrorContainer),
        outline = argbColor(scheme.outline),
        outlineVariant = argbColor(scheme.outlineVariant),
        scrim = argbColor(scheme.scrim),
        surfaceBright = argbColor(scheme.surfaceBright),
        surfaceDim = argbColor(scheme.surfaceDim),
        surfaceContainer = argbColor(scheme.surfaceContainer),
        surfaceContainerHigh = argbColor(scheme.surfaceContainerHigh),
        surfaceContainerHighest = argbColor(scheme.surfaceContainerHighest),
        surfaceContainerLow = argbColor(scheme.surfaceContainerLow),
        surfaceContainerLowest = argbColor(scheme.surfaceContainerLowest),
        primaryFixed = argbColor(scheme.primaryFixed),
        primaryFixedDim = argbColor(scheme.primaryFixedDim),
        onPrimaryFixed = argbColor(scheme.onPrimaryFixed),
        onPrimaryFixedVariant = argbColor(scheme.onPrimaryFixedVariant),
        secondaryFixed = argbColor(scheme.secondaryFixed),
        secondaryFixedDim = argbColor(scheme.secondaryFixedDim),
        onSecondaryFixed = argbColor(scheme.onSecondaryFixed),
        onSecondaryFixedVariant = argbColor(scheme.onSecondaryFixedVariant),
        tertiaryFixed = argbColor(scheme.tertiaryFixed),
        tertiaryFixedDim = argbColor(scheme.tertiaryFixedDim),
        onTertiaryFixed = argbColor(scheme.onTertiaryFixed),
        onTertiaryFixedVariant = argbColor(scheme.onTertiaryFixedVariant),
    )
}