package com.nevoit.cresto.settings

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import com.nevoit.cresto.R
import com.nevoit.cresto.data.TodoDatabase
import com.nevoit.cresto.ui.components.ConfigInfoHeader
import com.nevoit.cresto.ui.components.ConfigItem
import com.nevoit.cresto.ui.components.ConfigItemContainer
import com.nevoit.cresto.ui.components.DynamicSmallTitle
import com.nevoit.cresto.ui.components.glasense.DialogItemData
import com.nevoit.cresto.ui.components.glasense.DialogState
import com.nevoit.cresto.ui.components.glasense.GlasenseButton
import com.nevoit.cresto.ui.components.glasense.GlasenseDialog
import com.nevoit.cresto.ui.theme.glasense.Amber400
import com.nevoit.cresto.ui.theme.glasense.CalculatedColor
import com.nevoit.cresto.ui.theme.glasense.Emerald400
import com.nevoit.cresto.ui.theme.glasense.Red500
import com.nevoit.cresto.ui.theme.glasense.Slate500
import com.nevoit.cresto.util.g2
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
    var appSizeLong by remember { mutableStateOf(0L) }
    var dataSizeLong by remember { mutableStateOf(0L) }
    var cacheSizeLong by remember { mutableStateOf(0L) }

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
    val onSurfaceContainer = CalculatedColor.onSurfaceContainer
    val onBackground = MaterialTheme.colorScheme.onBackground
    val surfaceColor = CalculatedColor.hierarchicalBackgroundColor
    val hierarchicalSurfaceColor = CalculatedColor.hierarchicalSurfaceColor

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
            "Cancel",
            onClick = { dismissDialog() },
            isPrimary = false
        ),
        DialogItemData(
            "Confirm",
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
            "Cancel",
            onClick = { dismissDialog() },
            isPrimary = false
        ),
        DialogItemData(
            "Confirm",
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
                    title = "Data & Storage",
                    enableGlow = false,
                    info = "Manage your application's storage footprint."
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Item to display storage usage details
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Storage Usage",
                        fontSize = 14.sp,
                        lineHeight = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(.5f),
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
                            Spacer(modifier = Modifier.height(8.dp))
                            StorageChart(
                                appSize = appSizeLong,
                                dataSize = dataSizeLong,
                                cacheSize = cacheSizeLong
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            StorageInfoRow(
                                label = "App Size",
                                value = appSize,
                                icon = painterResource(R.drawable.ic_mini_parcel)
                            )
                            StorageInfoRow(
                                label = "User Data",
                                value = dataSize,
                                icon = painterResource(R.drawable.ic_mini_user)
                            )
                            StorageInfoRow(
                                label = "Cache",
                                value = cacheSize,
                                icon = painterResource(R.drawable.ic_mini_cache)
                            )
                            StorageInfoRow(
                                label = "Total",
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
                        ConfigItem(title = "Export Database") {
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
                        ConfigItem(title = "Import Database") {
                            // TODO: Implement database import functionality
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            // Item containing data reset and clearing options
            item {
                ConfigItemContainer(
                    title = "Reset",
                    backgroundColor = hierarchicalSurfaceColor
                ) {
                    Column {
                        ConfigItem(
                            title = "Reset All Settings",
                            color = Red500,
                            clickable = true,
                            indication = true,
                            onClick = {
                                showDialog(
                                    dialogItems2,
                                    "Reset All Settings?",
                                    "This will reset all settings to their defaults. Your todos will not be deleted."
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
                            title = "Clear All Data",
                            color = Red500,
                            clickable = true,
                            indication = true,
                            onClick = {
                                showDialog(
                                    dialogItems,
                                    "Clear All Data?",
                                    "This will permanently delete all application data, including todos and settings. This action cannot be undone."
                                )
                            }
                        ) {}
                    }

                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        // A small title that dynamically appears at the top when the user scrolls down
        DynamicSmallTitle(
            modifier = Modifier.align(Alignment.TopCenter),
            title = "Data & Storage",
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
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_forward_nav),
                contentDescription = "Back",
                modifier = Modifier.width(32.dp)
            )
        }
    }

    if (dialogState.isVisible) {
        GlasenseDialog(
            density = density,
            dialogState = dialogState,
            backdrop = backdrop,
            onDismiss = { dismissDialog() },
            modifier = Modifier
        )
    }
}

@Composable
private fun StorageInfoRow(label: String, value: String, icon: Painter, isTotal: Boolean = false) {
    val onBackground = MaterialTheme.colorScheme.onBackground
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

    val appColor = MaterialTheme.colorScheme.primary
    val dataColor = Amber400
    val cacheColor = Emerald400

    val hierarchicalSurfaceColor = CalculatedColor.hierarchicalSurfaceColor


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
            LegendItem(color = appColor, text = "App Size")
            Spacer(Modifier.width(16.dp))
            LegendItem(color = dataColor, text = "User Data")
            Spacer(Modifier.width(16.dp))
            LegendItem(color = cacheColor, text = "Cache")
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
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}
