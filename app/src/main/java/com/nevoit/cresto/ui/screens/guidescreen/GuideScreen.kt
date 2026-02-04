package com.nevoit.cresto.ui.screens.guidescreen

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.capsule.ContinuousRoundedRectangle
import com.nevoit.cresto.R
import com.nevoit.cresto.ui.components.CustomAnimatedVisibility
import com.nevoit.cresto.ui.components.glasense.GlasenseButtonAlt
import com.nevoit.cresto.ui.components.glasense.isScrolledPast
import com.nevoit.cresto.ui.theme.glasense.AppButtonColors
import com.nevoit.cresto.ui.theme.glasense.AppColors
import com.nevoit.cresto.ui.theme.glasense.AppSpecs
import com.nevoit.cresto.ui.theme.glasense.Blue500
import com.nevoit.cresto.ui.theme.glasense.Indigo500
import com.nevoit.cresto.ui.theme.glasense.Pink400
import com.nevoit.cresto.ui.theme.glasense.Purple500
import com.nevoit.cresto.ui.theme.glasense.defaultEnterTransition
import com.nevoit.cresto.ui.theme.glasense.defaultExitTransition
import com.nevoit.cresto.ui.theme.glasense.glasenseHighlight
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GuideScreen(onFinish: () -> Unit) {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val density = LocalDensity.current

    val hazeState = rememberHazeState()

    val onSurfaceContainer = AppColors.scrimNormal
    val onBackground = AppColors.content
    val surfaceColor = AppColors.pageBackground
    val hierarchicalSurfaceColor = AppColors.cardBackground

    val lazyListState = rememberLazyListState()

    val isSmallTitleVisible by lazyListState.isScrolledPast(statusBarHeight + 24.dp)

    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    var showButton by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        scope.launch {
            delay(2500)
            showButton = true
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaceColor)
    ) {

        Column() {
            Spacer(Modifier.height(statusBarHeight))
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f),
                contentPadding = PaddingValues(),

                ) { page ->
                when (page) {
                    0 -> WelcomePage()
                    1 -> InformationPage()
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .padding(horizontal = 48.dp)
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                CustomAnimatedVisibility(
                    visible = showButton,
                    enter = defaultEnterTransition,
                    exit = defaultExitTransition
                ) {
                    GlasenseButtonAlt(
                        onClick = {
                            if (pagerState.currentPage + 1 < pagerState.pageCount) {
                                scope.launch {
                                    pagerState.animateScrollToPage(
                                        pagerState.currentPage + 1,
                                        animationSpec = spring(1f, 300f)
                                    )
                                }
                            } else if (pagerState.currentPage == pagerState.currentPage) {
                                onFinish()
                            }
                        },
                        shape = AppSpecs.buttonShape,
                        modifier = Modifier
                            .fillMaxSize()
                            .glasenseHighlight(AppSpecs.buttonCorner),
                        colors = AppButtonColors.primary()
                    ) {
                        CustomAnimatedVisibility(
                            visible = pagerState.currentPage == 0,
                            enter = defaultEnterTransition,
                            exit = defaultExitTransition
                        ) {
                            Text(text = "开始")
                        }
                        CustomAnimatedVisibility(
                            visible = pagerState.currentPage == 1,
                            enter = defaultEnterTransition,
                            exit = defaultExitTransition
                        ) {
                            Text(text = "完成")
                        }
                    }
                }
            }
            Spacer(Modifier.height(navigationBarHeight + 64.dp))
        }
    }
}

@Composable
fun WelcomePage() {
    val density = LocalDensity.current

    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            val highlightBlurRadius = with(density) {
                24.dp.toPx()
            }
            val transition = rememberInfiniteTransition(label = "LightScan")
            val highlightProgress by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                    initialStartOffset = StartOffset(1600)
                ),
                label = "Progress"
            )
            val highlightPaint = remember {
                Paint().apply {
                    color = Color.Gray.copy(.8f)
                    blendMode = BlendMode.ColorDodge
                    asFrameworkPaint().maskFilter = BlurMaskFilter(
                        highlightBlurRadius,
                        BlurMaskFilter.Blur.NORMAL
                    )
                }
            }

            val revealBlurRadius = with(density) {
                48.dp.toPx()
            }
            val revealProgress = remember { Animatable(0f) }
            val alpha = remember { Animatable(0f) }

            LaunchedEffect(Unit) {
                revealProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 3000,
                        delayMillis = 500,
                        easing = CubicBezierEasing(0.3f, 0.1f, 0.5f, 1.0f)
                    )
                )
            }
            val revealProgress2 = remember { Animatable(0f) }

            LaunchedEffect(Unit) {
                revealProgress2.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 4000,
                        delayMillis = 100,
                        easing = CubicBezierEasing(0.3f, 0.1f, 0.2f, 1.0f)
                    )
                )
            }

            LaunchedEffect(Unit) {
                alpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 2000,
                        delayMillis = 500
                    )
                )
            }
            val blurPaint = remember {
                Paint().apply {
                    color = Color.Black
                    blendMode = BlendMode.DstOut
                    asFrameworkPaint().apply {
                        maskFilter = BlurMaskFilter(revealBlurRadius, BlurMaskFilter.Blur.NORMAL)
                    }
                }
            }

            val innerRadius = with(density) {
                16.dp.toPx()
            }

            val innerPaint = remember {
                Paint().apply {
                    blendMode = BlendMode.DstOut
                    color = Color.Red
                    asFrameworkPaint().maskFilter =
                        BlurMaskFilter(innerRadius, BlurMaskFilter.Blur.INNER)
                }
            }

            Image(
                painter = painterResource(R.drawable.cresto),
                contentDescription = "App Icon",
                modifier = Modifier
                    .size(96.dp)
                    .clip(ContinuousRoundedRectangle(24.dp))
                    .drawBackdrop(
                        backdrop = rememberLayerBackdrop(),
                        shape = { ContinuousRoundedRectangle(24.dp) },
                        effects = {},
                        highlight = {
                            Highlight.Default.copy(
                                width = 1.dp,
                                blurRadius = 1.dp,
                                style = HighlightStyle.Default(angle = -45f),
                                alpha = alpha.value
                            )
                        })
                    .drawWithContent() {
                        val outline = ContinuousRoundedRectangle(24.dp).createOutline(
                            size = size,
                            layoutDirection,
                            density
                        )
                        val diagonal = sqrt(size.width * size.width + size.height * size.height)

                        drawIntoCanvas { canvas ->
                            canvas.saveLayer(size.toRect(), Paint())

                            val totalDistance = diagonal + revealBlurRadius * 2
                            val currentPos =
                                (revealProgress2.value * totalDistance) - revealBlurRadius

                            drawContent()
                            canvas.save()
                            canvas.translate(0f, size.height)
                            canvas.rotate(-45f)
                            canvas.drawRect(
                                left = currentPos,
                                top = -diagonal * 2,
                                right = diagonal * 2,
                                bottom = diagonal * 2,
                                paint = blurPaint
                            )
                            canvas.restore()
                            canvas.drawOutline(outline = outline, paint = innerPaint)
                            canvas.restore()
                        }

                        drawIntoCanvas { canvas ->
                            canvas.saveLayer(size.toRect(), Paint())

                            val totalDistance = diagonal + revealBlurRadius * 2
                            val currentPos =
                                (revealProgress.value * totalDistance) - revealBlurRadius

                            drawContent()
                            canvas.save()
                            canvas.translate(0f, size.height)
                            canvas.rotate(-45f)
                            canvas.drawRect(
                                left = currentPos,
                                top = -diagonal * 2,
                                right = diagonal * 2,
                                bottom = diagonal * 2,
                                paint = blurPaint
                            )
                            canvas.restore()
                        }

                        drawIntoCanvas { canvas ->
                            val lightWidth = size.width / 2
                            val barWidth = lightWidth + highlightBlurRadius * 2

                            canvas.save()

                            canvas.translate(size.width / 2, size.height / 2)
                            canvas.rotate(-45f)

                            val totalDistance = barWidth + diagonal + highlightBlurRadius * 2
                            val current =
                                totalDistance * highlightProgress - barWidth - highlightBlurRadius - diagonal / 2

                            canvas.drawRect(
                                left = current,
                                top = -diagonal,
                                right = current + lightWidth,
                                bottom = diagonal,
                                paint = highlightPaint
                            )

                            canvas.restore()
                        }
                    }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
        item {
            Text(
                text = "欢迎使用Cresto",
                modifier = Modifier.fillMaxWidth(),
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun InformationPage() {
    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        item { Spacer(modifier = Modifier.height(96.dp)) }
        item {
            Text(
                text = "Cresto可以",
                modifier = Modifier.fillMaxWidth(),
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold
            )
        }
        item { Spacer(modifier = Modifier.height(36.dp)) }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .background(color = AppColors.cardBackground, shape = AppSpecs.cardShape)
                    .padding(all = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_checklist),
                        contentDescription = null,
                        modifier = Modifier
                            .size(32.dp),
                        tint = Blue500
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "极简待办记录",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "清爽无扰的界面，快速添加、标记完成，让你的每日规划一目了然。",
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.alpha(.5f)
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(12.dp)) }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .background(color = AppColors.cardBackground, shape = AppSpecs.cardShape)
                    .padding(all = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_sparkles),
                        contentDescription = null,
                        modifier = Modifier
                            .size(36.dp)
                            .graphicsLayer {
                                compositingStrategy = CompositingStrategy.Offscreen
                            }
                            .drawWithContent {
                                val brush = Brush.sweepGradient(
                                    colorStops = arrayOf(
                                        0f to Pink400,
                                        0.33f / 2 to Purple500,
                                        0.66f / 2 to Blue500,
                                        1f / 2 to Pink400
                                    ),
                                    center = Offset(size.width / 2, 0f)
                                )
                                drawContent()
                                drawRect(
                                    brush = brush,
                                    blendMode = BlendMode.SrcIn
                                )
                            },
                        tint = Color.Black
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "截图智能识别",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "自动提取关键信息并生成实时待办，信息收集从未如此高效。",
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.alpha(.5f)
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(12.dp)) }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .background(color = AppColors.cardBackground, shape = AppSpecs.cardShape)
                    .padding(all = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_timer),
                        contentDescription = null,
                        modifier = Modifier
                            .size(32.dp),
                        tint = Indigo500
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "高效计时管理",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "番茄钟、倒计时一键开启，帮你专注当下，掌控时间节奏。",
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.alpha(.5f)
                    )
                }
            }
        }
    }
}