package com.nevoit.cresto.ui.components.packed

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PageContent(
    state: LazyListState,
    modifier: Modifier = Modifier,
    tabPadding: Boolean = true,
    bottomPadding: Dp? = null,
    content: LazyListScope.() -> Unit
) {
    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LazyColumn(
        state = state,
        modifier = modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(
            start = 12.dp,
            top = 0.dp,
            end = 12.dp,
            bottom = if (tabPadding) 120.dp + navigationBarHeight else bottomPadding
                ?: navigationBarHeight
        )
    ) {
        content()
    }
}