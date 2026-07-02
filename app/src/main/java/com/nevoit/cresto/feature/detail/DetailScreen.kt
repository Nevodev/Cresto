package com.nevoit.cresto.feature.detail

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.Capsule
import com.nevoit.cresto.R
import com.nevoit.cresto.data.todo.EXTRA_DELETE_ID
import com.nevoit.cresto.data.todo.HomeGroupFilter
import com.nevoit.cresto.data.todo.RepeatFrequency
import com.nevoit.cresto.data.todo.RepeatRule
import com.nevoit.cresto.data.todo.RepeatRuleConfig
import com.nevoit.cresto.data.todo.SubTodoItem
import com.nevoit.cresto.data.todo.TodoItem
import com.nevoit.cresto.data.todo.TodoViewModel
import com.nevoit.cresto.data.todo.calendar.TodoCalendarSyncManager
import com.nevoit.cresto.feature.bottomsheet.CustomRepeatBottomSheet
import com.nevoit.cresto.feature.bottomsheet.isCustomRepeatRule
import com.nevoit.cresto.feature.bottomsheet.isSimpleFrequency
import com.nevoit.cresto.feature.bottomsheet.toCustomRepeatConfig
import com.nevoit.cresto.feature.bottomsheet.toPresetFrequency
import com.nevoit.cresto.feature.bottomsheet.toRepeatRuleConfig
import com.nevoit.cresto.feature.calendar.toToastMessage
import com.nevoit.cresto.feature.group.GroupBottomSheet
import com.nevoit.cresto.feature.main.rememberFlagMenuItems
import com.nevoit.cresto.feature.settings.util.SettingsViewModel
import com.nevoit.cresto.feature.sharetodo.TodoShareSheet
import com.nevoit.cresto.theme.AppButtonColors
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.AppSpecs
import com.nevoit.cresto.theme.getFlagColor
import com.nevoit.cresto.theme.isAppInDarkTheme
import com.nevoit.cresto.ui.components.glasense.DialogItemData
import com.nevoit.cresto.ui.components.glasense.DialogState
import com.nevoit.cresto.ui.components.glasense.GlasenseBottomBar
import com.nevoit.cresto.ui.components.glasense.GlasenseButton
import com.nevoit.cresto.ui.components.glasense.GlasenseDialog
import com.nevoit.cresto.ui.components.glasense.GlasenseDynamicSmallTitle
import com.nevoit.cresto.ui.components.glasense.GlasenseMenu
import com.nevoit.cresto.ui.components.glasense.GlasenseMenuItem
import com.nevoit.cresto.ui.components.glasense.MenuDivider
import com.nevoit.cresto.ui.components.glasense.MenuState
import com.nevoit.cresto.ui.components.glasense.PopupDirection
import com.nevoit.cresto.ui.components.glasense.SelectiveMenuItemData
import com.nevoit.cresto.ui.components.glasense.isScrolledPast
import com.nevoit.cresto.ui.components.glasense.rememberSwipeableListState
import com.nevoit.cresto.ui.components.packed.ConfigTextField
import com.nevoit.cresto.ui.components.packed.CustomReminderPopup
import com.nevoit.cresto.ui.components.packed.DueDatePicker
import com.nevoit.cresto.ui.components.packed.PageContent
import com.nevoit.cresto.ui.components.packed.SubTodoItemRowAdd
import com.nevoit.cresto.ui.components.packed.SwipeableSubTodoItemRowEditable
import com.nevoit.cresto.ui.components.packed.TimePicker
import com.nevoit.cresto.ui.components.packed.TodoItemRowEditable
import com.nevoit.cresto.ui.components.packed.TodoReminderConfig
import com.nevoit.cresto.ui.components.packed.compatibleWithAllDay
import com.nevoit.cresto.ui.components.packed.displayText
import com.nevoit.cresto.ui.components.packed.toReminderConfig
import com.nevoit.cresto.ui.components.packed.withReminderConfig
import com.nevoit.cresto.ui.modifier.pressIndentShaderEffect
import com.nevoit.cresto.ui.modifier.shaderRipple
import com.nevoit.cresto.util.formatRelativeTime
import com.nevoit.glasense.component.ProgressView
import com.nevoit.glasense.component.paddingItem
import com.nevoit.glasense.core.component.Icon
import com.nevoit.glasense.core.component.Text
import com.nevoit.glasense.core.component.VDivider
import com.nevoit.glasense.core.component.VGap
import com.nevoit.glasense.core.interaction.DimIndication
import com.nevoit.glasense.theme.GlasenseTheme
import com.nevoit.glasense.theme.LocalGlasenseContentColor
import com.nevoit.glasense.theme.tokens.Springs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@Composable
fun DetailScreen(
    todoId: Int,
    viewModel: TodoViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()

    val activity = LocalActivity.current
    val itemWithSubTodos by viewModel.getTodoWithSubTodos(todoId).collectAsState(initial = null)
    val currentItem = itemWithSubTodos
    val repeatRule by viewModel.getRepeatRule(
        itemWithSubTodos?.todoItem?.repeatRuleId
    ).collectAsState(initial = null)
    val homeGroups by viewModel.homeGroups.collectAsState()
    val homeGroupTodoCounts by viewModel.homeGroupTodoCounts.collectAsState()
    val homeGroupNames = remember(homeGroups) { homeGroups.associate { it.id to it.name } }

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val surfaceColor = AppColors.pageBackground

    val backdrop = rememberLayerBackdrop {
        drawRect(
            color = surfaceColor,
            size = Size(this.size.width * 3, this.size.height * 3),
            topLeft = Offset(-this.size.width, -this.size.height)
        )
        drawContent()
    }

    val backdrop2 = rememberLayerBackdrop {
        drawRect(
            color = surfaceColor,
            size = Size(this.size.width * 3, this.size.height * 3),
            topLeft = Offset(-this.size.width, -this.size.height)
        )
        drawContent()
    }

    val lazyListState = rememberLazyListState()
    val swipeListState = rememberSwipeableListState()

    val isSmallTitleVisible by lazyListState.isScrolledPast(statusBarHeight + 24.dp)

    var finalDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var title by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }

    var isInitialized by remember { mutableStateOf(false) }
    if (itemWithSubTodos != null && !isInitialized) {
        finalDate = itemWithSubTodos?.todoItem?.dueDate
        selectedIndex = itemWithSubTodos?.todoItem?.flag ?: 0
        title = itemWithSubTodos?.todoItem?.title ?: ""
        notesText = itemWithSubTodos?.todoItem?.notes ?: ""
        isInitialized = true
    }

    var reminderDraftTodoId by remember { mutableIntStateOf(-1) }
    var reminderDraftConfig by remember { mutableStateOf<TodoReminderConfig?>(null) }
    var reminderDraftPersistent by remember { mutableStateOf(false) }
    var reminderDraftStrong by remember { mutableStateOf(false) }
    var isEditingReminderDraft by remember { mutableStateOf(false) }

    LaunchedEffect(itemWithSubTodos?.todoItem?.id) {
        snapshotFlow { itemWithSubTodos?.todoItem }
            .collect { todoItem ->
                if (todoItem == null) return@collect
                if (!isEditingReminderDraft || reminderDraftTodoId != todoItem.id) {
                    reminderDraftTodoId = todoItem.id
                    reminderDraftConfig = todoItem.toReminderConfig()
                    reminderDraftPersistent = todoItem.reminderPersistent
                    reminderDraftStrong = todoItem.reminderStrong
                }
            }
    }

    var ticker by remember { mutableIntStateOf(0) }

    val deleteDialogItems = listOf(
        DialogItemData(
            stringResource(R.string.cancel),
            onClick = {},
            isPrimary = false
        ),
        DialogItemData(
            stringResource(R.string.delete),
            icon = painterResource(R.drawable.ic_trash),
            onClick = {
                val resultIntent = Intent().apply {
                    putExtra(EXTRA_DELETE_ID, todoId)
                }
                activity?.setResult(Activity.RESULT_OK, resultIntent)
                activity?.finish()
            },
            isPrimary = true,
            isDestructive = true
        )
    )

    var dialogState by remember { mutableStateOf(DialogState()) }

    val showDialog: (items: List<DialogItemData>, title: String, message: String?) -> Unit =
        { items, title, message ->
            dialogState =
                DialogState(isVisible = true, items = items, title = title, message = message)
        }

    val dismissDialog = {
        dialogState = dialogState.copy(isVisible = false)
    }

    var menuState by remember { mutableStateOf(MenuState()) }

    val showMenu: (anchorBounds: Rect, items: List<GlasenseMenuItem>) -> Unit =
        { bounds, items ->
            menuState = MenuState(isVisible = true, anchorBounds = bounds, items = items)
        }

    val dismissMenu = {
        menuState = menuState.copy(isVisible = false)
    }

    val context = LocalContext.current
    var isShareSheetVisible by remember { mutableStateOf(false) }
    val requirePermission = stringResource(R.string.calendar_sync_permission_required)
    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = TodoCalendarSyncManager.REQUIRED_PERMISSIONS.all { permission ->
            permissions[permission] == true
        }
        if (granted) {
            viewModel.syncTodoToCalendar(todoId)
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

    fun syncCurrentTodoToCalendar() {
        if (TodoCalendarSyncManager.hasCalendarPermissions(context)) {
            viewModel.syncTodoToCalendar(todoId)
        } else {
            calendarPermissionLauncher.launch(TodoCalendarSyncManager.REQUIRED_PERMISSIONS)
        }
    }

    val moreMenu = rememberMoreMenuItems(
        onDuplicateSelected = {
            dismissMenu()
            scope.launch {
                delay(200.milliseconds)
                viewModel.duplicateById(todoId).join()
                activity?.finish()
            }
        },
        onAddToCalendarSelected = {
            dismissMenu()
            syncCurrentTodoToCalendar()
        },
        onShareSelected = {
            dismissMenu()
            currentItem?.let {
                isShareSheetVisible = true
            }
        }
    )
    var moreButtonBounds by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var dateButtonBounds by remember { mutableStateOf(Rect.Zero) }
    var isDatePickerVisible by remember { mutableStateOf(false) }
    var isGroupBottomSheetVisible by remember { mutableStateOf(false) }

    val showDatePicker: (anchorBounds: Rect) -> Unit = { bounds ->
        dateButtonBounds = bounds
        isDatePickerVisible = true
    }

    val dismissDatePicker = {
        isDatePickerVisible = false
    }

    var isTimeBottomSheetVisible by remember { mutableStateOf(false) }
    var isTimePickerVisible by remember { mutableStateOf(false) }
    var timePickerRequestKey by remember { mutableIntStateOf(0) }
    var timeButtonBounds by remember { mutableStateOf(Rect.Zero) }
    var sheetFinalTime by remember { mutableStateOf<LocalTime?>(null) }
    var sheetMinTime by remember { mutableStateOf<LocalTime?>(null) }
    var sheetMaxTime by remember { mutableStateOf<LocalTime?>(null) }
    var onTimeSelectedCallback by remember { mutableStateOf<(LocalTime?) -> Unit>({}) }

    var isReminderBottomSheetVisible by remember { mutableStateOf(false) }
    var isCustomReminderPopupVisible by remember { mutableStateOf(false) }
    var reminderButtonBounds by remember { mutableStateOf(Rect.Zero) }
    var sheetReminderIsAllDay by remember { mutableStateOf(true) }
    var onReminderSelectedCallback by remember { mutableStateOf<(TodoReminderConfig) -> Unit>({}) }
    var isCustomRepeatBottomSheetVisible by remember { mutableStateOf(false) }

    val noneText = stringResource(R.string.none)
    val ungroupedTodosText = stringResource(R.string.ungrouped_todos)
    val customText = stringResource(R.string.custom)
    val repeatDailyText = stringResource(R.string.repeat_daily)
    val repeatWeeklyText = stringResource(R.string.repeat_weekly)
    val repeatMonthlyText = stringResource(R.string.repeat_monthly)
    val repeatYearlyText = stringResource(R.string.repeat_yearly)
    val repeatTodoItem = currentItem?.todoItem

    fun updateRepeat(config: RepeatRuleConfig?) {
        repeatTodoItem?.let { todoItem ->
            viewModel.updateRepeatRule(todoItem, config)
        }
    }

    val repeatMenuItems = remember(
        repeatTodoItem,
        repeatRule,
        noneText,
        customText,
        repeatDailyText,
        repeatWeeklyText,
        repeatMonthlyText,
        repeatYearlyText
    ) {
        listOf(
            SelectiveMenuItemData(
                text = noneText,
                isSelected = { repeatRule == null },
                onClick = { updateRepeat(null) }
            ),
            MenuDivider,
            SelectiveMenuItemData(
                text = customText,
                isSelected = { repeatRule?.isCustomRepeatRule() == true },
                onClick = { isCustomRepeatBottomSheetVisible = true }
            ),
            MenuDivider,
            SelectiveMenuItemData(
                text = repeatDailyText,
                isSelected = { repeatRule?.isSimpleFrequency(RepeatFrequency.Daily) == true },
                onClick = { updateRepeat(RepeatRuleConfig(frequency = RepeatFrequency.Daily)) }
            ),
            SelectiveMenuItemData(
                text = repeatWeeklyText,
                isSelected = { repeatRule?.isSimpleFrequency(RepeatFrequency.Weekly) == true },
                onClick = { updateRepeat(RepeatRuleConfig(frequency = RepeatFrequency.Weekly)) }
            ),
            SelectiveMenuItemData(
                text = repeatMonthlyText,
                isSelected = { repeatRule?.isSimpleFrequency(RepeatFrequency.Monthly) == true },
                onClick = { updateRepeat(RepeatRuleConfig(frequency = RepeatFrequency.Monthly)) }
            ),
            SelectiveMenuItemData(
                text = repeatYearlyText,
                isSelected = { repeatRule?.isSimpleFrequency(RepeatFrequency.Yearly) == true },
                onClick = { updateRepeat(RepeatRuleConfig(frequency = RepeatFrequency.Yearly)) }
            )
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1.minutes)
            ticker++
        }
    }

    LaunchedEffect(itemWithSubTodos) {
        if (itemWithSubTodos != null) {
            finalDate = itemWithSubTodos?.todoItem?.dueDate
            selectedIndex = itemWithSubTodos?.todoItem?.flag ?: 0
            title = itemWithSubTodos?.todoItem?.title ?: ""
            if (notesText.isEmpty() && !itemWithSubTodos?.todoItem?.notes.isNullOrEmpty()) {
                notesText = itemWithSubTodos?.todoItem?.notes ?: ""
            }
        }
    }

    LaunchedEffect(finalDate) {
        itemWithSubTodos?.let {
            viewModel.update(it.todoItem.copy(dueDate = finalDate))
        }
    }
    LaunchedEffect(title) {
        itemWithSubTodos?.let {
            viewModel.update(it.todoItem.copy(title = title))
        }
    }
    LaunchedEffect(notesText) {
        itemWithSubTodos?.let {
            viewModel.update(it.todoItem.copy(notes = notesText))
        }
    }

    val flagMenu = rememberFlagMenuItems(noneFirst = true) { index ->
        itemWithSubTodos?.let {
            viewModel.update(it.todoItem.copy(flag = index))
        }
    }

    val settingsViewModel: SettingsViewModel = viewModel()
    val isSuperGraphicUltraModernGirlEnabled by settingsViewModel.isSuperGraphicUltraModernGirlEnabled
    val hapticController = LocalHapticFeedback.current

    fun performPressHaptic() {
        hapticController.performHapticFeedback(HapticFeedbackType.ContextClick)
    }

    val density = LocalDensity.current
    Box(
        Modifier
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
                .layerBackdrop(backdrop2)
        ) {
            if (currentItem == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    ProgressView()
                }
            } else {
                PageContent(
                    state = lazyListState,
                    modifier = Modifier
                        .layerBackdrop(backdrop)
                        .imePadding(),
                    tabPadding = false,
                    bottomPadding = 64.dp + navigationBarHeight * 2
                ) {
                    item(key = "status_bar") {
                        Box(
                            modifier = Modifier
                                .animateItem(placementSpec = Springs.crisp())
                                .padding(top = 48.dp + statusBarHeight + 12.dp)
                        )
                    }
                    item(key = "edit") {
                        TodoItemRowEditable(
                            item = currentItem.todoItem,
                            onCheckedChange = { isChecked ->
                                viewModel.update(currentItem.todoItem.copy(isCompleted = isChecked))
                            },
                            modifier = Modifier.animateItem(placementSpec = Springs.crisp()),
                            onEditEnd = { string ->
                                // if update here will cause conflict
                                title = string
                            }
                        )
                        VGap()
                    }
                    item(key = "notes") {
                        ConfigTextField(
                            modifier = Modifier.animateItem(placementSpec = Springs.crisp()),
                            value = notesText,
                            onValueChange = { notesText = it },
                            backgroundColor = AppColors.cardBackground,
                            singleLine = false,
                            decorateText = stringResource(R.string.notes),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Default
                            )
                        )
                        VGap()
                    }
                    item(key = "information") {
                        CompositionLocalProvider(
                            LocalGlasenseContentColor provides AppColors.contentVariant
                        ) {
                            Column(
                                modifier = Modifier
                                    .animateItem(placementSpec = Springs.crisp())
                                    .fillMaxWidth()
                                    .background(
                                        color = AppColors.cardBackground,
                                        shape = AppSpecs.cardShape
                                    )
                                    .padding(horizontal = 12.dp)

                            ) {
                                TodoConfigRow(
                                    icon = painterResource(id = R.drawable.ic_flag),
                                    contentDescription = stringResource(R.string.flag),
                                    title = stringResource(R.string.flag),
                                    onButtonClick = { bounds ->
                                        showMenu(
                                            bounds,
                                            flagMenu
                                        )
                                    }
                                ) {
                                    if (selectedIndex != 0) {
                                        Box(
                                            modifier = Modifier
                                                .padding(end = 6.dp)
                                                .size(12.dp)
                                                .background(
                                                    color = getFlagColor(selectedIndex),
                                                    shape = CircleShape
                                                )
                                        )
                                    }
                                    Text(
                                        text = getFlagText(selectedIndex),
                                        fontWeight = FontWeight.Normal,
                                        color = AppColors.content
                                    )
                                }
                                VDivider()
                                TodoConfigRow(
                                    icon = painterResource(id = R.drawable.ic_folder),
                                    contentDescription = stringResource(R.string.group),
                                    title = stringResource(R.string.group),
                                    onButtonClick = {
                                        performPressHaptic()
                                        isGroupBottomSheetVisible = true
                                    }
                                ) {
                                    val groupName = currentItem.todoItem.groupId
                                        ?.let(homeGroupNames::get)
                                        ?: ungroupedTodosText
                                    Text(
                                        text = groupName,
                                        fontWeight = FontWeight.Normal,
                                        color = AppColors.content
                                    )
                                }
                                VDivider()
                                TodoConfigRow(
                                    icon = painterResource(id = R.drawable.ic_calendar),
                                    contentDescription = stringResource(R.string.due_date),
                                    title = stringResource(R.string.due_date),
                                    onButtonClick = { bounds ->
                                        showDatePicker(bounds)
                                    }
                                ) {
                                    Text(
                                        text = finalDate?.format(DateTimeFormatter.ofPattern("yyyy/M/d"))
                                            ?: stringResource(R.string.none),
                                        fontWeight = FontWeight.Normal,
                                        modifier = Modifier,
                                        color = AppColors.content
                                    )
                                }
                                VDivider()
                                TodoConfigRow(
                                    icon = painterResource(id = R.drawable.ic_clock),
                                    contentDescription = stringResource(R.string.time),
                                    title = stringResource(R.string.time),
                                    onButtonClick = {
                                        performPressHaptic()
                                        isTimeBottomSheetVisible = true
                                    }
                                ) {
                                    val timeText = currentItem.todoItem.formatTimeText()
                                    Text(
                                        text = timeText ?: stringResource(R.string.all_day),
                                        fontWeight = FontWeight.Normal,
                                        color = AppColors.content
                                    )
                                }
                                VDivider()
                                TodoConfigRow(
                                    icon = painterResource(id = R.drawable.ic_alarm),
                                    contentDescription = stringResource(R.string.reminder),
                                    title = stringResource(R.string.reminder),
                                    onButtonClick = {
                                        performPressHaptic()
                                        isReminderBottomSheetVisible = true
                                    }
                                ) {
                                    val height = with(density) { 18.sp.toDp() }
                                    if (currentItem.todoItem.reminderStrong) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_star),
                                            contentDescription = stringResource(R.string.strong_reminder),
                                            modifier = Modifier
                                                .padding(end = 4.dp)
                                                .size(20.dp)
                                                .shrinkBounds(DpSize(height, height)),
                                            tint = AppColors.highlightText
                                        )
                                    }
                                    if (currentItem.todoItem.reminderPersistent) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_clock_cycle),
                                            contentDescription = stringResource(R.string.persistent_reminder),
                                            modifier = Modifier
                                                .padding(end = 6.dp)
                                                .size(20.dp)
                                                .shrinkBounds(DpSize(height, height)),
                                            tint = AppColors.primary
                                        )
                                    }
                                    Text(
                                        text = currentItem.todoItem.formatReminderText(),
                                        fontWeight = FontWeight.Normal,
                                        color = AppColors.content
                                    )
                                }
                                VDivider()
                                TodoConfigRow(
                                    icon = painterResource(id = R.drawable.ic_repeat),
                                    contentDescription = stringResource(R.string.repeat),
                                    title = stringResource(R.string.repeat),
                                    onButtonClick = { bounds ->
                                        showMenu(bounds, repeatMenuItems)
                                    }
                                ) {
                                    Text(
                                        text = repeatRule.displayText(
                                            noneText = noneText,
                                            customText = customText,
                                            dailyText = repeatDailyText,
                                            weeklyText = repeatWeeklyText,
                                            monthlyText = repeatMonthlyText,
                                            yearlyText = repeatYearlyText
                                        ),
                                        fontWeight = FontWeight.Normal,
                                        color = AppColors.content
                                    )
                                }
                            }
                        }
                        VGap()
                    }
                    item(key = "small_title") {
                        Text(
                            text = stringResource(R.string.task),
                            style = GlasenseTheme.type.subHeadline.copy(lineHeight = 14.sp),
                            fontWeight = FontWeight.Normal,
                            color = AppColors.contentVariant,
                            modifier = Modifier
                                .animateItem(placementSpec = Springs.crisp())
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 8.dp, start = 12.dp)
                        )
                    }
                    items(items = currentItem.subTodos, key = { it.id }) { subTodo ->
                        SwipeableSubTodoItemRowEditable(
                            listState = swipeListState,
                            subTodo = subTodo,
                            modifier = Modifier.animateItem(placementSpec = Springs.crisp()),
                            onEditEnd = { string, checked ->
                                viewModel.updateSubTodo(
                                    subTodo.copy(
                                        description = string,
                                        isCompleted = checked
                                    )
                                )
                            },
                            onDelete = {
                                viewModel.deleteSubTodo(subTodo)
                            },
                            onPromote = {
                                scope.launch {
                                    viewModel.insert(
                                        TodoItem(
                                            title = subTodo.description,
                                            isCompleted = subTodo.isCompleted
                                        )
                                    )
                                    viewModel.deleteSubTodo(subTodo)
                                }
                            },
                        )
                        VGap()
                    }
                    item(key = "add") {
                        SubTodoItemRowAdd(
                            modifier = Modifier.animateItem(placementSpec = Springs.crisp()),
                            onEditEnd = { description, checked ->
                                viewModel.insertSubTodo(
                                    SubTodoItem(
                                        parentId = currentItem.todoItem.id,
                                        description = description,
                                        isCompleted = checked
                                    )
                                )
                            }
                        )
                    }
                    paddingItem(lazyListState)
                }
            }
            // A small title that dynamically appears at the top when the user scrolls down
            GlasenseDynamicSmallTitle(
                modifier = Modifier.align(Alignment.TopCenter),
                title = itemWithSubTodos?.todoItem?.title ?: stringResource(R.string.detail),
                statusBarHeight = statusBarHeight,
                isVisible = isSmallTitleVisible,
                backdrop = backdrop,
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
            val sharedInteractionSource = remember { MutableInteractionSource() }
            GlasenseButton(
                enabled = true,
                interactionSource = sharedInteractionSource,
                shape = CircleShape,
                onClick = {},
                modifier = Modifier
                    .padding(top = statusBarHeight, end = 12.dp)
                    .size(48.dp)
                    .align(Alignment.TopEnd),
                colors = AppButtonColors.action()
            ) {
                Box(
                    modifier = Modifier
                        .onGloballyPositioned { coordinates ->
                            moreButtonBounds = coordinates
                        }
                        .height(48.dp)
                        .width(48.dp)
                        .clickable(
                            interactionSource = sharedInteractionSource,
                            indication = null
                        ) {
                            moreButtonBounds?.let {
                                showMenu(
                                    it.boundsInWindow(),
                                    moreMenu
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_ellipsis),
                        contentDescription = stringResource(R.string.more),
                        modifier = Modifier.width(32.dp),
                        tint = AppColors.primary
                    )
                }
            }
            GlasenseBottomBar(
                modifier = Modifier.align(Alignment.BottomCenter),
                navigationBarHeight = navigationBarHeight,
                isVisible = true,
                backdrop = backdrop,
                surfaceColor = surfaceColor,
                height = 64.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = navigationBarHeight + 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val creationTime = remember(itemWithSubTodos, ticker) {
                        itemWithSubTodos?.todoItem?.creationDateTime?.let {
                            formatRelativeTime(it, context)
                        } ?: ""
                    }
                    Text(
                        text = stringResource(R.string.created, creationTime),
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .weight(1f),
                        style = GlasenseTheme.type.subHeadline.copy(
                            lineHeight = 14.sp, shadow = Shadow(
                                color = surfaceColor.copy(alpha = 1f),
                                offset = Offset(x = 0f, y = 0f),
                                blurRadius = 8f
                            )
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val subTodoCount = itemWithSubTodos?.subTodos?.size ?: 0
                    val deleteTodoSimpleText = stringResource(R.string.delete_todo_simple)
                    val deletePluralsText =
                        pluralStringResource(R.plurals.delete_todo_with_subtasks, subTodoCount)
                    val deleteCurrentTodoText = stringResource(R.string.delete_current_todo)
                    val message = if (subTodoCount == 0) {
                        deleteTodoSimpleText
                    } else {
                        deletePluralsText
                    }
                    GlasenseButton(
                        enabled = true,
                        shape = CircleShape,
                        onClick = {
                            showDialog(
                                deleteDialogItems,
                                deleteCurrentTodoText,
                                message
                            )
                        },
                        modifier = Modifier
                            .size(48.dp),
                        colors = AppButtonColors.action()
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_trash),
                            contentDescription = stringResource(R.string.delete_current_todo),
                            modifier = Modifier.width(32.dp),
                            tint = AppColors.error
                        )
                    }
                }

            }

            DueDatePicker(
                isVisible = isDatePickerVisible,
                anchorBounds = dateButtonBounds,
                initialDate = finalDate,
                onDismiss = dismissDatePicker,
                onDateSelected = { date ->
                    finalDate = date
                },
                direction = PopupDirection.Down
            )

            if (isGroupBottomSheetVisible) {
                currentItem?.let { item ->
                    GroupBottomSheet(
                        groups = homeGroups,
                        groupTodoCounts = homeGroupTodoCounts,
                        selectedFilter = item.todoItem.groupId
                            ?.let { HomeGroupFilter.Group(it) }
                            ?: HomeGroupFilter.Ungrouped,
                        onFilterSelected = { filter ->
                            val groupId = when (filter) {
                                HomeGroupFilter.All,
                                HomeGroupFilter.Ungrouped -> null
                                is HomeGroupFilter.Group -> filter.id
                            }
                            viewModel.update(item.todoItem.copy(groupId = groupId))
                        },
                        onCreateGroup = { name -> viewModel.createTodoGroup(name) },
                        onDeleteGroup = viewModel::deleteTodoGroup,
                        onDismissed = { isGroupBottomSheetVisible = false },
                        showAllFilter = false,
                        showRecentlyDeleted = false
                    )
                }
            }

            if (isTimeBottomSheetVisible) {
                DetailTimeBottomSheet(
                    startTime = currentItem?.todoItem?.startTime,
                    endTime = currentItem?.todoItem?.endTime,
                    onTimeChange = { newStartTime, newEndTime ->
                        currentItem?.let {
                            val isAllDayEnabled = newStartTime == null && newEndTime == null
                            val reminderConfig = it.todoItem
                                .toReminderConfig()
                                .compatibleWithAllDay(isAllDayEnabled)
                            viewModel.update(
                                it.todoItem.copy(
                                    startTime = newStartTime,
                                    endTime = newEndTime
                                ).withReminderConfig(reminderConfig)
                            )
                        }
                    },
                    onDismissed = {
                        isTimeBottomSheetVisible = false
                    },
                    onRequestCustomTime = { bounds, initialTime, minTime, maxTime, onSelected ->
                        timePickerRequestKey++
                        timeButtonBounds = bounds
                        sheetFinalTime = initialTime
                        sheetMinTime = minTime
                        sheetMaxTime = maxTime
                        onTimeSelectedCallback = onSelected
                        isTimePickerVisible = true
                    }
                )
            }

            if (isReminderBottomSheetVisible) {
                currentItem?.todoItem?.let { todoItem ->
                    val isAllDayEnabled = todoItem.startTime == null && todoItem.endTime == null
                    if (reminderDraftTodoId != todoItem.id) {
                        reminderDraftTodoId = todoItem.id
                        reminderDraftConfig = todoItem.toReminderConfig()
                        reminderDraftPersistent = todoItem.reminderPersistent
                        reminderDraftStrong = todoItem.reminderStrong
                    }

                    fun updateReminderDraft(
                        config: TodoReminderConfig? = reminderDraftConfig,
                        persistent: Boolean = reminderDraftPersistent,
                        strong: Boolean = reminderDraftStrong
                    ) {
                        isEditingReminderDraft = true
                        val mergedConfig = config?.copy(
                            persistent = persistent,
                            strong = strong
                        )
                        reminderDraftConfig = mergedConfig
                        reminderDraftPersistent = persistent
                        reminderDraftStrong = strong
                        viewModel.update(
                            todoItem.withReminderConfig(mergedConfig).copy(
                                reminderPersistent = persistent,
                                reminderStrong = strong
                            )
                        )
                    }

                    DetailReminderBottomSheet(
                        reminderConfig = reminderDraftConfig,
                        reminderPersistent = reminderDraftPersistent,
                        reminderStrong = reminderDraftStrong,
                        isAllDayEnabled = isAllDayEnabled,
                        onReminderConfigChange = { newConfig ->
                            updateReminderDraft(config = newConfig)
                        },
                        onPersistentChange = { persistent ->
                            updateReminderDraft(persistent = persistent)
                        },
                        onStrongChange = { strong ->
                            updateReminderDraft(strong = strong)
                        },
                        showMenu = showMenu,
                        onRequestCustomReminder = { bounds ->
                            reminderButtonBounds = bounds
                            sheetReminderIsAllDay = isAllDayEnabled
                            onReminderSelectedCallback = { selectedConfig ->
                                updateReminderDraft(
                                    config = selectedConfig.compatibleWithAllDay(isAllDayEnabled)
                                )
                            }
                            isCustomReminderPopupVisible = true
                        },
                        onDismissed = {
                            isEditingReminderDraft = false
                            isReminderBottomSheetVisible = false
                        }
                    )
                }
            }

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

            if (isShareSheetVisible) {
                currentItem?.let { item ->
                    TodoShareSheet(
                        todos = listOf(item),
                        onDismiss = { isShareSheetVisible = false }
                    )
                }
            }
            if (isCustomRepeatBottomSheetVisible) {
                CustomRepeatBottomSheet(
                    initialDate = currentItem?.todoItem?.dueDate,
                    initialConfig = repeatRule?.toCustomRepeatConfig(),
                    showMenu = showMenu,
                    onConfirm = { config ->
                        val anchorDate = currentItem?.todoItem?.dueDate ?: LocalDate.now()
                        val presetFrequency = config.toPresetFrequency(anchorDate)
                        updateRepeat(
                            presetFrequency?.let { RepeatRuleConfig(frequency = it) }
                                ?: config.toRepeatRuleConfig()
                        )
                    },
                    onDismissed = {
                        isCustomRepeatBottomSheetVisible = false
                    }
                )
            }
        }
        GlasenseDialog(
            dialogState = dialogState,
            backdrop = backdrop2,
            onDismiss = { dismissDialog() },
            modifier = Modifier
        )

        GlasenseMenu(
            menuState = menuState,
            backdrop = backdrop2,
            onDismiss = dismissMenu
        )
    }
}

private fun TodoItem.formatTimeText(): String? {
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    return when {
        startTime != null && endTime != null -> {
            "${startTime.format(formatter)}-${endTime.format(formatter)}"
        }

        startTime != null -> startTime.format(formatter)
        endTime != null -> endTime.format(formatter)
        else -> null
    }
}

private fun RepeatRule?.displayText(
    noneText: String,
    customText: String,
    dailyText: String,
    weeklyText: String,
    monthlyText: String,
    yearlyText: String
): String {
    val rule = this ?: return noneText
    if (rule.isCustomRepeatRule()) return customText
    return when (rule.frequency) {
        RepeatFrequency.Daily -> dailyText
        RepeatFrequency.Weekly -> weeklyText
        RepeatFrequency.Monthly -> monthlyText
        RepeatFrequency.Yearly -> yearlyText
    }
}

@Composable
private fun TodoItem.formatReminderText(): String {
    val noneText = stringResource(R.string.none)
    return toReminderConfig()?.displayText(
        noneText = noneText,
        allDayMorningText = stringResource(R.string.reminder_all_day_morning_8),
        oneMinuteBeforeText = stringResource(R.string.reminder_before_1_minute),
        fiveMinutesBeforeText = stringResource(R.string.reminder_before_5_minutes),
        thirtyMinutesBeforeText = stringResource(R.string.reminder_before_30_minutes),
        oneHourBeforeText = stringResource(R.string.reminder_before_1_hour),
        twoHoursBeforeText = stringResource(R.string.reminder_before_2_hours),
        beforePrefix = stringResource(R.string.reminder_before_prefix),
        dueDayText = stringResource(R.string.reminder_due_day),
        daysBeforeFormat = stringResource(R.string.reminder_days_before_format),
        hoursUnitFormat = stringResource(R.string.reminder_hours_unit_format),
        minutesUnitFormat = stringResource(R.string.reminder_minutes_unit_format)
    ) ?: noneText
}

@Composable
fun getFlagText(index: Int): String {
    val flagNames = listOf(
        stringResource(R.string.none),
        stringResource(R.string.flag_red),
        stringResource(R.string.flag_orange),
        stringResource(R.string.flag_yellow),
        stringResource(R.string.flag_green),
        stringResource(R.string.flag_blue),
        stringResource(R.string.flag_purple),
        stringResource(R.string.flag_gray)
    )
    return flagNames[index]
}

@Composable
fun TodoConfigRow(
    icon: Painter,
    contentDescription: String,
    title: String,
    onButtonClick: (Rect) -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    var bounds by remember { mutableStateOf(Rect.Zero) }
    Row(
        modifier = Modifier.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = icon,
            contentDescription = contentDescription,
            modifier = Modifier
                .padding(end = 8.dp)
                .width(28.dp)
        )
        Text(
            text = title,
            fontWeight = FontWeight.Normal,
            modifier = Modifier
                .align(Alignment.CenterVertically)
        )
        Spacer(modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier
                .onGloballyPositioned { coordinates ->
                    bounds = coordinates.boundsInWindow()
                }
                .align(Alignment.CenterVertically)
                .wrapContentSize()
                .clip(Capsule())
                .background(
                    color = AppColors.scrimNormal
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = DimIndication()
                ) {
                    onButtonClick(bounds)
                }
                .padding(
                    horizontal = 8.dp,
                    vertical = 4.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

fun Modifier.shrinkBounds(
    targetSize: DpSize
): Modifier {
    return this.layout { measurable, constraints ->
        val height = targetSize.height.roundToPx()
        val width = targetSize.width.roundToPx()
        val placeable = measurable.measure(constraints)
        layout(width, height) {
            placeable.placeRelative(
                x = (width - placeable.width) / 2,
                y = (height - placeable.height) / 2
            )
        }
    }
}
