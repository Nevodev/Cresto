package com.nevoit.cresto.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseOutExpo
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.nevoit.cresto.data.todo.TodoViewModel
import com.nevoit.cresto.data.todo.liveactivity.LiveActivityViewModel
import com.nevoit.cresto.ui.components.glasense.DialogItemData
import com.nevoit.cresto.ui.components.glasense.MenuItemData
import com.nevoit.cresto.ui.screens.HomeScreen
import com.nevoit.cresto.ui.screens.MindFlowScreen
import com.nevoit.cresto.ui.screens.SettingsScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun BoxScope.NavContainer(
    currentRoute: String,
    showMenu: (anchorPosition: androidx.compose.ui.geometry.Offset, items: List<MenuItemData>) -> Unit,
    showDialog: (items: List<DialogItemData>, title: String, message: String?) -> Unit,
    viewModel: TodoViewModel,
    liveActivityViewModel: LiveActivityViewModel
) {
    val saveableStateHolder = rememberSaveableStateHolder()

    ManualAnimatedVisibility(
        visible = currentRoute == Screen.Home.route
    ) {
        saveableStateHolder.SaveableStateProvider(key = Screen.Home.route) {
            HomeScreen(
                showMenu = showMenu,
                viewModel = viewModel,
                showDialog = showDialog,
                liveActivityViewModel = liveActivityViewModel
            )

        }
    }

    ManualAnimatedVisibility(
        visible = currentRoute == Screen.Star.route
    ) {
        saveableStateHolder.SaveableStateProvider(key = Screen.Star.route) {
            MindFlowScreen(viewModel)

        }
    }

    ManualAnimatedVisibility(
        visible = currentRoute == Screen.Settings.route
    ) {
        saveableStateHolder.SaveableStateProvider(key = Screen.Settings.route) {
            SettingsScreen()
        }
    }
}

@Composable
private fun ManualAnimatedVisibility(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    val alpha = remember { Animatable(if (visible) 1f else 0f, 0.000001f) }
    val scale = remember { Animatable(if (visible) 1f else 0.95f, 0.000001f) }

    LaunchedEffect(visible) {
        if (visible) {
            launch {
                delay(100)
                alpha.animateTo(1f, tween(200))
            }
            launch {
                delay(100)
                scale.animateTo(1f, tween(400, easing = EaseOutExpo))
            }
        } else {
            launch {
                alpha.animateTo(0f, tween(200))
            }
            launch {
                scale.animateTo(0.95f, tween(600, easing = CubicBezierEasing(.2f, .2f, .0f, 1f)))
            }
        }
    }

    if (visible || alpha.value > 0f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    this.alpha = alpha.value
                    scaleX = scale.value
                    scaleY = scale.value
                }
        ) {
            content()
        }
    }
}
