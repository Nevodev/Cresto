package com.nevoit.cresto.feature.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.nevoit.cresto.R
import com.nevoit.cresto.theme.AppButtonColors
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.harmonize
import com.nevoit.cresto.ui.components.glasense.GlasenseButtonToolBar
import com.nevoit.cresto.ui.components.glasense.GlasenseDynamicSmallTitle
import com.nevoit.cresto.ui.components.glasense.GlasensePageHeader
import com.nevoit.cresto.ui.components.glasense.isScrolledPast
import com.nevoit.cresto.ui.components.packed.AboutEntryItem
import com.nevoit.cresto.ui.components.packed.ConfigContainer
import com.nevoit.cresto.ui.components.packed.ConfigEntryItem
import com.nevoit.cresto.ui.components.packed.PageContent
import com.nevoit.glasense.component.paddingItem
import com.nevoit.glasense.core.component.Icon
import com.nevoit.glasense.core.component.VGap
import com.nevoit.glasense.theme.tokens.Blue500
import com.nevoit.glasense.theme.tokens.Pink400
import com.nevoit.glasense.theme.tokens.Purple500
import com.nevoit.glasense.theme.tokens.Slate500

@Composable
fun SettingsScreen() {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val hierarchicalSurfaceColor = AppColors.cardBackground

    val lazyListState = rememberLazyListState()

    val isSmallTitleVisible by lazyListState.isScrolledPast(statusBarHeight + 24.dp)

    val context = LocalContext.current
    val activity = LocalActivity.current

    val backgroundColor = AppColors.pageBackground
    val backdrop = rememberLayerBackdrop {
        drawRect(
            color = backgroundColor,
            size = Size(this.size.width * 3, this.size.height * 3),
            topLeft = Offset(-this.size.width, -this.size.height)
        )
        drawContent()
    }
    Box(modifier = Modifier.background(AppColors.pageBackground)) {
        PageContent(
            state = lazyListState,
            modifier = Modifier
                .layerBackdrop(backdrop),
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
                        onClick = {
                            context.startActivity(
                                SettingsActivity.createIntent(context, SettingsDestination.CREDITS)
                            )
                        }
                    )
                }
            }
            paddingItem(lazyListState)
        }
        GlasenseDynamicSmallTitle(
            modifier = Modifier.align(Alignment.TopCenter),
            title = stringResource(R.string.settings),
            statusBarHeight = statusBarHeight,
            isVisible = isSmallTitleVisible,
            backdrop = backdrop
        ) {
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 12.dp)
        ) {
            GlasenseButtonToolBar(
                enabled = true,
                shape = CircleShape,
                onClick = {
                    activity?.finish()
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(48.dp),
                colors = AppButtonColors.action(),
                interactionSource = remember { MutableInteractionSource() }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_forward_nav),
                    contentDescription = stringResource(R.string.back),
                    modifier = Modifier.width(32.dp)
                )
            }
        }
    }
}
