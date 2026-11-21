package com.nevoit.cresto.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import com.nevoit.cresto.data.todo.TodoItem
import com.nevoit.cresto.data.todo.TodoItemWithSubTodos
import com.nevoit.cresto.feature.home.TodoListItemRow
import com.nevoit.cresto.theme.GlasenseTheme
import com.nevoit.cresto.ui.components.glasense.rememberSwipeableListState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TodoCardNavigationTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun returningLongPressUsesTheCardsGestureAndAShortTapStillReversesTheExit() {
        var opens = 0
        var longPresses = 0
        var navigator: Navigator<Int>? = null
        val scopes = mutableMapOf<Int, NavigationScope<Int>>()
        compose.setContent {
            GlasenseTheme {
                val stack = remember { Navigator(0) }
                NavigationHost(
                    navigator = stack,
                    animationSpec = androidx.compose.animation.core.tween(1_500),
                    pageShape = com.kyant.shapes.RoundedRectangle(0.dp),
                ) {
                    val page = destination
                    val navigation = this
                    SideEffect {
                        navigator = stack
                        scopes[page] = navigation
                    }
                    if (page == 0) {
                        val swipeState = rememberSwipeableListState()
                        val interaction = remember { MutableInteractionSource() }
                        LazyColumn(Modifier.fillMaxSize()) {
                            item {
                                TodoListItemRow(
                                    modifier = Modifier.testTag("live-card"),
                                    item = TodoItemWithSubTodos(TodoItem(id = 7, title = "Card"), emptyList()),
                                    showDate = false,
                                    isDueTodayMarkerEnabled = false,
                                    isOverdueMarkerEnabled = false,
                                    isSelected = false,
                                    isSelectionModeActive = false,
                                    overlayInteractionSource = interaction,
                                    swipeListState = swipeState,
                                    onEnterSelection = { longPresses++ },
                                    onToggleSelection = {},
                                    onOpenDetail = {
                                        opens++
                                        navigation.navigate(1, it)
                                    },
                                    onCheckedChange = {},
                                    onDelete = {},
                                )
                            }
                        }
                    } else Box(Modifier.fillMaxSize().background(Color.Blue))
                }
            }
        }
        val card = compose.onNodeWithTag("live-card")
        card.performTouchInput { click(center) }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { scopes.getValue(1).back() }
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        card.performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(700)
        card.performTouchInput { up() }
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(1, longPresses)
            assertEquals(1, opens)
            assertEquals(0, requireNotNull(navigator).current.destination)
        }

        card.performTouchInput { click(center) }
        compose.waitForIdle()
        val entryId = requireNotNull(navigator).current.id
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { scopes.getValue(1).back() }
        compose.mainClock.advanceTimeBy(100)
        compose.waitForIdle()
        card.performTouchInput { click(center) }
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(1, longPresses)
            assertEquals(3, opens)
            assertEquals(entryId, requireNotNull(navigator).current.id)
            assertEquals(1, requireNotNull(navigator).current.destination)
        }
    }

    @Test
    fun revealedSpaceShowsThePageAndDoesNotNavigateButDeleteStillWorks() {
        var opens = 0
        var deleted = false
        var gapTapInset = 0f
        compose.setContent {
            GlasenseTheme {
                val swipeState = rememberSwipeableListState()
                val interaction = remember { MutableInteractionSource() }
                val density = LocalDensity.current
                SideEffect { gapTapInset = with(density) { 3.dp.toPx() } }
                Box(Modifier.fillMaxSize().background(Color.Magenta).testTag("page")) {
                    LazyColumn {
                        item {
                            TodoListItemRow(
                                modifier = Modifier.testTag("row"),
                                item = TodoItemWithSubTodos(TodoItem(id = 7, title = "Card"), emptyList()),
                                showDate = false,
                                isDueTodayMarkerEnabled = false,
                                isOverdueMarkerEnabled = false,
                                isSelected = false,
                                isSelectionModeActive = false,
                                overlayInteractionSource = interaction,
                                swipeListState = swipeState,
                                onEnterSelection = {},
                                onToggleSelection = {},
                                onOpenDetail = { opens++ },
                                onCheckedChange = {},
                                onDelete = { deleted = true },
                            )
                        }
                    }
                }
            }
        }
        val row = compose.onNodeWithTag("row")
        row.performTouchInput {
            swipe(Offset(width * 0.8f, height / 2f), Offset(width * 0.2f, height / 2f), 500)
        }
        compose.waitForIdle()
        val page = compose.onNodeWithTag("page")
        val rowBounds = row.fetchSemanticsNode().boundsInRoot
        val pageBounds = page.fetchSemanticsNode().boundsInRoot
        val pixels = page.captureToImage().toPixelMap()
        val gapX = (rowBounds.right - pageBounds.left - gapTapInset).toInt()
        val gapY = (rowBounds.center.y - pageBounds.top).toInt()
        assertEquals(Color.Magenta.toArgb(), pixels[gapX, gapY].toArgb())
        row.performTouchInput { click(Offset(width - gapTapInset, height / 2f)) }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(0, opens) }
        row.performTouchInput {
            click(Offset(width - gapTapInset * 12f, height / 2f))
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(0, opens)
            assertTrue(deleted)
        }
    }
}
