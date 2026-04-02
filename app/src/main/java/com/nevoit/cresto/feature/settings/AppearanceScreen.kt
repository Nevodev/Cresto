// Package declaration for the settings screen
package com.nevoit.cresto.feature.settings

// Import necessary libraries and components
import android.graphics.BlurMaskFilter
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.nativePaint
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nevoit.cresto.R
import com.nevoit.cresto.feature.settings.util.SettingsViewModel
import com.nevoit.cresto.glasense.AppButtonColors
import com.nevoit.cresto.glasense.AppColors
import com.nevoit.cresto.glasense.AppSpecs
import com.nevoit.cresto.glasense.LocalGlasenseSettings
import com.nevoit.cresto.glasense.isAppInDarkTheme
import com.nevoit.cresto.ui.components.glasense.DimIndication
import com.nevoit.cresto.ui.components.glasense.GlasenseButton
import com.nevoit.cresto.ui.components.glasense.GlasenseDynamicSmallTitle
import com.nevoit.cresto.ui.components.glasense.GlasenseSwitch
import com.nevoit.cresto.ui.components.glasense.ZeroHeightDivider
import com.nevoit.cresto.ui.components.glasense.extend.overscrollSpacer
import com.nevoit.cresto.ui.components.glasense.glasenseHighlight
import com.nevoit.cresto.ui.components.glasense.isScrolledPast
import com.nevoit.cresto.ui.components.packed.ColorModeSelector
import com.nevoit.cresto.ui.components.packed.ConfigInfoHeader
import com.nevoit.cresto.ui.components.packed.ConfigItem
import com.nevoit.cresto.ui.components.packed.ConfigItemContainer
import com.nevoit.cresto.ui.components.packed.PageContent
import com.nevoit.cresto.ui.components.packed.VGap
import com.nevoit.glasense.theme.Amber500
import com.nevoit.glasense.theme.Blue500
import com.nevoit.glasense.theme.Cyan500
import com.nevoit.glasense.theme.Emerald500
import com.nevoit.glasense.theme.Fuchsia500
import com.nevoit.glasense.theme.Green500
import com.nevoit.glasense.theme.Indigo500
import com.nevoit.glasense.theme.Lime500
import com.nevoit.glasense.theme.Orange500
import com.nevoit.glasense.theme.Pink500
import com.nevoit.glasense.theme.Purple500
import com.nevoit.glasense.theme.Red500
import com.nevoit.glasense.theme.Rose500
import com.nevoit.glasense.theme.Sky500
import com.nevoit.glasense.theme.Teal500
import com.nevoit.glasense.theme.Violet500
import com.nevoit.glasense.theme.Yellow500
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    val currentThemePrimaryColor by settingsViewModel.themePrimaryColor

    var showColorPicker by remember { mutableStateOf(false) }
    var pendingThemePrimaryColor by remember { mutableStateOf(currentThemePrimaryColor) }
    var latestColorPickerTriggerBounds by remember { mutableStateOf<Rect?>(null) }
    var colorPickerTriggerBounds by remember { mutableStateOf<Rect?>(null) }
    var colorPickerSelfBoundsSnapshot by remember { mutableStateOf<Rect?>(null) }
    var hasCapturedPickerBoundsForOpen by remember { mutableStateOf(false) }

    val xFraction = if (colorPickerTriggerBounds != null) {
        (colorPickerTriggerBounds!!.left + colorPickerTriggerBounds!!.width / 2) / density.run { (LocalWindowInfo.current.containerDpSize.width - 24.dp).toPx() }
    } else {
        0.5f
    }

    val yTranslation =
        if (colorPickerTriggerBounds != null && colorPickerSelfBoundsSnapshot != null) {
            colorPickerTriggerBounds!!.top - colorPickerSelfBoundsSnapshot!!.top - colorPickerSelfBoundsSnapshot!!.height - density.run { 12.dp.toPx() }
        } else {
            0f
        }

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

                ConfigItemContainer(
                    title = stringResource(R.string.color),
                    backgroundColor = hierarchicalSurfaceColor
                ) {
                    Column {
                        ConfigItem(title = stringResource(R.string.custom_primary_color)) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .onGloballyPositioned {
                                        // Keep latest layout info so click can snapshot current bounds.
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
                                        // Snapshot clicked trigger bounds at popup time.
                                        colorPickerTriggerBounds = latestColorPickerTriggerBounds
                                        showColorPicker = !showColorPicker
                                    }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            GlasenseSwitch(
                                backgroundColor = AppColors.cardBackground,
                                enabled = !isUseDynamicColorScheme,
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
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.when_use_dynamic_color_scheme_is_enabled_custom_primary_color_is_automatically_turned_off),
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 12.dp),
                    color = AppColors.contentVariant.copy(alpha = .3f)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            // Item container for design-related settings
            item {
                ConfigItemContainer(
                    title = stringResource(R.string.design),
                    backgroundColor = hierarchicalSurfaceColor
                ) {
                    ConfigItem(title = stringResource(R.string.lite_mode)) {
                        GlasenseSwitch(
                            backgroundColor = AppColors.cardBackground,
                            checked = isLiteMode,
                            onCheckedChange = { settingsViewModel.onLiteModeChanged(it) })
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.enabling_lite_mode_will_disable_some_blur_effects),
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 12.dp),
                    color = AppColors.contentVariant.copy(alpha = .3f)
                )
                VGap()
            }
            item {
                ConfigItemContainer(backgroundColor = AppColors.cardBackground) {
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
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        R.string.enabling_liquid_glass_can_significantly_impact_performance
                    ),
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 12.dp),
                    color = AppColors.contentVariant.copy(alpha = .3f)
                )
            }
            item { VGap() }
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

        val scaleAni = remember { Animatable(0.4f) }
        val alphaAni = remember { Animatable(0f) }

        val shadowRadiusPx = with(LocalDensity.current) { 32.dp.toPx() }
        val shadowDyPx = with(LocalDensity.current) { 16.dp.toPx() }

        val shadowPaint = remember {
            Paint().nativePaint.apply {
                isAntiAlias = true
                maskFilter = BlurMaskFilter(shadowRadiusPx, BlurMaskFilter.Blur.NORMAL)
            }
        }
        val dialogShape = AppSpecs.dialogShape

        var isColorPickerInComposition by remember { mutableStateOf(false) }

        LaunchedEffect(showColorPicker) {
            if (showColorPicker) {
                // Reset picker selection to the currently applied color whenever dialog opens.
                pendingThemePrimaryColor = currentThemePrimaryColor
                hasCapturedPickerBoundsForOpen = false
                delay(50)
                isColorPickerInComposition = true
                coroutineScope {
                    launch { scaleAni.animateTo(1f, spring(0.8f, 450f, 0.001f)) }
                    launch { alphaAni.animateTo(1f) }
                }
            } else {
                delay(50)
                coroutineScope {
                    launch { scaleAni.animateTo(0.4f, spring(0.7f, 600f)) }
                    launch { alphaAni.animateTo(0f) }
                }
                isColorPickerInComposition = false
            }
        }
        if (showColorPicker) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showColorPicker = false })
            )
            BackHandler() {
                pendingThemePrimaryColor = currentThemePrimaryColor
                showColorPicker = false
            }
        }
        if (isColorPickerInComposition) {
            Column(
                modifier = Modifier
                    .width(LocalWindowInfo.current.containerDpSize.width - 12.dp * 2)
                    .onGloballyPositioned {
                        if (showColorPicker && !hasCapturedPickerBoundsForOpen) {
                            // Capture once per open using layout bounds before graphicsLayer effects.
                            colorPickerSelfBoundsSnapshot = it.boundsInWindow()
                            hasCapturedPickerBoundsForOpen = true
                        }
                    }
                    .graphicsLayer {
                        translationY = yTranslation
                        translationX = 12 * dp
                        scaleX = scaleAni.value
                        scaleY = scaleAni.value
                        transformOrigin =
                            TransformOrigin(
                                xFraction,
                                1f
                            )
                    }
                    .drawBehind {
                        val currentAlpha = alphaAni.value

                        if (currentAlpha > 0f) {
                            val paintColor =
                                shadowBaseColor.copy(alpha = shadowBaseColor.alpha * currentAlpha)
                            shadowPaint.color = paintColor.toArgb()

                            drawIntoCanvas { canvas ->
                                canvas.save()
                                canvas.translate(0f, shadowDyPx)

                                when (val outline =
                                    dialogShape.createOutline(size, layoutDirection, this)) {
                                    is Outline.Rectangle -> {
                                        canvas.nativeCanvas.drawRect(
                                            outline.rect.left,
                                            outline.rect.top,
                                            outline.rect.right,
                                            outline.rect.bottom,
                                            shadowPaint
                                        )
                                    }

                                    is Outline.Rounded -> {
                                        canvas.nativeCanvas.drawRoundRect(
                                            outline.roundRect.left, outline.roundRect.top,
                                            outline.roundRect.right, outline.roundRect.bottom,
                                            outline.roundRect.bottomLeftCornerRadius.x,
                                            outline.roundRect.bottomLeftCornerRadius.y,
                                            shadowPaint
                                        )
                                    }

                                    is Outline.Generic -> {
                                        canvas.nativeCanvas.drawPath(
                                            outline.path.asAndroidPath(),
                                            shadowPaint
                                        )
                                    }
                                }

                                canvas.restore()
                            }
                        }
                    }
                    .graphicsLayer {
                        alpha = alphaAni.value
                    }
                    .background(color = AppColors.cardBackground, shape = AppSpecs.dialogShape)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlasenseButton(
                        enabled = true,
                        shape = CircleShape,
                        onClick = {
                            pendingThemePrimaryColor = currentThemePrimaryColor
                            showColorPicker = false
                        },
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
                        onClick = {
                            if (pendingThemePrimaryColor != currentThemePrimaryColor) {
                                settingsViewModel.onThemePrimaryColorChanged(
                                    pendingThemePrimaryColor
                                )
                            }
                            showColorPicker = false
                        },
                        modifier = Modifier
                            .width(48.dp)
                            .height(48.dp),
                        colors = AppButtonColors.primary()
                            .copy(containerColor = Color(pendingThemePrimaryColor))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .glasenseHighlight(48.dp),
                            contentAlignment = Alignment.Center
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
}
