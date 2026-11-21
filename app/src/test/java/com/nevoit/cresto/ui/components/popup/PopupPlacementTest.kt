package com.nevoit.cresto.ui.components.popup

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Test

class PopupPlacementTest {
    private val panel = Size(280f, 160f)
    private val safeViewport = Size(400f, 700f)

    @Test
    fun upwardMenuAlignsItsBottomWithTheCaller() {
        assertEquals(Offset(80f, 472f), place(Rect(328f, 600f, 360f, 632f)))
    }

    @Test
    fun upwardMenuNearTheTopStaysInsideTheSafeViewport() {
        assertEquals(Offset(20f, 0f), place(Rect(20f, 10f, 52f, 42f)))
    }

    @Test
    fun callerOutsideTheBottomAndRightIsClampedToTheSafeViewport() {
        assertEquals(Offset(120f, 540f), place(Rect(390f, 690f, 422f, 722f)))
    }

    @Test
    fun downwardMenuRetainsItsTopAlignment() {
        assertEquals(Offset(80f, 500f), place(Rect(328f, 500f, 360f, 532f), expandUpwards = false))
    }

    @Test
    fun rtlStillAttachesToThePhysicalSideNearestTheCaller() {
        assertEquals(Offset(80f, 472f), place(Rect(328f, 600f, 360f, 632f), direction = LayoutDirection.Rtl))
    }

    private fun place(
        caller: Rect,
        expandUpwards: Boolean = true,
        direction: LayoutDirection = LayoutDirection.Ltr,
    ) = popupPlacement(
        callerBounds = caller,
        panelSize = panel,
        viewport = safeViewport,
        margin = 0f,
        offset = Offset.Zero,
        mode = PopupMode.Menu,
        anchoredToCaller = true,
        expandUpwards = expandUpwards,
        layoutDirection = direction,
    )
}
