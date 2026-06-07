// Package declaration for the settings screen
package com.nevoit.cresto.feature.settings

// Import necessary libraries and components
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.nevoit.cresto.R
import com.nevoit.cresto.data.todo.calendar.TodoCalendarSyncManager
import com.nevoit.cresto.feature.screenextract.ShizukuScreenshotCapturer
import com.nevoit.cresto.feature.settings.util.AppLocaleManager
import com.nevoit.cresto.feature.settings.util.SettingsViewModel
import com.nevoit.cresto.theme.AppButtonColors
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.AppSpecs
import com.nevoit.cresto.theme.harmonize
import com.nevoit.cresto.ui.components.glasense.GlasenseButton
import com.nevoit.cresto.ui.components.glasense.GlasenseDynamicSmallTitle
import com.nevoit.cresto.ui.components.glasense.GlasenseMenu
import com.nevoit.cresto.ui.components.glasense.GlasenseMenuItem
import com.nevoit.cresto.ui.components.glasense.MenuDivider
import com.nevoit.cresto.ui.components.glasense.MenuState
import com.nevoit.cresto.ui.components.glasense.SelectiveMenuItemData
import com.nevoit.cresto.ui.components.glasense.extend.overscrollSpacer
import com.nevoit.cresto.ui.components.glasense.isScrolledPast
import com.nevoit.cresto.ui.components.packed.ConfigInfoHeader
import com.nevoit.cresto.ui.components.packed.TopBarSpacer
import com.nevoit.glasense.component.ListRowAccessory
import com.nevoit.glasense.component.ListStack
import com.nevoit.glasense.core.component.Icon
import com.nevoit.glasense.core.component.Text
import com.nevoit.glasense.core.component.VGap
import com.nevoit.glasense.theme.tokens.Slate500
import rikka.shizuku.Shizuku

@Composable
fun GeneralScreen(settingsViewModel: SettingsViewModel = viewModel()) {
    // Get the current activity instance to allow finishing the screen
    val activity = LocalActivity.current

    // Calculate the height of the status bar to adjust layout
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // Remember the state for the lazy list to control scrolling
    val lazyListState = rememberLazyListState()

    // Determine if the small title should be visible based on the scroll position
    val isSmallTitleVisible by lazyListState.isScrolledPast(statusBarHeight + 24.dp)
    val isDueTodayMarkerEnabled by settingsViewModel.isDueTodayMarker
    val isOverdueMarkerEnabled by settingsViewModel.isOverdueMarker
    val isCompletionSoundEnabled by settingsViewModel.isCompletionSoundEnabled
    val isEasterEggEnabled by settingsViewModel.isEasterEggEnabled
    val isSuperGraphicUltraModernGirlEnabled by settingsViewModel.isSuperGraphicUltraModernGirlEnabled
    val isExtractScreenQuickTileEnabled by settingsViewModel.isExtractScreenQuickTileEnabled
    val isAutoAddToSystemCalendarEnabled by settingsViewModel.isAutoAddToSystemCalendar
    val context = LocalContext.current
    val requirePermission = stringResource(R.string.calendar_sync_permission_required)
    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = TodoCalendarSyncManager.REQUIRED_PERMISSIONS.all { permission ->
            permissions[permission] == true
        }
        settingsViewModel.onAutoAddToSystemCalendarChanged(granted)
        if (!granted) {
            Toast.makeText(
                context,
                requirePermission,
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    val screenshotCapturer = remember { ShizukuScreenshotCapturer() }
    var isShizukuPermissionGranted by remember {
        mutableStateOf(screenshotCapturer.hasPermission())
    }

    DisposableEffect(screenshotCapturer) {
        val permissionResultListener =
            Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
                if (requestCode == ShizukuScreenshotCapturer.REQUEST_CODE) {
                    isShizukuPermissionGranted = grantResult == PackageManager.PERMISSION_GRANTED &&
                            screenshotCapturer.hasPermission()
                }
            }

        isShizukuPermissionGranted = screenshotCapturer.hasPermission()
        Shizuku.addRequestPermissionResultListener(permissionResultListener)

        onDispose {
            Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        }
    }

    val backgroundColor = AppColors.pageBackground
    val backdrop = rememberLayerBackdrop {
        drawRect(
            color = backgroundColor,
            size = Size(this.size.width * 3, this.size.height * 3),
            topLeft = Offset(-this.size.width, -this.size.height)
        )
        drawContent()
    }

    var languageButtonBounds by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var menuState by remember { mutableStateOf(MenuState()) }
    val showMenu: (anchorBounds: Rect, items: List<GlasenseMenuItem>) -> Unit =
        { bounds, items ->
            menuState = MenuState(isVisible = true, anchorBounds = bounds, items = items)
        }
    val dismissMenu = {
        menuState = menuState.copy(isVisible = false)
    }
    val selectedLanguageTag = AppLocaleManager.getLanguageTag(context)
    val systemLanguageText = stringResource(R.string.system_language)
    val englishText = stringResource(R.string.english)
    val simplifiedChineseText = stringResource(R.string.simplified_chinese)
    val currentLanguageText = when (selectedLanguageTag) {
        AppLocaleManager.ENGLISH -> englishText
        AppLocaleManager.SIMPLIFIED_CHINESE -> simplifiedChineseText
        else -> systemLanguageText
    }
    val languageMenuItems = remember(
        selectedLanguageTag,
        systemLanguageText,
        englishText,
        simplifiedChineseText,
        context
    ) {
        listOf(
            SelectiveMenuItemData(
                text = systemLanguageText,
                isSelected = { selectedLanguageTag == AppLocaleManager.SYSTEM },
                onClick = {
                    AppLocaleManager.setLanguageTag(context, AppLocaleManager.SYSTEM)
                }
            ),
            MenuDivider,
            SelectiveMenuItemData(
                text = englishText,
                isSelected = { selectedLanguageTag == AppLocaleManager.ENGLISH },
                onClick = {
                    AppLocaleManager.setLanguageTag(context, AppLocaleManager.ENGLISH)
                }
            ),
            SelectiveMenuItemData(
                text = simplifiedChineseText,
                isSelected = { selectedLanguageTag == AppLocaleManager.SIMPLIFIED_CHINESE },
                onClick = {
                    AppLocaleManager.setLanguageTag(context, AppLocaleManager.SIMPLIFIED_CHINESE)
                }
            )
        )
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
            item {
                ConfigInfoHeader(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    color = harmonize(Slate500),
                    backgroundColor = AppColors.cardBackground,
                    icon = painterResource(R.drawable.ic_twotone_gear),
                    title = stringResource(R.string.general),
                    info = stringResource(R.string.manage_startup_behavior_todo_marking_and_advanced_shortcuts)
                )
            }
            Section(topSpacing = 24.dp) {
                Row(onClick = {
                    languageButtonBounds?.let {
                        showMenu(it.boundsInWindow(), languageMenuItems)
                    }
                }, trailing = {
                    Text(
                        text = currentLanguageText,
                        modifier = Modifier
                            .onGloballyPositioned { coordinates ->
                                languageButtonBounds = coordinates
                            }
                    )
                }, accessory = ListRowAccessory.SelectIndicator) {
                    Text(text = stringResource(R.string.language))
                }
            }
            Section(
                header = { stringResource(R.string.todos) },
                footer = { stringResource(R.string.automatically_add_new_todos_as_events_in_system_calendar) }
            ) {
                CustomSwitchRow(
                    checked = isCompletionSoundEnabled,
                    onCheckedChange = { settingsViewModel.onCompletionSoundChanged(it) }) {
                    Text(stringResource(R.string.completion_sound))
                }
                CustomSwitchRow(
                    checked = isAutoAddToSystemCalendarEnabled,
                    onCheckedChange = { enabled ->
                        if (!enabled) {
                            settingsViewModel.onAutoAddToSystemCalendarChanged(false)
                        } else if (TodoCalendarSyncManager.hasCalendarPermissions(
                                context
                            )
                        ) {
                            settingsViewModel.onAutoAddToSystemCalendarChanged(true)
                        } else {
                            calendarPermissionLauncher.launch(TodoCalendarSyncManager.REQUIRED_PERMISSIONS)
                        }
                    },
                ) { Text(stringResource(R.string.auto_add_to_system_calendar)) }
            }
            Section(
                topSpacing = 12.dp,
                footer = { stringResource(R.string.add_a_marker_to_todos_due_today) }) {
                CustomSwitchRow(
                    checked = isDueTodayMarkerEnabled,
                    onCheckedChange = { settingsViewModel.onDueTodayMarkerChanged(it) }) {
                    Text(stringResource(R.string.due_today_marker))
                }
            }
            Section(
                topSpacing = 12.dp,
                footer = { stringResource(R.string.add_a_marker_to_overdue_todos_on_the_next_day) }) {
                CustomSwitchRow(
                    checked = isOverdueMarkerEnabled,
                    onCheckedChange = { settingsViewModel.onOverdueMarkerChanged(it) }) {
                    Text(stringResource(R.string.overdue_marker))
                }
            }
            Section(
                header = { stringResource(R.string.advanced) },
                footer = { stringResource(R.string.add_a_quick_toggle_to_control_center_for_one_tap_ai_screen_extraction_shizuku_required) }) {
                CustomSwitchRow(
                    checked = isShizukuPermissionGranted,
                    onCheckedChange = {
                        runCatching {
                            screenshotCapturer.requestPermission()
                        }.onFailure { error ->
                            isShizukuPermissionGranted =
                                screenshotCapturer.hasPermission()
                            Toast.makeText(
                                context,
                                error.localizedMessage ?: "Shizuku 未运行",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }) {
                    Text(stringResource(R.string.shizuku_permission))
                }
                CustomSwitchRow(
                    checked = isExtractScreenQuickTileEnabled,
                    onCheckedChange = { enabled ->
                        settingsViewModel.onExtractScreenQuickTileChanged(enabled)
                        if (enabled) {
                            requestAddExtractScreenTile(context)
                        }
                    }) {
                    Text(stringResource(R.string.enable_extract_screen_quick_toggle))
                }
            }

            if (isEasterEggEnabled) {
                Section(
                    header = { "???" },
                    footer = { "We're leaving the planet and you can't come." })
                {
                    CustomSwitchRow(
                        checked = isSuperGraphicUltraModernGirlEnabled,
                        onCheckedChange = {
                            settingsViewModel.onSuperGraphicUltraModernGirlChanged(
                                it
                            )
                        },
                    ) {
                        Text("Super Graphic Ultra Modern Girl")
                    }
                }
            }
            item { VGap() }
            overscrollSpacer(lazyListState)
        }
        // A small title that dynamically appears at the top when the user scrolls down
        GlasenseDynamicSmallTitle(
            modifier = Modifier.align(Alignment.TopCenter),
            title = stringResource(R.string.general),
            statusBarHeight = statusBarHeight,
            isVisible = isSmallTitleVisible,
            backdrop = backdrop
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
        GlasenseMenu(
            menuState = menuState,
            backdrop = backdrop,
            onDismiss = dismissMenu
        )
    }
}
