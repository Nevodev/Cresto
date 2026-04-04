// Package declaration for the settings screen
package com.nevoit.cresto.feature.settings

// Import necessary libraries and components
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush.Companion.sweepGradient
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nevoit.cresto.R
import com.nevoit.cresto.theme.AppButtonColors
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.harmonize
import com.nevoit.cresto.ui.components.glasense.GlasenseButton
import com.nevoit.cresto.ui.components.glasense.GlasenseDynamicSmallTitle
import com.nevoit.cresto.ui.components.glasense.GlasenseSwitch
import com.nevoit.cresto.ui.components.glasense.ZeroHeightDivider
import com.nevoit.cresto.ui.components.glasense.extend.overscrollSpacer
import com.nevoit.cresto.ui.components.glasense.isScrolledPast
import com.nevoit.cresto.ui.components.packed.ConfigInfoHeader
import com.nevoit.cresto.ui.components.packed.ConfigItem
import com.nevoit.cresto.ui.components.packed.ConfigItemContainer
import com.nevoit.cresto.ui.components.packed.PageContent
import com.nevoit.cresto.ui.components.packed.VGap
import com.nevoit.glasense.theme.Blue500
import com.nevoit.glasense.theme.Pink400
import com.nevoit.glasense.theme.Purple500
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/**
 * This composable function defines the AI Settings screen.
 * It provides options to configure AI-related features.
 * It uses experimental APIs for Material 3 and Haze effects.
 */
@OptIn(ExperimentalHazeApi::class)
@Composable
fun AIScreen() {
    // Get the current activity instance to allow finishing the screen
    val activity = LocalActivity.current

    // Calculate the height of the status bar to adjust layout
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val density = LocalDensity.current

    // Remember the state for the Haze effect, a library for blurring content behind a surface
    val hazeState = rememberHazeState()

    // Get colors from the app's custom theme
    val onSurfaceContainer = AppColors.scrimNormal
    val onBackground = AppColors.content
    val surfaceColor = AppColors.pageBackground
    val hierarchicalSurfaceColor = AppColors.cardBackground

    // Remember the state for the lazy list to control scrolling
    val lazyListState = rememberLazyListState()

    // Determine if the small title should be visible based on the scroll position
    val isSmallTitleVisible by lazyListState.isScrolledPast(statusBarHeight + 24.dp)


    // Root container for the screen, filling the entire available space
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaceColor)
    ) {
        // A vertically scrolling list that only composes and lays out the currently visible items
        PageContent(
            state = lazyListState,
            modifier = Modifier
                .hazeSource(hazeState, 0f),
            tabPadding = false
        ) {
            // Spacer item at the top of the list to push content below the top bar and back button
            item {
                Box(modifier = Modifier.padding(top = 48.dp + statusBarHeight + 12.dp))
            }
            // Header item for the AI section with a gradient brush and glow effect
            item {
                ConfigInfoHeader(
                    brush = sweepGradient(
                        colorStops = arrayOf(
                            0f to harmonize(Pink400),
                            0.33f to harmonize(Purple500),
                            0.66f to harmonize(Blue500),
                            1f to harmonize(Pink400)
                        )
                    ),
                    backgroundColor = hierarchicalSurfaceColor,
                    icon = painterResource(R.drawable.ic_twotone_sparkles),
                    title = stringResource(R.string.ai),
                    enableGlow = true,
                    info = stringResource(R.string.boost_your_experience_with_intelligent_cresto_function_calling)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            // Item container for API-related settings
            item {
                ConfigItemContainer(
                    title = stringResource(R.string.api),
                    backgroundColor = hierarchicalSurfaceColor
                ) {
                    Column {
                        ConfigItem(title = stringResource(R.string.api_key)) {
                            // Placeholder switch; functionality to be implemented
                            GlasenseSwitch(
                                checked = true,
                                onCheckedChange = {},
                                backgroundColor = surfaceColor
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        // Visual divider line
                        ZeroHeightDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        ConfigItem(title = stringResource(R.string.url)) {
                            // Placeholder switch; functionality to be implemented
                            GlasenseSwitch(
                                checked = true,
                                onCheckedChange = {},
                                backgroundColor = surfaceColor
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            // Item container for testing the AI functionality
            item {
                ConfigItemContainer(
                    title = stringResource(R.string.test),
                    backgroundColor = hierarchicalSurfaceColor
                ) {
                    Column {
                        ConfigItem(title = stringResource(R.string.input)) {
                            // Placeholder switch; functionality to be implemented
                            GlasenseSwitch(
                                checked = true,
                                onCheckedChange = {},
                                backgroundColor = surfaceColor
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        // Visual divider line
                        ZeroHeightDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        ConfigItem(title = stringResource(R.string.response)) {
                            // Placeholder switch; functionality to be implemented
                            GlasenseSwitch(
                                checked = true,
                                onCheckedChange = {},
                                backgroundColor = surfaceColor
                            )
                        }
                    }
                }
            }
            item { VGap() }
            overscrollSpacer(lazyListState)
        }
        // A small title that dynamically appears at the top when the user scrolls down
        GlasenseDynamicSmallTitle(
            modifier = Modifier.align(Alignment.TopCenter),
            title = stringResource(R.string.ai),
            statusBarHeight = statusBarHeight,
            isVisible = isSmallTitleVisible,
            hazeState = hazeState,
            surfaceColor = surfaceColor
        ) {
            // This lambda is empty as the component handles its own content
        }
        // Back button positioned at the top-start of the screen
        GlasenseButton(
            enabled = true,
            shape = CircleShape,
            onClick = { activity?.finish() }, // Closes the current activity, navigating back
            modifier = Modifier
                .padding(top = statusBarHeight, start = 12.dp)
                .size(48.dp)
                .align(Alignment.TopStart),
            colors = AppButtonColors.action()
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_forward_nav),
                contentDescription = stringResource(R.string.back),
                modifier = Modifier.width(32.dp)
            )
        }
    }
}
