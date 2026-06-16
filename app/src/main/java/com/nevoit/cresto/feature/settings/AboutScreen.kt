package com.nevoit.cresto.feature.settings

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.widget.Toast
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.Capsule
import com.nevoit.cresto.R
import com.nevoit.cresto.feature.settings.update.UpdateBottomSheet
import com.nevoit.cresto.feature.settings.update.UpdateCheckResult
import com.nevoit.cresto.feature.settings.update.UpdateChecker
import com.nevoit.cresto.feature.settings.update.UpdateInfo
import com.nevoit.cresto.feature.settings.util.SettingsViewModel
import com.nevoit.cresto.theme.AppButtonColors
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.AppSpecs
import com.nevoit.cresto.theme.isAppInDarkTheme
import com.nevoit.cresto.ui.components.glasense.GlasenseButton
import com.nevoit.cresto.ui.components.glasense.GlasenseDynamicSmallTitle
import com.nevoit.cresto.ui.components.glasense.isScrolledPast
import com.nevoit.cresto.ui.components.packed.TopBarSpacer
import com.nevoit.cresto.ui.modifier.pressIndentShaderEffect
import com.nevoit.cresto.ui.modifier.shaderRipple
import com.nevoit.cresto.ui.modifier.tiltOnPress
import com.nevoit.glasense.component.ListRowAccessory
import com.nevoit.glasense.component.ListStack
import com.nevoit.glasense.core.component.Icon
import com.nevoit.glasense.core.component.Text
import com.nevoit.glasense.core.component.VGap
import com.nevoit.glasense.theme.GlasenseTheme
import com.nevoit.glasense.theme.tokens.Amber400
import com.nevoit.glasense.theme.tokens.Blue400
import com.nevoit.glasense.theme.tokens.Green400
import com.nevoit.glasense.theme.tokens.Orange400
import com.nevoit.glasense.theme.tokens.Pink400
import com.nevoit.glasense.theme.tokens.Purple400
import com.nevoit.glasense.theme.tokens.Rose400
import io.github.vinceglb.confettikit.compose.ConfettiKit
import io.github.vinceglb.confettikit.core.Angle
import io.github.vinceglb.confettikit.core.Party
import io.github.vinceglb.confettikit.core.Position
import io.github.vinceglb.confettikit.core.emitter.Emitter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * This composable function defines the About screen.
 * It displays information about the app, developers, and version.
 * It uses an experimental API for Haze effects.
 */

@Composable
fun AboutScreen(settingsViewModel: SettingsViewModel = viewModel()) {
    // Get the current activity instance to allow finishing the screen
    val activity = LocalActivity.current

    // Calculate the height of the status bar to adjust layout
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val density = LocalDensity.current

    val onBackground = AppColors.content
    val backgroundColor = AppColors.pageBackground
    val hierarchicalSurfaceColor = AppColors.cardBackground

    // Remember the state for the lazy list to control scrolling
    val lazyListState = rememberLazyListState()

    val isSmallTitleVisible by lazyListState.isScrolledPast(statusBarHeight + 24.dp)

    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()

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
    var aboutCardTapCount by remember { mutableIntStateOf(0) }
    var confettiBurstKey by remember { mutableIntStateOf(0) }
    val hapticController = LocalHapticFeedback.current

    val isSuperGraphicUltraModernGirlEnabled by settingsViewModel.isSuperGraphicUltraModernGirlEnabled
    val isCheckUpdatesOnStartupEnabled by settingsViewModel.isCheckUpdatesOnStartup
    var isCheckingForUpdates by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    val alreadyLatestVersionText = stringResource(R.string.already_latest_version)
    val checkUpdateFailedText = stringResource(R.string.check_update_failed)
    val updateManifestUrlNotConfiguredText =
        stringResource(R.string.update_manifest_url_not_configured)

    fun checkForUpdates() {
        if (isCheckingForUpdates) return
        scope.launch {
            isCheckingForUpdates = true
            try {
                when (val result = UpdateChecker.check()) {
                    is UpdateCheckResult.HasUpdate -> updateInfo = result.updateInfo
                    UpdateCheckResult.NoUpdate -> Toast.makeText(
                        context,
                        alreadyLatestVersionText,
                        Toast.LENGTH_SHORT
                    ).show()

                    UpdateCheckResult.NotConfigured -> Toast.makeText(
                        context,
                        updateManifestUrlNotConfiguredText,
                        Toast.LENGTH_SHORT
                    ).show()

                    is UpdateCheckResult.Failed -> Toast.makeText(
                        context,
                        checkUpdateFailedText,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                isCheckingForUpdates = false
            }
        }
    }

    val backdrop = rememberLayerBackdrop {
        drawRect(
            color = backgroundColor,
            size = Size(this.size.width * 3, this.size.height * 3),
            topLeft = Offset(-this.size.width, -this.size.height)
        )
        drawContent()
    }

    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Root container for the screen, filling the entire available space
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        ListStack(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
            cornerRadius = AppSpecs.cardCorner,
            contentPadding = PaddingValues(bottom = navigationBarHeight)
        ) {
            TopBarSpacer()
            // An item that displays a background image for the About screen
            item {
                val shape = AppSpecs.cardShape
                Box(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .aspectRatio(3f / 4f)
                        .fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .tiltOnPress(maxTilt = 10f) {
                                aboutCardTapCount += 1
                                if (aboutCardTapCount >= 10) {
                                    aboutCardTapCount = 0
                                    confettiBurstKey += 1
                                    settingsViewModel.unlockEasterEgg()
                                }
                            }
                            .then(
                                if (isSuperGraphicUltraModernGirlEnabled) {
                                    Modifier
                                        .pressIndentShaderEffect()
                                } else {
                                    Modifier
                                }
                            )
                            .clip(shape)
                            .shaderRipple()
                            .paint(
                                painter = if (darkMode) painterResource(R.drawable.about_background) else painterResource(
                                    R.drawable.about_background_light
                                ),
                                contentScale = ContentScale.Crop
                            )
                            .drawBehind {
                                val outline = shape.createOutline(
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
                            }
                    )
                }
            }
            // Item container for displaying developer information
            Section(header = { stringResource(R.string.developer) }, topSpacing = 24.dp) {
                Row(
                    onClick = { uriHandler.openUri("https://github.com/nevodev") },
                    leading = {
                        Image(
                            painter = painterResource(R.drawable.avatar),
                            contentDescription = stringResource(R.string.developer_avatar),
                            modifier = Modifier
                                .clip(CircleShape)
                                .size(48.dp)
                        )
                    },
                    accessory = ListRowAccessory.Chevron
                ) {
                    DeveloperContent(
                        name = "Nevoit",
                        tagline = "Create awesome."
                    ) {
                        DeveloperBadge(stringResource(R.string.main_developer))
                        DeveloperBadge(stringResource(R.string.designer))
                    }
                }
                Row(
                    onClick = { uriHandler.openUri("https://github.com/kyant0") },
                    leading = {
                        Image(
                            painter = painterResource(R.drawable.kyant_avatar),
                            contentDescription = stringResource(R.string.developer_avatar),
                            modifier = Modifier
                                .clip(CircleShape)
                                .size(48.dp)
                        )
                    },
                    accessory = ListRowAccessory.Chevron
                ) {
                    DeveloperContent(
                        name = "Kyant0",
                        tagline = "Create rubbish."
                    ) {
                        DeveloperBadge(stringResource(R.string.overscroll_animation_developer))
                    }
                }
            }
            // Item for displaying version information
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(top = 24.dp)
                ) {
                    Text(
                        text = stringResource(R.string.version_info),
                        style = GlasenseTheme.type.subHeadline.copy(lineHeight = 14.sp),
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
                                shape = AppSpecs.cardShape
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
            }
            Section {
                CustomSwitchRow(
                    checked = isCheckUpdatesOnStartupEnabled,
                    onCheckedChange = {
                        settingsViewModel.onCheckUpdatesOnStartupChanged(it)
                    }
                ) {
                    Text(stringResource(R.string.check_for_updates_on_startup))
                }
                Row(
                    enabled = !isCheckingForUpdates,
                    onClick = { checkForUpdates() }
                ) {
                    Text(
                        if (isCheckingForUpdates) {
                            stringResource(R.string.checking_for_updates)
                        } else {
                            stringResource(R.string.check_for_updates)
                        }
                    )
                }
            }
            item { VGap() }
        }
        // A small title that dynamically appears at the top when the user scrolls down
        GlasenseDynamicSmallTitle(
            modifier = Modifier.align(Alignment.TopCenter),
            title = stringResource(R.string.about),
            statusBarHeight = statusBarHeight,
            isVisible = isSmallTitleVisible,
            backdrop = backdrop,
            surfaceColor = backgroundColor
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
            colors = AppButtonColors.action()
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_forward_nav),
                contentDescription = stringResource(R.string.back),
                modifier = Modifier.width(32.dp)
            )
        }
        if (confettiBurstKey > 0) {
            // Force a fresh composition so each 10-tap milestone retriggers the burst.
            key(confettiBurstKey) {
                LaunchedEffect(Unit) {
                    repeat(10) {
                        hapticController.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        delay(Random.nextLong(50, 100).milliseconds)
                    }
                }
                ConfettiKit(
                    modifier = Modifier.fillMaxSize(),
                    parties = parade()
                )
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

@Composable
private fun DeveloperContent(
    name: String,
    tagline: String,
    badges: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text = name,
            fontSize = 20.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.W500,
            color = AppColors.content,
        )
        Text(
            text = tagline,
            fontSize = 12.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.W400,
            color = AppColors.contentVariant,
        )
        Row(
            modifier = Modifier
                .offset((-2).dp)
                .padding(top = 4.dp)
        ) {
            badges()
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun DeveloperBadge(text: String) {
    Box(
        modifier = Modifier
            .padding(end = 4.dp)
            .background(
                AppColors.scrimMedium,
                Capsule()
            )
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            lineHeight = 10.sp,
            fontWeight = FontWeight.W400,
            color = AppColors.contentVariant,
            modifier = Modifier.padding(vertical = 2.dp, horizontal = 8.dp)
        )
    }
}

fun parade(): List<Party> {
    val party = Party(
        speed = 10f,
        maxSpeed = 30f,
        damping = 0.91f,
        angle = Angle.RIGHT - 45,
        spread = 270,
        colors = listOf(
            Amber400.toArgb(),
            Purple400.toArgb(),
            Rose400.toArgb(),
            Pink400.toArgb(),
            Blue400.toArgb(),
            Orange400.toArgb(),
            Green400.toArgb()
        ),
        timeToLive = 4000L,
        emitter = Emitter(duration = 0.8.seconds).perSecond(150),
        position = Position.Relative(-0.1, 0.1)
    )

    return listOf(
        party,
        party.copy(
            angle = party.angle - 90, // flip angle from right to left
            position = Position.Relative(1.1, 0.1)
        ),
    )
}
