package com.nevoit.cresto.feature.settings.util

import org.junit.Assert.assertEquals
import org.junit.Test

class AiResponseTimeoutTest {

    @Test
    fun `default sits inside the allowed range`() {
        assertEquals(
            AiResponseTimeout.DEFAULT_SECONDS,
            AiResponseTimeout.clamp(AiResponseTimeout.DEFAULT_SECONDS)
        )
        assert(AiResponseTimeout.DEFAULT_SECONDS in AiResponseTimeout.MIN_SECONDS..AiResponseTimeout.MAX_SECONDS)
    }

    @Test
    fun `values below the minimum are raised to the minimum`() {
        assertEquals(AiResponseTimeout.MIN_SECONDS, AiResponseTimeout.clamp(0))
        assertEquals(AiResponseTimeout.MIN_SECONDS, AiResponseTimeout.clamp(-5))
        assertEquals(AiResponseTimeout.MIN_SECONDS, AiResponseTimeout.clamp(9))
    }

    @Test
    fun `values above the maximum are lowered to the maximum`() {
        assertEquals(AiResponseTimeout.MAX_SECONDS, AiResponseTimeout.clamp(301))
        assertEquals(AiResponseTimeout.MAX_SECONDS, AiResponseTimeout.clamp(9999))
    }

    @Test
    fun `values inside the range are returned unchanged`() {
        assertEquals(10, AiResponseTimeout.clamp(10))
        assertEquals(45, AiResponseTimeout.clamp(45))
        assertEquals(300, AiResponseTimeout.clamp(300))
    }

    @Test
    fun `stepping from the minimum reaches the maximum without going past it`() {
        var value = AiResponseTimeout.MIN_SECONDS
        while (value < AiResponseTimeout.MAX_SECONDS) {
            value = AiResponseTimeout.clamp(value + AiResponseTimeout.STEP_SECONDS)
        }
        assertEquals(AiResponseTimeout.MAX_SECONDS, value)
    }

    @Test
    fun `every preset is a reachable value inside the range`() {
        AiResponseTimeout.PRESET_SECONDS.forEach { preset ->
            assertEquals(preset, AiResponseTimeout.clamp(preset))
            assertEquals(
                0,
                (preset - AiResponseTimeout.MIN_SECONDS) % AiResponseTimeout.STEP_SECONDS
            )
        }
    }
}
