package com.nevoit.cresto.feature.recentlydeleted

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.shapes.Capsule
import com.nevoit.cresto.R
import com.nevoit.cresto.navigation.rememberAppBack
import com.nevoit.cresto.data.todo.TodoViewModel
import com.nevoit.cresto.feature.home.TodoListSectionHead
import com.nevoit.cresto.theme.AppButtonColors
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.LocalThemeSettings
import com.nevoit.cresto.toolkit.gaussiangradient.smoothGradientMask
import com.nevoit.cresto.ui.components.glasense.DialogItemData
import com.nevoit.cresto.ui.components.glasense.DialogState
import com.nevoit.cresto.ui.components.glasense.GlasenseButtonAdaptable
import com.nevoit.cresto.ui.components.glasense.GlasenseButtonToolBar
import com.nevoit.cresto.ui.components.glasense.GlasenseDialog
import com.nevoit.cresto.ui.components.glasense.GlasenseDynamicSmallTitle
import com.nevoit.cresto.ui.components.glasense.isScrolledPast
import com.nevoit.cresto.ui.components.glasense.rememberSwipeableListState
import com.nevoit.cresto.ui.components.packed.PageContent
import com.nevoit.glasense.component.paddingItem
import com.nevoit.glasense.core.component.Icon
import com.nevoit.glasense.core.component.Text
import com.nevoit.glasense.core.component.VGap
import com.nevoit.glasense.theme.GlasenseTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Composable
fun RecentlyDeletedScreen(viewModel: TodoViewModel) {
    val onBack = rememberAppBack()
    val todos by viewModel.recentlyDeletedTodos.collectAsStateWithLifecycle()
    val selectedItemIds by viewModel.selectedItemIds.collectAsStateWithLifecycle()
    val selectedItemCount by viewModel.selectedItemCount.collectAsStateWithLifecycle()
    val isSelectionModeActive by viewModel.isSelectionModeActive.collectAsStateWithLifecycle()
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val swipeListState = rememberSwipeableListState()
    val selectionInteractionSource = remember { MutableInteractionSource() }
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val isSmallTitleVisible by listState.isScrolledPast(12.dp)
    val backgroundColor = AppColors.pageBackground
    var lastNonZeroSelected by remember { mutableIntStateOf(1) }
    if (selectedItemCount != 0) {
        lastNonZeroSelected = selectedItemCount
    }
    var isSelectionUiComposed by remember { mutableStateOf(isSelectionModeActive) }
    var isDefaultTopBarGone by remember { mutableStateOf(isSelectionModeActive) }
    val targetBlurRadius = with(density) { 16.dp.toPx() }
    val topBarAlphaAnimation = remember {
        Animatable(if (isSelectionModeActive) 1f else 0f)
    }
    val topBarBlurAnimation = remember {
        Animatable(if (isSelectionModeActive) 0f else targetBlurRadius)
    }
    val bottomBarAlphaAnimation = remember {
        Animatable(if (isSelectionModeActive) 1f else 0f)
    }
    val bottomBarBlurAnimation = remember {
        Animatable(if (isSelectionModeActive) 0f else targetBlurRadius)
    }
    val contentBottomPadding by animateDpAsState(
        targetValue = navigationBarHeight + if (isSelectionModeActive) 96.dp else 0.dp,
        animationSpec = tween(300),
        label = "recentlyDeletedSelectionBottomPadding"
    )
    val backdrop = rememberLayerBackdrop {
        drawRect(
            color = backgroundColor,
            size = Size(this.size.width * 3, this.size.height * 3),
            topLeft = Offset(-this.size.width, -this.size.height)
        )
        drawContent()
    }

    val floatingBarColor = AppColors.pageBackground.copy(.5f)
    val liquidGlass = LocalThemeSettings.current.liquidGlass
    val today = LocalDate.now()
    val todosByRemainingDays = remember(todos, today) {
        todos.groupBy { item ->
            recentlyDeletedDaysRemaining(
                deletedAt = requireNotNull(item.todoItem.deletedAt) {
                    "Recently deleted todo ${item.todoItem.id} must have a deletion time"
                },
                today = today
            )
        }.toSortedMap(reverseOrder())
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            swipeListState.close()
        }
    }
    LaunchedEffect(isSelectionModeActive) {
        if (!isSelectionModeActive) {
            swipeListState.close()
        }

        if (isSelectionModeActive) {
            isSelectionUiComposed = true
            scope.launch { topBarAlphaAnimation.animateTo(1f, tween(300)) }
            scope.launch { bottomBarAlphaAnimation.animateTo(1f, tween(300)) }
            scope.launch { bottomBarBlurAnimation.animateTo(0f, tween(300)) }
            topBarBlurAnimation.animateTo(0f, tween(300))
            isDefaultTopBarGone = true
        } else {
            isDefaultTopBarGone = false
            scope.launch { topBarAlphaAnimation.animateTo(0f, tween(300)) }
            scope.launch { bottomBarAlphaAnimation.animateTo(0f, tween(300)) }
            scope.launch { bottomBarBlurAnimation.animateTo(targetBlurRadius, tween(300)) }
            topBarBlurAnimation.animateTo(targetBlurRadius, tween(300))
            isSelectionUiComposed = false
        }
    }

    BackHandler(enabled = isSelectionModeActive) {
        viewModel.clearSelections()
    }

    var dialogState by remember { mutableStateOf(DialogState()) }
    val cancelText = stringResource(R.string.cancel)
    val deletePermanentlyText = stringResource(R.string.delete_permanently)
    val deletePermanentlyTitle = stringResource(R.string.delete_permanently_title)
    val deletePermanentlyMessage = stringResource(R.string.delete_permanently_message)
    val deleteSelectedPermanentlyTitle = pluralStringResource(
        R.plurals.delete_selected_permanently_title,
        selectedItemCount,
        selectedItemCount
    )
    val deleteSelectedPermanentlyMessage =
        stringResource(R.string.delete_selected_permanently_message)
    val deleteIcon = painterResource(R.drawable.ic_trash)
    val requestPermanentDelete: (Int) -> Unit = { todoId ->
        dialogState = DialogState(
            isVisible = true,
            items = listOf(
                DialogItemData(
                    text = cancelText,
                    onClick = {},
                    isPrimary = false
                ),
                DialogItemData(
                    text = deletePermanentlyText,
                    icon = deleteIcon,
                    onClick = { viewModel.deletePermanentlyById(todoId) },
                    isPrimary = true,
                    isDestructive = true
                )
            ),
            title = deletePermanentlyTitle,
            message = deletePermanentlyMessage
        )
    }
    val requestSelectedPermanentDelete: () -> Unit = {
        dialogState = DialogState(
            isVisible = true,
            items = listOf(
                DialogItemData(
                    text = cancelText,
                    onClick = {},
                    isPrimary = false
                ),
                DialogItemData(
                    text = deletePermanentlyText,
                    icon = deleteIcon,
                    onClick = viewModel::deleteSelectedItemsPermanently,
                    isPrimary = true,
                    isDestructive = true
                )
            ),
            title = deleteSelectedPermanentlyTitle,
            message = deleteSelectedPermanentlyMessage
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        PageContent(
            state = listState,
            modifier = Modifier.layerBackdrop(backdrop),
            tabPadding = false,
            bottomPadding = contentBottomPadding
        ) {
            item(key = "header") {
                Box(
                    modifier = Modifier
                        .statusBarsPadding()
                        .height(60.dp)
                )
            }
            item(key = "tip") {
                Text(
                    text = stringResource(R.string.recently_deleted_tip),
                    style = GlasenseTheme.type.subHeadline,
                    modifier = Modifier.padding(horizontal = 12.dp),
                    color = GlasenseTheme.colors.contentVariant
                )
                VGap()
            }
            todosByRemainingDays.forEach { (daysRemaining, groupedTodos) ->
                item(key = "days_remaining_$daysRemaining") {
                    TodoListSectionHead(
                        title = pluralStringResource(
                            R.plurals.recently_deleted_days_remaining,
                            daysRemaining,
                            daysRemaining
                        ),
                        showExpandIcon = false,
                        horizontalPadding = false
                    )
                }
                itemsIndexed(
                    items = groupedTodos,
                    key = { _, item -> item.todoItem.id }
                ) { _, item ->
                    RecentlyDeletedTodoListItemRow(
                        item = item,
                        isSelected = item.todoItem.id in selectedItemIds,
                        isSelectionModeActive = isSelectionModeActive,
                        overlayInteractionSource = selectionInteractionSource,
                        swipeListState = swipeListState,
                        onEnterSelection = {
                            viewModel.enterSelectionMode(item.todoItem.id)
                        },
                        onToggleSelection = {
                            viewModel.toggleSelection(item.todoItem.id)
                        },
                        onRestore = { viewModel.restoreById(item.todoItem.id) },
                        onDeletePermanently = {
                            requestPermanentDelete(item.todoItem.id)
                        }
                    )
                    VGap()
                }
            }
            paddingItem(listState)
        }

        val resolvedTitle = if (isSelectionModeActive) {
            stringResource(R.string.selected_todos, lastNonZeroSelected)
        } else {
            stringResource(R.string.recently_deleted)
        }

        GlasenseDynamicSmallTitle(
            modifier = Modifier.align(Alignment.TopCenter),
            statusBarHeight = statusBarHeight,
            isVisible = isSmallTitleVisible,
            backdrop = backdrop
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = resolvedTitle,
                    style = GlasenseTheme.type.headline.copy(fontFeatureSettings = "tnum"),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.recently_deleted_item_count,
                        todos.size,
                        todos.size
                    ),
                    style = GlasenseTheme.type.footnote.copy(fontFeatureSettings = "tnum"),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = GlasenseTheme.colors.contentVariant
                )
            }
        }
        AnimatedVisibility(
            visible = isSelectionModeActive,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(120.dp + navigationBarHeight)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .smoothGradientMask(
                        backgroundColor,
                        0f,
                        0.5f,
                        0.7f
                    )
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 12.dp)
        ) {
            if (!isDefaultTopBarGone) {
                GlasenseButtonToolBar(
                    enabled = true,
                    shape = CircleShape,
                    onClick = onBack,
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = 1f - topBarAlphaAnimation.value
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

            if (isSelectionUiComposed) {
                GlasenseButtonAdaptable(
                    width = { 48.dp },
                    height = { 48.dp },
                    enabled = true,
                    shape = CircleShape,
                    onClick = viewModel::clearSelections,
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
                    onClick = {
                        viewModel.toggleSelectAllItems(todos.map { it.todoItem.id })
                    },
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
        if (isSelectionUiComposed) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 16.dp)
                    .height(48.dp)
                    .graphicsLayer {
                        alpha = bottomBarAlphaAnimation.value
                        renderEffect = if (bottomBarBlurAnimation.value > 0f) {
                            BlurEffect(
                                radiusX = bottomBarBlurAnimation.value,
                                radiusY = bottomBarBlurAnimation.value,
                                edgeTreatment = TileMode.Decal
                            )
                        } else {
                            null
                        }
                    },
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                GlasenseButtonAdaptable(
                    width = { 48.dp },
                    height = { 48.dp },
                    tint = AppColors.primary,
                    enabled = true,
                    shape = CircleShape,
                    onClick = viewModel::restoreSelectedItems,
                    modifier = Modifier
                        .size(48.dp)
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { Capsule() },
                            shadow = null,
                            innerShadow = null,
                            highlight = {
                                if (liquidGlass) Highlight.Default.copy(
                                    style = HighlightStyle.Default(
                                        angle = 90f
                                    )
                                ) else null
                            },
                            effects = {
                                blur(
                                    if (liquidGlass) 8f.dp.toPx() else 32f.dp.toPx(),
                                    TileMode.Decal
                                )
                                if (liquidGlass) lens(16f.dp.toPx(), 48f.dp.toPx())
                            },
                            onDrawSurface = {
                                drawRect(color = floatingBarColor)
                            }
                        ),
                    colors = AppButtonColors.action()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_counterclockwise),
                        contentDescription = stringResource(R.string.restore_selected_todos),
                        modifier = Modifier.width(32.dp)
                    )
                }

                GlasenseButtonAdaptable(
                    width = { 48.dp },
                    height = { 48.dp },
                    tint = AppColors.error,
                    enabled = true,
                    shape = CircleShape,
                    onClick = requestSelectedPermanentDelete,
                    modifier = Modifier
                        .size(48.dp)
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { Capsule() },
                            shadow = null,
                            innerShadow = null,
                            highlight = {
                                if (liquidGlass) Highlight.Default.copy(
                                    style = HighlightStyle.Default(
                                        angle = 90f
                                    )
                                ) else null
                            },
                            effects = {
                                blur(
                                    if (liquidGlass) 8f.dp.toPx() else 32f.dp.toPx(),
                                    TileMode.Decal
                                )
                                if (liquidGlass) lens(16f.dp.toPx(), 48f.dp.toPx())
                            },
                            onDrawSurface = {
                                drawRect(color = floatingBarColor)
                            }
                        ),
                    colors = AppButtonColors.action()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_trash),
                        contentDescription = stringResource(
                            R.string.delete_selected_permanently
                        ),
                        modifier = Modifier.width(32.dp)
                    )
                }
            }
        }

        GlasenseDialog(
            dialogState = dialogState,
            backdrop = backdrop,
            onDismiss = { dialogState = dialogState.copy(isVisible = false) }
        )
    }
}

internal fun recentlyDeletedDaysRemaining(
    deletedAt: LocalDateTime,
    today: LocalDate
): Int = ChronoUnit.DAYS.between(
    today,
    deletedAt.toLocalDate().plusDays(RECENTLY_DELETED_RETENTION_DAYS)
).toInt().coerceIn(0, RECENTLY_DELETED_RETENTION_DAYS.toInt())

private const val RECENTLY_DELETED_RETENTION_DAYS = 30L
