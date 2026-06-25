package com.nevoit.cresto.feature.bottomsheet

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nevoit.cresto.R
import com.nevoit.cresto.data.todo.RepeatFrequency
import com.nevoit.cresto.data.todo.RepeatRuleConfig
import com.nevoit.cresto.data.todo.TodoViewModel
import com.nevoit.cresto.feature.screenextract.AiExtractSource
import com.nevoit.cresto.feature.screenextract.ScreenExtractEvents
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.ui.components.glasense.DialogItemData
import com.nevoit.cresto.ui.components.glasense.GlasenseMenuItem
import com.nevoit.cresto.ui.components.packed.TodoReminderConfig
import com.nevoit.cresto.ui.components.packed.compatibleWithAllDay
import com.nevoit.cresto.ui.viewmodel.AiSideEffect
import com.nevoit.cresto.ui.viewmodel.AiViewModel
import com.nevoit.cresto.ui.viewmodel.UiState
import com.nevoit.glasense.core.modifier.cachedClip
import com.nevoit.glasense.core.utility.deviceCornerShape
import com.nevoit.glasense.theme.tokens.Springs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.LocalTime
import kotlin.time.Duration.Companion.milliseconds

internal enum class SheetInputMode { Basic, Advanced }

private fun defaultRangeStartTime(now: LocalTime = LocalTime.now()): LocalTime {
    val hour = when {
        now.hour >= 23 -> 23
        now.minute >= 30 -> now.hour + 1
        else -> now.hour
    }
    return LocalTime.of(hour, 0)
}

private fun defaultRangeEndTime(startTime: LocalTime): LocalTime {
    return if (startTime.hour >= 23) {
        LocalTime.of(23, 59)
    } else {
        startTime.plusHours(1)
    }
}


/**
 * A composable function that displays a bottom sheet with custom animations.
 *
 * @param onDismiss Callback function to be invoked when the bottom sheet is dismissed.
 * @param onAddClick Callback function to be invoked when the "add" button inside the sheet is clicked.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BottomSheet(
    onDismiss: () -> Unit,
    onAddClick: (String, String, Int, LocalDate?, LocalTime?, LocalTime?, TodoReminderConfig?, RepeatFrequency?, RepeatRuleConfig?) -> Unit,
    aiViewModel: AiViewModel = viewModel(),
    showDialog: (items: List<DialogItemData>, title: String, message: String?) -> Unit,
    showMenu: (anchorBounds: Rect, items: List<GlasenseMenuItem>) -> Unit,
    onRequestCustomDate: (Rect, LocalDate?, (LocalDate?) -> Unit) -> Unit,
    onRequestCustomTime: (Rect, LocalTime?, LocalTime?, LocalTime?, (LocalTime?) -> Unit) -> Unit,
    onRequestCustomReminder: (Rect, Boolean, (TodoReminderConfig) -> Unit) -> Unit
) {
    val scope = rememberCoroutineScope()

    val density = LocalDensity.current
    val context = LocalContext.current

    val uiState by aiViewModel.uiState.collectAsState()
    val windowInfo = LocalWindowInfo.current

    // State to control the visibility of the bottom sheet and its scrim.
    var isVisible by remember { mutableStateOf(false) }
    var hasSlidedIn by remember { mutableStateOf(false) }

    var currentInputMode by remember { mutableStateOf(SheetInputMode.Basic) }
    var isReturningFromAdvanced by remember { mutableStateOf(false) }
    val basicFocusRequester = remember { FocusRequester() }

    val bottomSheetHeight =
        windowInfo.containerDpSize.height - WindowInsets.statusBars.asPaddingValues()
            .calculateTopPadding()
    val bottomSheetHeightPx = with(density) { bottomSheetHeight.toPx() }
    var innerHeightPx by remember { mutableIntStateOf(0) }

    val scaleAnimation = remember { Animatable(0f) }

    // Coroutine scope for launching animations.

    val keyboardController = LocalSoftwareKeyboardController.current

    val isImeVisible = WindowInsets.isImeVisible

    val state = rememberTextFieldState()
    val viewModel: TodoViewModel = koinViewModel()

    val bottomSheetUiState by viewModel.bottomSheetState.collectAsState()

    var finalDate by remember {
        mutableStateOf<LocalDate?>(
            bottomSheetUiState.initialDate ?: LocalDate.now()
        )
    }

    val errorDialogItems = listOf(
        DialogItemData(
            stringResource(R.string.ok),
            onClick = {},
            isPrimary = true,
            isDestructive = true
        )
    )
    val isLoading = uiState is UiState.Loading
    val errorTitle = stringResource(R.string.error)

    val offset = remember { Animatable(bottomSheetHeightPx) }
    val totalOffset = remember { Animatable(bottomSheetHeightPx) }
    var isReturningToBasic by remember { mutableStateOf(false) }

    val imeHeight =
        WindowInsets.ime.exclude(WindowInsets.navigationBars).getBottom(density).toFloat()

    LaunchedEffect(offset.value, imeHeight, currentInputMode, isReturningToBasic) {
        if (currentInputMode == SheetInputMode.Basic && !isReturningToBasic) {
            totalOffset.snapTo(offset.value - imeHeight)
        }
    }

    fun adjustOffset() {
        when (currentInputMode) {
            SheetInputMode.Basic -> {
                scope.launch {
                    try {
                        totalOffset.animateTo(
                            targetValue = offset.value - imeHeight,
                            animationSpec = Springs.smooth(300)
                        )
                    } finally {
                        if (currentInputMode == SheetInputMode.Basic) {
                            isReturningToBasic = false
                        }
                    }
                }
            }

            SheetInputMode.Advanced -> {
                scope.launch {
                    totalOffset.animateTo(
                        targetValue = 0f,
                        animationSpec = Springs.smooth(350)
                    )
                }
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult

        scope.launch {
            val imageDataUrl = withContext(Dispatchers.IO) {
                uri.toImageDataUrl(context)
            }

            if (imageDataUrl.isBlank()) {
                showDialog(errorDialogItems, errorTitle, "图片读取失败，请重试")
                return@launch
            }

            aiViewModel.generateContentFromImage(imageDataUrl)
        }
    }

    LaunchedEffect(true) {
        aiViewModel.sideEffect.collect { effect ->
            when (effect) {
                is AiSideEffect.ProcessSuccess -> {
                    ScreenExtractEvents.emitPendingTodos(
                        effect.response.items,
                        AiExtractSource.InApp
                    )
                }

                is AiSideEffect.ShowError -> {
                    showDialog(
                        errorDialogItems,
                        errorTitle,
                        effect.message
                    )
                }
            }
        }
    }

    val navigationBarHeightPx = WindowInsets.navigationBars.getBottom(density).toFloat()

    LaunchedEffect(Unit) {
        isVisible = true
    }
    // Animate the bottom sheet into view when its height is measured.
    var isReady by remember { mutableStateOf(false) }

    var composeAiInput by remember { mutableStateOf(false) }
    var targetAiInputVisible by remember { mutableStateOf(false) }
    var notesText by remember { mutableStateOf("") }
    var isTimeRangeEnabled by remember { mutableStateOf(false) }
    var isAllDayEnabled by remember { mutableStateOf(true) }
    val initialRangeStartTime = remember { defaultRangeStartTime() }
    var rangeStartTime by remember { mutableStateOf(initialRangeStartTime) }
    var rangeEndTime by remember { mutableStateOf(defaultRangeEndTime(initialRangeStartTime)) }
    var reminderConfig by remember { mutableStateOf<TodoReminderConfig?>(null) }
    var reminderPersistent by remember { mutableStateOf(false) }
    var reminderStrong by remember { mutableStateOf(false) }
    var repeatFrequency by remember { mutableStateOf<RepeatFrequency?>(null) }
    var customRepeatConfig by remember { mutableStateOf<CustomRepeatConfig?>(null) }
    var isCustomRepeatBottomSheetVisible by remember { mutableStateOf(false) }

    fun closeAiInput() {
        targetAiInputVisible = false
        scope.launch {
            scaleAnimation.animateTo(
                targetValue = 0f,
                animationSpec = tween(200)
            )
            // Wait for advanced page slide animation to complete 
            // before removing AiInputBox from composition to avoid mid-animation stutters
            // caused by TextField disposal and InputMethodManager IPC overhead.
            delay(200.milliseconds)
            if (!targetAiInputVisible) {
                composeAiInput = false
            }
        }
    }

    fun showAiInput() {
        targetAiInputVisible = true
        scope.launch {
            composeAiInput = true
            delay(300.milliseconds)
            if (targetAiInputVisible) {
                scaleAnimation.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(0.8f, 300f, 0.001f)
                )
            }
        }
    }

    LaunchedEffect(isReady, bottomSheetHeightPx, innerHeightPx, navigationBarHeightPx) {
        if (isReady) {
            isVisible = true
            scope.launch {
                if (!hasSlidedIn) {
                    showAiInput()
                    offset.animateTo(
                        targetValue = bottomSheetHeightPx - innerHeightPx - navigationBarHeightPx,
                        animationSpec = tween(
                            durationMillis = 200,
                            delayMillis = 100,
                            easing = CubicBezierEasing(.2f, .2f, 0f, 1f)
                        )
                    )
                    hasSlidedIn = true
                } else {
                    offset.snapTo(bottomSheetHeightPx - innerHeightPx - navigationBarHeightPx)
                }
            }
        }
    }

    var showAdvancedPage by remember { mutableStateOf(false) }
    var animateAdvancedPage by remember { mutableStateOf(false) }

    val screenWidth = with(density) { windowInfo.containerDpSize.width.toPx() }
    val advancedPageHorizontalOffset = remember { Animatable(screenWidth) }
    val basicScrimAlpha = remember { Animatable(0f) }

    fun slideAdvancedPage(isIn: Boolean = true) {
        if (isIn) {
            showAdvancedPage = true
            if (animateAdvancedPage) {
                closeAiInput()
                adjustOffset()
                scope.launch {
                    advancedPageHorizontalOffset.animateTo(0f, Springs.smooth(300))
                }
                scope.launch {
                    basicScrimAlpha.animateTo(0.3f, tween(300))
                }
            }
        } else {
            showAiInput()
            adjustOffset()
            scope.launch {
                advancedPageHorizontalOffset.animateTo(screenWidth, Springs.smooth(300))
                showAdvancedPage = false
                animateAdvancedPage = false
            }
            scope.launch {
                basicScrimAlpha.animateTo(0f, tween(300))
            }
        }
    }

    LaunchedEffect(animateAdvancedPage) {
        if (animateAdvancedPage) {
            closeAiInput()
            adjustOffset()
            launch {
                advancedPageHorizontalOffset.animateTo(0f, Springs.smooth(300))
            }
            launch {
                basicScrimAlpha.animateTo(0.3f, tween(300))
            }
        }
    }
    fun navigateToBasic() {
        isReturningToBasic = true
        currentInputMode = SheetInputMode.Basic
        slideAdvancedPage(false)
    }

    fun navigateToAdvanced() {
        keyboardController?.hide()
        isReturningFromAdvanced = true
        isReturningToBasic = false
        currentInputMode = SheetInputMode.Advanced

        slideAdvancedPage(true)
    }

    fun slideOut() {
        scope.launch {
            isVisible = false
            scope.launch {
                closeAiInput()
                aiViewModel.cancelRequest()
                aiViewModel.clearState()
            }
            totalOffset.stop()
            offset.animateTo(
                targetValue = bottomSheetHeightPx,
                animationSpec = tween(
                    durationMillis = 200,
                    delayMillis = 0,
                    easing = FastOutSlowInEasing
                )
            )
            onDismiss()
        }
    }

    val bottomSheetShape = deviceCornerShape(
        bottomLeft = false,
        bottomRight = false
    )

    BackHandler {
        slideOut()
    }

    // Main container for the bottom sheet and scrim.
    Box(modifier = Modifier.fillMaxSize()) {
        DismissScrim(visible = isVisible) {
            if (isImeVisible) {
                keyboardController?.hide()
            } else {
                slideOut()
            }
        }

        // The bottom sheet content itself.
        Column(
            modifier = Modifier
                .graphicsLayer {
                    translationY = totalOffset.value
                }
                .layout { measurable, constraints ->
                    val unboundedConstraints = constraints.copy(maxHeight = Constraints.Infinity)
                    val placeable = measurable.measure(unboundedConstraints)
                    if (!isReady) isReady = true
                    layout(placeable.width, constraints.maxHeight) {
                        placeable.place(0, constraints.maxHeight - placeable.height)
                    }
                }
        ) {
            if (composeAiInput) {
                AiInputBox(
                    modifier = Modifier.graphicsLayer {
                        scaleX = scaleAnimation.value
                        scaleY = scaleAnimation.value
                    },
                    isLoading = isLoading,
                    textFieldState = state,
                    aiViewModel = aiViewModel,
                    imagePickerLauncher = imagePickerLauncher
                )
            }
            // Container for the AddTodoSheet content.
            Box(
                modifier = Modifier
                    .height(bottomSheetHeight)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = true,
                        onClick = {}
                    )
                    .cachedClip(
                        bottomSheetShape
                    )
                    .background(
                        color = AppColors.elevatedPageBackground
                    )
                    .fillMaxWidth()
            ) {
                AddTodoSheet(
                    modifier = Modifier.layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        if (innerHeightPx != placeable.measuredHeight) innerHeightPx =
                            placeable.measuredHeight
                        layout(placeable.width, placeable.height) {
                            placeable.place(0, 0)
                        }
                    },
                    finalDate = finalDate,
                    onFinalDateChange = { finalDate = it },
                    focusRequester = basicFocusRequester,
                    autoRequestFocus = !isReturningFromAdvanced,
                    onAddClick = { title, flagIndex, date ->
                        scope.launch {
                            slideOut()
                            val startTime = when {
                                isAllDayEnabled -> null
                                else -> rangeStartTime
                            }
                            val endTime = when {
                                isAllDayEnabled -> null
                                isTimeRangeEnabled -> rangeEndTime
                                else -> null
                            }
                            val reminder = reminderConfig?.copy(
                                persistent = reminderPersistent,
                                strong = reminderStrong
                            ).compatibleWithAllDay(isAllDayEnabled)
                            onAddClick(
                                title,
                                notesText,
                                flagIndex,
                                date,
                                startTime,
                                endTime,
                                reminder,
                                repeatFrequency,
                                customRepeatConfig?.toRepeatRuleConfig()
                            )
                        }
                    }, onClose = {
                        keyboardController?.hide()
                        slideOut()
                    }, onRequestCustomDate = onRequestCustomDate,
                    onNavigate = {
                        navigateToAdvanced()
                    }
                )

                if (showAdvancedPage) {
                    BackHandler {
                        navigateToBasic()
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                this.alpha = basicScrimAlpha.value
                            }
                            .background(Color.Black))
                    AdvancedPage(
                        modifier = Modifier
                            .graphicsLayer {
                                translationX = advancedPageHorizontalOffset.value
                            }
                            .layout { measurable, constraints ->
                                val placeable = measurable.measure(constraints)
                                if (!animateAdvancedPage) animateAdvancedPage = true
                                layout(placeable.width, placeable.height) {
                                    placeable.place(0, 0)
                                }
                            },
                        notesText = notesText,
                        onNotesChange = { notesText = it },
                        finalDate = finalDate,
                        onFinalDateChange = { finalDate = it },
                        isTimeRangeEnabled = isTimeRangeEnabled,
                        isAllDayEnabled = isAllDayEnabled,
                        rangeStartTime = rangeStartTime,
                        rangeEndTime = rangeEndTime,
                        onTimeRangeEnabledChange = { enabled ->
                            isTimeRangeEnabled = enabled
                            if (enabled) {
                                isAllDayEnabled = false
                                rangeEndTime = defaultRangeEndTime(rangeStartTime)
                            }
                        },
                        onAllDayEnabledChange = { enabled ->
                            isAllDayEnabled = enabled
                            if (enabled) {
                                isTimeRangeEnabled = false
                                reminderConfig =
                                    reminderConfig.compatibleWithAllDay(isAllDayEnabled = true)
                            }
                        },
                        onRangeStartTimeChange = { newTime ->
                            if (newTime != null) {
                                rangeStartTime = newTime
                                isAllDayEnabled = false
                            }
                        },
                        onRangeEndTimeChange = { newTime ->
                            if (newTime != null) {
                                rangeEndTime = newTime
                                isAllDayEnabled = false
                            }
                        },
                        reminderConfig = reminderConfig,
                        onReminderConfigChange = { reminderConfig = it },
                        reminderPersistent = reminderPersistent,
                        reminderStrong = reminderStrong,
                        onReminderPersistentChange = { reminderPersistent = it },
                        onReminderStrongChange = { reminderStrong = it },
                        repeatFrequency = repeatFrequency,
                        customRepeatConfig = customRepeatConfig,
                        onRepeatFrequencyChange = {
                            repeatFrequency = it
                            customRepeatConfig = null
                        },
                        onRequestCustomRepeat = {
                            isCustomRepeatBottomSheetVisible = true
                        },
                        showMenu = showMenu,
                        onRequestCustomDate = onRequestCustomDate,
                        onRequestCustomTime = onRequestCustomTime,
                        onRequestCustomReminder = { bounds ->
                            onRequestCustomReminder(bounds, isAllDayEnabled) { config ->
                                reminderConfig = config.compatibleWithAllDay(isAllDayEnabled)
                            }
                        },
                        navigateToBasic = {
                            navigateToBasic()
                        })
                }
            }
        }

        if (isCustomRepeatBottomSheetVisible) {
            CustomRepeatBottomSheet(
                initialDate = finalDate,
                initialConfig = customRepeatConfig
                    ?: repeatFrequency?.toCustomRepeatConfig(finalDate ?: LocalDate.now()),
                showMenu = showMenu,
                onConfirm = { config ->
                    val anchorDate = finalDate ?: LocalDate.now()
                    val presetFrequency = config.toPresetFrequency(anchorDate = anchorDate)
                    if (presetFrequency != null) {
                        repeatFrequency = presetFrequency
                        customRepeatConfig = null
                    } else {
                        customRepeatConfig = config
                        repeatFrequency = null
                    }
                },
                onDismissed = {
                    isCustomRepeatBottomSheetVisible = false
                }
            )
        }
    }
}

private fun Uri.toImageDataUrl(context: Context): String {
    val resolver = context.contentResolver
    val mime = resolver.getType(this) ?: "image/jpeg"
    val bytes = resolver.openInputStream(this)?.use { it.readBytes() } ?: return ""

    // Keep payload size bounded to reduce request failures on very large images.
    val maxSizeBytes = 5 * 1024 * 1024
    if (bytes.size > maxSizeBytes) return ""

    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
    return "data:$mime;base64,$base64"
}

@Composable
private fun DismissScrim(
    visible: Boolean,
    onDismiss: () -> Unit
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 0.4f else 0f,
        animationSpec = tween(200)
    )
    Box(
        modifier = Modifier
            .graphicsLayer {
                this.alpha = alpha
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = true,
                onClick = {
                    onDismiss()
                }
            )
            .background(Color.Black)
            .fillMaxSize())
}
