package com.nevoit.glasense

import androidx.compose.ui.graphics.Color
import com.nevoit.glasense.theme.extractOverlay
import org.junit.Assert.assertEquals
import org.junit.Test

class ColorExtTest {

    private val delta = 0.01f

    @Test
    fun testExtractOverlay_IdenticalColors() {
        val target = Color(0.5f, 0.5f, 0.5f)
        val background = Color(0.5f, 0.5f, 0.5f)
        val overlay = target.extractOverlay(background)
        
        assertEquals(0f, overlay.alpha, delta)
    }

    @Test
    fun testExtractOverlay_BlackOnWhite() {
        val background = Color.White
        val target = Color(0.5f, 0.5f, 0.5f) // Gray
        val overlay = target.extractOverlay(background)

        // Target = Overlay * Alpha + Background * (1 - Alpha)
        // 0.5 = 0 * 0.5 + 1 * (1 - 0.5)
        assertEquals(0.5f, overlay.alpha, delta)
        assertEquals(0f, overlay.red, delta)
        assertEquals(0f, overlay.green, delta)
        assertEquals(0f, overlay.blue, delta)
    }

    @Test
    fun testExtractOverlay_WhiteOnBlack() {
        val background = Color.Black
        val target = Color(0.5f, 0.5f, 0.5f) // Gray
        val overlay = target.extractOverlay(background)

        // 0.5 = 1 * 0.5 + 0 * (1 - 0.5)
        assertEquals(0.5f, overlay.alpha, delta)
        assertEquals(1f, overlay.red, delta)
        assertEquals(1f, overlay.green, delta)
        assertEquals(1f, overlay.blue, delta)
    }

    @Test
    fun testExtractOverlay_RedOnGreen() {
        val background = Color.Green
        val target = Color.Red
        val overlay = target.extractOverlay(background)

        // Since they are fully different, alpha should be 1
        assertEquals(1f, overlay.alpha, delta)
        assertEquals(1f, overlay.red, delta)
        assertEquals(0f, overlay.green, delta)
        assertEquals(0f, overlay.blue, delta)
    }

    @Test
    fun testExtractOverlay_HalfTransparentRedOnWhite() {
        val background = Color.White
        // If we overlay Color.Red.copy(alpha = 0.5f) on White:
        // Target R = 1.0 * 0.5 + 1.0 * (1 - 0.5) = 1.0
        // Target G = 0.0 * 0.5 + 1.0 * (1 - 0.5) = 0.5
        // Target B = 0.0 * 0.5 + 1.0 * (1 - 0.5) = 0.5
        val target = Color(1f, 0.5f, 0.5f)
        
        val overlayExtracted = target.extractOverlay(background)
        
        assertEquals(0.5f, overlayExtracted.alpha, delta)
        assertEquals(1f, overlayExtracted.red, delta)
        assertEquals(0f, overlayExtracted.green, delta)
        assertEquals(0f, overlayExtracted.blue, delta)
    }
}
