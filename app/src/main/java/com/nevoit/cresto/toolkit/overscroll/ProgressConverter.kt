package com.nevoit.cresto.toolkit.overscroll

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sign

object ProgressConverter {
    fun convert(progress: Float, fraction: Float): Float {
        return (1f - exp(-abs(progress * fraction))) * progress.sign
    }
}