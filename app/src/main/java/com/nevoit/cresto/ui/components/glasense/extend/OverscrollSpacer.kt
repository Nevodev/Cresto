package com.nevoit.cresto.ui.components.glasense.extend

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
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

                var contentHeight = 0
                val visibleItems = layoutInfo.visibleItemsInfo
                val itemsCount = visibleItems.size

                for (i in 0 until itemsCount) {
                    val item = visibleItems[i]
                    if (item.key != OVERSCROLL_SPACER_KEY) {
                        contentHeight += item.size
                    }
                }

                if (contentHeight < viewportHeight) {
                    val neededHeightPx = (viewportHeight - contentHeight).toFloat()
                    lastHeightPx = neededHeightPx
                    with(density) { neededHeightPx.toDp() }
                } else {
                    0.dp
                }
            }
        }

        Spacer(modifier = Modifier.height(spacerHeight))
    }
}