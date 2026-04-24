package com.nevoit.cresto.feature.home

import android.graphics.BlurMaskFilter
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.nativePaint
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.shapes.Capsule
import com.nevoit.cresto.R
import com.nevoit.cresto.data.todo.TodoViewModel
import com.nevoit.cresto.theme.AppButtonColors
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.isAppInDarkTheme
import com.nevoit.cresto.ui.components.glasense.GlasenseButtonAdaptable
import com.nevoit.cresto.ui.components.glasense.GlasenseButtonToolBar
import com.nevoit.cresto.ui.components.glasense.GlasenseDynamicSmallTitle
import com.nevoit.cresto.ui.components.glasense.GlasenseMenuItem
import com.nevoit.cresto.ui.components.glasense.glasenseHighlight
import com.nevoit.glasense.modifier.OverlayWeight
import com.nevoit.glasense.modifier.glasenseOverlay
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.launch

@Composable
fun BoxScope.HomeTopAppBar(
    menuController: (anchorBounds: Rect, items: List<GlasenseMenuItem>) -> Unit,
    menuItems: List<GlasenseMenuItem>,
    isTitleVisible: Boolean,
    hazeState: HazeState,
    viewModel: TodoViewModel
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val selectedItemCount by viewModel.selectedItemCount.collectAsState()
    val isSelectionModeActive by viewModel.isSelectionModeActive.collectAsState()
    val isSearchBoxOpen by viewModel.isSearchBoxOpen.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var lastNonZeroSelected by remember { mutableIntStateOf(1) }

    if (selectedItemCount != 0) {
        lastNonZeroSelected = selectedItemCount
    }
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    var isComposed by remember { mutableStateOf(isSelectionModeActive) }
    var isGone by remember { mutableStateOf(isSelectionModeActive) }
    val targetBlurRadius = with(density) {
        16.dp.toPx()
    }
    val topBarAlphaAnimation = remember { Animatable(if (isSelectionModeActive) 1f else 0f) }

    val topBarBlurAnimation =
        remember { Animatable(if (isSelectionModeActive) 0f else targetBlurRadius) }

    LaunchedEffect(isSelectionModeActive) {
        if (isSelectionModeActive) {
            isComposed = true
            scope.launch { topBarAlphaAnimation.animateTo(1f, tween(300)) }
            topBarBlurAnimation.animateTo(0f, tween(300))
            isGone = true
        } else {
            isGone = false
            scope.launch { topBarAlphaAnimation.animateTo(0f, tween(300)) }
            topBarBlurAnimation.animateTo(targetBlurRadius, tween(300))
            isComposed = false
        }
    }
    val resolvedTitle = if (isTitleVisible) {
        if (isSelectionModeActive) stringResource(
            R.string.selected_todos,
            lastNonZeroSelected
        ) else stringResource(R.string.all_todos)
    } else if (isComposed) stringResource(
        R.string.selected_todos,
        lastNonZeroSelected
    ) else stringResource(R.string.all_todos)

    val darkTheme = isAppInDarkTheme()

    val shadowRadiusPx = with(LocalDensity.current) { 32.dp.toPx() }
    val shadowDyPx = with(LocalDensity.current) { 16.dp.toPx() }

    val shadowPaint = remember {
        Paint().nativePaint.apply {
            isAntiAlias = true
            maskFilter = BlurMaskFilter(shadowRadiusPx, BlurMaskFilter.Blur.NORMAL)
        }
    }
    val shadowBaseColor = if (darkTheme) Color.Black.copy(alpha = 0.6f) else Color.Black.copy(
        alpha = 0.1f
    )

    val alphaAni by animateFloatAsState(
        targetValue = if (isTitleVisible) 1f else 0f,
        animationSpec = tween(300)
    )

    val hapticController = LocalHapticFeedback.current
    var isSearchBoxComposed by remember { mutableStateOf(false) }
    val searchBoxAlphaAnimation = remember { Animatable(if (isSearchBoxOpen) 1f else 0f) }
    val searchIconWidthAnimation = remember { Animatable(if (isSearchBoxOpen) 0f else 1f) }

    val searchBoxBlurAnimation =
        remember { Animatable(if (isSearchBoxOpen) 0f else targetBlurRadius) }

    LaunchedEffect(isSearchBoxOpen) {
        if (isSearchBoxOpen) {
            isSearchBoxComposed = true
            hapticController.performHapticFeedback(HapticFeedbackType.ContextClick)
            scope.launch { searchBoxAlphaAnimation.animateTo(1f, tween(300)) }
            scope.launch { searchIconWidthAnimation.animateTo(0f, tween(300)) }
            searchBoxBlurAnimation.animateTo(0f, tween(300))
        } else {
            scope.launch { searchBoxAlphaAnimation.animateTo(0f, tween(300)) }
            scope.launch { searchIconWidthAnimation.animateTo(1f, spring(0.8f, 400f)) }
            searchBoxBlurAnimation.animateTo(targetBlurRadius, tween(300))
            isSearchBoxComposed = false
        }
    }

    GlasenseDynamicSmallTitle(
        modifier = Modifier.align(Alignment.TopCenter),
        title = resolvedTitle,
        textStyle = TextStyle(fontFeatureSettings = "tnum"),
        statusBarHeight = statusBarHeight,
        isVisible = if (isSelectionModeActive) true else isTitleVisible,
        hazeState = hazeState,
        surfaceColor = AppColors.pageBackground
    ) {
        var coordinatesCaptured by remember { mutableStateOf<LayoutCoordinates?>(null) }
        val sharedInteractionSource = remember { MutableInteractionSource() }

        Box(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            if (!isGone) {
                GlasenseButtonToolBar(
                    enabled = true,
                    interactionSource = sharedInteractionSource,
                    shape = Capsule(),
                    onClick = {},
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = 1 - topBarAlphaAnimation.value
                            val blurRadius = targetBlurRadius - topBarBlurAnimation.value
                            renderEffect = if (blurRadius > 0f) {
                                BlurEffect(
                                    radiusX = blurRadius,
                                    radiusY = blurRadius,
                                    edgeTreatment = TileMode.Decal
                                )
                            } else {
                                null
                            }
                        }
                        .align(Alignment.TopStart),
                    colors = AppButtonColors.action()
                ) {
                    Row(
                        modifier = Modifier
                            .height(48.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .height(48.dp)
                                .width(48.dp * searchIconWidthAnimation.value)
                                .wrapContentSize(
                                    unbounded = true,
                                    align = Alignment.CenterEnd
                                )
                                .clickable(
                                    interactionSource = sharedInteractionSource,
                                    indication = null
                                ) {
                                    viewModel.openSearchBox()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_magnifying_glass),
                                contentDescription = stringResource(R.string.search_all_todos),
                                modifier = Modifier
                                    .padding(8.dp)
                                    .graphicsLayer {
                                        alpha = 1 - searchBoxAlphaAnimation.value
                                        val blurRadius =
                                            targetBlurRadius - searchBoxBlurAnimation.value
                                        renderEffect = if (blurRadius > 0f) {
                                            BlurEffect(
                                                radiusX = blurRadius,
                                                radiusY = blurRadius,
                                                edgeTreatment = TileMode.Decal
                                            )
                                        } else {
                                            null
                                        }
                                    }
                                    .width(32.dp),
                                tint = AppColors.primary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .height(48.dp)
                                .width(48.dp)
                                .onGloballyPositioned { coordinates ->
                                    coordinatesCaptured = coordinates
                                }
                                .clickable(
                                    interactionSource = sharedInteractionSource,
                                    indication = null
                                ) {
                                    coordinatesCaptured?.let {
                                        menuController(it.boundsInWindow(), menuItems)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_sort),
                                contentDescription = stringResource(R.string.sort),
                                modifier = Modifier.width(32.dp),
                                tint = AppColors.primary
                            )
                        }
                    }
                }
                GlasenseButtonAdaptable(
                    enabled = true,
                    shape = CircleShape,
                    onClick = { viewModel.showBottomSheet() },
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = 1 - topBarAlphaAnimation.value
                            val blurRadius = targetBlurRadius - topBarBlurAnimation.value
                            renderEffect = if (blurRadius > 0f) {
                                BlurEffect(
                                    radiusX = blurRadius,
                                    radiusY = blurRadius,
                                    edgeTreatment = TileMode.Decal
                                )
                            } else {
                                null
                            }
                        }
                        .size(48.dp)
                        .align(Alignment.TopEnd),
                    colors = AppButtonColors.action(),
                    width = { 48.dp },
                    height = { 48.dp }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add_large),
                        contentDescription = stringResource(R.string.add_new_todo),
                        modifier = Modifier.width(32.dp)
                    )
                }
            }
            if (isComposed) {
                GlasenseButtonAdaptable(
                    width = { 48.dp },
                    height = { 48.dp },
                    enabled = true,
                    shape = CircleShape,
                    onClick = { viewModel.clearSelections() },
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = topBarAlphaAnimation.value
                            renderEffect = if (topBarBlurAnimation.value > 0f) {
                                BlurEffect(
                                    radiusX = topBarBlurAnimation.value,
                                    radiusY = topBarBlurAnimation.value,
                                    edgeTreatment = TileMode.Decal
                                )
                            } else {
                                null
                            }
                        }
                        .align(Alignment.TopStart),
                    colors = AppButtonColors.action()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_cross),
                        contentDescription = stringResource(R.string.exit_selection_mode),
                        modifier = Modifier.width(32.dp)
                    )
                }
                GlasenseButtonAdaptable(
                    width = { 48.dp },
                    height = { 48.dp },
                    enabled = true,
                    shape = CircleShape,
                    onClick = { viewModel.toggleSelectAllItems() },
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = topBarAlphaAnimation.value
                            renderEffect = if (topBarBlurAnimation.value > 0f) {
                                BlurEffect(
                                    radiusX = topBarBlurAnimation.value,
                                    radiusY = topBarBlurAnimation.value,
                                    edgeTreatment = TileMode.Decal
                                )
                            } else {
                                null
                            }
                        }
                        .align(Alignment.TopEnd),
                    colors = AppButtonColors.action()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_square_dashed),
                        contentDescription = stringResource(R.string.select_all),
                        modifier = Modifier.width(32.dp)
                    )
                }
            }
        }
    }

    val searchFocusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    if (isSearchBoxComposed) {
        BackHandler { viewModel.onSearchCloseIconClick() }
        Box(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .statusBarsPadding()
                .padding(top = 48.dp + 12.dp)
                .fillMaxWidth()
                .height(48.dp)
                .drawBehind {
                    val finalAlpha = alphaAni * searchBoxAlphaAnimation.value
                    if (finalAlpha > 0f) {
                        val paintColor =
                            shadowBaseColor.copy(alpha = shadowBaseColor.alpha * finalAlpha)
                        shadowPaint.color = paintColor.toArgb()

                        drawIntoCanvas { canvas ->
                            canvas.save()
                            canvas.translate(0f, shadowDyPx)
                            canvas.nativeCanvas.drawRoundRect(
                                0f,
                                0f,
                                size.width,
                                size.height,
                                size.height / 2,
                                size.height / 2,
                                shadowPaint
                            )
                            canvas.restore()
                        }
                    }
                }
                .graphicsLayer {
                    if (searchBoxBlurAnimation.value > 0f) {
                        renderEffect = BlurEffect(
                            radiusX = searchBoxBlurAnimation.value,
                            radiusY = searchBoxBlurAnimation.value,
                            edgeTreatment = TileMode.Decal
                        )
                    }
                }
                .clip(Capsule())
                .graphicsLayer {
                    alpha = searchBoxAlphaAnimation.value
                }
                .hazeEffect(
                    hazeState,
                    HazeStyle(tint = null, backgroundColor = AppColors.pageBackground)
                ) {
                    blurRadius = 32.dp
                    noiseFactor = .1f
                }
                .glasenseOverlay(dark = isAppInDarkTheme(), weight = OverlayWeight.Normal)
        ) {
            Box(
                modifier = Modifier
                    .glasenseHighlight(cornerRadius = 100.dp)
                    .fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        alpha = 1 - alphaAni
                    }
                    .background(color = AppColors.scrimNormal.compositeOver(AppColors.pageBackground))
                    .fillMaxSize()
            )
            Icon(
                painter = painterResource(R.drawable.ic_magnifying_glass),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(10.dp)
                    .size(28.dp),
                tint = AppColors.contentVariant
            )
            LaunchedEffect(Unit) {
                searchFocusRequester.requestFocus()
            }
            BasicTextField(
                value = searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                modifier = Modifier
                    .focusRequester(searchFocusRequester)
                    .height(48.dp)
                    .padding(start = 44.dp, end = 42.dp)
                    .fillMaxWidth(),
                cursorBrush = SolidColor(AppColors.primary),
                textStyle = TextStyle(
                    color = AppColors.content,
                    fontSize = 16.sp
                ),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = stringResource(R.string.search_all_todos),
                                color = AppColors.contentVariant,
                                fontSize = 16.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )
            Icon(
                painter = painterResource(R.drawable.ic_xmark_bold_circle_fill),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clickable(
                        enabled = true,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (isSearchBoxOpen) {
                            keyboard?.hide()
                        }
                        viewModel.onSearchCloseIconClick()
                    }
                    .padding(14.dp)
                    .size(20.dp),
                tint = AppColors.content.copy(alpha = 0.4f)
            )
        }
    }
}