package com.nevoit.cresto.ui.components.glasense

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.AppSpecs
import com.nevoit.cresto.ui.components.packed.rememberCupertinoDecaySpec
import com.nevoit.cresto.ui.components.packed.rememberGlasenseSnapFlingBehavior
import com.nevoit.glasense.theme.Springs
import kotlin.math.absoluteValue
import kotlin.math.sin

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlasenseWheelPicker(
    modifier: Modifier = Modifier,
    items: List<String>,
    currentSelected: Int = 0,
    visibleItemsCount: Int = 7,
    itemHeight: Dp = 40.dp,
    textStyle: TextStyle = LocalTextStyle.current,
    indicator: Boolean = true,
    onItemSelected: (Int) -> Unit
) {
    val density = LocalDensity.current
    val hapticController = LocalHapticFeedback.current
    var hasEmittedInitialSelection by remember { mutableStateOf(false) }
    val visibleCount = if (visibleItemsCount % 2 == 0) visibleItemsCount + 1 else visibleItemsCount
    val halfCount = visibleCount / 2
    val boundedCurrentSelected = if (items.isEmpty()) {
        0
    } else {
        currentSelected.coerceIn(0, items.lastIndex)
    }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = boundedCurrentSelected)

    val snapLayoutInfoProvider = remember(listState) {
        SnapLayoutInfoProvider(lazyListState = listState)
    }

    val decayAnimationSpec = rememberCupertinoDecaySpec()

    val customSnapAnimationSpec = remember { Springs.smooth<Float>() }

    val flingBehavior = rememberGlasenseSnapFlingBehavior(
        snapLayoutInfoProvider = snapLayoutInfoProvider,
        decayAnimationSpec = decayAnimationSpec,
        snapAnimationSpec = customSnapAnimationSpec,
    )

    val itemHeightPx = with(density) { itemHeight.toPx() }
    val maxRotationDeg = 90f
    val maxAngleRad = Math.toRadians(maxRotationDeg.toDouble()).toFloat()
    val geometricHalfCount = halfCount + 1
    val wheelExtentPx = (geometricHalfCount * itemHeightPx).coerceAtLeast(1f)
    val radiusPx = wheelExtentPx / maxAngleRad

    fun distanceToAngleRad(distanceToCenter: Float): Float {
        val normalized = (distanceToCenter / wheelExtentPx).coerceIn(-1f, 1f)
        return normalized * maxAngleRad
    }

    fun mapToWheelY(distanceToCenter: Float): Float {
        val angle = distanceToAngleRad(distanceToCenter)
        return sin(angle) * radiusPx
    }

    val visibleHalfLinearPx = halfCount * itemHeightPx
    val visibleHalfArcPx = mapToWheelY(visibleHalfLinearPx).absoluteValue
    val wheelContainerHeight = with(density) {
        (itemHeightPx + (visibleHalfArcPx * 2f)).toDp()
    }

    val centeredIndex by remember(listState, items) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            if (layoutInfo.visibleItemsInfo.isEmpty()) return@derivedStateOf boundedCurrentSelected
            val viewportCenter =
                (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
            layoutInfo.visibleItemsInfo
                .minByOrNull { visible ->
                    val itemCenter = visible.offset + visible.size / 2f
                    mapToWheelY(itemCenter - viewportCenter).absoluteValue
                }
                ?.index
                ?.let { index ->
                    if (items.isEmpty()) 0 else index.coerceIn(0, items.lastIndex)
                }
                ?: boundedCurrentSelected
        }
    }

    LaunchedEffect(centeredIndex) {
        if (hasEmittedInitialSelection) {
            onItemSelected(centeredIndex)
            hapticController.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        } else {
            hasEmittedInitialSelection = true
        }
    }


    LaunchedEffect(currentSelected, items.size) {
        if (items.isEmpty()) return@LaunchedEffect
        val targetIndex = currentSelected.coerceIn(0, items.lastIndex)
        if (targetIndex != centeredIndex && !listState.isScrollInProgress) {
            listState.animateScrollToItem(targetIndex)
        }
    }

    val shape = AppSpecs.cardShape
    val color = AppColors.scrimNormal

    Box(
        modifier = modifier
            .height(wheelContainerHeight)
            .clipToBounds()
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        if (indicator) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .drawWithContent {
                        drawContent()
                        val outline = shape.createOutline(size, layoutDirection, this)
                        drawOutline(outline, color)
                    }
            )
        }

        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.requiredHeight(visibleCount * itemHeight),
            contentPadding = PaddingValues(vertical = itemHeight * halfCount)
        ) {
            items(items.size) { index ->
                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth()
                        .graphicsLayer {
                            val layoutInfo = listState.layoutInfo
                            val visibleInfo = layoutInfo.visibleItemsInfo.find { it.index == index }

                            if (visibleInfo != null) {
                                val itemCenterY = visibleInfo.offset + (visibleInfo.size / 2f)
                                val viewportCenterY =
                                    (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
                                val distanceToCenter = itemCenterY - viewportCenterY

                                val angleRad = distanceToAngleRad(distanceToCenter)
                                val wheelY = mapToWheelY(distanceToCenter)
                                val falloff = (angleRad / maxAngleRad).absoluteValue

                                translationY = wheelY - distanceToCenter
                                rotationX = -Math.toDegrees(angleRad.toDouble()).toFloat()

                                scaleX = 1f - (falloff * 0.12f)
                                scaleY = 1f - (falloff * 0.12f)
                                alpha = 1f - falloff
                                cameraDistance = 4f * density.density
                            } else {
                                alpha = 0f
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val isSelected = centeredIndex == index
                    Text(
                        text = items[index],
                        style = textStyle,
                        textAlign = TextAlign.Center,
                        color = if (isSelected) AppColors.content else AppColors.contentVariant
                    )
                }
            }
        }
    }
}