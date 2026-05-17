package com.nevoit.cresto.feature.screenextract

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ScreenshotDataUrlTest {

    @Test
    fun toPngDataUrl_encodesBytesAsPngDataUrl() {
        val result = byteArrayOf(1, 2, 3).toPngDataUrl()

        assertEquals("data:image/png;base64,AQID", result)
    }

    @Test
    fun toPngDataUrl_rejectsEmptyBytes() {
        assertThrows(IllegalArgumentException::class.java) {
            byteArrayOf().toPngDataUrl()
        }
    }

    @Test
    fun toPngDataUrl_rejectsOversizedBytes() {
        assertThrows(IllegalArgumentException::class.java) {
            byteArrayOf(1, 2).toPngDataUrl(maxSizeBytes = 1)
        }
    }
}
