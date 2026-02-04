package com.nevoit.cresto.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import com.nevoit.cresto.R
import com.nevoit.cresto.data.todo.EXTRA_TODO_ID
import com.nevoit.cresto.data.todo.TodoViewModel
import com.nevoit.cresto.ui.components.glasense.DialogItemData
import com.nevoit.cresto.ui.components.glasense.DimIndication
import com.nevoit.cresto.ui.components.glasense.GlasenseButton
import com.nevoit.cresto.ui.components.glasense.GlasenseButtonAdaptable
import com.nevoit.cresto.ui.components.glasense.GlasenseDynamicSmallTitle
import com.nevoit.cresto.ui.components.glasense.GlasensePageHeader
import com.nevoit.cresto.ui.components.glasense.MenuItemData
import com.nevoit.cresto.ui.components.glasense.extend.overscrollSpacer
import com.nevoit.cresto.ui.components.glasense.isScrolledPast
import com.nevoit.cresto.ui.components.glasense.rememberSwipeableListState
import com.nevoit.cresto.ui.components.packed.PageContent
import com.nevoit.cresto.ui.components.packed.SwipeableTodoItem
import com.nevoit.cresto.ui.screens.detailscreen.DetailActivity
import com.nevoit.cresto.ui.theme.glasense.AppColors
import com.nevoit.cresto.ui.theme.glasense.AppSpecs
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeApi::class)
@Composable
fun BoxScope.HomeScreen(
    showMenu: (anchorPosition: Offset, items: List<MenuItemData>) -> Unit,
    showDialog: (items: List<DialogItemData>, title: String, message: String?) -> Unit,
    viewModel: TodoViewModel
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val todoList by viewModel.allTodos.collectAsStateWithLifecycle()
    val revealedItemId by viewModel.revealedItemId.collectAsState()
    val selectedItemIds by viewModel.selectedItemIds.collectAsState()
    val isSelectionModeActive by viewModel.isSelectionModeActive.collectAsState()
    val selectedItemCount by viewModel.selectedItemCount.collectAsState()


    var lastNonZeroSelected by remember { mutableIntStateOf(1) }

    if (selectedItemCount != 0) {
        lastNonZeroSelected = selectedItemCount
    }

    val title = pluralStringResource(
        id = R.plurals.delete_todo_dialog_title,
        count = selectedItemCount,
        selectedItemCount
    )

    val message = pluralStringResource(
        id = R.plurals.delete_todo_dialog_msg,
        count = selectedItemCount
    )

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val density = LocalDensity.current

    val hazeState = rememberHazeState()

    // val colorMode = if (MaterialTheme.colorScheme.background == Color.White) true else false

    val onSurfaceContainer = AppColors.scrimNormal

    val surfaceColor = AppColors.pageBackground

    val lazyListState = rememberLazyListState()
    LaunchedEffect(lazyListState.isScrollInProgress, revealedItemId) {
        if (lazyListState.isScrollInProgress && revealedItemId != null) {
            viewModel.collapseRevealedItem()
        }
    }
    val swipeListState = rememberSwipeableListState()

    val isSmallTitleVisible by lazyListState.isScrolledPast(statusBarHeight + 24.dp)
    val interactionSource = remember { MutableInteractionSource() }

    val cancelText = stringResource(R.string.cancel)
    val deleteText = stringResource(R.string.delete)
    val deleteIcon = painterResource(R.drawable.ic_trash)

    val dialogItems = remember(cancelText, deleteText, deleteIcon, viewModel) {
        listOf(
            DialogItemData(
                text = cancelText,
                onClick = {},
                isPrimary = false
            ),
            DialogItemData(
                text = deleteText,
                icon = deleteIcon,
                onClick = { viewModel.deleteSelectedItems() },
                isPrimary = true,
                isDestructive = true
            )
        )
    }

    val defaultText = stringResource(R.string.filter_default)
    val dueDateText = stringResource(R.string.due_date)
    val flagText = stringResource(R.string.flag)
    val titleText = stringResource(R.string.title)

    val rankIcon = painterResource(R.drawable.ic_rank)
    val calendarAltIcon = painterResource(R.drawable.ic_calendar_alt)
    val flagIcon = painterResource(R.drawable.ic_flag)
    val characterIcon = painterResource(R.drawable.ic_character)

    val menuItemsFilter = remember(
        defaultText,
        rankIcon,
        calendarAltIcon,
        flagIcon,
        characterIcon,
        dueDateText,
        flagText,
        titleText
    ) {
        listOf(
            MenuItemData(
                defaultText,
                rankIcon,
                onClick = {}
            ),
            MenuItemData(
                dueDateText,
                calendarAltIcon,
                onClick = {}
            ),
            MenuItemData(
                flagText,
                flagIcon,
                onClick = {}
            ),
            MenuItemData(
                titleText,
                characterIcon,
                onClick = {}
            )
        )
    }
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

    val (incompleteTodos, completeTodos) = todoList.partition { !it.todoItem.isCompleted }
    var completedVisible by remember { mutableStateOf(true) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val deleteId = result.data?.getIntExtra("extra_delete_id", -1) ?: -1
            if (deleteId != -1) {
                scope.launch {
                    delay(300)
                    viewModel.deleteById(deleteId)
                }
            }
        }
    }
    if (isSelectionModeActive) {
        BackHandler { viewModel.clearSelections() }
    }

    val selectionOutline = AppColors.primary
    val cardCorner = AppSpecs.cardCorner

    PageContent(
        state = lazyListState,
        modifier = Modifier
            .hazeSource(hazeState, 0f),
        tabPadding = true
    ) {
        item {
            GlasensePageHeader(
                title = stringResource(R.string.all_todos)
            )
        }
        items(
            items = incompleteTodos,
            key = { it.todoItem.id },
        ) { item ->
            val isSelected = item.todoItem.id in selectedItemIds
            val alpha = remember { Animatable(if (isSelected) 1f else 0f) }

            var isChecked by remember(item.todoItem.id) { mutableStateOf(item.todoItem.isCompleted) }

            val displayItem = remember(item, isChecked) {
                item.copy(todoItem = item.todoItem.copy(isCompleted = isChecked))
            }

            LaunchedEffect(isSelected) {
                if (isSelected) {
                    alpha.animateTo(1f, tween(100))
                } else {
                    alpha.animateTo(0f, tween(100))
                }
            }
            Box(
                modifier = Modifier
                    .animateItem(placementSpec = spring(0.9f, 400f))
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = DimIndication(shape = AppSpecs.cardShape),
                        onLongClick = {
                            if (!isSelectionModeActive) {
                                scope.launch {
                                    viewModel.enterSelectionMode(item.todoItem.id)
                                }
                            } else {
                                viewModel.toggleSelection(item.todoItem.id)
                            }
                        },
                        onClick = {
                            if (isSelectionModeActive) {
                                scope.launch {
                                    viewModel.toggleSelection(item.todoItem.id)
                                }
                            } else {
                                val intent =
                                    Intent(context, DetailActivity::class.java).apply {
                                        putExtra("todo_id", item.todoItem.id)
                                    }
                                launcher.launch(intent)
                            }
                        }
                    )) {
                SwipeableTodoItem(
                    item = displayItem,
                    onCheckedChange = { checked ->
                        isChecked = checked
                        scope.launch {
                            delay(300) // 2. 等待动画播放
                            // 3. 提交真实数据，触发列表重排
                            viewModel.update(item.todoItem.copy(isCompleted = checked))
                        }
                    },
                    onDelete = { viewModel.delete(item.todoItem) },
                    modifier = Modifier.drawBehind {
                        if (isComposed) {
                            val outline =
                                ContinuousRoundedRectangle(cardCorner - 3.dp / 2).createOutline(
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
                                    color = selectionOutline,
                                    alpha = alpha.value,
                                    style = Stroke(width = 3.dp.toPx()),
                                )
                            }
                        }
                    },
                    listState = swipeListState
                )
                // Selection mode selector box
                if (isSelectionModeActive) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .combinedClickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = {
                                    scope.launch {
                                        viewModel.toggleSelection(item.todoItem.id)
                                    }
                                }
                            )) {}
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (completeTodos.isNotEmpty()) {
            item(key = "small_title") {
                val degree = remember { Animatable(if (completedVisible) 90f else 180f) }
                LaunchedEffect(completedVisible) {
                    if (completedVisible) {
                        degree.animateTo(90f, tween(200))
                    } else {
                        degree.animateTo(180f, tween(200))
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .zIndex(-1f)
                        .animateItem(placementSpec = spring(0.9f, 400f))
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { completedVisible = !completedVisible }
                        )
                        .padding(top = 8.dp, bottom = 8.dp, start = 12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.completed),
                        fontSize = 14.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = AppColors.contentVariant
                    )
                    Icon(
                        painter = painterResource(R.drawable.ic_forward_nav),
                        contentDescription = stringResource(R.string.expand),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(20.dp)
                            .alpha(.5f)
                            .graphicsLayer {
                                rotationZ = degree.value
                            }
                    )
                }
            }
            if (completedVisible) {
                items(
                    items = completeTodos,
                    key = { it.todoItem.id },
                ) { item ->
                    val isSelected = item.todoItem.id in selectedItemIds
                    val alpha = remember { Animatable(if (isSelected) 1f else 0f) }

                    LaunchedEffect(isSelected) {
                        if (isSelected) {
                            alpha.animateTo(1f, tween(100))
                        } else {
                            alpha.animateTo(0f, tween(100))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .animateItem(placementSpec = spring(0.9f, 400f))
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = DimIndication(shape = AppSpecs.cardShape),
                                onLongClick = {
                                    if (!isSelectionModeActive) {
                                        scope.launch {
                                            viewModel.enterSelectionMode(item.todoItem.id)
                                        }
                                    } else {
                                        viewModel.toggleSelection(item.todoItem.id)
                                    }
                                },
                                onClick = {
                                    if (isSelectionModeActive) {
                                        scope.launch {
                                            viewModel.toggleSelection(item.todoItem.id)
                                        }
                                    } else {
                                        val intent =
                                            Intent(
                                                context,
                                                DetailActivity::class.java
                                            ).apply {
                                                putExtra(EXTRA_TODO_ID, item.todoItem.id)
                                            }
                                        launcher.launch(intent)
                                    }
                                }
                            )) {
                        SwipeableTodoItem(
                            item = item,
                            onCheckedChange = { isChecked ->
                                viewModel.update(item.todoItem.copy(isCompleted = isChecked))
                            },
                            onDelete = { viewModel.delete(item.todoItem) },
                            modifier = Modifier.drawBehind {
                                if (isComposed) {
                                    val outline =
                                        ContinuousRoundedRectangle(cardCorner - 3.dp / 2).createOutline(
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
                                            color = selectionOutline,
                                            alpha = alpha.value,
                                            style = Stroke(width = 3.dp.toPx()),
                                        )
                                    }
                                }
                            },
                            listState = swipeListState
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
                                                viewModel.toggleSelection(item.todoItem.id)
                                            }
                                        }
                                    )) {}
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
        overscrollSpacer(lazyListState)
    }
    GlasenseDynamicSmallTitle(
        modifier = Modifier.align(Alignment.TopCenter),
        title = if (isComposed) stringResource(
            R.string.selected_todos,
            lastNonZeroSelected
        ) else stringResource(R.string.all_todos),
        textStyle = TextStyle(fontFeatureSettings = "tnum"),
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
                    .padding(top = statusBarHeight, start = 12.dp)
                    .align(Alignment.TopStart),
                colors = ButtonDefaults.buttonColors(
                    containerColor = onSurfaceContainer,
                    contentColor = AppColors.primary
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
                    .padding(top = statusBarHeight, end = 12.dp)
                    .size(48.dp)
                    .align(Alignment.TopEnd),
                colors = ButtonDefaults.buttonColors(
                    containerColor = onSurfaceContainer,
                    contentColor = AppColors.primary
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add_large),
                    contentDescription = stringResource(R.string.add_new_todo),
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
            tint = AppColors.error,
            enabled = true,
            shape = ContinuousCapsule,
            onClick = {
                showDialog(
                    dialogItems,
                    title,
                    message
                )
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
                .align(Alignment.TopStart),
            colors = ButtonDefaults.buttonColors(
                containerColor = onSurfaceContainer,
                contentColor = AppColors.primary
            )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_trash),
                contentDescription = stringResource(R.string.delete_selected_todo_s),
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
                containerColor = onSurfaceContainer,
                contentColor = AppColors.primary
            )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_cross),
                contentDescription = stringResource(R.string.exit_selection_mode),
                modifier = Modifier.width(32.dp)
            )
        }
    }
}