package com.nevoit.cresto.ui.components.bottomsheet

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nevoit.cresto.R
import com.nevoit.cresto.data.todo.TodoViewModel
import com.nevoit.cresto.theme.AppButtonColors
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.AppSpecs
import com.nevoit.cresto.ui.components.glasense.DialogItemData
import com.nevoit.cresto.ui.components.glasense.DimIndication
import com.nevoit.cresto.ui.components.glasense.GlasenseButton
import com.nevoit.cresto.ui.components.glasense.GlasenseSwitch
import com.nevoit.cresto.ui.components.glasense.ZeroHeightDivider
import com.nevoit.cresto.ui.components.glasense.extend.overscrollSpacer
import com.nevoit.cresto.ui.components.packed.ConfigItem
import com.nevoit.cresto.ui.components.packed.ConfigItemContainer
import com.nevoit.cresto.ui.components.packed.ConfigTextField
import com.nevoit.cresto.ui.components.packed.VGap
import com.nevoit.cresto.ui.viewmodel.AiSideEffect
import com.nevoit.cresto.ui.viewmodel.AiViewModel
import com.nevoit.cresto.ui.viewmodel.UiState
import com.nevoit.cresto.util.deviceCornerShape
import com.nevoit.glasense.theme.Springs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import kotlin.time.Duration.Companion.milliseconds

enum class SheetInputMode { Basic, Advanced }

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
    onAddClick: (String, Int, LocalDate?) -> Unit,
    aiViewModel: AiViewModel = viewModel(),
    showDialog: (items: List<DialogItemData>, title: String, message: String?) -> Unit,
    onRequestCustomDate: (Rect, LocalDate?, (LocalDate?) -> Unit) -> Unit
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

    val imeHeight =
        WindowInsets.ime.exclude(WindowInsets.navigationBars).getBottom(density).toFloat()

    LaunchedEffect(offset.value, imeHeight) {
        if (currentInputMode == SheetInputMode.Basic) {
            totalOffset.snapTo(offset.value - imeHeight)
        }
    }

    LaunchedEffect(currentInputMode) {
        if (currentInputMode == SheetInputMode.Advanced) {
            totalOffset.animateTo(
                targetValue = 0f,
                animationSpec = Springs.smooth(350)
            )
        } else if (currentInputMode == SheetInputMode.Basic) {
            totalOffset.animateTo(
                targetValue = offset.value - imeHeight,
                animationSpec = Springs.smooth(300)
            )
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
                    viewModel.insertAiGeneratedTodos(effect.response.items)
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

    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val navigationBarHeightPx = WindowInsets.navigationBars.getBottom(density).toFloat()

    LaunchedEffect(Unit) {
        isVisible = true
    }
    // Animate the bottom sheet into view when its height is measured.
    var isReady by remember { mutableStateOf(false) }

    var composeAiInput by remember { mutableStateOf(false) }

    fun closeAiInput() {
        scope.launch {
            scaleAnimation.animateTo(
                targetValue = 0f,
                animationSpec = tween(200)
            )
            composeAiInput = false
        }
    }

    fun showAiInput() {
        scope.launch {
            composeAiInput = true
            delay(300.milliseconds)
            scaleAnimation.animateTo(
                targetValue = 1f,
                animationSpec = spring(0.8f, 300f, 0.001f)
            )
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
                scope.launch {
                    advancedPageHorizontalOffset.animateTo(0f, Springs.smooth(400))
                }
                scope.launch {
                    basicScrimAlpha.animateTo(0.3f, tween(300))
                }
            }
        } else {
            showAiInput()
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
            launch {
                advancedPageHorizontalOffset.animateTo(0f, Springs.smooth(400))
            }
            launch {
                basicScrimAlpha.animateTo(0.3f, tween(300))
            }
        }
    }
    fun navigateToBasic() {
        currentInputMode = SheetInputMode.Basic
        slideAdvancedPage(false)
    }

    fun navigateToAdvanced() {
        keyboardController?.hide()
        isReturningFromAdvanced = true
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
                    isReady = true
                    layout(placeable.width, constraints.maxHeight) {
                        placeable.place(0, constraints.maxHeight - placeable.height)
                    }
                }
        ) {
            Box(modifier = Modifier.height(300.dp))
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
                    .clip(
                        bottomSheetShape
                    )
                    .background(
                        color = AppColors.pageBackground
                    )
                    .fillMaxWidth()
            ) {
                AddTodoSheet(
                    modifier = Modifier.layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        innerHeightPx = placeable.measuredHeight
                        layout(placeable.width, placeable.height) {
                            placeable.place(0, 0)
                        }
                    },
                    initialDate = bottomSheetUiState.initialDate,
                    focusRequester = basicFocusRequester,
                    autoRequestFocus = !isReturningFromAdvanced,
                    onAddClick = { title, flagIndex, finalDate ->
                        scope.launch {
                            slideOut()
                            onAddClick(title, flagIndex, finalDate)
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
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                translationX = advancedPageHorizontalOffset.value
                            }
                            .layout { measurable, constraints ->
                                val placeable = measurable.measure(constraints)
                                animateAdvancedPage = true
                                layout(placeable.width, placeable.height) {
                                    placeable.place(0, 0)
                                }
                            }
                            .fillMaxSize()
                            .background(AppColors.pageBackground)
                            .padding(horizontal = 12.dp)
                    ) {
                        val lazyListState = rememberLazyListState()

                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = navigationBarHeight)
                        ) {
                            item { Spacer(Modifier.height(48.dp + 12.dp + 12.dp)) }
                            item {
                                ConfigTextField(
                                    value = "",
                                    onValueChange = {},
                                    backgroundColor = AppColors.cardBackground,
                                    singleLine = false,
                                    decorateText = "备注",
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Uri,
                                        imeAction = ImeAction.Done
                                    )
                                )
                                VGap()
                            }
                            item {
                                Text(
                                    text = "日期",
                                    fontSize = 14.sp,
                                    lineHeight = 14.sp,
                                    color = AppColors.contentVariant,
                                    modifier = Modifier
                                        .padding(
                                            start = 12.dp,
                                            top = 0.dp,
                                            end = 12.dp,
                                            bottom = 12.dp
                                        )
                                        .fillMaxWidth()
                                )
                            }
                            item {
                                ConfigItemContainer(
                                    backgroundColor = AppColors.cardBackground,
                                    title = "时间"
                                ) {
                                    Column {
                                        val style = TextStyle(
                                            fontFeatureSettings = "tnum",
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 24.sp,
                                            lineHeight = 24.sp
                                        )

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(
                                                8.dp,
                                                Alignment.CenterHorizontally
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(AppSpecs.cardShape)
                                                    .background(color = AppColors.scrimNormal)
                                                    .clickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = DimIndication()
                                                    ) {

                                                    }
                                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                            ) {
                                                Text(
                                                    text = "13" + "∶" + "00",
                                                    modifier = Modifier.align(Alignment.Center),
                                                    style = style
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        ZeroHeightDivider()
                                        Spacer(modifier = Modifier.height(8.dp))
                                        ConfigItem(title = "时间段") {
                                            GlasenseSwitch(
                                                backgroundColor = AppColors.cardBackground,
                                                checked = false,
                                                onCheckedChange = { })
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        ZeroHeightDivider()
                                        Spacer(modifier = Modifier.height(8.dp))
                                        ConfigItem(title = "全天") {
                                            GlasenseSwitch(
                                                backgroundColor = AppColors.cardBackground,
                                                checked = false,
                                                onCheckedChange = { })
                                        }
                                    }
                                }
                                VGap()
                            }
                            item {
                                ConfigItemContainer(
                                    backgroundColor = AppColors.cardBackground,
                                    title = "提醒"
                                ) {
                                    Column {
                                        ConfigItem(title = "提醒时机") {

                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        ZeroHeightDivider()
                                        Spacer(modifier = Modifier.height(8.dp))
                                        ConfigItem(title = "持续提醒") {
                                            GlasenseSwitch(
                                                backgroundColor = AppColors.cardBackground,
                                                checked = false,
                                                onCheckedChange = { })
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        ZeroHeightDivider()
                                        Spacer(modifier = Modifier.height(8.dp))
                                        ConfigItem(title = "强提醒") {
                                            GlasenseSwitch(
                                                backgroundColor = AppColors.cardBackground,
                                                checked = false,
                                                onCheckedChange = { })
                                        }
                                    }
                                }
                                VGap()
                            }
                            item {
                                ConfigItemContainer(
                                    backgroundColor = AppColors.cardBackground,
                                    title = "重复"
                                ) {
                                    Column {
                                        ConfigItem(title = "重复周期") {

                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        ZeroHeightDivider()
                                        Spacer(modifier = Modifier.height(8.dp))
                                        ConfigItem(title = "过期后顺延") {
                                            GlasenseSwitch(
                                                backgroundColor = AppColors.cardBackground,
                                                checked = false,
                                                onCheckedChange = { })
                                        }
                                    }
                                }
                            }
                            item {
                                VGap()
                            }
                            overscrollSpacer(lazyListState)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .height(48.dp)
                        ) {
                            GlasenseButton(
                                enabled = true,
                                shape = CircleShape,
                                onClick = { navigateToBasic() },
                                modifier = Modifier
                                    .width(48.dp)
                                    .height(48.dp),
                                colors = AppButtonColors.secondary(),
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_forward_nav),
                                    contentDescription = stringResource(R.string.back),
                                    modifier = Modifier.width(28.dp)
                                )
                            }
                            Text(
                                text = stringResource(R.string.advanced),
                                modifier = Modifier.align(Alignment.Center),
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                    }
                }
            }
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
fun DismissScrim(
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
