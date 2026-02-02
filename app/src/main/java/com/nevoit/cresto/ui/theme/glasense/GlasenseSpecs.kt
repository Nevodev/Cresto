package com.nevoit.cresto.ui.theme.glasense

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle

@Immutable
data class GlasenseSpecs(
    val cardCorner: Dp,
    val cardShape: Shape,
    val buttonCorner: Dp,
    val buttonShape: Shape
)

val GlasenseSpecsStandard = GlasenseSpecs(
    cardCorner = 12.dp,
    cardShape = ContinuousRoundedRectangle(12.dp),
    buttonCorner = 12.dp,
    buttonShape = ContinuousRoundedRectangle(12.dp)
)

val GlasenseSpecsVariant = GlasenseSpecs(
    cardCorner = 16.dp,
    cardShape = ContinuousRoundedRectangle(16.dp),
    buttonCorner = 1000000.dp,
    buttonShape = ContinuousCapsule
)

val LocalGlasenseSpecs = staticCompositionLocalOf { GlasenseSpecsStandard }