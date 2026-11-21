package com.nevoit.cresto.ui.components.popup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kyant.shapes.RoundedRectangle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class PopupLayerAnimationTest {
    @get:Rule
    val compose = createComposeRule()

    private val panelColor = mutableStateOf(Color.Red)
    private val callerColor = mutableStateOf(Color.Green)
    private var panelDraws = 0
    private var callerDraws = 0
    private var closed = false
    private lateinit var dismiss: () -> Unit

    @Test
    fun upwardMenuNearTheBottomRespectsAnAlreadyConsumedSafeArea() {
        assertMenuFitsSafeArea(Alignment.BottomEnd)
    }

    @Test
    fun upwardMenuNearTheTopRespectsAnAlreadyConsumedSafeArea() {
        assertMenuFitsSafeArea(Alignment.TopEnd)
    }

    private fun assertMenuFitsSafeArea(callerAlignment: Alignment) {
        var safeBounds = Rect.Zero
        var marginPx = 0f
        compose.setContent {
            val caller = rememberPopupCaller(RoundedRectangle(12.dp))
            val density = LocalDensity.current
            SideEffect { marginPx = with(density) { 12.dp.toPx() } }
            Box(
                Modifier.fillMaxSize()
                    .windowInsetsPadding(
                        WindowInsets(left = 24, top = 80, right = 24, bottom = 90)
                            .union(WindowInsets.safeDrawing),
                    )
                    .onGloballyPositioned { safeBounds = it.boundsInRoot() },
            ) {
                Box(
                    Modifier.align(callerAlignment).size(32.dp)
                        .popupCaller(caller).background(Color.Green),
                )
                PopupLayer(
                    onDismissed = {},
                    transition = PopupTransition.FromAnchor(
                        caller = caller,
                        mode = PopupMode.Menu,
                        anchoredToCaller = true,
                        expandUpwards = true,
                    ),
                ) {
                    Box(Modifier.size(280.dp, 160.dp).testTag("safe-panel").background(Color.Red))
                }
            }
        }
        compose.waitForIdle()
        val panel = compose.onNodeWithTag("safe-panel").fetchSemanticsNode().boundsInRoot
        assertTrue(panel.left >= safeBounds.left + marginPx - 1f)
        assertTrue(panel.top >= safeBounds.top + marginPx - 1f)
        assertTrue(panel.bottom <= safeBounds.bottom - marginPx + 1f)
        assertEquals(safeBounds.right - marginPx, panel.right, 1f)
        if (callerAlignment == Alignment.TopEnd) {
            assertEquals(safeBounds.top + marginPx, panel.top, 1f)
        } else {
            assertEquals(safeBounds.bottom - marginPx, panel.bottom, 1f)
        }
    }

    @Test
    fun revealFramesReuseStaticContentWhileContentChangesStillDraw() {
        setContent()
        advanceFrames(5)
        val before = compose.runOnIdle { panelDraws }
        val callerBefore = compose.runOnIdle { callerDraws }
        assertTrue("Panel was never recorded", before > 0)
        advanceFrames(12)
        compose.runOnIdle {
            assertEquals("Reveal frames re-recorded static panel content", before, panelDraws)
            assertEquals("Reveal frames re-recorded static caller content", callerBefore, callerDraws)
            panelColor.value = Color.Blue
        }
        advanceFrames(2)
        compose.runOnIdle {
            assertTrue("Live panel content stopped updating", panelDraws > before)
        }
        advanceFrames(90)
        val center = compose.onNodeWithTag("panel").fetchSemanticsNode().boundsInRoot.center
        val pixels = compose.onRoot().captureToImage().toPixelMap()
        assertEquals(Color.Blue, pixels[center.x.roundToInt(), center.y.roundToInt()])
    }

    @Test
    fun returningCallerUsesItsUpdatedColorThroughTheCachedReplay() {
        setContent()
        advanceFrames(90)
        val center = compose.onNodeWithTag("caller").fetchSemanticsNode().boundsInRoot.center
        compose.runOnIdle { dismiss() }
        advanceFrames(4)
        val callerBefore = compose.runOnIdle { callerDraws }
        advanceFrames(3)
        compose.runOnIdle {
            assertEquals("Return frames re-recorded static caller content", callerBefore, callerDraws)
        }
        compose.onNodeWithTag("panel").assertDoesNotExist()
        compose.runOnIdle { callerColor.value = Color.Blue }
        advanceFrames(10)
        compose.runOnIdle { assertFalse("Return animation already finished", closed) }
        compose.runOnIdle { assertTrue("Live caller content stopped updating", callerDraws > callerBefore) }
        val pixels = compose.onRoot().captureToImage().toPixelMap()
        val returning = pixels[center.x.roundToInt(), center.y.roundToInt()]
        assertTrue("Cached replay retained the old caller color", returning.blue > 0.98f)
        assertTrue("Old caller color leaked into the return", returning.green < 0.02f)
        advanceFrames(90)
        compose.runOnIdle { assertTrue(closed) }
    }

    private fun setContent() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            val caller = rememberPopupCaller(RoundedRectangle(12.dp))
            SideEffect { assertEquals(CompositingStrategy.Offscreen, caller.layer.compositingStrategy) }
            Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(40.dp).testTag("caller").popupCaller(caller)
                        .drawBehind {
                            callerDraws++
                            drawRect(callerColor.value)
                        },
                )
                PopupLayer(
                    onDismissed = { closed = true },
                    scrimAlpha = 0f,
                    transition = PopupTransition.FromAnchor(
                        caller = caller,
                        // Keep both centres together so the return can be sampled before it ends.
                        offset = DpOffset(0.dp, (-30).dp),
                        handoffDistance = 1000f,
                    ),
                ) { onDismiss ->
                    SideEffect { dismiss = onDismiss }
                    Box(
                        Modifier.size(160.dp, 100.dp).testTag("panel").drawBehind {
                            panelDraws++
                            drawRect(panelColor.value)
                        },
                    )
                }
            }
        }
    }

    private fun advanceFrames(count: Int) {
        repeat(count) {
            compose.mainClock.advanceTimeByFrame()
            compose.waitForIdle()
        }
    }
}
