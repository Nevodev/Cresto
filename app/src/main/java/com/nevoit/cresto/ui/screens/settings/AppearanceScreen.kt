// Package declaration for the settings screen
package com.nevoit.cresto.ui.screens.settings

// Import necessary libraries and components
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.shapes.RoundedRectangle
import com.kyant.shapes.UnevenRoundedRectangle
import com.nevoit.cresto.R
import com.nevoit.cresto.ui.components.glasense.DimIndication
import com.nevoit.cresto.ui.components.glasense.GlasenseButton
import com.nevoit.cresto.ui.components.glasense.GlasenseDynamicSmallTitle
import com.nevoit.cresto.ui.components.glasense.GlasenseSwitch
import com.nevoit.cresto.ui.components.glasense.ZeroHeightDivider
import com.nevoit.cresto.ui.components.glasense.extend.overscrollSpacer
import com.nevoit.cresto.ui.components.glasense.isScrolledPast
import com.nevoit.cresto.ui.components.packed.ColorModeSelector
import com.nevoit.cresto.ui.components.packed.ConfigInfoHeader
import com.nevoit.cresto.ui.components.packed.ConfigItem
import com.nevoit.cresto.ui.components.packed.ConfigItemContainer
import com.nevoit.cresto.ui.components.packed.PageContent
import com.nevoit.cresto.ui.components.packed.VGap
import com.nevoit.cresto.ui.screens.settings.util.SettingsViewModel
import com.nevoit.cresto.ui.theme.glasense.Amber500
import com.nevoit.cresto.ui.theme.glasense.AppButtonColors
import com.nevoit.cresto.ui.theme.glasense.AppColors
import com.nevoit.cresto.ui.theme.glasense.AppSpecs
import com.nevoit.cresto.ui.theme.glasense.Blue500
import com.nevoit.cresto.ui.theme.glasense.Cyan500
import com.nevoit.cresto.ui.theme.glasense.Emerald500
import com.nevoit.cresto.ui.theme.glasense.Fuchsia500
import com.nevoit.cresto.ui.theme.glasense.Green500
import com.nevoit.cresto.ui.theme.glasense.Indigo500
import com.nevoit.cresto.ui.theme.glasense.Lime500
import com.nevoit.cresto.ui.theme.glasense.LocalGlasenseSettings
import com.nevoit.cresto.ui.theme.glasense.Orange500
import com.nevoit.cresto.ui.theme.glasense.Pink500
import com.nevoit.cresto.ui.theme.glasense.Purple500
import com.nevoit.cresto.ui.theme.glasense.Red500
import com.nevoit.cresto.ui.theme.glasense.Rose500
import com.nevoit.cresto.ui.theme.glasense.Sky500
import com.nevoit.cresto.ui.theme.glasense.Teal500
import com.nevoit.cresto.ui.theme.glasense.Violet500
import com.nevoit.cresto.ui.theme.glasense.Yellow500
import com.nevoit.cresto.ui.theme.glasense.isAppInDarkTheme
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/**
 * This composable function defines the Appearance screen.
 * It allows users to customize the look and feel of the application.
 * It uses experimental APIs for Material 3 and Haze effects.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeApi::class)
@Composable
fun AppearanceScreen(settingsViewModel: SettingsViewModel = viewModel()) {
    // Get the current activity instance to allow finishing the screen
    val activity = LocalActivity.current

    // Calculate the height of the status bar to adjust layout
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val density = LocalDensity.current
    // Calculate the scroll threshold in pixels for showing/hiding the small title

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

    // Get the pixel value for 1dp, used for drawing divider lines
    val dp = with(density) {
        1.dp.toPx()
    }

    // State variables for the various appearance settings, managed by the ViewModel
    var isCustomPrimaryColor by settingsViewModel.isCustomPrimaryColorEnabled
    var isUseDynamicColorScheme by settingsViewModel.isUseDynamicColor
    var isLiteMode by settingsViewModel.isLiteMode
    var isLiquidGlass by settingsViewModel.isLiquidGlass
    val currentMode by settingsViewModel.colorMode

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
            // Header item for the Appearance section
            item {
                ConfigInfoHeader(
                    color = Blue500,
                    backgroundColor = hierarchicalSurfaceColor,
                    icon = painterResource(R.drawable.ic_twotone_image),
                    title = stringResource(R.string.appearance),
                    info = stringResource(R.string.craft_your_unique_style_with_a_few_adorable_tweaks)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            // Item for selecting the color mode (light/dark/system)
            item {
                ColorModeSelector(
                    backgroundColor = hierarchicalSurfaceColor,
                    onChange = { settingsViewModel.colorMode(it) },
                    currentMode = currentMode
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            // Item container for color-related settings
            item {
                val scrim = AppColors.scrimMedium
                val stroke = with(density) { 2.dp.toPx() }
                val primary = AppColors.primary

                ConfigItemContainer(
                    title = stringResource(R.string.color),
                    backgroundColor = hierarchicalSurfaceColor
                ) {
                    Column {
                        ConfigItem(title = stringResource(R.string.custom_primary_color)) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .drawBehind {
                                        drawCircle(
                                            color = scrim,
                                            style = Stroke(width = stroke),
                                            radius = (size.minDimension - stroke) / 2
                                        )
                                        drawCircle(
                                            color = primary,
                                            radius = (size.minDimension - stroke * 4) / 2
                                        )
                                    }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            GlasenseSwitch(
                                backgroundColor = AppColors.cardBackground,
                                checked = isCustomPrimaryColor,
                                onCheckedChange = { settingsViewModel.onCustomPrimaryColorChanged(it) })
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        // Visual divider line
                        ZeroHeightDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        ConfigItem(title = stringResource(R.string.use_dynamic_color_scheme)) {
                            GlasenseSwitch(
                                backgroundColor = AppColors.cardBackground,
                                checked = isUseDynamicColorScheme,
                                onCheckedChange = { settingsViewModel.onUseDynamicColorChanged(it) })
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            // Item container for design-related settings
            item {
                ConfigItemContainer(
                    title = stringResource(R.string.design),
                    backgroundColor = hierarchicalSurfaceColor
                ) {
                    Column {
                        ConfigItem(title = stringResource(R.string.lite_mode)) {
                            GlasenseSwitch(
                                backgroundColor = AppColors.cardBackground,
                                checked = isLiteMode,
                                onCheckedChange = { settingsViewModel.onLiteModeChanged(it) })
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        // Visual divider line
                        ZeroHeightDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        ConfigItem(title = stringResource(R.string.liquid_glass)) {
                            CompositionLocalProvider(
                                LocalGlasenseSettings provides LocalGlasenseSettings.current.copy(
                                    liquidGlass = true
                                )
                            ) {
                                GlasenseSwitch(
                                    backgroundColor = AppColors.cardBackground,
                                    checked = isLiquidGlass,
                                    onCheckedChange = { settingsViewModel.onLiquidGlassChanged(it) })
                            }
                        }
                    }
                }
            }
            overscrollSpacer(lazyListState)
        }
        // A small title that dynamically appears at the top when the user scrolls down
        GlasenseDynamicSmallTitle(
            modifier = Modifier.align(Alignment.TopCenter),
            title = stringResource(R.string.appearance),
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

        val shadowBaseColor =
            if (isAppInDarkTheme()) Color.Black.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.1f)

        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 12.dp)
                .dropShadow(
                    shape = AppSpecs.dialogShape,
                    shadow = Shadow(
                        radius = 32.dp,
                        offset = DpOffset(0.dp, 16.dp),
                        color = shadowBaseColor
                    )
                )
                .background(color = AppColors.pageBackground, shape = AppSpecs.dialogShape)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlasenseButton(
                    enabled = true,
                    shape = CircleShape,
                    onClick = { },
                    modifier = Modifier
                        .width(48.dp)
                        .height(48.dp),
                    colors = AppButtonColors.secondary(),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_cross),
                        contentDescription = stringResource(R.string.cancel),
                        modifier = Modifier.width(28.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.custom_primary_color),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineSmall
                )
                GlasenseButton(
                    enabled = true,
                    shape = CircleShape,
                    onClick = {},
                    modifier = Modifier
                        .width(48.dp)
                        .height(48.dp),
                    colors = AppButtonColors.primary()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(), contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_checkmark),
                            contentDescription = stringResource(R.string.done),
                            modifier = Modifier.width(28.dp)
                        )
                    }
                }
            }
            VGap()
            Text(
                text = "颜色",
                fontSize = 14.sp,
                lineHeight = 14.sp,
                color = AppColors.contentVariant,
                modifier = Modifier
                    .padding(
                        start = 12.dp,
                        top = 0.dp,
                        end = 12.dp,
                        bottom = 12.dp
                    )
                    .fillMaxWidth()
            )
            val colorList = remember {
                listOf(
                    Rose500, Red500, Orange500, Amber500,
                    Yellow500, Lime500, Green500, Emerald500,
                    Teal500, Cyan500, Sky500, Blue500,
                    Indigo500, Violet500, Purple500, Fuchsia500,
                    Pink500,
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 32.dp),
                contentPadding = PaddingValues(0.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(colorList) { color ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .background(color = color, shape = CircleShape)
                    )
                }
            }
            VGap()
            ConfigItemContainer(title = "调整", backgroundColor = AppColors.cardBackground) {
                ConfigItem(title = "亮度增益") {
                    Row(modifier = Modifier.height(32.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(32.dp)
                                .clip(shape = UnevenRoundedRectangle(16.dp, 4.dp, 4.dp, 16.dp))
                                .background(color = AppColors.inactiveTrack)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = DimIndication(),
                                    onClick = {}),
                            contentAlignment = Alignment.Center
                        ) {

                        }
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .fillMaxHeight()
                                .width(32.dp)
                                .clip(shape = RoundedRectangle(4.dp))
                                .background(color = AppColors.inactiveTrack),
                            contentAlignment = Alignment.Center
                        ) {

                        }
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(32.dp)
                                .clip(shape = UnevenRoundedRectangle(4.dp, 16.dp, 16.dp, 4.dp))
                                .background(color = AppColors.inactiveTrack)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = DimIndication(),
                                    onClick = {}),
                            contentAlignment = Alignment.Center
                        ) {

                        }
                    }
                }
            }
        }
    }
}
