package com.nevoit.cresto.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nevoit.cresto.R
import com.nevoit.cresto.data.todo.TodoViewModel
import com.nevoit.cresto.ui.components.glasense.GlasenseDynamicSmallTitle
import com.nevoit.cresto.ui.components.glasense.GlasenseLoadingIndicator
import com.nevoit.cresto.ui.components.glasense.GlasensePageHeader
import com.nevoit.cresto.ui.components.packed.CardWithTitle
import com.nevoit.cresto.ui.theme.glasense.CalculatedColor
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeApi::class)
@Composable
fun MindFlowScreen(viewModel: TodoViewModel) {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val density = LocalDensity.current
    val thresholdPx = if (statusBarHeight > 0.dp) {
        with(density) {
            (statusBarHeight + 24.dp).toPx()
        }
    } else 0f

    val hazeState = rememberHazeState()

    val surfaceColor = CalculatedColor.hierarchicalBackgroundColor

    val lazyListState = rememberLazyListState()

    val isSmallTitleVisible by remember(thresholdPx) { derivedStateOf { ((lazyListState.firstVisibleItemIndex == 0) && (lazyListState.firstVisibleItemScrollOffset > thresholdPx)) || lazyListState.firstVisibleItemIndex > 0 } }

    val stats by viewModel.statistics.collectAsStateWithLifecycle()
    val dailyStats by viewModel.dailyStats.collectAsStateWithLifecycle()
    val todayStat = dailyStats.find { it.date == LocalDate.now() }
    val completedCount = todayStat?.count ?: 0
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                clip = true
            }
            .background(surfaceColor)
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .hazeSource(hazeState, 0f)
                .fillMaxSize()
                .padding(0.dp)
                .background(surfaceColor),
            contentPadding = PaddingValues(
                start = 12.dp,
                top = 0.dp,
                end = 12.dp,
                bottom = 136.dp
            )
        ) {
            item {
                GlasensePageHeader(
                    title = stringResource(R.string.mind_flow),
                    statusBarHeight = statusBarHeight
                )
            }
            item {
                Row(modifier = Modifier.height(160.dp)) {
                    CardWithTitle(
                        title = stringResource(R.string.today_stat),
                        icon = painterResource(R.drawable.ic_mini_checkmark_seal),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "$completedCount",
                                fontSize = 48.sp,
                                lineHeight = 48.sp,
                                letterSpacing = (-2).sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = stringResource(R.string.completed),
                                color = MaterialTheme.colorScheme.onBackground.copy(.5f)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    CardWithTitle(
                        title = stringResource(R.string.statistics),
                        icon = painterResource(R.drawable.ic_mini_analytics),
                        modifier = Modifier.weight(1f)
                    ) { GlasenseLoadingIndicator(modifier = Modifier.fillMaxSize(), size = 24.dp) }
                }
            }
            item {

            }
        }
        GlasenseDynamicSmallTitle(
            modifier = Modifier.align(Alignment.TopCenter),
            title = stringResource(R.string.mind_flow),
            statusBarHeight = statusBarHeight,
            isVisible = isSmallTitleVisible,
            hazeState = hazeState,
            surfaceColor = surfaceColor
        ) {
        }
    }

}
