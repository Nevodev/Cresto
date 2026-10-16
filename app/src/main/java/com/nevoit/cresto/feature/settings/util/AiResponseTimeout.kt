package com.nevoit.cresto.feature.settings.util

/**
 * Rules for the user configurable AI response timeout (the HTTP read timeout).
 *
 * Deliberately dependency free so the clamping rules can be unit tested on a plain JVM without
 * initializing MMKV.
 */
object AiResponseTimeout {

    /** Inclusive lower bound, in seconds. */
    const val MIN_SECONDS = 10

    /** Inclusive upper bound, in seconds. */
    const val MAX_SECONDS = 300

    /** Amount added or removed per stepper press, in seconds. */
    const val STEP_SECONDS = 5

    /** Value used on a fresh install; matches the previous hard coded read timeout. */
    const val DEFAULT_SECONDS = 30

    /** Clamps [raw] into [MIN_SECONDS]..[MAX_SECONDS]. */
    fun clamp(raw: Int): Int = raw.coerceIn(MIN_SECONDS, MAX_SECONDS)

    /** Quick presets offered next to the stepper; every entry is inside the valid range. */
    val PRESET_SECONDS: List<Int> = listOf(15, 30, 120)
}
