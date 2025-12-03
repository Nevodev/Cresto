package com.nevoit.cresto.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.capsule.ContinuousRoundedRectangle
import com.nevoit.cresto.R
import com.nevoit.cresto.data.todo.TodoViewModel
import com.nevoit.cresto.ui.components.CustomAnimatedVisibility
import com.nevoit.cresto.ui.components.glasense.GlasenseButtonAlt
import com.nevoit.cresto.ui.components.glasense.GlasenseDynamicSmallTitle
import com.nevoit.cresto.ui.components.glasense.GlasenseLoadingIndicator
import com.nevoit.cresto.ui.components.glasense.GlasensePageHeader
import com.nevoit.cresto.ui.components.packed.CardWithTitle
import com.nevoit.cresto.ui.components.packed.CardWithoutTitle
import com.nevoit.cresto.ui.components.packed.CircularTimer
import com.nevoit.cresto.ui.components.packed.StrictText
import com.nevoit.cresto.ui.components.packed.ZenCirclesBreathing
import com.nevoit.cresto.ui.theme.glasense.AppButtonColors
import com.nevoit.cresto.ui.theme.glasense.CalculatedColor
import com.nevoit.cresto.ui.theme.glasense.Red500
import com.nevoit.cresto.ui.theme.glasense.defaultEnterTransition
import com.nevoit.cresto.ui.theme.glasense.defaultExitTransition
import com.nevoit.cresto.ui.theme.glasense.glasenseHighlight
import com.nevoit.cresto.ui.viewmodel.ModeTimerViewModel
import com.nevoit.cresto.util.g2
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeApi::class)
@Composable
fun MindFlowScreen(
    viewModel: TodoViewModel,
    timerViewModel: ModeTimerViewModel = viewModel()
) {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val density = LocalDensity.current
    val thresholdPx = if (statusBarHeight > 0.dp) {
        with(density) {
            (statusBarHeight + 24.dp).toPx()
        }
    } else 0f

    val hazeState = rememberHazeState()

    val backgroundColor = CalculatedColor.hierarchicalBackgroundColor
    val surfaceColor = CalculatedColor.hierarchicalSurfaceColor

    val lazyListState = rememberLazyListState()

    val isSmallTitleVisible by remember(thresholdPx) { derivedStateOf { ((lazyListState.firstVisibleItemIndex == 0) && (lazyListState.firstVisibleItemScrollOffset > thresholdPx)) || lazyListState.firstVisibleItemIndex > 0 } }

    val stats by viewModel.statistics.collectAsStateWithLifecycle()
    val dailyStats by viewModel.dailyStats.collectAsStateWithLifecycle()
    val todayStat = dailyStats.find { it.date == LocalDate.now() }
    val completedCount = todayStat?.count ?: 0

    val setupMinutes = timerViewModel.setupMinutes
    val finalMinutes = if (setupMinutes == 0) 1 else setupMinutes
    val isStopwatch = setupMinutes == 0
    val isRunning = timerViewModel.isRunning
    val isTimerMode = timerViewModel.isTimerMode
    val isFinished = timerViewModel.isFinished
    val isPaused = timerViewModel.isPaused


    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                clip = true
            }
            .background(backgroundColor)
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .hazeSource(hazeState, 0f)
                .fillMaxSize()
                .padding(0.dp)
                .background(backgroundColor),
            contentPadding = PaddingValues(
                start = 12.dp,
                top = 0.dp,
                end = 12.dp,
                bottom = 136.dp
            )
        ) {
            item {
                GlasensePageHeader(
                    title = stringResource(R.string.mind_flow),
                    statusBarHeight = statusBarHeight
                )
            }
            item {
                Row(modifier = Modifier) {
                    CardWithoutTitle(
                        modifier = Modifier
                            .weight(1f)
                            .animateContentSize()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Spacer(Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .size(288.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CustomAnimatedVisibility(
                                    visible = isTimerMode,
                                    enter = defaultEnterTransition,
                                    exit = defaultExitTransition
                                ) {
                                    Text(
                                        text = timerViewModel.formattedTime,
                                        fontSize = 48.sp,
                                        fontWeight = FontWeight.W400,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                        style = TextStyle(
                                            fontFeatureSettings = "tnum"
                                        )
                                    )
                                }
                                CustomAnimatedVisibility(
                                    visible = !isTimerMode,
                                    enter = defaultEnterTransition,
                                    exit = defaultExitTransition
                                ) {
                                    CircularTimer(
                                        modifier = Modifier
                                            .size(288.dp),
                                        currentMinutes = setupMinutes,
                                        onMinutesChange = { timerViewModel.updateSetupTime(it) },
                                        startIcon = painterResource(R.drawable.ic_bell),
                                        endIcon = painterResource(R.drawable.ic_play),
                                        knobSize = 36.dp,
                                        iconSize = 24.dp,
                                        strokeWidth = 48.dp,
                                        thumbWidth = 36.dp,
                                        progressColor = surfaceColor,
                                        trackColor = backgroundColor,
                                        iconColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onBackground,
                                        innerIconSize = 24.dp
                                    )
                                }
                                ZenCirclesBreathing(backgroundColor = surfaceColor, scale = 1.8f)
                            }
                            Spacer(Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                            ) {
                                CustomAnimatedVisibility(
                                    visible = isStopwatch && !isRunning,
                                    enter = defaultEnterTransition,
                                    exit = defaultExitTransition
                                ) {
                                    Text(
                                        text = stringResource(R.string.stopwatch),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.W400,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )
                                }
                                CustomAnimatedVisibility(
                                    visible = isStopwatch && isRunning && !isPaused,
                                    enter = defaultEnterTransition,
                                    exit = defaultExitTransition
                                ) {
                                    Text(
                                        text = stringResource(R.string.counting_up),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.W400,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )
                                }
                                CustomAnimatedVisibility(
                                    visible = !isStopwatch && !isRunning && !isFinished,
                                    enter = defaultEnterTransition,
                                    exit = defaultExitTransition
                                ) {
                                    Text(
                                        text = stringResource(R.string.min, finalMinutes),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.W400,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                        style = TextStyle(
                                            fontFeatureSettings = "tnum"
                                        )
                                    )
                                }
                                CustomAnimatedVisibility(
                                    visible = isFinished,
                                    enter = defaultEnterTransition,
                                    exit = defaultExitTransition
                                ) {
                                    Text(
                                        text = stringResource(R.string.finished),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.W400,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                    )
                                }
                                CustomAnimatedVisibility(
                                    visible = !isStopwatch && isRunning && !isPaused,
                                    enter = defaultEnterTransition,
                                    exit = defaultExitTransition
                                ) {
                                    Text(
                                        text = stringResource(R.string.in_progress_timer),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.W400,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                    )
                                }
                                CustomAnimatedVisibility(
                                    visible = isRunning && isPaused,
                                    enter = defaultEnterTransition,
                                    exit = defaultExitTransition
                                ) {
                                    Text(
                                        text = stringResource(R.string.paused),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.W400,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .height(48.dp)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                CustomAnimatedVisibility(
                                    visible = !isTimerMode,
                                    enter = defaultEnterTransition,
                                    exit = defaultExitTransition
                                ) {
                                    GlasenseButtonAlt(
                                        enabled = true,
                                        shape = ContinuousRoundedRectangle(12.dp, g2),
                                        onClick = { timerViewModel.startTimer() },
                                        modifier = Modifier
                                            .height(48.dp)
                                            .width(96.dp)
                                            .glasenseHighlight(12.dp, 3.dp),
                                        colors = AppButtonColors.primary()
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(modifier = Modifier.size(24.dp)) {
                                                CustomAnimatedVisibility(
                                                    visible = isStopwatch,
                                                    enter = defaultEnterTransition,
                                                    exit = defaultExitTransition
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.ic_timer),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                                CustomAnimatedVisibility(
                                                    visible = !isStopwatch,
                                                    enter = defaultEnterTransition,
                                                    exit = defaultExitTransition
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.ic_hourglass),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = stringResource(R.string.start),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                                CustomAnimatedVisibility(
                                    visible = isTimerMode,
                                    enter = defaultEnterTransition,
                                    exit = defaultExitTransition
                                ) {
                                    Row(modifier = Modifier.height(48.dp)) {
                                        GlasenseButtonAlt(
                                            enabled = true,
                                            shape = ContinuousRoundedRectangle(24.dp, g2),
                                            onClick = { timerViewModel.exitTimerMode() },
                                            modifier = Modifier
                                                .height(48.dp)
                                                .width(48.dp)
                                                .glasenseHighlight(24.dp, 3.dp),
                                            colors = AppButtonColors.primary()
                                                .copy(containerColor = Red500)
                                        ) {
                                            Box(modifier = Modifier.size(24.dp)) {
                                                Icon(
                                                    painter = painterResource(R.drawable.ic_stop),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(32.dp)
                                                )
                                            }
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        GlasenseButtonAlt(
                                            enabled = true,
                                            shape = ContinuousRoundedRectangle(24.dp, g2),
                                            onClick = { if (isPaused) timerViewModel.resumeTimer() else timerViewModel.pauseTimer() },
                                            modifier = Modifier
                                                .height(48.dp)
                                                .width(48.dp)
                                                .glasenseHighlight(24.dp, 3.dp),
                                            colors = AppButtonColors.primary()
                                        ) {
                                            Box(modifier = Modifier.size(24.dp)) {
                                                CustomAnimatedVisibility(
                                                    visible = isPaused,
                                                    enter = defaultEnterTransition,
                                                    exit = defaultExitTransition
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.ic_play),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(32.dp)
                                                    )
                                                }
                                                CustomAnimatedVisibility(
                                                    visible = !isPaused,
                                                    enter = defaultEnterTransition,
                                                    exit = defaultExitTransition
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.ic_pause),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(32.dp)
                                                    )
                                                }

                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.height(12.dp))
            }
            item {
                Row(modifier = Modifier.height(160.dp)) {
                    CardWithTitle(
                        title = stringResource(R.string.today_stat),
                        icon = painterResource(R.drawable.ic_mini_checkmark_seal),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize(),
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f),
                                horizontalAlignment = Alignment.Start
                            ) {
                                StrictText(
                                    text = "$completedCount",
                                    fontSize = 48.sp,
                                    lineHeight = 48.sp,
                                    letterSpacing = (-2).sp,
                                    fontWeight = FontWeight.W300,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                                Text(
                                    text = stringResource(R.string.completed),
                                    fontSize = 14.sp,
                                    lineHeight = 14.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(.5f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            Text(
                                text = "Great!",
                                fontSize = 14.sp,
                                lineHeight = 14.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(.5f)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    CardWithTitle(
                        title = stringResource(R.string.statistics),
                        icon = painterResource(R.drawable.ic_mini_analytics),
                        modifier = Modifier.weight(1f)
                    ) { GlasenseLoadingIndicator(modifier = Modifier.fillMaxSize(), size = 24.dp) }
                }
            }
            item {

            }
        }
        GlasenseDynamicSmallTitle(
            modifier = Modifier.align(Alignment.TopCenter),
            title = stringResource(R.string.mind_flow),
            statusBarHeight = statusBarHeight,
            isVisible = isSmallTitleVisible,
            hazeState = hazeState,
            surfaceColor = backgroundColor
        ) {
        }
    }

}
