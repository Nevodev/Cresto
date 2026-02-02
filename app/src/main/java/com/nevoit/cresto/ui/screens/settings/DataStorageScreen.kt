package com.nevoit.cresto.ui.screens.settings

import android.text.format.Formatter
import androidx.activity.compose.LocalActivity
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.capsule.ContinuousCapsule
import com.nevoit.cresto.R
import com.nevoit.cresto.data.todo.TodoDatabase
import com.nevoit.cresto.ui.components.glasense.DialogItemData
import com.nevoit.cresto.ui.components.glasense.DialogState
import com.nevoit.cresto.ui.components.glasense.GlasenseButton
import com.nevoit.cresto.ui.components.glasense.GlasenseDialog
import com.nevoit.cresto.ui.components.glasense.GlasenseDynamicSmallTitle
import com.nevoit.cresto.ui.components.glasense.extend.overscrollSpacer
import com.nevoit.cresto.ui.components.packed.ConfigInfoHeader
import com.nevoit.cresto.ui.components.packed.ConfigItem
import com.nevoit.cresto.ui.components.packed.ConfigItemContainer
import com.nevoit.cresto.ui.theme.glasense.Amber400
import com.nevoit.cresto.ui.theme.glasense.AppColors
import com.nevoit.cresto.ui.theme.glasense.AppSpecs
import com.nevoit.cresto.ui.theme.glasense.Emerald400
import com.nevoit.cresto.ui.theme.glasense.Slate500
import com.tencent.mmkv.MMKV
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * This composable function defines the Data & Storage screen.
 * It uses experimental APIs for Material 3 and Haze effects.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeApi::class)
@Composable
fun DataStorageScreen() {

    // Get the current activity instance to allow finishing the screen
    val activity = LocalActivity.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State variables to hold storage information
    var appSize by remember { mutableStateOf("Calculating...") }
    var dataSize by remember { mutableStateOf("Calculating...") }
    var cacheSize by remember { mutableStateOf("Calculating...") }
    var totalSize by remember { mutableStateOf("Calculating...") }
    var appSizeLong by remember { mutableLongStateOf(0L) }
    var dataSizeLong by remember { mutableLongStateOf(0L) }
    var cacheSizeLong by remember { mutableLongStateOf(0L) }

    // Fetch storage stats when the composable is first launched
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            // Function to recursively calculate directory size
            fun getDirectorySize(dir: File): Long {
                var size = 0L
                if (!dir.exists()) return 0L
                dir.walkTopDown().forEach {
                    size += it.length()
                }
                return size
            }

            // Calculate Cache Size
            val cache = context.cacheDir?.let { getDirectorySize(it) } ?: 0L

            // Calculate Data Size (internal storage for files, databases, shared_prefs)
            // dataDir includes cacheDir, so we subtract it for a more accurate data size.
            val dataDirSize = context.dataDir?.let { getDirectorySize(it) } ?: 0L
            val data = dataDirSize - cache

            // Calculate App Size (APK)
            val app = try {
                File(context.applicationInfo.sourceDir).length()
            } catch (e: Exception) {
                0L
            }

            // Update UI state on the main thread
            withContext(Dispatchers.Main) {
                cacheSize = Formatter.formatShortFileSize(context, cache)
                dataSize = Formatter.formatShortFileSize(context, data)
                appSize = Formatter.formatShortFileSize(context, app)
                totalSize = Formatter.formatShortFileSize(context, app + data + cache)
                appSizeLong = app
                dataSizeLong = data
                cacheSizeLong = cache
            }
        }
    }

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

    // Get the pixel value for 1dp, used for drawing divider lines
    val dp = with(density) {
        1.dp.toPx()
    }

    val backdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }

    var dialogState by remember { mutableStateOf(DialogState()) }

    val showDialog: (items: List<DialogItemData>, title: String, message: String?) -> Unit =
        { items, title, message ->
            dialogState =
                DialogState(isVisible = true, items = items, title = title, message = message)
        }

    val dismissDialog = {
        dialogState = dialogState.copy(isVisible = false)
    }

    val dialogItems = listOf(
        DialogItemData(
            stringResource(R.string.cancel),
            onClick = { dismissDialog() },
            isPrimary = false
        ),
        DialogItemData(
            stringResource(R.string.clear),
            onClick = {
                scope.launch {
                    TodoDatabase.getDatabase(context).todoDao().deleteAllTodos()
                    MMKV.defaultMMKV().clearAll()
                }
                dismissDialog()
                activity?.finish()
            },
            isPrimary = true,
            isDestructive = true
        )
    )
    val dialogItems2 = listOf(
        DialogItemData(
            stringResource(R.string.cancel),
            onClick = { dismissDialog() },
            isPrimary = false
        ),
        DialogItemData(
            stringResource(R.string.reset),
            onClick = {
                scope.launch {
                    MMKV.defaultMMKV().clearAll()
                }
                dismissDialog()
            },
            isPrimary = true,
            isDestructive = true
        )
    )

    val resetAllSettingsText = stringResource(R.string.reset_all_settings)
    val resetContentText =
        stringResource(R.string.this_will_reset_all_settings_to_their_defaults_your_todos_will_not_be_deleted)
    val clearAllDataText = stringResource(R.string.clear_all_data)
    val clearContentText =
        stringResource(R.string.this_will_permanently_delete_all_application_data_including_todos_and_settings_this_action_cannot_be_undone)
    // Root container for the screen, filling the entire available space
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaceColor)
            .layerBackdrop(backdrop)
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
            // Header item for the Data & Storage section
            item {
                ConfigInfoHeader(
                    color = Slate500,
                    backgroundColor = hierarchicalSurfaceColor,
                    icon = painterResource(R.drawable.ic_twotone_storage),
                    title = stringResource(R.string.data_storage),
                    enableGlow = false,
                    info = stringResource(R.string.manage_your_application_s_storage_footprint)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Item to display storage usage details
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.storage_usage),
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
                                shape = AppSpecs.cardShape
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Spacer(modifier = Modifier.height(8.dp))
                            StorageChart(
                                appSize = appSizeLong,
                                dataSize = dataSizeLong,
                                cacheSize = cacheSizeLong
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            StorageInfoRow(
                                label = stringResource(R.string.app_size),
                                value = appSize,
                                icon = painterResource(R.drawable.ic_mini_parcel)
                            )
                            StorageInfoRow(
                                label = stringResource(R.string.user_data),
                                value = dataSize,
                                icon = painterResource(R.drawable.ic_mini_user)
                            )
                            StorageInfoRow(
                                label = stringResource(R.string.cache),
                                value = cacheSize,
                                icon = painterResource(R.drawable.ic_mini_cache)
                            )
                            StorageInfoRow(
                                label = stringResource(R.string.total),
                                value = totalSize,
                                isTotal = true,
                                icon = painterResource(R.drawable.ic_mini_drive)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            // Item containing database import/export options
            item {
                ConfigItemContainer(
                    backgroundColor = hierarchicalSurfaceColor
                ) {
                    Column {
                        ConfigItem(title = stringResource(R.string.export_database)) {
                            // TODO: Implement database export functionality
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        // Visual divider line
                        Spacer(
                            modifier = Modifier
                                .drawBehind {
                                    drawLine(
                                        color = onBackground.copy(.1f),
                                        start = Offset(x = 0f, y = 0f),
                                        end = Offset(this.size.width, y = 0f),
                                        strokeWidth = dp
                                    )
                                }
                                .fillMaxWidth()
                                .height(0.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        ConfigItem(title = stringResource(R.string.import_database)) {
                            // TODO: Implement database import functionality
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            // Item containing data reset and clearing options
            item {
                ConfigItemContainer(
                    title = stringResource(R.string.reset),
                    backgroundColor = hierarchicalSurfaceColor
                ) {
                    Column {
                        ConfigItem(
                            title = stringResource(R.string.reset_all_settings),
                            color = AppColors.error,
                            clickable = true,
                            indication = true,
                            onClick = {
                                showDialog(
                                    dialogItems2,
                                    "$resetAllSettingsText?",
                                    resetContentText
                                )
                            }
                        ) {}
                        Spacer(modifier = Modifier.height(8.dp))
                        // Visual divider line
                        Spacer(
                            modifier = Modifier
                                .drawBehind {
                                    drawLine(
                                        color = onBackground.copy(.1f),
                                        start = Offset(x = 0f, y = 0f),
                                        end = Offset(this.size.width, y = 0f),
                                        strokeWidth = dp
                                    )
                                }
                                .fillMaxWidth()
                                .height(0.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        ConfigItem(
                            title = stringResource(R.string.clear_all_data),
                            color = AppColors.error,
                            clickable = true,
                            indication = true,
                            onClick = {
                                showDialog(
                                    dialogItems,
                                    "$clearAllDataText?",
                                    clearContentText
                                )
                            }
                        ) {}
                    }

                }
            }
            overscrollSpacer(lazyListState)
        }
        // A small title that dynamically appears at the top when the user scrolls down
        GlasenseDynamicSmallTitle(
            modifier = Modifier.align(Alignment.TopCenter),
            title = stringResource(R.string.data_storage),
            statusBarHeight = statusBarHeight,
            isVisible = isSmallTitleVisible,
            hazeState = hazeState,
            surfaceColor = surfaceColor
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

    if (dialogState.isVisible) {
        GlasenseDialog(
            dialogState = dialogState,
            backdrop = backdrop,
            onDismiss = { dismissDialog() },
            modifier = Modifier
        )
    }
}

@Composable
private fun StorageInfoRow(label: String, value: String, icon: Painter, isTotal: Boolean = false) {
    val onBackground = AppColors.content
    Row(
        modifier = Modifier.height(32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = icon,
            contentDescription = null,
            colorFilter = ColorFilter.tint(onBackground),
            alpha = if (isTotal) 1f else .5f,
            modifier = Modifier
                .size(24.dp)

        )
        Text(
            text = label,
            fontSize = 16.sp,
            lineHeight = 16.sp,
            color = if (isTotal) onBackground.copy(1f) else onBackground.copy(.5f),
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Text(
            text = value,
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

@Composable
private fun StorageChart(appSize: Long, dataSize: Long, cacheSize: Long) {
    val total = (appSize + dataSize + cacheSize)
    if (total == 0L) return

    val appWeight = appSize.toFloat() / total
    val dataWeight = dataSize.toFloat() / total
    val cacheWeight = cacheSize.toFloat() / total

    var totalWidth by remember { mutableStateOf(0.dp) }

    val appColor = AppColors.primary
    val dataColor = Amber400
    val cacheColor = Emerald400

    val hierarchicalSurfaceColor = AppColors.cardBackground


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(ContinuousCapsule)
                .onGloballyPositioned { coordinates ->
                    totalWidth = coordinates.size.width.dp
                }
        ) {
            if (appWeight > 0) {
                Box(
                    modifier = Modifier
                        .weight(appWeight)
                        .fillMaxHeight()
                        .background(appColor)
                )
            }

            if (dataWeight > 0) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(hierarchicalSurfaceColor)
                )
                Box(
                    modifier = Modifier
                        .weight(dataWeight)
                        .fillMaxHeight()
                        .background(dataColor)
                )
            }
            if (cacheWeight > 0) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(hierarchicalSurfaceColor)
                )

                Box(
                    modifier = Modifier
                        .weight(if ((cacheWeight * totalWidth) <= 8.dp) 8.dp / totalWidth else cacheWeight)
                        .fillMaxHeight()
                        .background(cacheColor)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            LegendItem(color = appColor, text = stringResource(R.string.app_size))
            Spacer(Modifier.width(16.dp))
            LegendItem(color = dataColor, text = stringResource(R.string.user_data))
            Spacer(Modifier.width(16.dp))
            LegendItem(color = cacheColor, text = stringResource(R.string.cache))
        }
    }
}

@Composable
private fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.content.copy(alpha = 0.5f)
        )
    }
}
