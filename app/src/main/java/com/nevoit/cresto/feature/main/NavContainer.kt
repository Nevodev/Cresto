package com.nevoit.cresto.feature.main

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect
import com.nevoit.cresto.data.todo.TodoViewModel
import com.nevoit.cresto.feature.calendar.CalendarScreen
import com.nevoit.cresto.feature.home.HomeScreen
import com.nevoit.cresto.feature.insights.InsightsScreen
import com.nevoit.cresto.navigation.CrossfadeNavContainer
import com.nevoit.cresto.ui.components.glasense.GlasenseMenuItem

@Composable
fun BoxScope.NavContainer(
    currentRoute: String,
    showMenu: (anchorBounds: Rect, items: List<GlasenseMenuItem>) -> Unit,
    viewModel: TodoViewModel,
    onOpenGroupBottomSheet: () -> Unit
) {
    CrossfadeNavContainer(
        destination = currentRoute,
        destinations = listOf(Screen.Home.route, Screen.Star.route, Screen.Settings.route),
    ) { route ->
        when (route) {
            Screen.Home.route -> HomeScreen(
                showMenu = showMenu,
                viewModel = viewModel,
                onOpenGroupBottomSheet = onOpenGroupBottomSheet
            )
            Screen.Star.route -> CalendarScreen(viewModel = viewModel)
            Screen.Settings.route -> InsightsScreen(viewModel = viewModel)
        }
    }
}
