package com.nevoit.cresto.feature.main

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import com.nevoit.cresto.data.todo.calendar.TodoCalendarSyncManager
import com.nevoit.cresto.feature.bottomsheet.BottomSheet
import com.nevoit.cresto.feature.calendar.toToastMessage
import com.nevoit.cresto.feature.screenextract.ScreenExtractEvents
import com.nevoit.cresto.feature.settings.update.UpdateBottomSheet
import com.nevoit.cresto.feature.settings.update.UpdateCheckResult
import com.nevoit.cresto.feature.settings.update.UpdateChecker
import com.nevoit.cresto.feature.settings.update.UpdateInfo
import com.nevoit.cresto.feature.settings.util.SettingsManager
import com.nevoit.cresto.feature.settings.util.SettingsViewModel
import com.nevoit.cresto.feature.sharetodo.TodoShareSheet
import com.nevoit.cresto.theme.AppButtonColors
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.isAppInDarkTheme
import com.nevoit.cresto.toolkit.gaussiangradient.smoothGradientMask
import com.nevoit.cresto.ui.components.glasense.DialogItemData
import com.nevoit.cresto.ui.components.glasense.DialogState
import com.nevoit.cresto.ui.components.glasense.GlasenseButtonAdaptable
import com.nevoit.cresto.ui.components.glasense.GlasenseButtonToolBar
import com.nevoit.cresto.ui.components.glasense.GlasenseDialog
import com.nevoit.cresto.ui.components.glasense.GlasenseMenu
import com.nevoit.cresto.ui.components.glasense.GlasenseMenuItem
import com.nevoit.cresto.ui.components.glasense.MenuState
import com.nevoit.cresto.ui.components.glasense.PopupDirection
import com.nevoit.cresto.ui.components.packed.CustomReminderPopup
import com.nevoit.cresto.ui.components.packed.DueDatePicker
import com.nevoit.cresto.ui.components.packed.TimePicker
import com.nevoit.cresto.ui.components.packed.TodoReminderConfig
import com.nevoit.cresto.ui.modifier.pressIndentShaderEffect
import com.nevoit.cresto.ui.modifier.shaderRipple
import com.nevoit.glasense.core.component.Icon
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.LocalTime

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Star : Screen("star")
    object Settings : Screen("settings")
}


@Composable
fun MainScreen() {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> ScreenExtractEvents.setMainUiOpen(true)
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_DESTROY -> ScreenExtractEvents.setMainUiOpen(false)

                else -> Unit
            }
        }

        ScreenExtractEvents.setMainUiOpen(
            lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        )
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            ScreenExtractEvents.setMainUiOpen(false)
        }
    }

    val surfaceColor = AppColors.pageBackground
    var currentRoute by rememberSaveable { mutableStateOf(Screen.Home.route) }
    val settingsViewModel: SettingsViewModel = viewModel()

    val liquidGlass by settingsViewModel.isLiquidGlass
    val isSuperGraphicUltraModernGirlEnabled by settingsViewModel.isSuperGraphicUltraModernGirlEnabled

    val backdrop = rememberLayerBackdrop {
        drawRect(
            color = surfaceColor,
            size = Size(this.size.width * 3, this.size.height * 3),
            topLeft = Offset(-this.size.width, -this.size.height)
        )
        drawContent()
    }

    var menuState by remember { mutableStateOf(MenuState()) }

    val showMenu: (anchorBounds: Rect, items: List<GlasenseMenuItem>) -> Unit =
        { bounds, items ->
            menuState = MenuState(isVisible = true, anchorBounds = bounds, items = items)
        }

    val dismissMenu = {
        menuState = menuState.copy(isVisible = false)
    }

    var dialogState by remember { mutableStateOf(DialogState()) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }

    val showDialog: (items: List<DialogItemData>, title: String, message: String?) -> Unit =
        { items, title, message ->
            dialogState =
                DialogState(isVisible = true, items = items, title = title, message = message)
        }

    val dismissDialog = {
        dialogState = dialogState.copy(isVisible = false)
    }

    val pendingAiTodos by ScreenExtractEvents.pendingTodos.collectAsState()

    LaunchedEffect(Unit) {
        val now = System.currentTimeMillis()
        val oneDayMs = 24L * 60L * 60L * 1000L
        if (!SettingsManager.isCheckUpdatesOnStartup) return@LaunchedEffect
        if (now - SettingsManager.lastUpdateCheckAt < oneDayMs) return@LaunchedEffect

        SettingsManager.lastUpdateCheckAt = now
        when (val result = UpdateChecker.check()) {
            is UpdateCheckResult.HasUpdate -> updateInfo = result.updateInfo
            else -> Unit
        }
    }

    val errorOkText = stringResource(R.string.ok)
    val errorTitleText = stringResource(R.string.extract_screen_failed)
    LaunchedEffect(Unit) {
        ScreenExtractEvents.errors.collect { errorMessage ->
            showDialog(
                listOf(
                    DialogItemData(
                        text = errorOkText,
                        onClick = {},
                        isPrimary = true
                    )
                ),
                errorTitleText,
                errorMessage
            )
        }
    }

    val density = LocalDensity.current

    val viewModel: TodoViewModel = koinViewModel()
    val requirePermission = stringResource(R.string.calendar_sync_permission_required)
    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = TodoCalendarSyncManager.REQUIRED_PERMISSIONS.all { permission ->
            permissions[permission] == true
        }
        if (granted) {
            viewModel.syncSelectedItemsToCalendar()
        } else {
            Toast.makeText(
                context,
                requirePermission,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    LaunchedEffect(viewModel, context) {
        viewModel.calendarSyncEvents.collect { summary ->
            Toast.makeText(context, summary.toToastMessage(context), Toast.LENGTH_SHORT).show()
        }
    }

    fun syncSelectedItemsToCalendar() {
        if (TodoCalendarSyncManager.hasCalendarPermissions(context)) {
            viewModel.syncSelectedItemsToCalendar()
        } else {
            calendarPermissionLauncher.launch(TodoCalendarSyncManager.REQUIRED_PERMISSIONS)
        }
    }

    val bottomSheetState by viewModel.bottomSheetState.collectAsState()

    var isDatePickerVisible by remember { mutableStateOf(false) }
    var dateButtonBounds by remember { mutableStateOf(Rect.Zero) }
    var sheetFinalDate by remember { mutableStateOf<LocalDate?>(null) }
    var onDateSelectedCallback by remember { mutableStateOf<(LocalDate?) -> Unit>({}) }
    var isTimePickerVisible by remember { mutableStateOf(false) }
    var timePickerRequestKey by remember { mutableIntStateOf(0) }
    var timeButtonBounds by remember { mutableStateOf(Rect.Zero) }
    var sheetFinalTime by remember { mutableStateOf<LocalTime?>(null) }
    var sheetMinTime by remember { mutableStateOf<LocalTime?>(null) }
    var sheetMaxTime by remember { mutableStateOf<LocalTime?>(null) }
    var onTimeSelectedCallback by remember { mutableStateOf<(LocalTime?) -> Unit>({}) }
    var isCustomReminderPopupVisible by remember { mutableStateOf(false) }
    var reminderButtonBounds by remember { mutableStateOf(Rect.Zero) }
    var sheetReminderIsAllDay by remember { mutableStateOf(true) }
    var onReminderSelectedCallback by remember { mutableStateOf<(TodoReminderConfig) -> Unit>({}) }

    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val sharedInteractionSource = remember { MutableInteractionSource() }

    val isSelectionModeActive by viewModel.isSelectionModeActive.collectAsState()
    val selectedItemCount by viewModel.selectedItemCount.collectAsState()
    val selectedTodos by viewModel.selectedTodos.collectAsState()
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
    var isShareSheetVisible by remember { mutableStateOf(false) }
    val moreMenu = rememberMoreMenuItems(
        onDuplicateSelected = viewModel::duplicateSelectedItems,
        onMergeSelected = { viewModel.mergeSelectedItems(newMergedTodoTitle) },
        onShareSelected = { isShareSheetVisible = true },
        canMerge = selectedItemCount >= 2
    )
    var moreButtonBounds by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val flagMenu = rememberFlagMenuItems(onFlagSelected = viewModel::flagSelectedItems)
    var flagButtonBounds by remember { mutableStateOf<LayoutCoordinates?>(null) }

    BackHandler(enabled = currentRoute != Screen.Home.route) {
        currentRoute = Screen.Home.route
    }

    val superBackdrop = rememberLayerBackdrop {
        drawRect(
            color = surfaceColor,
            size = Size(this.size.width * 3, this.size.height * 3),
            topLeft = Offset(-this.size.width, -this.size.height)
        )
        drawContent()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaceColor)
            .then(
                if (isSuperGraphicUltraModernGirlEnabled) {
                    Modifier
                        .shaderRipple(dark = !isAppInDarkTheme())
                        .pressIndentShaderEffect()
                } else {
                    Modifier
                }
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(superBackdrop)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop)
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
                    .smoothGradientMask(
                        surfaceColor.copy(alpha = 0f),
                        surfaceColor.copy(alpha = 1f),
                        0f,
                        0.8f,
                        0.7f
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
                                    showMenu(
                                        it.boundsInWindow(),
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
                            colors = AppButtonColors.action()
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
                                                showMenu(
                                                    it.boundsInWindow(),
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
                                        .width(48.dp)
                                        .clickable(
                                            interactionSource = sharedInteractionSource,
                                            indication = null
                                        ) {
                                            syncSelectedItemsToCalendar()
                                        },
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
                            colors = AppButtonColors.action()
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
                    NavigationBar(
                        tabBarY = { tabBarHideAnimation.value * tabBarTotalHeight },
                        currentRoute = { currentRoute },
                        onCurrentRouteChange = { currentRoute = it },
                        backdrop = backdrop,
                        liquidGlass = liquidGlass
                    )
                }
            }

            if (bottomSheetState.isVisible) {
                BottomSheet(
                    onDismiss = { viewModel.hideBottomSheet() },
                    onAddClick = { title, notes, flagIndex, finalDate, startTime, endTime, reminder, repeatFrequency, repeatRuleConfig ->
                        viewModel.insert(
                            TodoItem(
                                title = title,
                                notes = notes,
                                flag = flagIndex,
                                dueDate = finalDate,
                                startTime = startTime,
                                endTime = endTime,
                                reminderMode = reminder?.mode,
                                reminderOffsetMinutes = reminder?.offsetMinutes,
                                reminderDayOffset = reminder?.dayOffset,
                                reminderTime = reminder?.time,
                                reminderPersistent = reminder?.persistent ?: false,
                                reminderStrong = reminder?.strong ?: false
                            ),
                            repeatFrequency = repeatFrequency,
                            repeatRuleConfig = repeatRuleConfig
                        )
                    },
                    showDialog = showDialog,
                    onRequestCustomDate = { bounds, initialDate, onSelected ->
                        dateButtonBounds = bounds
                        sheetFinalDate = initialDate
                        onDateSelectedCallback = onSelected
                        isDatePickerVisible = true
                    },
                    onRequestCustomTime = { bounds, initialTime, minTime, maxTime, onSelected ->
                        timePickerRequestKey++
                        timeButtonBounds = bounds
                        sheetFinalTime = initialTime
                        sheetMinTime = minTime
                        sheetMaxTime = maxTime
                        onTimeSelectedCallback = onSelected
                        isTimePickerVisible = true
                    },
                    onRequestCustomReminder = { bounds, isAllDayEnabled, onSelected ->
                        reminderButtonBounds = bounds
                        sheetReminderIsAllDay = isAllDayEnabled
                        onReminderSelectedCallback = onSelected
                        isCustomReminderPopupVisible = true
                    },
                    showMenu = showMenu
                )
            }

            if (isShareSheetVisible) {
                TodoShareSheet(
                    todos = selectedTodos,
                    onDismiss = { isShareSheetVisible = false }
                )
            }

            DueDatePicker(
                isVisible = isDatePickerVisible,
                anchorBounds = dateButtonBounds,
                initialDate = sheetFinalDate,
                onDismiss = { isDatePickerVisible = false },
                onDateSelected = { date ->
                    onDateSelectedCallback(date)
                },
                direction = PopupDirection.Up
            )

            key(timePickerRequestKey) {
                TimePicker(
                    isVisible = isTimePickerVisible,
                    anchorBounds = timeButtonBounds,
                    initialTime = sheetFinalTime,
                    minTime = sheetMinTime,
                    maxTime = sheetMaxTime,
                    onDismiss = { isTimePickerVisible = false },
                    onTimeSelected = { time ->
                        onTimeSelectedCallback(time)
                    },
                    direction = PopupDirection.Down
                )
            }

            CustomReminderPopup(
                isVisible = isCustomReminderPopupVisible,
                anchorBounds = reminderButtonBounds,
                isAllDayEnabled = sheetReminderIsAllDay,
                onDismiss = { isCustomReminderPopupVisible = false },
                onConfirm = { config ->
                    onReminderSelectedCallback(config)
                    isCustomReminderPopupVisible = false
                }
            )
        }

        GlasenseMenu(
            menuState = menuState,
            backdrop = superBackdrop,
            onDismiss = dismissMenu
        )

        GlasenseDialog(
            dialogState = dialogState,
            backdrop = superBackdrop,
            onDismiss = { dismissDialog() }
        )

        pendingAiTodos?.let { pendingTodos ->
            AiTodoReviewContainer(
                backdrop = superBackdrop,
                pendingTodos = pendingTodos,
                onDismiss = { ScreenExtractEvents.clearPendingTodos() }
            ) { items ->
                viewModel.insertAiGeneratedTodos(items)
                ScreenExtractEvents.clearPendingTodos()
            }
        }

        updateInfo?.let { info ->
            UpdateBottomSheet(
                updateInfo = info,
                onDismissed = {
                    if (!info.isRequired) updateInfo = null
                }
            )
        }
    }
}
