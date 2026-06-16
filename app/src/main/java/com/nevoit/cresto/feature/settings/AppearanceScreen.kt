// Package declaration for the settings screen
package com.nevoit.cresto.feature.settings

// Import necessary libraries and components
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.RoundedRectangle
import com.nevoit.cresto.R
import com.nevoit.cresto.feature.settings.util.AppIconManager
import com.nevoit.cresto.feature.settings.util.SettingsViewModel
import com.nevoit.cresto.theme.AppButtonColors
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.AppSpecs
import com.nevoit.cresto.theme.harmonize
import com.nevoit.cresto.ui.components.glasense.GlasenseButton
import com.nevoit.cresto.ui.components.glasense.GlasenseDynamicSmallTitle
import com.nevoit.cresto.ui.components.glasense.GlasenseModalTopBar
import com.nevoit.cresto.ui.components.glasense.GlasensePopup
import com.nevoit.cresto.ui.components.glasense.PopupDirection
import com.nevoit.cresto.ui.components.glasense.PopupState
import com.nevoit.cresto.ui.components.glasense.isScrolledPast
import com.nevoit.cresto.ui.components.packed.ColorModeSelector
import com.nevoit.cresto.ui.components.packed.ConfigInfoHeader
import com.nevoit.cresto.ui.components.packed.TopBarSpacer
import com.nevoit.glasense.component.ListStack
import com.nevoit.glasense.core.component.Icon
import com.nevoit.glasense.core.component.Text
import com.nevoit.glasense.core.component.VGap
import com.nevoit.glasense.core.interaction.DimIndication
import com.nevoit.glasense.core.interaction.overscroll.rememberOffsetOverscrollFactory
import com.nevoit.glasense.theme.tokens.Amber500
import com.nevoit.glasense.theme.tokens.Blue500
import com.nevoit.glasense.theme.tokens.Cyan500
import com.nevoit.glasense.theme.tokens.Emerald500
import com.nevoit.glasense.theme.tokens.Fuchsia500
import com.nevoit.glasense.theme.tokens.Green500
import com.nevoit.glasense.theme.tokens.Indigo500
import com.nevoit.glasense.theme.tokens.Lime500
import com.nevoit.glasense.theme.tokens.Orange500
import com.nevoit.glasense.theme.tokens.Pink500
import com.nevoit.glasense.theme.tokens.Purple500
import com.nevoit.glasense.theme.tokens.Red500
import com.nevoit.glasense.theme.tokens.Rose500
import com.nevoit.glasense.theme.tokens.Sky500
import com.nevoit.glasense.theme.tokens.Teal500
import com.nevoit.glasense.theme.tokens.Violet500
import com.nevoit.glasense.theme.tokens.Yellow500

@Composable
fun AppearanceScreen(settingsViewModel: SettingsViewModel = viewModel()) {
    // Get the current activity instance to allow finishing the screen
    val activity = LocalActivity.current
    val context = LocalContext.current

    // Calculate the height of the status bar to adjust layout
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val density = LocalDensity.current
    // Calculate the scroll threshold in pixels for showing/hiding the small title

    // Get colors from the app's custom theme
    val backgroundColor = AppColors.pageBackground

    // Remember the state for the lazy list to control scrolling
    val lazyListState = rememberLazyListState()

    // Determine if the small title should be visible based on the scroll position
    val isSmallTitleVisible by lazyListState.isScrolledPast(statusBarHeight + 24.dp)


    // State variables for the various appearance settings, managed by the ViewModel
    var isCustomPrimaryColor by settingsViewModel.isCustomPrimaryColorEnabled
    var isUseDynamicColorScheme by settingsViewModel.isUseDynamicColor
    var isLiteMode by settingsViewModel.isLiteMode
    var isLiquidGlass by settingsViewModel.isLiquidGlass
    val currentMode by settingsViewModel.colorMode
    val currentThemePrimaryColor by settingsViewModel.themePrimaryColor
    val currentAppIcon by settingsViewModel.appIcon
    val systemInDarkTheme = isSystemInDarkTheme()
    val appIconEntries = AppIconManager.AppIcon.entries

    var showColorPicker by remember { mutableStateOf(false) }
    var pendingThemePrimaryColor by remember { mutableIntStateOf(currentThemePrimaryColor) }
    var latestColorPickerTriggerBounds by remember { mutableStateOf<Rect?>(null) }
    var popupAnchorBounds by remember { mutableStateOf(Rect.Zero) }

    val overscrollFactory = rememberOffsetOverscrollFactory()

    val backdrop = rememberLayerBackdrop {
        drawRect(
            color = backgroundColor,
            size = Size(this.size.width * 3, this.size.height * 3),
            topLeft = Offset(-this.size.width, -this.size.height)
        )
        drawContent()
    }

    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val scrim = AppColors.scrimMedium
    val stroke = with(density) { 2.dp.toPx() }

    // Root container for the screen, filling the entire available space
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // A vertically scrolling list that only composes and lays out the currently visible items
        ListStack(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
            cornerRadius = AppSpecs.cardCorner,
            contentPadding = PaddingValues(bottom = navigationBarHeight)
        ) {
            TopBarSpacer()
            // Header item for the Appearance section
            item {
                ConfigInfoHeader(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    color = harmonize(Blue500),
                    backgroundColor = AppColors.cardBackground,
                    icon = painterResource(R.drawable.ic_twotone_image),
                    title = stringResource(R.string.appearance),
                    info = stringResource(R.string.craft_your_unique_style_with_a_few_adorable_tweaks)
                )
            }
            item { VGap(24.dp) }
            // Item for selecting the color mode (light/dark/system)
            ColorModeSelector(
                onChange = { settingsViewModel.colorMode(it) },
                currentMode = currentMode,
                systemInDarkTheme = systemInDarkTheme
            )
            // Item container for color-related settings
            Section(
                header = { stringResource(R.string.color) },
                footer = { stringResource(R.string.when_use_dynamic_color_scheme_is_enabled_custom_primary_color_is_automatically_turned_off) }) {
                CustomSwitchRow(
                    trailing = {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .onGloballyPositioned {
                                    latestColorPickerTriggerBounds = it.boundsInWindow()
                                }
                                .drawBehind {
                                    drawCircle(
                                        color = scrim,
                                        style = Stroke(width = stroke),
                                        radius = (size.minDimension - stroke) / 2
                                    )
                                    drawCircle(
                                        color = Color(currentThemePrimaryColor),
                                        radius = (size.minDimension - stroke * 4) / 2
                                    )
                                }
                                .clickable(
                                    enabled = !isUseDynamicColorScheme,
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = DimIndication(shape = CircleShape)
                                ) {
                                    // Snapshot clicked trigger bounds to anchor popup animation/placement.
                                    popupAnchorBounds =
                                        latestColorPickerTriggerBounds ?: Rect.Zero
                                    pendingThemePrimaryColor = currentThemePrimaryColor
                                    showColorPicker = !showColorPicker
                                }
                        )
                    },
                    checked = isCustomPrimaryColor,
                    onCheckedChange = { settingsViewModel.onCustomPrimaryColorChanged(it) },
                    enabled = !isUseDynamicColorScheme
                )
                {
                    Text(stringResource(R.string.custom_primary_color))
                }
                CustomSwitchRow(
                    checked = isUseDynamicColorScheme,
                    onCheckedChange = { settingsViewModel.onUseDynamicColorChanged(it) }) {
                    Text(stringResource(R.string.use_dynamic_color_scheme))
                }
            }
            Section(
                header = { stringResource(R.string.design) },
                footer = { stringResource(R.string.enabling_lite_mode_will_disable_some_blur_effects) }) {
                CustomSwitchRow(
                    checked = isLiteMode,
                    onCheckedChange = { settingsViewModel.onLiteModeChanged(it) }) {
                    Text(stringResource(R.string.lite_mode))
                }
            }
            Section(topSpacing = 12.dp, footer = {
                stringResource(
                    R.string.enabling_liquid_glass_can_significantly_impact_performance
                )
            }) {
                CustomSwitchRow(
                    glass = true,
                    checked = isLiquidGlass,
                    onCheckedChange = { settingsViewModel.onLiquidGlassChanged(it) }) {
                    Text(stringResource(R.string.liquid_glass))
                }
            }
            NoPaddingSection(header = { stringResource(R.string.app_icon) }) {
                Row {
                    CompositionLocalProvider(LocalOverscrollFactory provides overscrollFactory) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 16.dp, horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            itemsIndexed(
                                items = appIconEntries,
                                key = { index, icon -> "${index}_${icon.alias}" }) { _, icon ->
                                AppIconOption(
                                    icon = icon,
                                    selected = currentAppIcon == icon,
                                    onClick = {
                                        if (currentAppIcon != icon) {
                                            settingsViewModel.onAppIconChanged(context, icon)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
            item { VGap() }
        }
        // A small title that dynamically appears at the top when the user scrolls down
        GlasenseDynamicSmallTitle(
            modifier = Modifier.align(Alignment.TopCenter),
            title = stringResource(R.string.appearance),
            statusBarHeight = statusBarHeight,
            isVisible = isSmallTitleVisible,
            backdrop = backdrop,
            surfaceColor = AppColors.pageBackground
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
        GlasensePopup(
            popupState = PopupState(
                isVisible = showColorPicker,
                anchorBounds = popupAnchorBounds
            ),
            onDismiss = {
                pendingThemePrimaryColor = currentThemePrimaryColor
                showColorPicker = false
            },
            width = LocalWindowInfo.current.containerDpSize.width - 24.dp,
            popupMargin = 12.dp,
            anchorGap = 12.dp,
            direction = PopupDirection.Up
        ) {
            GlasenseModalTopBar(
                leading = {
                    Action(
                        icon = painterResource(id = R.drawable.ic_cross),
                        contentDescription = stringResource(R.string.cancel),
                        onClick = {
                            pendingThemePrimaryColor = currentThemePrimaryColor
                            showColorPicker = false
                        }
                    )
                },
                title = stringResource(R.string.custom_primary_color),
                trailing = {
                    Action(
                        icon = painterResource(id = R.drawable.ic_checkmark),
                        contentDescription = stringResource(R.string.done),
                        onClick = {
                            if (pendingThemePrimaryColor != currentThemePrimaryColor) {
                                settingsViewModel.onThemePrimaryColorChanged(
                                    pendingThemePrimaryColor
                                )
                            }
                            showColorPicker = false
                        },
                        colors = AppButtonColors.primary()
                            .copy(containerColor = Color(pendingThemePrimaryColor)),
                        highlight = true
                    )
                }
            )
            VGap()
            val hapticController = LocalHapticFeedback.current

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
                itemsIndexed(
                    items = colorList,
                    key = { index, color -> "${index}_${color.toArgb()}" }) { _, color ->
                    val isSelected = color.toArgb() == pendingThemePrimaryColor
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = DimIndication()
                            ) {
                                hapticController.performHapticFeedback(HapticFeedbackType.ContextClick)
                                pendingThemePrimaryColor = color.toArgb()
                            }
                            .background(color = color)
                            .drawBehind {
                                if (isSelected) {
                                    drawCircle(
                                        color = Color.White.copy(alpha = .6f),
                                        radius = size.minDimension / 4
                                    )
                                }
                            }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppIconOption(
    icon: AppIconManager.AppIcon,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .border(
                    if (selected) 2.dp else 1.dp,
                    if (selected) AppColors.primary else Color.Black.copy(alpha = 0.05f),
                    RoundedRectangle(16.dp)
                )
                .size(64.dp)
                .clip(RoundedRectangle(16.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = DimIndication(),
                    onClick = onClick
                )
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(icon.mipmapResId),
                contentDescription = stringResource(icon.displayNameResId),
                modifier = Modifier.size(64.dp)
            )

        }
        Text(
            stringResource(icon.displayNameResId),
            fontSize = 12.sp,
            lineHeight = 12.sp,
            modifier = Modifier.padding(top = 8.dp),
            color = AppColors.contentVariant
        )
    }
}
