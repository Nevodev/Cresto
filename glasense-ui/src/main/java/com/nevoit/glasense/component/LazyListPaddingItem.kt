package com.nevoit.glasense.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.util.fastCoerceAtLeast

fun LazyListScope.paddingItem(state: LazyListState) {
    item(contentType = LazyListPaddingItem) {
        Box(
            Modifier.layout { _, constraints ->
                val layoutInfo = state.layoutInfo
                val viewportBottom = layoutInfo.viewportEndOffset - layoutInfo.afterContentPadding
                val lastContentItem =
                    layoutInfo.visibleItemsInfo.lastOrNull { it.contentType !== LazyListPaddingItem }
                val contentBottom = lastContentItem?.let { it.offset + it.size } ?: 0
                val height = (viewportBottom - contentBottom + 1).fastCoerceAtLeast(0)
                layout(constraints.maxWidth, height) {}
            }
        )
    }
}

private data object LazyListPaddingItem
