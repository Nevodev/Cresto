package com.nevoit.cresto.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nevoit.cresto.R
import com.nevoit.cresto.ui.components.glasense.GlasenseDynamicSmallTitle
import com.nevoit.cresto.ui.components.glasense.GlasensePageHeader
import com.nevoit.cresto.ui.components.glasense.extend.overscrollSpacer
import com.nevoit.cresto.ui.components.packed.AboutEntryItem
import com.nevoit.cresto.ui.components.packed.ConfigContainer
import com.nevoit.cresto.ui.components.packed.ConfigEntryItem
import com.nevoit.cresto.ui.screens.settings.AIActivity
import com.nevoit.cresto.ui.screens.settings.AboutActivity
import com.nevoit.cresto.ui.screens.settings.AppearanceActivity
import com.nevoit.cresto.ui.screens.settings.DataStorageActivity
import com.nevoit.cresto.ui.theme.glasense.AppColors
import com.nevoit.cresto.ui.theme.glasense.Blue500
import com.nevoit.cresto.ui.theme.glasense.Pink400
import com.nevoit.cresto.ui.theme.glasense.Purple500
import com.nevoit.cresto.ui.theme.glasense.Slate500
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeApi::class)
@Composable
fun BoxScope.SettingsScreen() {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val density = LocalDensity.current
    val thresholdPx = if (statusBarHeight > 0.dp) {
        with(density) {
            (statusBarHeight + 24.dp).toPx()
        }
    } else 0f

    val hazeState = rememberHazeState()

    val surfaceColor = AppColors.pageBackground
    val hierarchicalSurfaceColor = AppColors.cardBackground

    val lazyListState = rememberLazyListState()

    val isSmallTitleVisible by remember(thresholdPx) { derivedStateOf { ((lazyListState.firstVisibleItemIndex == 0) && (lazyListState.firstVisibleItemScrollOffset > thresholdPx)) || lazyListState.firstVisibleItemIndex > 0 } }

    val context = LocalContext.current



    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .hazeSource(hazeState, 0f)
            .fillMaxSize()
            .padding(0.dp),
        contentPadding = PaddingValues(
            start = 12.dp,
            top = 0.dp,
            end = 12.dp,
            bottom = 120.dp + navigationBarHeight
        )
    ) {
        item {
            GlasensePageHeader(
                title = stringResource(R.string.settings)
            )
        }
        item {
            ConfigContainer(backgroundColor = hierarchicalSurfaceColor) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ConfigEntryItem(
                        brush = Brush.sweepGradient(
                            colorStops = arrayOf(
                                0f to Pink400,
                                0.33f to Purple500,
                                0.66f to Blue500,
                                1f to Pink400
                            )
                        ),
                        icon = painterResource(R.drawable.ic_twotone_sparkles),
                        title = stringResource(R.string.ai),
                        enableGlow = true,
                        onClick = {
                            val intent = Intent(context, AIActivity::class.java)
                            context.startActivity(intent)
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        item {
            ConfigContainer(backgroundColor = hierarchicalSurfaceColor) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ConfigEntryItem(
                        color = Blue500,
                        icon = painterResource(R.drawable.ic_twotone_image),
                        title = stringResource(R.string.appearance),
                        onClick = {
                            val intent = Intent(context, AppearanceActivity::class.java)
                            context.startActivity(intent)
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    ConfigEntryItem(
                        color = Slate500,
                        icon = painterResource(R.drawable.ic_twotone_storage),
                        title = stringResource(R.string.data_storage),
                        onClick = {
                            val intent = Intent(context, DataStorageActivity::class.java)
                            context.startActivity(intent)
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    ConfigEntryItem(
                        color = Slate500,
                        icon = painterResource(R.drawable.ic_twotone_gear),
                        title = stringResource(R.string.general),
                        onClick = {}
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        item {
            ConfigContainer(backgroundColor = hierarchicalSurfaceColor) {
                AboutEntryItem(
                    icon = painterResource(R.drawable.cresto_foreground),
                    onClick = {
                        val intent = Intent(context, AboutActivity::class.java)
                        context.startActivity(intent)
                    }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        item {
            ConfigContainer(backgroundColor = hierarchicalSurfaceColor) {
                ConfigEntryItem(
                    color = Slate500,
                    icon = painterResource(R.drawable.ic_twotone_info),
                    title = stringResource(R.string.credits),
                    onClick = {}
                )
            }
        }
        overscrollSpacer(lazyListState)
    }
    GlasenseDynamicSmallTitle(
        modifier = Modifier.align(Alignment.TopCenter),
        title = stringResource(R.string.settings),
        statusBarHeight = statusBarHeight,
        isVisible = isSmallTitleVisible,
        hazeState = hazeState,
        surfaceColor = surfaceColor
    ) {
    }
}
