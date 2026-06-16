package com.nevoit.cresto.feature.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceIn
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.Capsule
import com.kyant.shapes.UnevenRoundedRectangle
import com.nevoit.cresto.R
import com.nevoit.cresto.data.statistics.DailyStat
import com.nevoit.cresto.data.todo.InsightsUiState
import com.nevoit.cresto.data.todo.PressureLevel
import com.nevoit.cresto.data.todo.PressureSource
import com.nevoit.cresto.data.todo.TodoViewModel
import com.nevoit.cresto.data.todo.calculatePressureIndex
import com.nevoit.cresto.data.todo.pressureLevelOf
import com.nevoit.cresto.feature.settings.SettingsActivity
import com.nevoit.cresto.feature.settings.SettingsDestination
import com.nevoit.cresto.theme.AppButtonColors
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.ui.components.glasense.GlasenseButtonToolBar
import com.nevoit.cresto.ui.components.glasense.GlasenseDynamicSmallTitle
import com.nevoit.cresto.ui.components.glasense.GlasensePageHeader
import com.nevoit.cresto.ui.components.glasense.isScrolledPast
import com.nevoit.cresto.ui.components.packed.CardWithTitle
import com.nevoit.cresto.ui.components.packed.PageContent
import com.nevoit.glasense.component.paddingItem
import com.nevoit.glasense.core.component.HGap
import com.nevoit.glasense.core.component.Icon
import com.nevoit.glasense.core.component.Text
import com.nevoit.glasense.core.component.VGap
import com.nevoit.glasense.theme.GlasenseTheme
import com.nevoit.glasense.theme.lumify
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun BoxScope.InsightsScreen(viewModel: TodoViewModel) {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val lazyListState = rememberLazyListState()

    val isSmallTitleVisible by lazyListState.isScrolledPast(statusBarHeight + 24.dp)

    val context = LocalContext.current
    val insights by viewModel.insights.collectAsStateWithLifecycle()

    val backgroundColor = AppColors.pageBackground

    val backdrop = rememberLayerBackdrop {
        drawRect(
            color = backgroundColor,
            size = Size(this.size.width * 3, this.size.height * 3),
            topLeft = Offset(-this.size.width, -this.size.height)
        )
        drawContent()
    }

    PageContent(
        state = lazyListState,
        modifier = Modifier
            .layerBackdrop(backdrop),
        tabPadding = true
    ) {
        item {
            GlasensePageHeader(
                title = stringResource(R.string.insights)
            )
        }
        item {
            Row {
                TodayOverviewCard(insights)
                HGap()
                PressureIndexCard(insights)
            }
        }
        item {
            VGap()
        }
        item {
            BacklogCard(insights = insights)
        }
        item {
            VGap()
        }
        item {
            WeeklyTrendCard(insights = insights)
        }
        paddingItem(lazyListState)
    }
    GlasenseDynamicSmallTitle(
        modifier = Modifier.align(Alignment.TopCenter),
        title = stringResource(R.string.insights),
        statusBarHeight = statusBarHeight,
        isVisible = isSmallTitleVisible,
        backdrop = backdrop
    ) {
    }
    Box(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .statusBarsPadding()
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 12.dp)
    ) {
        GlasenseButtonToolBar(
            enabled = true,
            shape = CircleShape,
            onClick = {
                context.startActivity(
                    SettingsActivity.createIntent(context, SettingsDestination.SETTINGS)
                )
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(48.dp),
            colors = AppButtonColors.action(),
            interactionSource = remember { MutableInteractionSource() }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_gear),
                contentDescription = stringResource(R.string.settings),
                modifier = Modifier.width(32.dp)
            )
        }
    }

}

@Composable
private fun RowScope.PressureIndexCard(insights: InsightsUiState) {
    val pressureIndex = remember(insights) { calculatePressureIndex(insights) }
    val pressureColor = pressureColor(pressureIndex.score)
    val pressureLevelText = when (pressureLevelOf(pressureIndex.score)) {
        PressureLevel.HIGH -> stringResource(R.string.insights_pressure_level_high)
        PressureLevel.MEDIUM -> stringResource(R.string.insights_pressure_level_medium)
        PressureLevel.LOW -> stringResource(R.string.insights_pressure_level_low)
        PressureLevel.CLEAR -> stringResource(R.string.insights_pressure_level_clear)
    }
    val sourceText = when (pressureIndex.primarySource) {
        PressureSource.OVERDUE -> stringResource(R.string.insights_pressure_source_overdue)
        PressureSource.TODAY -> stringResource(R.string.insights_pressure_source_today)
        PressureSource.BACKLOG -> stringResource(R.string.insights_pressure_source_backlog)
        PressureSource.WEEK -> stringResource(R.string.insights_pressure_source_week)
        PressureSource.NONE -> stringResource(R.string.insights_pressure_source_none)
    }

    CardWithTitle(
        icon = painterResource(R.drawable.ic_ecg),
        title = stringResource(R.string.insights_pressure_index),
        modifier = Modifier
            .weight(1f)
            .height(176.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PressureGauge(
                score = pressureIndex.score,
                level = pressureLevelText,
                color = pressureColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = sourceText,
                color = AppColors.content,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PressureGauge(
    score: Int,
    level: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val progress = score.coerceIn(0, 100) / 100f
    val contentColor = AppColors.content
    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomCenter
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 8.dp.toPx()
            val startAngle = 150f
            val sweepAngle = 240f
            val radius =
                minOf(size.width / 2f - strokeWidth / 2f, size.height / 1.5f - strokeWidth / 2f)
            val center = Offset(size.width / 2f, radius + strokeWidth / 2f)
            val arcSize = Size(radius * 2f, radius * 2f)
            val arcTopLeft = Offset(center.x - radius, center.y - radius)
            val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)

            drawArc(
                color = contentColor.copy(alpha = 0.08f),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = stroke
            )
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle * progress,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = stroke
            )

            val angle = Math.toRadians((startAngle + sweepAngle * progress).toDouble())
            drawCircle(
                color = color,
                radius = 4.dp.toPx(),
                center = Offset(
                    x = center.x + cos(angle).toFloat() * radius,
                    y = center.y + sin(angle).toFloat() * radius
                )
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Text(
                text = score.toString(),
                color = color,
                style = GlasenseTheme.type.largeTitle.copy(lineHeight = 32.sp),
                fontWeight = FontWeight.W500,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = level,
                color = AppColors.contentVariant,
                style = GlasenseTheme.type.footnote,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RowScope.TodayOverviewCard(insights: InsightsUiState) {
    val primaryColor = AppColors.primary

    CardWithTitle(
        icon = painterResource(R.drawable.ic_checkmark_circle),
        title = stringResource(R.string.insights_today_overview),
        modifier = Modifier
            .weight(1f)
            .height(176.dp)
    ) {
        Column {
            val text = buildAnnotatedString {
                append(insights.todayCompleted.toString())
                withStyle(
                    SpanStyle(
                        color = AppColors.contentVariant,
                        fontSize = 18.sp
                    )
                ) {
                    append("/${insights.todayTotal}")
                }
            }
            Text(
                text = text,
                color = AppColors.content,
                fontSize = 36.sp,
                lineHeight = 36.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Row {
                Text(
                    text = stringResource(R.string.completed),
                    color = AppColors.contentVariant,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatPercent(insights.todayProgress),
                    color = AppColors.content,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            CapsuleProgressBar(
                progress = insights.todayProgress,
                color = primaryColor
            )
        }
    }
}

@Composable
private fun WeeklyTrendCard(insights: InsightsUiState) {
    CardWithTitle(
        title = stringResource(R.string.insights_weekly_trend),
        icon = painterResource(R.drawable.ic_mini_analytics),
        modifier = Modifier
            .fillMaxWidth()
            .height(236.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatBlock(
                    label = stringResource(R.string.insights_completed_this_week),
                    value = insights.weekCompletedTotal.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatBlock(
                    label = stringResource(R.string.insights_week_completion),
                    value = formatPercent(insights.weekDueProgress),
                    modifier = Modifier.weight(1f)
                )
            }
            WeeklyTrendBars(
                trend = insights.weeklyCompletedTrend,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
            )
        }
    }
}

@Composable
private fun BacklogCard(insights: InsightsUiState) {
    val oldestPendingText = insights.oldestPendingAgeDays?.let { ageDays ->
        stringResource(R.string.insights_days_format, ageDays.toInt())
    } ?: stringResource(R.string.insights_no_pending)

    CardWithTitle(
        title = stringResource(R.string.insights_backlog),
        icon = painterResource(R.drawable.ic_tray_full),
        modifier = Modifier
            .fillMaxWidth()
            .height(184.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatBlock(
                    label = stringResource(R.string.insights_pending_total),
                    value = insights.pendingTotal.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatBlock(
                    label = stringResource(R.string.overdue),
                    value = insights.overdueTotal.toString(),
                    valueColor = if (insights.overdueTotal > 0) AppColors.error else AppColors.content,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CompactMetric(
                    label = stringResource(R.string.insights_oldest_pending),
                    value = oldestPendingText,
                    modifier = Modifier.weight(1f)
                )
                CompactMetric(
                    label = stringResource(R.string.insights_stale_pending),
                    value = insights.stalePendingTotal.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatBlock(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = AppColors.content
) {
    Column(modifier = modifier) {
        Text(
            text = value,
            color = valueColor,
            fontSize = 36.sp,
            lineHeight = 36.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            color = AppColors.contentVariant,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CompactMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.content.copy(alpha = 0.05f))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = AppColors.contentVariant,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            color = AppColors.content,
            fontSize = 15.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.W500,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun WeeklyTrendBars(
    trend: List<DailyStat>,
    modifier: Modifier = Modifier
) {
    val maxCount = trend.maxOfOrNull { it.count } ?: 0
    val weekdayFormatter = remember { DateTimeFormatter.ofPattern("E", Locale.getDefault()) }
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(AppColors.primary, AppColors.primary.lumify(1.3f))
    )

    Box(modifier = modifier) {
        if (maxCount == 0) {
            Text(
                text = stringResource(R.string.insights_empty_trend),
                color = AppColors.contentVariant,
                style = GlasenseTheme.type.subHeadline,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            trend.forEach { stat ->
                val barProgress = if (maxCount > 0) stat.count.toFloat() / maxCount else 0f
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.62f)
                                .fillMaxHeight(barProgress.coerceIn(0f, 1f))
                                .clip(UnevenRoundedRectangle(topStart = 8.dp, topEnd = 8.dp))
                                .then(if (barProgress > 0f) Modifier.background(gradientBrush) else Modifier)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stat.date.format(weekdayFormatter),
                        color = AppColors.contentVariant,
                        fontSize = 11.sp,
                        lineHeight = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private fun formatPercent(progress: Float): String {
    return "${(progress.coerceIn(0f, 1f) * 100).roundToInt()}%"
}

@Composable
private fun pressureColor(score: Int): Color {
    return lerp(
        AppColors.primary,
        AppColors.highlightText,
        score.coerceIn(0, 100) / 100f
    )
}

@Composable
private fun CapsuleProgressBar(
    modifier: Modifier = Modifier,
    progress: Float = 0f,
    color: Color = AppColors.primary
) {
    Box(
        modifier = modifier
            .heightIn(min = 8.dp)
            .fillMaxWidth()
    ) {
        Canvas(
            modifier = Modifier.matchParentSize()
        ) {
            val outline = Capsule().createOutline(size, layoutDirection, this)
            val progressOutline = Capsule().createOutline(
                Size(
                    size.width * progress.fastCoerceIn(0f, 1f),
                    size.height
                ), layoutDirection, this
            )
            drawOutline(outline, color = color.copy(.2f))
            drawOutline(progressOutline, color = color)
        }
    }
}