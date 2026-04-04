package com.nevoit.cresto.feature.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.kyant.shapes.Capsule
import com.nevoit.cresto.R
import com.nevoit.cresto.data.todo.TodoViewModel
import com.nevoit.cresto.theme.AppButtonColors
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.ui.components.glasense.GlasenseButton
import com.nevoit.cresto.ui.components.glasense.GlasenseButtonAdaptable
import com.nevoit.cresto.ui.components.glasense.GlasenseDynamicSmallTitle
import com.nevoit.cresto.ui.components.glasense.GlasenseMenuItem
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch

@Composable
fun BoxScope.HomeTopAppBar(
    menuController: (anchorPosition: Offset, items: List<GlasenseMenuItem>) -> Unit,
    menuItems: List<GlasenseMenuItem>,
    isTitleVisible: Boolean,
    hazeState: HazeState,
    viewModel: TodoViewModel
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val selectedItemCount by viewModel.selectedItemCount.collectAsState()
    val isSelectionModeActive by viewModel.isSelectionModeActive.collectAsState()
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
    val dpPx = with(density) { 1.dp.toPx() }
    val resolvedTitle = if (isTitleVisible) {
        if (isSelectionModeActive) stringResource(
            R.string.selected_todos,
            lastNonZeroSelected
        ) else stringResource(R.string.all_todos)
    } else if (isComposed) stringResource(
        R.string.selected_todos,
        lastNonZeroSelected
    ) else stringResource(R.string.all_todos)

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
                GlasenseButton(
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
                            .height(48.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .height(48.dp)
                                .width(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_magnifying_glass),
                                contentDescription = stringResource(R.string.search_all_todos),
                                modifier = Modifier.width(32.dp),
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
                                        val position = Offset(
                                            x = it.positionInWindow().x,
                                            y = it.positionInWindow().y + it.boundsInWindow().height + 8 * dpPx,
                                        )
                                        menuController(position, menuItems)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_funnel),
                                contentDescription = stringResource(R.string.filter),
                                modifier = Modifier.width(32.dp),
                                tint = AppColors.primary
                            )
                        }
                    }
                }
                GlasenseButton(
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
                    colors = AppButtonColors.action()
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.scrimNormal,
                        contentColor = AppColors.primary
                    )
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.scrimNormal,
                        contentColor = AppColors.primary
                    )
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
}