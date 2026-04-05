package com.nevoit.cresto.feature.settings

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nevoit.cresto.R
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.harmonize
import com.nevoit.cresto.ui.components.glasense.GlasenseDynamicSmallTitle
import com.nevoit.cresto.ui.components.glasense.GlasensePageHeader
import com.nevoit.cresto.ui.components.glasense.extend.overscrollSpacer
import com.nevoit.cresto.ui.components.glasense.isScrolledPast
import com.nevoit.cresto.ui.components.packed.AboutEntryItem
import com.nevoit.cresto.ui.components.packed.ConfigContainer
import com.nevoit.cresto.ui.components.packed.ConfigEntryItem
import com.nevoit.cresto.ui.components.packed.PageContent
import com.nevoit.cresto.ui.components.packed.VGap
import com.nevoit.glasense.theme.Blue500
import com.nevoit.glasense.theme.Pink400
import com.nevoit.glasense.theme.Purple500
import com.nevoit.glasense.theme.Slate500
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeApi::class)
@Composable
fun BoxScope.SettingsScreen() {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val hazeState = rememberHazeState()

    val surfaceColor = AppColors.pageBackground
    val hierarchicalSurfaceColor = AppColors.cardBackground

    val lazyListState = rememberLazyListState()

    val isSmallTitleVisible by lazyListState.isScrolledPast(statusBarHeight + 24.dp)

    val context = LocalContext.current

    PageContent(
        state = lazyListState,
        modifier = Modifier
            .hazeSource(hazeState, 0f),
        tabPadding = true
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
                                0f to harmonize(Pink400),
                                0.33f to harmonize(Purple500),
                                0.66f to harmonize(Blue500),
                                1f to harmonize(Pink400)
                            )
                        ),
                        icon = painterResource(R.drawable.ic_twotone_sparkles),
                        title = stringResource(R.string.ai),
                        enableGlow = true,
                        onClick = {
                            context.startActivity(
                                SettingsActivity.createIntent(context, SettingsDestination.AI)
                            )
                        }
                    )
                }
            }
            VGap()
        }
        item {
            ConfigContainer(backgroundColor = hierarchicalSurfaceColor) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ConfigEntryItem(
                        color = harmonize(Blue500),
                        icon = painterResource(R.drawable.ic_twotone_image),
                        title = stringResource(R.string.appearance),
                        onClick = {
                            context.startActivity(
                                SettingsActivity.createIntent(
                                    context,
                                    SettingsDestination.APPEARANCE
                                )
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    ConfigEntryItem(
                        color = harmonize(Slate500),
                        icon = painterResource(R.drawable.ic_twotone_storage),
                        title = stringResource(R.string.data_storage),
                        onClick = {
                            context.startActivity(
                                SettingsActivity.createIntent(
                                    context,
                                    SettingsDestination.DATA_STORAGE
                                )
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    ConfigEntryItem(
                        color = harmonize(Slate500),
                        icon = painterResource(R.drawable.ic_twotone_gear),
                        title = stringResource(R.string.general),
                        onClick = {
                            context.startActivity(
                                SettingsActivity.createIntent(
                                    context,
                                    SettingsDestination.GENERAL
                                )
                            )
                        }
                    )
                }
            }
            VGap()
        }
        item {
            ConfigContainer(backgroundColor = hierarchicalSurfaceColor) {
                AboutEntryItem(
                    icon = painterResource(R.drawable.cresto),
                    onClick = {
                        context.startActivity(
                            SettingsActivity.createIntent(context, SettingsDestination.ABOUT)
                        )
                    }
                )
            }
            VGap()
        }
        item {
            ConfigContainer(backgroundColor = hierarchicalSurfaceColor) {
                ConfigEntryItem(
                    color = harmonize(Slate500),
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
