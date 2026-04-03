// Package declaration for the settings screen
package com.nevoit.cresto.feature.settings

// Import necessary libraries and components
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nevoit.cresto.R
import com.nevoit.cresto.theme.AppButtonColors
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.ui.components.glasense.GlasenseButton
import com.nevoit.cresto.ui.components.glasense.GlasenseDynamicSmallTitle
import com.nevoit.cresto.ui.components.glasense.GlasenseSwitch
import com.nevoit.cresto.ui.components.glasense.ZeroHeightDivider
import com.nevoit.cresto.ui.components.glasense.extend.overscrollSpacer
import com.nevoit.cresto.ui.components.glasense.isScrolledPast
import com.nevoit.cresto.ui.components.packed.ConfigInfoHeader
import com.nevoit.cresto.ui.components.packed.ConfigItem
import com.nevoit.cresto.ui.components.packed.ConfigItemContainer
import com.nevoit.cresto.ui.components.packed.PageContent
import com.nevoit.cresto.ui.components.packed.VGap
import com.nevoit.glasense.theme.Slate500
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/**
 * This composable function defines the AI Settings screen.
 * It provides options to configure AI-related features.
 * It uses experimental APIs for Material 3 and Haze effects.
 */
@OptIn(ExperimentalHazeApi::class)
@Composable
fun GeneralScreen() {
    // Get the current activity instance to allow finishing the screen
    val activity = LocalActivity.current

    // Calculate the height of the status bar to adjust layout
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // Remember the state for the Haze effect, a library for blurring content behind a surface
    val hazeState = rememberHazeState()

    // Remember the state for the lazy list to control scrolling
    val lazyListState = rememberLazyListState()

    // Determine if the small title should be visible based on the scroll position
    val isSmallTitleVisible by lazyListState.isScrolledPast(statusBarHeight + 24.dp)

    // Root container for the screen, filling the entire available space
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.pageBackground)
    ) {
        // A vertically scrolling list that only composes and lays out the currently visible items
        PageContent(
            state = lazyListState,
            modifier = Modifier
                .hazeSource(hazeState, 0f),
            tabPadding = false
        ) {
            item {
                Box(modifier = Modifier.padding(top = 48.dp + statusBarHeight + 12.dp))
            }
            item {
                ConfigInfoHeader(
                    color = Slate500,
                    backgroundColor = AppColors.cardBackground,
                    icon = painterResource(R.drawable.ic_twotone_gear),
                    title = stringResource(R.string.general),
                    info = stringResource(R.string.manage_startup_behavior_todo_marking_and_advanced_shortcuts)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            item {
                ConfigItemContainer(
                    title = stringResource(R.string.general_section_title),
                    backgroundColor = AppColors.cardBackground
                ) {
                    Column {
                        ConfigItem(title = stringResource(R.string.check_for_updates_on_startup)) {
                            GlasenseSwitch(
                                checked = false,
                                onCheckedChange = {},
                                backgroundColor = AppColors.cardBackground
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        ZeroHeightDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        ConfigItem(title = stringResource(R.string.completion_sound)) {
                            GlasenseSwitch(
                                checked = false,
                                onCheckedChange = {},
                                backgroundColor = AppColors.cardBackground
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        ZeroHeightDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        ConfigItem(title = stringResource(R.string.language)) {
                            Text(
                                text = stringResource(R.string.chinese),
                                color = AppColors.contentVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            item {
                ConfigItemContainer(
                    title = stringResource(R.string.todos),
                    backgroundColor = AppColors.cardBackground
                ) {
                    Column {
                        ConfigItem(title = stringResource(R.string.auto_add_to_system_calendar)) {
                            GlasenseSwitch(
                                checked = false,
                                onCheckedChange = {},
                                backgroundColor = AppColors.cardBackground
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.automatically_add_new_todos_as_events_in_system_calendar),
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 12.dp),
                    color = AppColors.contentVariant.copy(alpha = .3f)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            item {
                ConfigItemContainer(
                    backgroundColor = AppColors.cardBackground
                ) {
                    Column {
                        ConfigItem(title = stringResource(R.string.due_today_marker)) {
                            GlasenseSwitch(
                                checked = false,
                                onCheckedChange = {},
                                backgroundColor = AppColors.cardBackground
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.add_a_marker_to_todos_due_today),
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 12.dp),
                    color = AppColors.contentVariant.copy(alpha = .3f)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            item {
                ConfigItemContainer(
                    backgroundColor = AppColors.cardBackground
                ) {
                    Column {
                        ConfigItem(title = stringResource(R.string.overdue_marker)) {
                            GlasenseSwitch(
                                checked = false,
                                onCheckedChange = {},
                                backgroundColor = AppColors.cardBackground
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.add_a_marker_to_overdue_todos_on_the_next_day),
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 12.dp),
                    color = AppColors.contentVariant.copy(alpha = .3f)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            item {
                ConfigItemContainer(
                    title = stringResource(R.string.advanced),
                    backgroundColor = AppColors.cardBackground
                ) {
                    Column {
                        ConfigItem(title = stringResource(R.string.shizuku_permission)) {
                            GlasenseSwitch(
                                checked = false,
                                onCheckedChange = {},
                                backgroundColor = AppColors.cardBackground
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        ZeroHeightDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        ConfigItem(title = stringResource(R.string.enable_extract_screen_quick_toggle)) {
                            GlasenseSwitch(
                                checked = false,
                                onCheckedChange = {},
                                backgroundColor = AppColors.cardBackground
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.add_a_quick_toggle_to_control_center_for_one_tap_ai_screen_extraction_shizuku_required),
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 12.dp),
                    color = AppColors.contentVariant.copy(alpha = .3f)
                )
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
            hazeState = hazeState,
            surfaceColor = AppColors.pageBackground
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
    }
}
