package com.nevoit.cresto.ui.components.packed

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.nevoit.glasense.core.interaction.rememberFlingBehavior

@Composable
fun PageContent(
    state: LazyListState,
    modifier: Modifier = Modifier,
    tabPadding: Boolean = true,
    topPadding: () -> Dp = { 0.dp },
    horizontalPadding: Boolean = true,
    bottomPadding: Dp? = null,
    overscroll: Boolean = true,
    content: LazyListScope.() -> Unit
) {
    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val flingBehavior = rememberFlingBehavior()

    val paddingValues = remember(tabPadding, bottomPadding, navigationBarHeight) {
        object : PaddingValues {
            override fun calculateLeftPadding(layoutDirection: LayoutDirection) =
                if (horizontalPadding) 12.dp else 0.dp

            override fun calculateTopPadding() = topPadding()
            override fun calculateRightPadding(layoutDirection: LayoutDirection) =
                if (horizontalPadding) 12.dp else 0.dp

            override fun calculateBottomPadding() =
                if (tabPadding) 120.dp + navigationBarHeight else bottomPadding
                    ?: navigationBarHeight
        }
    }

    LazyColumn(
        state = state,
        modifier = modifier
            .fillMaxSize(),
        contentPadding = paddingValues,
        flingBehavior = flingBehavior,
        overscrollEffect = if (overscroll) rememberOverscrollEffect() else null
    ) {
        content()
    }
}