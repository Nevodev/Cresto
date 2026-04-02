package com.nevoit.glasense.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle

@Immutable
data class GlasenseSpecs(
    val cardCorner: Dp,
    val cardShape: Shape,
    val buttonCorner: Dp,
    val buttonShape: Shape,
    val textFieldCorner: Dp,
    val textFieldShape: Shape,
    val dialogCorner: Dp,
    val dialogShape: Shape
)

val GlasenseSpecsStandard = GlasenseSpecs(
    cardCorner = 12.dp,
    cardShape = RoundedRectangle(12.dp),
    buttonCorner = 12.dp,
    buttonShape = RoundedRectangle(12.dp),
    textFieldCorner = 12.dp,
    textFieldShape = RoundedRectangle(12.dp),
    dialogCorner = 24.dp,
    dialogShape = RoundedRectangle(24.dp)
)

val GlasenseSpecsVariant = GlasenseSpecs(
    cardCorner = 16.dp,
    cardShape = RoundedRectangle(16.dp),
    buttonCorner = 1000000.dp,
    buttonShape = Capsule(),
    textFieldCorner = 16.dp,
    textFieldShape = RoundedRectangle(16.dp),
    dialogCorner = 24.dp,
    dialogShape = RoundedRectangle(24.dp)
)

val LocalGlasenseSpecs = staticCompositionLocalOf { GlasenseSpecsStandard }