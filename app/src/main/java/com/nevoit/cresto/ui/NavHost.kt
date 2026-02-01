package com.nevoit.cresto.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseOutExpo
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import com.nevoit.cresto.data.todo.TodoViewModel
import com.nevoit.cresto.ui.components.glasense.DialogItemData
import com.nevoit.cresto.ui.components.glasense.MenuItemData
import com.nevoit.cresto.ui.screens.HomeScreen
import com.nevoit.cresto.ui.screens.MindFlowScreen
import com.nevoit.cresto.ui.screens.SettingsScreen

@Composable
fun AppNavHost(
    currentRoute: String,
    showMenu: (anchorPosition: androidx.compose.ui.geometry.Offset, items: List<MenuItemData>) -> Unit,
    showDialog: (items: List<DialogItemData>, title: String, message: String?) -> Unit,
    viewModel: TodoViewModel
) {
    val commonEnterTransition = fadeIn(animationSpec = tween(200, 100)) + scaleIn(
        animationSpec = tween(400, 100, EaseOutExpo),
        initialScale = 0.95f
    )

    val commonExitTransition = fadeOut(animationSpec = tween(200)) + scaleOut(
        animationSpec = tween(600, 0, CubicBezierEasing(.2f, .2f, .0f, 1f)),
        targetScale = 0.95f
    )
    val saveableStateHolder = rememberSaveableStateHolder()

    AnimatedVisibility(
        visible = currentRoute == Screen.Home.route,
        enter = commonEnterTransition,
        exit = commonExitTransition
    ) {
        saveableStateHolder.SaveableStateProvider(key = Screen.Home.route) {
            HomeScreen(showMenu = showMenu, viewModel = viewModel, showDialog = showDialog)
        }
    }

    AnimatedVisibility(
        visible = currentRoute == Screen.Star.route,
        enter = commonEnterTransition,
        exit = commonExitTransition
    ) {
        saveableStateHolder.SaveableStateProvider(key = Screen.Star.route) {
            MindFlowScreen(viewModel)
        }
    }

    AnimatedVisibility(
        visible = currentRoute == Screen.Settings.route,
        enter = commonEnterTransition,
        exit = commonExitTransition
    ) {
        saveableStateHolder.SaveableStateProvider(key = Screen.Settings.route) {
            SettingsScreen()
        }
    }
}
