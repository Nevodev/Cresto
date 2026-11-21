package com.nevoit.cresto.navigation

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import com.nevoit.cresto.data.todo.TodoViewModel
import com.nevoit.cresto.feature.detail.DetailScreen
import com.nevoit.cresto.feature.main.MainScreen
import com.nevoit.cresto.feature.recentlydeleted.RecentlyDeletedScreen
import com.nevoit.cresto.feature.settings.AIScreen
import com.nevoit.cresto.feature.settings.AboutScreen
import com.nevoit.cresto.feature.settings.AppearanceScreen
import com.nevoit.cresto.feature.settings.CreditsScreen
import com.nevoit.cresto.feature.settings.DataStorageScreen
import com.nevoit.cresto.feature.settings.GeneralScreen
import com.nevoit.cresto.feature.settings.SettingsDestination
import com.nevoit.cresto.feature.settings.SettingsScreen
import com.nevoit.cresto.feature.settings.util.SettingsManager
import com.nevoit.cresto.navigation.shape.DeviceCornerState
import com.nevoit.cresto.navigation.shape.LocalDeviceCorners
import com.nevoit.cresto.theme.AppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel

@Serializable
sealed interface AppDestination {
    @Serializable
    data object Main : AppDestination

    @Serializable
    data class Detail(val todoId: Int) : AppDestination

    @Serializable
    data class Settings(val page: SettingsDestination = SettingsDestination.SETTINGS) : AppDestination

    @Serializable
    data object RecentlyDeleted : AppDestination
}

val LocalAppNavigation = staticCompositionLocalOf<NavigationScope<AppDestination>> {
    error("AppNavigationHost has not been provided")
}

private val LocalAppBack = staticCompositionLocalOf<(() -> Unit)?> { null }

@Composable
fun rememberAppBack(): () -> Unit {
    val activity = LocalActivity.current
    return LocalAppBack.current ?: remember(activity) { { activity?.finish() } }
}

@Composable
fun AppNavigationHost(root: AppDestination = AppDestination.Main) {
    val activity = LocalActivity.current
    val view = LocalView.current
    val corners = remember(view) {
        DeviceCornerState().apply { view.rootWindowInsets?.let(::update) }
    }
    val navigator = rememberNavigator(root)
    val todoViewModel: TodoViewModel = koinViewModel()
    val scope = rememberCoroutineScope()
    val background = AppColors.pageBackground

    CompositionLocalProvider(
        LocalDeviceCorners provides corners,
        LocalContinuousTransitionsEnabled provides SettingsManager.isContinuousTransitionsEnabledState.value,
    ) {
        NavigationHost(
            modifier = Modifier.onGloballyPositioned {
                view.rootWindowInsets?.let(corners::update)
            },
            navigator = navigator,
            callerUnderlayScale = 0.92f,
            transitionBackground = background,
        ) {
            val back: () -> Unit = {
                if (entry.id == navigator.backStack.first().id) activity?.finish() else this.back()
            }
            ReportNavigationBackground(ColorProducer { background })
            CompositionLocalProvider(
                LocalAppNavigation provides this,
                LocalAppBack provides back,
            ) {
                Box(Modifier.fillMaxSize().background(background)) {
                    when (val page = destination) {
                        AppDestination.Main -> MainScreen(viewModel = todoViewModel)
                        is AppDestination.Detail -> DetailScreen(
                            todoId = page.todoId,
                            viewModel = koinViewModel(),
                            detailState = remember(todoViewModel, page.todoId) {
                                todoViewModel.getTodoDetail(page.todoId)
                            },
                            onDelete = { id ->
                                back()
                                // Keep the source row alive during the return, as the Activity result did.
                                scope.launch {
                                    delay(300)
                                    todoViewModel.deleteById(id)
                                }
                            },
                        )
                        is AppDestination.Settings -> when (page.page) {
                            SettingsDestination.SETTINGS -> SettingsScreen()
                            SettingsDestination.APPEARANCE -> AppearanceScreen()
                            SettingsDestination.AI -> AIScreen()
                            SettingsDestination.DATA_STORAGE -> DataStorageScreen()
                            SettingsDestination.GENERAL -> GeneralScreen()
                            SettingsDestination.ABOUT -> AboutScreen()
                            SettingsDestination.CREDITS -> CreditsScreen()
                        }
                        AppDestination.RecentlyDeleted -> RecentlyDeletedScreen(viewModel = koinViewModel())
                    }
                }
            }
        }
    }
}
