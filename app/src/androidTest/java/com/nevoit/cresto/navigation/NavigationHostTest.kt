package com.nevoit.cresto.navigation

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.shapes.RoundedRectangle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NavigationHostTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun tabChangeImmediatelyReleasesAnExitAndItsCaller() {
        val scopes = mutableMapOf<Int, NavigationScope<Int>>()
        val composed = mutableSetOf<Int>()
        var caller: NavigationCaller? = null
        var childModel: PageModel? = null
        var childLifecycle: Lifecycle? = null
        var navigator: Navigator<Int>? = null
        compose.setContent {
            val stack = remember { Navigator(0) }
            SideEffect { navigator = stack }
            NavigationHost(navigator = stack, animationSpec = tween(1_500), pageShape = RoundedRectangle(0.dp)) {
                val page = destination
                SideEffect { scopes[page] = this }
                DisposableEffect(page) {
                    composed += page
                    onDispose { composed -= page }
                }
                if (page == 0) {
                    val card = rememberNavigationCaller(RoundedRectangle(12.dp), Color.White, "card")
                    SideEffect { caller = card }
                    Box(Modifier.size(100.dp).navigationCaller(card, clipContent = false).background(Color.White))
                } else {
                    val model: PageModel = viewModel()
                    val lifecycle = LocalLifecycleOwner.current.lifecycle
                    SideEffect {
                        childModel = model
                        childLifecycle = lifecycle
                    }
                    Box(Modifier.fillMaxSize())
                }
            }
        }
        compose.runOnIdle { scopes.getValue(0).navigate(1, requireNotNull(caller)) }
        compose.waitForIdle()
        val firstEntryId = requireNotNull(navigator).current.id
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { scopes.getValue(1).back() }
        compose.mainClock.advanceTimeBy(100)
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(setOf(0, 1), composed)
            assertTrue(requireNotNull(caller).visibility.owner != null)
            scopes.getValue(0).finishExitAnimations()
            assertEquals(null, requireNotNull(caller).visibility.owner)
            assertEquals(CompositingStrategy.Auto, requireNotNull(caller).layer.compositingStrategy)
            assertTrue(requireNotNull(childModel).cleared)
            assertEquals(Lifecycle.State.DESTROYED, requireNotNull(childLifecycle).currentState)
        }
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(setOf(0), composed)
            assertEquals(0, requireNotNull(navigator).current.destination)
            scopes.getValue(0).navigate(1, requireNotNull(caller))
        }
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        compose.runOnIdle {
            assertTrue(requireNotNull(navigator).current.id != firstEntryId)
            assertEquals(setOf(0, 1), composed)
        }
    }

    @Test
    fun continuousTransitionsCanBeDisabledAndEnabledWithoutRecreatingTheHost() {
        val enabled = mutableStateOf(false)
        val scopes = mutableMapOf<Int, NavigationScope<Int>>()
        val composed = mutableSetOf<Int>()
        var caller: NavigationCaller? = null
        compose.setContent {
            CompositionLocalProvider(LocalContinuousTransitionsEnabled provides enabled.value) {
                val navigator = remember { Navigator(0) }
                NavigationHost(navigator = navigator, animationSpec = tween(50), pageShape = RoundedRectangle(0.dp)) {
                    val page = destination
                    SideEffect { scopes[page] = this }
                    DisposableEffect(page) {
                        composed += page
                        onDispose { composed -= page }
                    }
                    if (page == 0) {
                        val card = rememberNavigationCaller(RoundedRectangle(12.dp), Color.White, "card")
                        SideEffect { caller = card }
                        Box(Modifier.size(100.dp).navigationCaller(card, clipContent = false).background(Color.White))
                    } else Box(Modifier.fillMaxSize())
                }
            }
        }
        compose.runOnIdle { scopes.getValue(0).navigate(1, requireNotNull(caller)) }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(setOf(1), composed)
            scopes.getValue(1).back()
        }
        compose.waitForIdle()
        compose.runOnIdle { enabled.value = true }
        compose.waitForIdle()
        compose.runOnIdle { scopes.getValue(0).navigate(1, requireNotNull(caller)) }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(setOf(0, 1), composed) }
    }

    @Test
    fun entranceWaitsForContentAndBackDoesNotWaitForAnUnfinishedQuery() {
        val ready = mutableStateOf(false)
        val scopes = mutableMapOf<Int, NavigationScope<Int>>()
        val transitions = mutableMapOf<Int, PageTransitionState>()
        compose.setContent {
            val navigator = remember { Navigator(0) }
            NavigationHost(navigator = navigator, animationSpec = tween(100), pageShape = RoundedRectangle(0.dp)) {
                val page = destination
                if (page == 1) ReportNavigationReady(ready.value)
                val transition = LocalPageTransitionState.current!!
                SideEffect {
                    scopes[page] = this
                    transitions[page] = transition
                }
                Box(Modifier.fillMaxSize())
            }
        }
        compose.runOnIdle { scopes.getValue(0).navigate(1) }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(0f, transitions.getValue(1).progress.value)
            ready.value = true
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(1f, transitions.getValue(1).progress.value)
            scopes.getValue(1).back()
        }
        compose.waitForIdle()
        compose.runOnIdle {
            ready.value = false
            scopes.getValue(0).navigate(1)
        }
        compose.waitForIdle()
        compose.runOnIdle { scopes.getValue(1).back() }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(1f, transitions.getValue(0).progress.value) }
    }

    @Test
    fun callerUsesAnOffscreenLayerOnlyDuringTransitionsAndItsPageStaysComposed() {
        val contentReady = mutableStateOf(false)
        var rootScope: NavigationScope<Int>? = null
        var caller: NavigationCaller? = null
        val composed = mutableSetOf<Int>()
        var rootLifecycle: Lifecycle? = null
        compose.setContent {
            val navigator = remember { Navigator(0) }
            NavigationHost(navigator = navigator, animationSpec = tween(50), pageShape = RoundedRectangle(0.dp)) {
                val page = destination
                DisposableEffect(page) {
                    composed += page
                    onDispose { composed -= page }
                }
                if (page == 0) {
                    val card = rememberNavigationCaller(RoundedRectangle(12.dp), Color.White, "card")
                    val lifecycle = LocalLifecycleOwner.current.lifecycle
                    SideEffect {
                        caller = card
                        rootScope = this
                        rootLifecycle = lifecycle
                    }
                    Box(
                        Modifier.size(100.dp)
                            .navigationCaller(card, clipContent = false)
                            .background(Color.White, RoundedRectangle(12.dp))
                    )
                } else {
                    ReportNavigationReady(contentReady.value)
                    Box(Modifier.fillMaxSize())
                }
            }
        }
        compose.waitForIdle()
        compose.runOnIdle {
            val card = requireNotNull(caller)
            assertEquals(IntSize.Zero, card.layer.size)
            rootScope!!.navigate(1, card)
        }
        compose.waitForIdle()
        compose.runOnIdle {
            val card = requireNotNull(caller)
            assertEquals(setOf(0, 1), composed)
            assertFalse(card.layer.size == IntSize.Zero)
            assertTrue(card.visibility.owner != null)
            assertEquals(CompositingStrategy.Offscreen, card.layer.compositingStrategy)
            assertFalse(card.recordingRequested)
            contentReady.value = true
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(setOf(0, 1), composed)
            assertEquals(Lifecycle.State.CREATED, rootLifecycle!!.currentState)
            assertEquals(CompositingStrategy.Auto, requireNotNull(caller).layer.compositingStrategy)
            rootScope!!.navigate(2)
        }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(setOf(0, 1), composed) }
    }

    @Test
    fun returnReusesTheOriginalLiveLayerWithUpdatedContent() {
        val contentReady = mutableStateOf(false)
        val rootReady = mutableStateOf(true)
        val color = mutableStateOf(Color.Red)
        val scopes = mutableMapOf<Int, NavigationScope<Int>>()
        val recordedColors = mutableListOf<Color>()
        var currentCaller: NavigationCaller? = null
        compose.setContent {
            val navigator = remember { Navigator(0) }
            NavigationHost(navigator = navigator, animationSpec = tween(50), pageShape = RoundedRectangle(0.dp)) {
                val page = destination
                SideEffect { scopes[page] = this }
                if (page == 0) {
                    ReportNavigationReady(rootReady.value)
                    val cardColor = color.value
                    val shape = RoundedRectangle(12.dp)
                    val card = rememberNavigationCaller(shape, cardColor, "card")
                    SideEffect { currentCaller = card }
                    Box(
                        Modifier.size(100.dp)
                            .navigationCaller(card, clipContent = false)
                            .background(cardColor, shape)
                            .drawWithContent {
                                if (card.recordingRequested) recordedColors += cardColor
                                drawContent()
                            }
                    )
                } else {
                    ReportNavigationReady(contentReady.value)
                    Box(Modifier.fillMaxSize())
                }
            }
        }
        compose.runOnIdle { scopes.getValue(0).navigate(1, requireNotNull(currentCaller)) }
        compose.waitForIdle()
        val entranceCaller = requireNotNull(currentCaller)
        val transition = requireNotNull(entranceCaller.visibility.owner)
        val entranceLayer = transition.layer
        val entranceCoordinates = entranceCaller.coordinates
        compose.runOnIdle {
            assertEquals(Color.Red, recordedColors.last())
            contentReady.value = true
        }
        compose.waitForIdle()
        compose.runOnIdle {
            color.value = Color.Blue
            rootReady.value = false
            scopes.getValue(1).back()
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertSame(entranceLayer, transition.layer)
            assertEquals(Color.Red, recordedColors.last())
            rootReady.value = true
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertSame(entranceCoordinates, requireNotNull(currentCaller).coordinates)
            assertEquals(Color.Blue, recordedColors.last())
            assertSame(entranceLayer, transition.layer)
            assertSame(entranceLayer, requireNotNull(currentCaller).layer)
            assertFalse(requireNotNull(currentCaller).recordingRequested)
        }
    }

    @Test
    fun coveredPageRestoresStateAndKeepsItsModelUntilPopped() {
        val scopes = mutableMapOf<Int, NavigationScope<Int>>()
        val models = mutableMapOf<Int, PageModel>()
        val lifecycles = mutableMapOf<Int, Lifecycle>()
        val values = mutableMapOf<Int, Int>()
        val setters = mutableMapOf<Int, (Int) -> Unit>()
        val composed = mutableSetOf<Int>()
        compose.setContent {
            val navigator = remember { Navigator(0) }
            NavigationHost(
                navigator = navigator,
                animationSpec = tween(50),
                pageShape = RoundedRectangle(0.dp),
            ) {
                val page = destination
                val model: PageModel = viewModel()
                val lifecycle = LocalLifecycleOwner.current.lifecycle
                var value by rememberSaveable { mutableIntStateOf(0) }
                SideEffect {
                    scopes[page] = this
                    models[page] = model
                    lifecycles[page] = lifecycle
                    values[page] = value
                    setters[page] = { value = it }
                }
                DisposableEffect(page) {
                    composed += page
                    onDispose { composed -= page }
                }
                Box(Modifier.fillMaxSize())
            }
        }
        compose.runOnIdle {
            setters.getValue(0)(17)
            scopes.getValue(0).navigate(1)
        }
        compose.waitForIdle()
        val rootModel = models.getValue(0)
        val childModel = models.getValue(1)
        compose.runOnIdle {
            assertEquals(setOf(1), composed)
            assertEquals(Lifecycle.State.CREATED, lifecycles.getValue(0).currentState)
            assertFalse(rootModel.cleared)
            scopes.getValue(1).back()
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(setOf(0), composed)
            assertEquals(17, values.getValue(0))
            assertSame(rootModel, models.getValue(0))
            assertTrue(childModel.cleared)
            assertEquals(Lifecycle.State.DESTROYED, lifecycles.getValue(1).currentState)
        }
    }

    class PageModel : ViewModel() {
        var cleared = false
        override fun onCleared() {
            cleared = true
        }
    }
}
