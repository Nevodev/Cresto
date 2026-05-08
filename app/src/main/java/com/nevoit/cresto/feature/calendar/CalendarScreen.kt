package com.nevoit.cresto.feature.calendar

import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.shapes.Capsule
import com.nevoit.cresto.R
import com.nevoit.cresto.data.todo.EXTRA_TODO_ID
import com.nevoit.cresto.data.todo.TodoViewModel
import com.nevoit.cresto.feature.detail.DetailActivity
import com.nevoit.cresto.feature.home.TodoListItemRow
import com.nevoit.cresto.feature.settings.util.SettingsViewModel
import com.nevoit.cresto.theme.AppButtonColors
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.LocalGlasenseSettings
import com.nevoit.cresto.theme.linearGradientMaskT2B70
import com.nevoit.cresto.toolkit.gaussiangradient.smoothGradientMask
import com.nevoit.cresto.toolkit.gaussiangradient.smoothGradientMaskFallbackInvert
import com.nevoit.cresto.ui.components.CustomAnimatedVisibility
import com.nevoit.cresto.ui.components.glasense.GlasenseButtonToolBar
import com.nevoit.cresto.ui.components.glasense.rememberSwipeableListState
import com.nevoit.cresto.ui.components.myFadeIn
import com.nevoit.cresto.ui.components.myFadeOut
import com.nevoit.cresto.ui.components.packed.PageContent
import com.nevoit.cresto.ui.components.packed.VGap
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@OptIn(ExperimentalHazeApi::class)
@Composable
fun BoxScope.CalendarScreen() {
    val lazyListState = rememberLazyListState()

    val localDateSaver = Saver<LocalDate, String>(
        save = { it.toString() },
        restore = { LocalDate.parse(it) }
    )
    val yearMonthSaver = Saver<YearMonth, String>(
        save = { it.toString() },
        restore = { YearMonth.parse(it) }
    )

    var selectedDate by rememberSaveable(stateSaver = localDateSaver) { mutableStateOf(LocalDate.now()) }
    var displayMonth by rememberSaveable(stateSaver = yearMonthSaver) {
        mutableStateOf(
            YearMonth.from(
                selectedDate
            )
        )
    }

    val viewModel: TodoViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()

    val datesWithTodo by viewModel.datesWithTodo.collectAsStateWithLifecycle()

    val todosForSelectedDate by remember(selectedDate) {
        viewModel.getTodosByDate(selectedDate)
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val isOverdueMarkerEnabled by settingsViewModel.isOverdueMarker

    val selectedItemIds by viewModel.selectedItemIds.collectAsStateWithLifecycle()
    val isSelectionModeActive by viewModel.isSelectionModeActive.collectAsStateWithLifecycle()

    val interactionSource = remember { MutableInteractionSource() }
    val swipeListState = rememberSwipeableListState()

    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (lazyListState.isScrollInProgress) {
            swipeListState.close()
        }
    }
    LaunchedEffect(isSelectionModeActive) {
        if (!isSelectionModeActive) {
            swipeListState.close()
        }
    }

    if (isSelectionModeActive) {
        BackHandler { viewModel.clearSelections() }
    }

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val deleteId = result.data?.getIntExtra("extra_delete_id", -1) ?: -1
            if (deleteId != -1) {
                viewModel.deleteById(deleteId)
            }
        }
    }

    val metrics = rememberCalendarMetrics()
    var calendarOffsetPx by remember(metrics.maxCollapseOffsetPx) { mutableFloatStateOf(-metrics.maxCollapseOffsetPx) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < 0 && calendarOffsetPx > -metrics.maxCollapseOffsetPx) {
                    val oldOffset = calendarOffsetPx
                    calendarOffsetPx =
                        (calendarOffsetPx + delta).coerceIn(-metrics.maxCollapseOffsetPx, 0f)
                    return Offset(0f, calendarOffsetPx - oldOffset)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val delta = available.y
                if (delta > 0 && calendarOffsetPx < 0f) {
                    val oldOffset = calendarOffsetPx
                    calendarOffsetPx =
                        (calendarOffsetPx + delta).coerceIn(-metrics.maxCollapseOffsetPx, 0f)
                    return Offset(0f, calendarOffsetPx - oldOffset)
                }
                return Offset.Zero
            }
        }
    }

    val monthFormatter = remember { DateTimeFormatter.ofPattern("M月") }

    val density = LocalDensity.current
    var headerHeightPx by remember { mutableFloatStateOf(0f) }
    val hazeState = rememberHazeState()
    val backgroundColor = AppColors.pageBackground
    val blur = !LocalGlasenseSettings.current.liteMode

    val isBlurVisible by remember {
        derivedStateOf {
            lazyListState.canScrollBackward && calendarOffsetPx <= -metrics.maxCollapseOffsetPx || calendarOffsetPx > -metrics.maxCollapseOffsetPx && lazyListState.canScrollBackward
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        PageContent(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState, 0f),
            tabPadding = true,
            topPadding = { with(density) { headerHeightPx.toDp() } }
        ) {
            itemsIndexed(
                items = todosForSelectedDate,
                key = { _, item -> item.todoItem.id }
            ) { index, item ->
                TodoListItemRow(
                    item = item,
                    showDate = false,
                    isDueTodayMarkerEnabled = false,
                    isOverdueMarkerEnabled = isOverdueMarkerEnabled,
                    onCheckedChange = { isChecked ->
                        viewModel.update(item.todoItem.copy(isCompleted = isChecked))
                    },
                    isSelected = item.todoItem.id in selectedItemIds,
                    isSelectionModeActive = isSelectionModeActive,
                    overlayInteractionSource = interactionSource,
                    swipeListState = swipeListState,
                    onEnterSelection = { viewModel.enterSelectionMode(item.todoItem.id) },
                    onToggleSelection = { viewModel.toggleSelection(item.todoItem.id) },
                    onOpenDetail = {
                        val intent = Intent(context, DetailActivity::class.java).apply {
                            putExtra(EXTRA_TODO_ID, item.todoItem.id)
                        }
                        launcher.launch(intent)
                    },
                    onDelete = { viewModel.delete(item.todoItem) },
                    onCheckboxTapPosition = { }
                )
                if (index != todosForSelectedDate.lastIndex) {
                    VGap()
                }
            }
        }
        CustomAnimatedVisibility(
            visible = isBlurVisible,
            enter = myFadeIn(),
            exit = myFadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (blur) Modifier.hazeEffect(hazeState) {
                        blurRadius = 2.dp
                        noiseFactor = 0f
                        inputScale = HazeInputScale.Fixed(0.5f)
                        mask = linearGradientMaskT2B70
                        style = HazeStyle(backgroundColor = backgroundColor, tint = null)
                    } else Modifier)
                    .then(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Modifier.smoothGradientMask(
                            backgroundColor.copy(alpha = 1f),
                            backgroundColor.copy(alpha = 0f),
                            0.5f,
                            0.5f,
                            0.7f
                        ) else Modifier.smoothGradientMaskFallbackInvert(backgroundColor, 0.7f)
                    )
                    .statusBarsPadding()
                    .height(with(density) { headerHeightPx.toDp() })
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    headerHeightPx = coordinates.size.height.toFloat()
                }
        ) {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayMonth.format(monthFormatter),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    maxLines = 1,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )

                val sharedInteractionSource = remember { MutableInteractionSource() }

                GlasenseButtonToolBar(
                    enabled = true,
                    interactionSource = sharedInteractionSource,
                    shape = Capsule(),
                    onClick = {},
                    colors = AppButtonColors.action()
                ) {
                    Row(
                        modifier = Modifier
                            .height(48.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .height(48.dp)
                                .width(48.dp)
                                .clickable(
                                    interactionSource = sharedInteractionSource,
                                    indication = null
                                ) {
                                    viewModel.showBottomSheet(date = selectedDate)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_add_large),
                                contentDescription = stringResource(R.string.add_new_todo),
                                modifier = Modifier.width(32.dp),
                                tint = AppColors.primary
                            )
                        }
                    }
                }
            }
            VGap()
            WeekDaysIndicator()
            Spacer(Modifier.height(4.dp))
            MonthlyPagerCalendar(
                selectedDate = selectedDate,
                datesWithTodo = datesWithTodo,
                onDateSelected = { selectedDate = it },
                onMonthChanged = { displayMonth = it },
                collapseFractionProvider = { abs(calendarOffsetPx) / metrics.maxCollapseOffsetPx }
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}
