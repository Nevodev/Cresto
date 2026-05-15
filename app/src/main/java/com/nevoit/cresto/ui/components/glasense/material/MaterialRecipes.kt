package com.nevoit.cresto.ui.components.glasense.material

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.nevoit.glasense.theme.LocalGlasenseDarkTheme

@Immutable
data class Vec4(
    val p0: Float,
    val p1: Float,
    val p2: Float,
    val p3: Float
)

@Immutable
data class MaterialRecipe(
    val luminanceMapCurve: Vec4,
    val luminanceMapIntensity: Float,
    val saturation: Float,
    val extraBrightness: Float
) {
    operator fun invoke(extraBrightness: Float = 0f): MaterialRecipe {
        return this.copy(extraBrightness = this.extraBrightness + extraBrightness)
    }
}

private val UltraThinLightCurve = Vec4(0.45f, 0.55f, 0.65f, 0.68f)
private val UltraThinDarkCurve = Vec4(0.24f, 0.24f, 0.3f, 0.39f)

private val ThinLightCurve = Vec4(0.725f, 0.825f, 0.76f, 0.73f)
private val ThinDarkCurve = Vec4(0.2f, 0.21f, 0.1f, 0.15f)

private val RegularLightCurve = Vec4(0.9f, 0.83f, 0.925f, 0.815f)
private val RegularDarkCurve = Vec4(0.16f, 0.26f, 0.1f, 0.1f)

private val ThickLightCurve = Vec4(0.99f, 0.95f, 0.98f, 0.905f)
private val ThickDarkCurve = Vec4(0.14f, 0.16f, 0.1f, 0.03f)

private val ChromeLightCurve = Vec4(0.8f, 0.9f, 1.1f, 0.825f)
private val ChromeDarkCurve = Vec4(0.23f, 0.52f, 0.27f, 0.255f)

@Suppress("unused")
object MaterialRecipes {
    val UltraThinLight = MaterialRecipe(UltraThinLightCurve, 0.5f, 1.1f, 0.12f)
    val UltraThinDark = MaterialRecipe(UltraThinDarkCurve, 0.5f, 1.1f, 0f)

    val ThinLight = MaterialRecipe(ThinLightCurve, 0.6f, 1.35f, 0.12f)
    val ThinDark = MaterialRecipe(ThinDarkCurve, 0.6f, 1.35f, 0f)

    val RegularLight = MaterialRecipe(RegularLightCurve, 0.75f, 1.5f, 0.1f)
    val RegularDark = MaterialRecipe(RegularDarkCurve, 0.75f, 1.5f, 0f)

    val ThickLight = MaterialRecipe(ThickLightCurve, 0.88f, 1.5f, 0.045f)
    val ThickDark = MaterialRecipe(ThickDarkCurve, 0.88f, 1.5f, 0f)

    val ChromeLight = MaterialRecipe(ChromeLightCurve, 0.75f, 1.1f, 0.1f)
    val ChromeDark = MaterialRecipe(ChromeDarkCurve, 0.75f, 2.0f, -0.1f)

    val MediumLight = MaterialRecipe(ThickLightCurve, 0.5f, 1.5f, 0.1f)
    val MediumDark = MaterialRecipe(ThickDarkCurve, 0.5f, 1.5f, 0f)

    @Composable
    fun ultraThin(
        isDark: Boolean = LocalGlasenseDarkTheme.current,
        extraBrightness: Float = 0f
    ): MaterialRecipe =
        if (isDark) UltraThinDark(extraBrightness) else UltraThinLight(extraBrightness)

    @Composable
    fun thin(
        isDark: Boolean = LocalGlasenseDarkTheme.current,
        extraBrightness: Float = 0f
    ): MaterialRecipe =
        if (isDark) ThinDark(extraBrightness) else ThinLight(extraBrightness)

    @Composable
    fun regular(
        isDark: Boolean = LocalGlasenseDarkTheme.current,
        extraBrightness: Float = 0f
    ): MaterialRecipe =
        if (isDark) RegularDark(extraBrightness) else RegularLight(extraBrightness)

    @Composable
    fun medium(
        isDark: Boolean = LocalGlasenseDarkTheme.current,
        extraBrightness: Float = 0f
    ): MaterialRecipe =
        if (isDark) MediumDark(extraBrightness) else MediumLight(extraBrightness)

    @Composable
    fun thick(
        isDark: Boolean = LocalGlasenseDarkTheme.current,
        extraBrightness: Float = 0f
    ): MaterialRecipe =
        if (isDark) ThickDark(extraBrightness) else ThickLight(extraBrightness)

    @Composable
    fun chrome(
        isDark: Boolean = LocalGlasenseDarkTheme.current,
        extraBrightness: Float = 0f
    ): MaterialRecipe =
        if (isDark) ChromeDark(extraBrightness) else ChromeLight(extraBrightness)

    @Composable
    fun appBar(
        isDark: Boolean = LocalGlasenseDarkTheme.current
    ): MaterialRecipe =
        if (isDark) ChromeDark else ChromeLight(0.05f)

    @Composable
    fun menu(
        isDark: Boolean = LocalGlasenseDarkTheme.current
    ): MaterialRecipe =
        if (isDark) RegularDark else RegularLight(0.1f)
}
