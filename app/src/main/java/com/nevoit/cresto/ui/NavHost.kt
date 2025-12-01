package com.nevoit.cresto.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseOutQuint
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.nevoit.cresto.ui.components.glasense.DialogItemData
import com.nevoit.cresto.ui.components.glasense.MenuItemData
import com.nevoit.cresto.ui.screens.HomeScreen
import com.nevoit.cresto.ui.screens.MindFlowScreen
import com.nevoit.cresto.ui.screens.SettingsScreen
import com.nevoit.cresto.ui.viewmodel.TodoViewModel

@Composable
fun AppNavHost(
    navController: NavHostController,
    showMenu: (anchorPosition: androidx.compose.ui.geometry.Offset, items: List<MenuItemData>) -> Unit,
    showDialog: (items: List<DialogItemData>, title: String, message: String?) -> Unit,
    viewModel: TodoViewModel
) {

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        enterTransition = {
            fadeIn(animationSpec = tween(250)) + scaleIn(
                animationSpec = tween(400, 0, EaseOutQuint),
                initialScale = 0.90f
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(150)) + scaleOut(
                animationSpec = tween(300, 0, CubicBezierEasing(.2f, .2f, .0f, 1f)),
                targetScale = 0.90f
            )
        }
    ) {
        composable(
            route = Screen.Home.route
        ) {
            HomeScreen(showMenu = showMenu, viewModel = viewModel, showDialog = showDialog)
        }

        composable(
            route = Screen.Star.route
        ) {
            MindFlowScreen()
        }

        composable(
            route = Screen.Settings.route
        ) {
            SettingsScreen()
        }
    }
}
