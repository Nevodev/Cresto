package com.nevoit.cresto.ui.components.glasense.extend

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

private const val OVERSCROLL_SPACER_KEY = "Overscroll_Spacer_Key"

fun LazyListScope.overscrollSpacer(state: LazyListState) {
    item(key = OVERSCROLL_SPACER_KEY) {
        val density = LocalDensity.current

        var lastHeightPx by rememberSaveable { mutableFloatStateOf(0f) }

        val spacerHeight by remember(state) {
            derivedStateOf {
                val layoutInfo = state.layoutInfo
                val viewportHeight = layoutInfo.viewportSize.height

                if (viewportHeight <= 0) {
                    return@derivedStateOf with(density) { lastHeightPx.toDp() }
                }

                val visibleItems = layoutInfo.visibleItemsInfo
                    .filter { it.key != OVERSCROLL_SPACER_KEY }

                val contentHeight = visibleItems.sumOf { it.size }

                if (state.firstVisibleItemIndex > 0) {
                    if (contentHeight >= viewportHeight) {
                        return@derivedStateOf 0.dp
                    }
                    return@derivedStateOf with(density) { lastHeightPx.toDp() }
                }

                if (contentHeight < viewportHeight) {
                    val neededHeightPx = viewportHeight - contentHeight
                    with(density) { neededHeightPx.toDp() }
                } else {
                    0.dp
                }
            }
        }

        LaunchedEffect(spacerHeight) {
            with(density) {
                lastHeightPx = spacerHeight.toPx()
            }
        }

        Spacer(modifier = Modifier.height(spacerHeight))
    }
}