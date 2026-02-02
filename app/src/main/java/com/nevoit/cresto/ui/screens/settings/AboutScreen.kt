package com.nevoit.cresto.ui.screens.settings

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import com.nevoit.cresto.R
import com.nevoit.cresto.ui.components.glasense.GlasenseButton
import com.nevoit.cresto.ui.components.glasense.GlasenseDynamicSmallTitle
import com.nevoit.cresto.ui.components.glasense.ZeroHeightDivider
import com.nevoit.cresto.ui.components.packed.ConfigItemContainer
import com.nevoit.cresto.ui.theme.glasense.AppColors
import com.nevoit.cresto.ui.theme.glasense.isAppInDarkTheme
import com.nevoit.cresto.util.g2
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/**
 * This composable function defines the About screen.
 * It displays information about the app, developers, and version.
 * It uses an experimental API for Haze effects.
 */
@OptIn(ExperimentalHazeApi::class)
@Composable
fun AboutScreen() {
    // Get the current activity instance to allow finishing the screen
    val activity = LocalActivity.current

    // Calculate the height of the status bar to adjust layout
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val density = LocalDensity.current
    // Calculate the scroll threshold in pixels for showing/hiding the small title
    val thresholdPx = if (statusBarHeight > 0.dp) {
        with(density) {
            (statusBarHeight + 24.dp).toPx()
        }
    } else 0f

    // Remember the state for the Haze effect, a library for blurring content behind a surface
    val hazeState = rememberHazeState()

    // Get colors from the app's custom theme
    val onSurfaceContainer = AppColors.scrimNormal
    val onBackground = AppColors.content
    val surfaceColor = AppColors.pageBackground
    val hierarchicalSurfaceColor = AppColors.cardBackground

    // Remember the state for the lazy list to control scrolling
    val lazyListState = rememberLazyListState()

    // Determine if the small title should be visible based on the scroll position
    val isSmallTitleVisible by remember(thresholdPx) { derivedStateOf { ((lazyListState.firstVisibleItemIndex == 0) && (lazyListState.firstVisibleItemScrollOffset > thresholdPx)) || lazyListState.firstVisibleItemIndex > 0 } }

    val context = LocalContext.current

    // Retrieve the app's package information to display version name and code
    val packageInfo: PackageInfo? = remember {
        try {
            val packageName = context.packageName
            context.packageManager.getPackageInfo(packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            null
        }
    }
    val darkMode = isAppInDarkTheme()

    // Root container for the screen, filling the entire available space
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaceColor)
    ) {
        // A vertically scrolling list that only composes and lays out the currently visible items
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .hazeSource(hazeState, 0f) // This view is the source for the Haze effect
                .fillMaxSize()
                .padding(0.dp)
                .background(surfaceColor),
            contentPadding = PaddingValues(
                start = 12.dp,
                top = 0.dp,
                end = 12.dp,
                bottom = 136.dp
            )
        ) {
            // Spacer item at the top of the list to push content below the top bar and back button
            item {
                Box(modifier = Modifier.padding(top = 48.dp + statusBarHeight + 12.dp))
            }
            // An item that displays a background image for the About screen
            item {
                Column(
                    modifier = Modifier
                        .aspectRatio(3f / 4f)
                        .fillMaxWidth()
                        .clip(ContinuousRoundedRectangle(12.dp, g2))
                        .paint(
                            painter = if (darkMode) painterResource(R.drawable.about_background) else painterResource(
                                R.drawable.about_background_light
                            ),
                            contentScale = ContentScale.Crop
                        )
                        .drawBehind {
                            val outline = ContinuousRoundedRectangle(12.dp, g2).createOutline(
                                size = size,
                                layoutDirection = LayoutDirection.Ltr,
                                density = density
                            )
                            // Draw a white glowing border around the image
                            drawOutline(
                                outline = outline,
                                style = Stroke(4.dp.toPx()),
                                color = if (darkMode) Color.White.copy(.2f) else Color.Black.copy(
                                    .05f
                                ),
                                blendMode = BlendMode.Luminosity
                            )
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            // Item container for displaying developer information
            item {
                ConfigItemContainer(
                    backgroundColor = hierarchicalSurfaceColor,
                    title = stringResource(R.string.developer)
                ) {
                    Column {
                        // Row for the main developer's information
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(R.drawable.avatar),
                                contentDescription = stringResource(R.string.developer_avatar),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .size(48.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(vertical = 4.dp)
                                    .weight(1f)
                            ) {
                                Text(
                                    text = "Nevoit",
                                    fontSize = 20.sp,
                                    lineHeight = 20.sp,
                                    fontWeight = FontWeight.W500,
                                    color = AppColors.content,
                                )
                                Text(
                                    text = "Create awesome.",
                                    fontSize = 12.sp,
                                    lineHeight = 12.sp,
                                    fontWeight = FontWeight.W400,
                                    color = AppColors.contentVariant,
                                )
                                // Row for displaying developer roles/tags
                                Row(
                                    modifier = Modifier
                                        .offset((-4).dp)
                                        .padding(top = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(end = 4.dp)
                                            .background(
                                                AppColors.content.copy(.1f),
                                                ContinuousCapsule
                                            )
                                    ) {
                                        Text(
                                            text = stringResource(R.string.main_developer),
                                            fontSize = 10.sp,
                                            lineHeight = 10.sp,
                                            fontWeight = FontWeight.W400,
                                            color = AppColors.contentVariant,
                                            modifier = Modifier
                                                .padding(vertical = 2.dp, horizontal = 8.dp)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                AppColors.content.copy(.1f),
                                                ContinuousCapsule
                                            )
                                    ) {
                                        Text(
                                            text = stringResource(R.string.designer),
                                            fontSize = 10.sp,
                                            lineHeight = 10.sp,
                                            fontWeight = FontWeight.W400,
                                            color = AppColors.contentVariant,
                                            modifier = Modifier
                                                .padding(vertical = 2.dp, horizontal = 8.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(
                                painter = painterResource(R.drawable.ic_forward),
                                tint = AppColors.content.copy(.2f),
                                contentDescription = stringResource(R.string.enter_icon),
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .offset(4.dp)
                                    .height(40.dp)
                                    .width(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        // Visual divider line
                        ZeroHeightDivider(
                            color = onBackground.copy(.1f), width = 1.dp,
                            modifier = Modifier,
                            blendMode = BlendMode.SrcOver
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // Row for the overscroll animation developer's information
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(R.drawable.kyant_avatar),
                                contentDescription = stringResource(R.string.developer_avatar),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .size(48.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(vertical = 4.dp)
                                    .weight(1f)
                            ) {
                                Text(
                                    text = "Kyant0",
                                    fontSize = 20.sp,
                                    lineHeight = 20.sp,
                                    fontWeight = FontWeight.W500,
                                    color = AppColors.content,
                                )
                                Text(
                                    text = "Create rubbish.",
                                    fontSize = 12.sp,
                                    lineHeight = 12.sp,
                                    fontWeight = FontWeight.W400,
                                    color = AppColors.contentVariant,
                                )
                                Row(
                                    modifier = Modifier
                                        .offset((-4).dp)
                                        .padding(top = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(end = 6.dp)
                                            .background(
                                                AppColors.content.copy(.1f),
                                                ContinuousCapsule
                                            )
                                    ) {
                                        Text(
                                            text = stringResource(R.string.overscroll_animation_developer),
                                            fontSize = 10.sp,
                                            lineHeight = 10.sp,
                                            fontWeight = FontWeight.W400,
                                            color = AppColors.contentVariant,
                                            modifier = Modifier
                                                .padding(vertical = 2.dp, horizontal = 8.dp)

                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(
                                painter = painterResource(R.drawable.ic_forward),
                                tint = AppColors.content.copy(.2f),
                                contentDescription = stringResource(R.string.enter_icon),
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .offset(4.dp)
                                    .height(40.dp)
                                    .width(20.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            // Item for displaying version information
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.version_info),
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = hierarchicalSurfaceColor,
                                shape = ContinuousRoundedRectangle(12.dp, g2)
                            )
                    ) {

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            // Row displaying the version name
                            Row(
                                modifier = Modifier.height(32.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.ic_mini_info),
                                    contentDescription = stringResource(R.string.version_name),
                                    colorFilter = ColorFilter.tint(onBackground),
                                    alpha = .5f,
                                    modifier = Modifier
                                        .size(24.dp)

                                )
                                Text(
                                    text = stringResource(R.string.version_name),
                                    fontSize = 16.sp,
                                    lineHeight = 16.sp,
                                    color = onBackground.copy(.5f),
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                Text(
                                    text = "${packageInfo?.versionName}",
                                    fontSize = 16.sp,
                                    lineHeight = 16.sp,
                                    textAlign = TextAlign.End,
                                    color = onBackground,
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .weight(1f)
                                )
                            }
                            // Row displaying the version code
                            Row(
                                modifier = Modifier.height(32.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.ic_mini_version_code),
                                    contentDescription = stringResource(R.string.version_code),
                                    colorFilter = ColorFilter.tint(onBackground),
                                    alpha = .5f,
                                    modifier = Modifier
                                        .size(24.dp)

                                )
                                Text(
                                    text = stringResource(R.string.version_code),
                                    fontSize = 16.sp,
                                    lineHeight = 16.sp,
                                    color = onBackground.copy(.5f),
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                Text(
                                    text = "${packageInfo?.longVersionCode}",
                                    fontSize = 16.sp,
                                    lineHeight = 16.sp,
                                    textAlign = TextAlign.End,
                                    color = onBackground,
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .weight(1f)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        // A small title that dynamically appears at the top when the user scrolls down
        GlasenseDynamicSmallTitle(
            modifier = Modifier.align(Alignment.TopCenter),
            title = stringResource(R.string.about),
            statusBarHeight = statusBarHeight,
            isVisible = isSmallTitleVisible,
            hazeState = hazeState,
            surfaceColor = surfaceColor
        ) {
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
            colors = ButtonDefaults.buttonColors(
                containerColor = onSurfaceContainer,
                contentColor = AppColors.primary
            )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_forward_nav),
                contentDescription = stringResource(R.string.back),
                modifier = Modifier.width(32.dp)
            )
        }
    }
}
