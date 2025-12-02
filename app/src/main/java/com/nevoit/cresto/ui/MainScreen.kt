package com.nevoit.cresto.ui

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.nevoit.cresto.CrestoApplication
import com.nevoit.cresto.R
import com.nevoit.cresto.data.todo.TodoItem
import com.nevoit.cresto.data.todo.TodoViewModel
import com.nevoit.cresto.data.todo.TodoViewModelFactory
import com.nevoit.cresto.toolkit.gaussiangradient.smoothGradientMask
import com.nevoit.cresto.toolkit.gaussiangradient.smoothGradientMaskFallback
import com.nevoit.cresto.ui.components.bottomsheet.BottomSheet
import com.nevoit.cresto.ui.components.glasense.DialogItemData
import com.nevoit.cresto.ui.components.glasense.DialogState
import com.nevoit.cresto.ui.components.glasense.GlasenseDialog
import com.nevoit.cresto.ui.components.glasense.GlasenseMenu
import com.nevoit.cresto.ui.components.glasense.GlasenseNavigationButton
import com.nevoit.cresto.ui.components.glasense.MenuItemData
import com.nevoit.cresto.ui.components.glasense.MenuState
import com.nevoit.cresto.ui.theme.glasense.CalculatedColor
import com.nevoit.cresto.ui.theme.glasense.linearGradientMaskB2T70
import com.nevoit.cresto.ui.theme.glasense.linearGradientMaskB2T90
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Star : Screen("star")
    object Settings : Screen("settings")
}

@OptIn(ExperimentalHazeApi::class)
@Composable
fun MainScreen() {
    val surfaceColor = CalculatedColor.hierarchicalBackgroundColor
    val navController = rememberNavController()
    val hazeState = rememberHazeState()
    val backdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }

    var menuState by remember { mutableStateOf(MenuState()) }

    val showMenu: (anchorPosition: androidx.compose.ui.geometry.Offset, items: List<MenuItemData>) -> Unit =
        { position, items ->
            menuState = MenuState(isVisible = true, anchorPosition = position, items = items)
        }

    val dismissMenu = {
        menuState = menuState.copy(isVisible = false)
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


    val density = LocalDensity.current
    val interactionSource = remember { MutableInteractionSource() }


    val application = LocalContext.current.applicationContext as CrestoApplication
    val viewModel: TodoViewModel = viewModel(
        factory = TodoViewModelFactory(application.repository)
    )

    val bottomSheetState by viewModel.bottomSheetState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .hazeSource(hazeState, 0f)
                .background(color = CalculatedColor.hierarchicalBackgroundColor)
        ) {
            AppNavHost(
                navController = navController,
                showMenu = showMenu,
                viewModel = viewModel,
                showDialog = showDialog
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(136.dp)
                .align(Alignment.BottomCenter)
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Modifier.hazeEffect(
                        hazeState
                    ) {
                        blurRadius = 10.dp
                        progressive = HazeProgressive.verticalGradient(
                            startIntensity = 0.2f,
                            endIntensity = 0.6f
                        )
                        noiseFactor = 0f
                        mask = linearGradientMaskB2T90
                        inputScale = HazeInputScale.Fixed(0.5f)
                    } else Modifier.hazeEffect(
                        hazeState
                    ) {
                        blurRadius = 4.dp
                        noiseFactor = 0f
                        mask = linearGradientMaskB2T70
                    })

                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Modifier.smoothGradientMask(
                        surfaceColor.copy(alpha = 0f), surfaceColor.copy(alpha = 1f), 0f, 1f, 0.6f
                    ) else Modifier.smoothGradientMaskFallback(surfaceColor, 0.6f)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 32.dp)
                    .height(56.dp)
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                GlasenseNavigationButton(
                    modifier = Modifier.weight(1f),
                    isActive = currentRoute == Screen.Home.route,
                    onClick = {
                        if (currentRoute != Screen.Home.route) {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }; launchSingleTop = true; restoreState = true
                            }
                        }
                    },
                    backdrop = backdrop
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_list),
                        contentDescription = stringResource(R.string.all_todos)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                GlasenseNavigationButton(
                    modifier = Modifier.weight(1f),
                    isActive = currentRoute == Screen.Star.route,
                    onClick = {
                        if (currentRoute != Screen.Star.route) {
                            navController.navigate(Screen.Star.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }; launchSingleTop = true; restoreState = true
                            }
                        }
                    },
                    backdrop = backdrop
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_star),
                        contentDescription = stringResource(R.string.mind_flow)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                GlasenseNavigationButton(
                    modifier = Modifier.weight(1f),
                    isActive = currentRoute == Screen.Settings.route,
                    onClick = {
                        if (currentRoute != Screen.Settings.route) {
                            navController.navigate(Screen.Settings.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }; launchSingleTop = true; restoreState = true
                            }
                        }
                    },
                    backdrop = backdrop
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_gear),
                        contentDescription = stringResource(R.string.settings)
                    )
                }
            }
        }

        val scaleAni = remember { Animatable(0.4f) }
        val alphaAni = remember { Animatable(0f) }
        var isMenuInComposition by remember { mutableStateOf(false) }

        LaunchedEffect(menuState.isVisible) {
            if (menuState.isVisible) {
                delay(50)
                isMenuInComposition = true
                coroutineScope {
                    launch { scaleAni.animateTo(1f, spring(0.8f, 450f, 0.001f)) }
                    launch { alphaAni.animateTo(1f) }
                }
            } else {
                delay(50)
                coroutineScope {
                    launch { scaleAni.animateTo(0.4f, spring(0.7f, 600f)) }
                    launch { alphaAni.animateTo(0f) }
                }
                isMenuInComposition = false
            }
        }

        if (menuState.isVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { dismissMenu() })
            )
        }
        if (isMenuInComposition || menuState.isVisible) {
            GlasenseMenu(
                density = density,
                menuState = menuState,
                backdrop = backdrop,
                onDismiss = dismissMenu,
                modifier = Modifier
                    .width(228.dp)
                    .graphicsLayer {
                        scaleX = scaleAni.value
                        scaleY = scaleAni.value
                        transformOrigin = TransformOrigin(0f, 0f)
                    },
                alphaAni = { alphaAni.value },
                scaleAni = { scaleAni.value }
            )
        }
        if (bottomSheetState.isVisible) {
            Box(modifier = Modifier.fillMaxSize()) {
                BottomSheet(
                    onDismiss = { viewModel.hideBottomSheet() },
                    onAddClick = { title, flagIndex, finalDate ->
                        viewModel.insert(
                            TodoItem(
                                title = title,
                                flag = flagIndex,
                                dueDate = finalDate
                            )
                        )
                    }, showDialog = showDialog
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
}

