package com.nevoit.cresto.feature.main

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.shapes.Capsule
import com.nevoit.cresto.R
import com.nevoit.cresto.data.todo.TodoItem
import com.nevoit.cresto.data.todo.TodoViewModel
import com.nevoit.cresto.feature.settings.util.SettingsViewModel
import com.nevoit.cresto.theme.AppButtonColors
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.LocalGlasenseSettings
import com.nevoit.cresto.theme.linearGradientMaskB2T70
import com.nevoit.cresto.theme.linearGradientMaskB2T90
import com.nevoit.cresto.toolkit.gaussiangradient.smoothGradientMask
import com.nevoit.cresto.toolkit.gaussiangradient.smoothGradientMaskFallback
import com.nevoit.cresto.ui.components.bottomsheet.BottomSheet
import com.nevoit.cresto.ui.components.glasense.DialogItemData
import com.nevoit.cresto.ui.components.glasense.DialogState
import com.nevoit.cresto.ui.components.glasense.GlasenseButtonAdaptable
import com.nevoit.cresto.ui.components.glasense.GlasenseButtonToolBar
import com.nevoit.cresto.ui.components.glasense.GlasenseDialog
import com.nevoit.cresto.ui.components.glasense.GlasenseMenu
import com.nevoit.cresto.ui.components.glasense.GlasenseMenuItem
import com.nevoit.cresto.ui.components.glasense.GlasenseNavigationButton
import com.nevoit.cresto.ui.components.glasense.MenuState
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Star : Screen("star")
    object Settings : Screen("settings")
}

@OptIn(ExperimentalHazeApi::class)
@Composable
fun MainScreen() {
    val surfaceColor = AppColors.pageBackground
    var currentRoute by rememberSaveable { mutableStateOf(Screen.Home.route) }
    val settingsViewModel: SettingsViewModel = viewModel()

    val liquidGlass by settingsViewModel.isLiquidGlass

    val hazeState = rememberHazeState()
    val backdrop = rememberLayerBackdrop {
        drawRect(
            color = surfaceColor,
            size = Size(this.size.width * 3, this.size.height * 3),
            topLeft = Offset(-this.size.width, -this.size.height)
        )
        drawContent()
    }

    var menuState by remember { mutableStateOf(MenuState()) }

    val showMenu: (anchorPosition: Offset, items: List<GlasenseMenuItem>) -> Unit =
        { position, items ->
            menuState = MenuState(isVisible = true, anchorPosition = position, items = items)
        }

    val dismissMenu = {
        menuState = menuState.copy(isVisible = false)
    }

    var dialogState by remember { mutableStateOf(DialogState()) }

    val showDialog: (items: List<DialogItemData>, title: String, message: String?) -> Unit =
        { items, title, message ->
            dialogState =
                DialogState(isVisible = true, items = items, title = title, message = message)
        }

    val dismissDialog = {
        dialogState = dialogState.copy(isVisible = false)
    }


    val density = LocalDensity.current

    val viewModel: TodoViewModel = koinViewModel()

    val bottomSheetState by viewModel.bottomSheetState.collectAsState()

    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val sharedInteractionSource = remember { MutableInteractionSource() }

    val isSelectionModeActive by viewModel.isSelectionModeActive.collectAsState()
    val selectedItemCount by viewModel.selectedItemCount.collectAsState()
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
    val title = pluralStringResource(
        id = R.plurals.delete_todo_dialog_title,
        count = selectedItemCount,
        selectedItemCount
    )
    val message = pluralStringResource(
        id = R.plurals.delete_todo_dialog_msg,
        count = selectedItemCount
    )

    val scope = rememberCoroutineScope()

    var isComposed by remember { mutableStateOf(isSelectionModeActive) }
    var isGone by remember { mutableStateOf(isSelectionModeActive) }
    val targetBlurRadius = with(density) {
        16.dp.toPx()
    }
    val bottomBarAlphaAnimation = remember { Animatable(if (isSelectionModeActive) 1f else 0f) }

    val bottomBarBlurAnimation =
        remember { Animatable(if (isSelectionModeActive) 0f else targetBlurRadius) }

    val tabBarHideAnimation = remember { Animatable(if (isSelectionModeActive) 1f else 0f) }

    val tabBarTotalHeight = density.run {
        (16.dp + 56.dp + 16.dp + WindowInsets.navigationBars.asPaddingValues()
            .calculateBottomPadding()).toPx()
    }

    LaunchedEffect(isSelectionModeActive) {
        if (isSelectionModeActive) {
            isComposed = true
            scope.launch { bottomBarAlphaAnimation.animateTo(1f, tween(300)) }
            scope.launch { tabBarHideAnimation.animateTo(1f, tween(300)) }
            bottomBarBlurAnimation.animateTo(0f, tween(300))
            isGone = true
        } else {
            isGone = false
            scope.launch { bottomBarAlphaAnimation.animateTo(0f, tween(300)) }
            scope.launch { tabBarHideAnimation.animateTo(0f, spring(0.75f, 300f, 0.0001f)) }
            bottomBarBlurAnimation.animateTo(targetBlurRadius, tween(300))
            isComposed = false
        }
    }

    val floatingBarColor = AppColors.pageBackground.copy(.5f)

    val newMergedTodoTitle = stringResource(R.string.new_merged_todo_title)
    val moreMenu = rememberMoreMenuItems(
        onDuplicateSelected = viewModel::duplicateSelectedItems,
        onMergeSelected = { viewModel.mergeSelectedItems(newMergedTodoTitle) },
        canMerge = selectedItemCount >= 2
    )
    var moreButtonBounds by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val flagMenu = rememberFlagMenuItems(onFlagSelected = viewModel::flagSelectedItems)
    var flagButtonBounds by remember { mutableStateOf<LayoutCoordinates?>(null) }

    BackHandler(enabled = currentRoute != Screen.Home.route) {
        currentRoute = Screen.Home.route
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaceColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .hazeSource(hazeState, 0f)
        ) {
            NavContainer(
                currentRoute = currentRoute,
                showMenu = showMenu,
                viewModel = viewModel
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp + navigationBarHeight)
                .align(Alignment.BottomCenter)
                .then(
                    if (LocalGlasenseSettings.current.liteMode) Modifier
                    else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Modifier.hazeEffect(
                        hazeState
                    ) {
                        blurRadius = 10.dp
                        progressive = HazeProgressive.verticalGradient(
                            startIntensity = 0.2f,
                            endIntensity = 0.6f
                        )
                        noiseFactor = 0f
                        mask = linearGradientMaskB2T90
                        inputScale = HazeInputScale.Fixed(0.5f)
                        style = HazeStyle(backgroundColor = surfaceColor, tint = null)
                    } else Modifier.hazeEffect(
                        hazeState
                    ) {
                        blurRadius = 4.dp
                        noiseFactor = 0f
                        mask = linearGradientMaskB2T70
                    })

                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Modifier.smoothGradientMask(
                        surfaceColor.copy(alpha = 0f), surfaceColor.copy(alpha = 1f), 0f, 1f, 0.6f
                    ) else Modifier.smoothGradientMaskFallback(surfaceColor, 0.6f)
                )
        ) {
            if (isComposed) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = 12.dp, end = 12.dp, bottom = 16.dp)
                        .height(48.dp)
                        .align(Alignment.BottomCenter)
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
                        shape = Capsule(),
                        onClick = {
                            moreButtonBounds?.let {
                                val position = Offset(
                                    x = it.positionInWindow().x,
                                    y = it.positionInWindow().y - with(density) { 8.dp.toPx() },
                                )
                                showMenu(
                                    position,
                                    moreMenu
                                )
                            }
                        },
                        modifier = Modifier
                            .onGloballyPositioned { coordinates ->
                                moreButtonBounds = coordinates
                            }
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = { Capsule() },
                                shadow = null,
                                innerShadow = null,
                                highlight = { if (liquidGlass) Highlight.Default else null },
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.scrimNormal,
                            contentColor = AppColors.primary
                        )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_ellipsis),
                            contentDescription = stringResource(R.string.more),
                            modifier = Modifier.width(32.dp)
                        )
                    }
                    GlasenseButtonToolBar(
                        enabled = true,
                        interactionSource = sharedInteractionSource,
                        shape = Capsule(),
                        onClick = {},
                        modifier = Modifier
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = { Capsule() },
                                shadow = null,
                                innerShadow = null,
                                highlight = { if (liquidGlass) Highlight.Default else null },
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
                        Row(
                            modifier = Modifier
                                .height(48.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .onGloballyPositioned { coordinates ->
                                        flagButtonBounds = coordinates
                                    }
                                    .height(48.dp)
                                    .width(48.dp)
                                    .clickable(
                                        interactionSource = sharedInteractionSource,
                                        indication = null
                                    ) {
                                        flagButtonBounds?.let {
                                            val position = Offset(
                                                x = it.positionInWindow().x,
                                                y = it.positionInWindow().y - with(density) { 8.dp.toPx() },
                                            )
                                            showMenu(
                                                position,
                                                flagMenu
                                            )
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_flag),
                                    contentDescription = stringResource(R.string.set_flag),
                                    modifier = Modifier.width(32.dp),
                                    tint = AppColors.primary
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .height(48.dp)
                                    .width(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_calendar_add),
                                    contentDescription = stringResource(R.string.add_to_calendar),
                                    modifier = Modifier.width(32.dp),
                                    tint = AppColors.primary
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .height(48.dp)
                                    .width(48.dp)
                                    .clickable(
                                        interactionSource = sharedInteractionSource,
                                        indication = null
                                    ) {
                                        viewModel.completeSelectedItems()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_checkmark_circle),
                                    contentDescription = stringResource(R.string.check_all),
                                    modifier = Modifier.width(32.dp),
                                    tint = AppColors.primary
                                )
                            }
                        }
                    }
                    GlasenseButtonAdaptable(
                        width = { 48.dp },
                        height = { 48.dp },
                        tint = AppColors.error,
                        enabled = true,
                        shape = Capsule(),
                        onClick = {
                            showDialog(
                                dialogItems,
                                title,
                                message
                            )
                        },
                        modifier = Modifier
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = { Capsule() },
                                shadow = null,
                                innerShadow = null,
                                highlight = { if (liquidGlass) Highlight.Default else null },
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.scrimNormal,
                            contentColor = AppColors.primary
                        )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_trash),
                            contentDescription = stringResource(R.string.delete_selected_todo_s),
                            modifier = Modifier.width(32.dp)
                        )
                    }
                }
            }
            if (!isGone) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                        .graphicsLayer {
                            translationY = tabBarHideAnimation.value * tabBarTotalHeight
                        }
                        .height(56.dp)
                        .align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    GlasenseNavigationButton(
                        modifier = Modifier.weight(1f),
                        isActive = currentRoute == Screen.Home.route,
                        onClick = {
                            if (currentRoute != Screen.Home.route) {
                                currentRoute = Screen.Home.route
                            }
                        },
                        backdrop = backdrop,
                        liquidGlass = liquidGlass
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_list),
                            contentDescription = stringResource(R.string.all_todos)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    GlasenseNavigationButton(
                        modifier = Modifier.weight(1f),
                        isActive = currentRoute == Screen.Star.route,
                        onClick = {
                            if (currentRoute != Screen.Star.route) {
                                currentRoute = Screen.Star.route
                            }
                        },
                        backdrop = backdrop,
                        liquidGlass = liquidGlass
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_star),
                            contentDescription = stringResource(R.string.mind_flow)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    GlasenseNavigationButton(
                        modifier = Modifier.weight(1f),
                        isActive = currentRoute == Screen.Settings.route,
                        onClick = {
                            if (currentRoute != Screen.Settings.route) {
                                currentRoute = Screen.Settings.route
                            }
                        },
                        backdrop = backdrop,
                        liquidGlass = liquidGlass
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_gear),
                            contentDescription = stringResource(R.string.settings)
                        )
                    }
                }
            }
        }

        if (bottomSheetState.isVisible) {
            BottomSheet(
                onDismiss = { viewModel.hideBottomSheet() },
                onAddClick = { title, flagIndex, finalDate ->
                    viewModel.insert(
                        TodoItem(
                            title = title,
                            flag = flagIndex,
                            dueDate = finalDate
                        )
                    )
                }, showDialog = showDialog
            )
        }

        GlasenseMenu(
            menuState = menuState,
            backdrop = backdrop,
            onDismiss = dismissMenu
        )

        GlasenseDialog(
            dialogState = dialogState,
            backdrop = backdrop,
            onDismiss = { dismissDialog() }
        )
    }
}
