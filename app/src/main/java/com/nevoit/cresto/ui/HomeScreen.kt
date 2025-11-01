package com.nevoit.cresto.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import com.nevoit.cresto.R
import com.nevoit.cresto.ui.components.DynamicSmallTitle
import com.nevoit.cresto.ui.components.PageHeader
import com.nevoit.cresto.ui.components.SwipeableTodoItem
import com.nevoit.cresto.ui.components.glasense.DialogItemData
import com.nevoit.cresto.ui.components.glasense.GlasenseButton
import com.nevoit.cresto.ui.components.glasense.GlasenseButtonAdaptable
import com.nevoit.cresto.ui.components.glasense.MenuItemData
import com.nevoit.cresto.ui.theme.glasense.Blue500
import com.nevoit.cresto.ui.theme.glasense.CalculatedColor
import com.nevoit.cresto.ui.theme.glasense.Red500
import com.nevoit.cresto.util.g2
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeApi::class)
@Composable
fun HomeScreen(
    showMenu: (anchorPosition: androidx.compose.ui.geometry.Offset, items: List<MenuItemData>) -> Unit,
    showDialog: (items: List<DialogItemData>, title: String, message: String?) -> Unit,
    viewModel: TodoViewModel
) {
    val scope = rememberCoroutineScope()


    val todoList by viewModel.allTodos.collectAsStateWithLifecycle()
    val revealedItemId by viewModel.revealedItemId.collectAsState()
    val selectedItemIds by viewModel.selectedItemIds.collectAsState()
    val isSelectionModeActive by viewModel.isSelectionModeActive.collectAsState()
    val selectedItemCount by viewModel.selectedItemCount.collectAsState()

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val density = LocalDensity.current
    val thresholdPx = if (statusBarHeight > 0.dp) {
        with(density) {
            (statusBarHeight + 24.dp).toPx()
        }
    } else 0f

    val hazeState = rememberHazeState()

    // val colorMode = if (MaterialTheme.colorScheme.background == Color.White) true else false

    val onSurfaceContainer = CalculatedColor.onSurfaceContainer

    val surfaceColor = CalculatedColor.hierarchicalBackgroundColor

    val lazyListState = rememberLazyListState()
    LaunchedEffect(lazyListState.isScrollInProgress, revealedItemId) {
        if (lazyListState.isScrollInProgress && revealedItemId != null) {
            viewModel.collapseRevealedItem()
        }
    }

    val isSmallTitleVisible by remember(thresholdPx) { derivedStateOf { ((lazyListState.firstVisibleItemIndex == 0) && (lazyListState.firstVisibleItemScrollOffset > thresholdPx)) || lazyListState.firstVisibleItemIndex > 0 } }
    val interactionSource = remember { MutableInteractionSource() }

    val menuItems = listOf(
        MenuItemData("Set Flag", painterResource(R.drawable.ic_flag), onClick = {}),
        MenuItemData(
            "Delete",
            painterResource(R.drawable.ic_trash),
            isDestructive = true,
            onClick = {})
    )

    val dialogItems = listOf(
        DialogItemData(
            "Cancel",
            onClick = {},
            isPrimary = false
        ),
        DialogItemData(
            "Delete",
            icon = painterResource(R.drawable.ic_trash),
            onClick = { viewModel.deleteSelectedItems() },
            isPrimary = true,
            isDestructive = true
        )
    )

    val menuItemsFilter = listOf(
        MenuItemData(
            "Default",
            painterResource(R.drawable.ic_rank),
            onClick = {}
        ),
        MenuItemData(
            "Due Date",
            painterResource(R.drawable.ic_calendar_alt),
            onClick = {}
        ),
        MenuItemData(
            "Flag",
            painterResource(R.drawable.ic_flag),
            onClick = {}
        ),
        MenuItemData(
            "Title",
            painterResource(R.drawable.ic_character),
            onClick = {}
        )
    )
    var isComposed by remember { mutableStateOf(isSelectionModeActive) }
    var isGone by remember { mutableStateOf(isSelectionModeActive) }
    val targetBlurRadius = 16f
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CalculatedColor.hierarchicalBackgroundColor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        viewModel.collapseRevealedItem()
                    }
                )
            }
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .hazeSource(hazeState, 0f)
                .fillMaxSize()
                .padding(0.dp)
                .background(CalculatedColor.hierarchicalBackgroundColor),
            contentPadding = PaddingValues(
                start = 12.dp,
                top = 0.dp,
                end = 12.dp,
                bottom = 136.dp
            )
        ) {
            item {
                PageHeader(title = "All Todos", statusBarHeight = statusBarHeight)
            }
            items(
                items = todoList,
                key = { it.id },
            ) { item ->
                val isSelected = item.id in selectedItemIds
                val alpha = remember { Animatable(if (isSelected) 1f else 0f) }
                LaunchedEffect(isSelected) {
                    if (isSelected) {
                        alpha.animateTo(1f, tween(100))
                    } else {
                        alpha.animateTo(0f, tween(100))
                    }
                }

                Column(
                    modifier = Modifier
                        .animateItem(placementSpec = spring(0.9f, 400f))
                        .combinedClickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onLongClick = {
                                if (!isSelectionModeActive) {
                                    scope.launch {
                                        viewModel.enterSelectionMode(item.id)
                                    }
                                } else {
                                    viewModel.toggleSelection(item.id)
                                }
                            },
                            onClick = {
                                if (isSelectionModeActive) {
                                    scope.launch {
                                        viewModel.toggleSelection(item.id)
                                    }
                                }
                            }
                        )
                ) {
                    Box {
                        SwipeableTodoItem(
                            item = item,
                            isRevealed = (item.id == revealedItemId),
                            onExpand = { viewModel.onItemExpanded(item.id) },
                            onCollapse = { viewModel.onItemCollapsed(item.id) },
                            onCheckedChange = { isChecked ->
                                viewModel.update(item.copy(isCompleted = isChecked))
                            },
                            onDeleteClick = { viewModel.delete(item) },
                            modifier = Modifier
                                .then(if (isComposed) Modifier.drawBehind {
                                    val outline =
                                        ContinuousRoundedRectangle(10.5.dp, g2).createOutline(
                                            size = Size(
                                                this.size.width - 3.dp.toPx(),
                                                this.size.height - 3.dp.toPx()
                                            ),
                                            layoutDirection = LayoutDirection.Ltr,
                                            density = density
                                        )
                                    translate(1.5.dp.toPx(), 1.5.dp.toPx()) {
                                        drawOutline(
                                            outline = outline,
                                            color = Blue500,
                                            alpha = alpha.value,
                                            style = Stroke(width = 3.dp.toPx()),
                                        )
                                    }
                                } else Modifier)
                        )
                        if (isSelectionModeActive) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .combinedClickable(
                                        interactionSource = interactionSource,
                                        indication = null,
                                        onClick = {
                                            scope.launch {
                                                viewModel.toggleSelection(item.id)
                                            }
                                        }
                                    )) {}
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
        DynamicSmallTitle(
            modifier = Modifier.align(Alignment.TopCenter),
            title = if (isComposed) "$selectedItemCount Selected" else "All Todos",
            statusBarHeight = statusBarHeight,
            isVisible = if (isSelectionModeActive) true else isSmallTitleVisible,
            hazeState = hazeState,
            surfaceColor = surfaceColor
        ) {
            var coordinatesCaptured by remember { mutableStateOf<LayoutCoordinates?>(null) }
            if (!isGone) {
                GlasenseButton(
                    enabled = true,
                    shape = ContinuousCapsule,
                    onClick = {},
                    modifier = Modifier
                        .blur(
                            targetBlurRadius.dp - topBarBlurAnimation.value.dp,
                            BlurredEdgeTreatment.Unbounded
                        )
                        .graphicsLayer {
                            alpha = 1 - topBarAlphaAnimation.value
                        }
                        .padding(top = statusBarHeight, start = 12.dp)
                        .align(Alignment.TopStart),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = onSurfaceContainer,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
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
                                contentDescription = "Search all todos",
                                modifier = Modifier.width(32.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .height(48.dp)
                                .width(48.dp)
                                .onGloballyPositioned { coordinates ->
                                    coordinatesCaptured = coordinates
                                }
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = {
                                            coordinatesCaptured?.let {
                                                val position = Offset(
                                                    x = it.positionOnScreen().x,
                                                    y = it.positionOnScreen().y + it.size.height + 8 * dpPx
                                                )
                                                showMenu(position, menuItemsFilter)
                                            }
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_funnel),
                                contentDescription = "Filter",
                                modifier = Modifier.width(32.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                GlasenseButton(
                    enabled = true,
                    shape = CircleShape,
                    onClick = { viewModel.showBottomSheet() },
                    modifier = Modifier
                        .blur(
                            targetBlurRadius.dp - topBarBlurAnimation.value.dp,
                            BlurredEdgeTreatment.Unbounded
                        )
                        .graphicsLayer {
                            alpha = 1 - topBarAlphaAnimation.value
                        }
                        .padding(top = statusBarHeight, end = 12.dp)
                        .size(48.dp)
                        .align(Alignment.TopEnd),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = onSurfaceContainer,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add_large),
                        contentDescription = "Add new todo",
                        modifier = Modifier.width(32.dp)
                    )
                }
            }
        }
        if (isComposed) {
            GlasenseButtonAdaptable(
                width = { 48.dp },
                height = { 48.dp },
                padding = PaddingValues(top = statusBarHeight, start = 12.dp),
                tint = Red500,
                enabled = true,
                shape = ContinuousCapsule,
                onClick = {
                    showDialog(
                        dialogItems,
                        "Delete $selectedItemCount ${if (selectedItemCount == 1) "todo" else ("todos")}?",
                        "This will delete selected ${if (selectedItemCount == 1) "todo" else ("todos")} permanently. This action cannot be undone."
                    )
                },
                modifier = Modifier
                    .blur(topBarBlurAnimation.value.dp, BlurredEdgeTreatment.Unbounded)
                    .graphicsLayer {
                        alpha = topBarAlphaAnimation.value
                    }
                    .align(Alignment.TopStart),
                colors = ButtonDefaults.buttonColors(
                    containerColor = onSurfaceContainer,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_trash),
                    contentDescription = "Delete selected todo(s)",
                    modifier = Modifier.width(32.dp)
                )
            }
            GlasenseButtonAdaptable(
                width = { 48.dp },
                height = { 48.dp },
                padding = PaddingValues(top = statusBarHeight, end = 12.dp),
                enabled = true,
                shape = CircleShape,
                onClick = { viewModel.clearSelections() },
                modifier = Modifier
                    .blur(
                        topBarBlurAnimation.value.dp,
                        BlurredEdgeTreatment.Unbounded
                    )
                    .graphicsLayer {
                        alpha = topBarAlphaAnimation.value
                    }
                    .align(Alignment.TopEnd),
                colors = ButtonDefaults.buttonColors(
                    containerColor = onSurfaceContainer,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_cross),
                    contentDescription = "Exit selection mode",
                    modifier = Modifier.width(32.dp)
                )
            }
        }
    }
}