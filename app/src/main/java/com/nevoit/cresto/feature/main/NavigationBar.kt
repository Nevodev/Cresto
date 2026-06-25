package com.nevoit.cresto.feature.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.nevoit.cresto.R
import com.nevoit.cresto.ui.components.glasense.GlasenseNavigationButton
import com.nevoit.glasense.core.component.Icon

@Composable
internal fun BoxScope.NavigationBar(
    tabBarY: () -> Float,
    currentRoute: () -> String,
    onCurrentRouteChange: (String) -> Unit,
    backdrop: LayerBackdrop,
    liquidGlass: Boolean
) {
    val currentRoute = currentRoute()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            .graphicsLayer {
                translationY = tabBarY()
            }
            .height(56.dp)
            .align(Alignment.BottomCenter),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        GlasenseNavigationButton(
            modifier = Modifier.weight(1f),
            isActive = currentRoute == Screen.Home.route,
            onClick = {
                if (currentRoute != Screen.Home.route) {
                    onCurrentRouteChange(Screen.Home.route)
                }
            },
            backdrop = backdrop,
            liquidGlass = liquidGlass
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_list),
                contentDescription = stringResource(R.string.todos)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        GlasenseNavigationButton(
            modifier = Modifier.weight(1f),
            isActive = currentRoute == Screen.Star.route,
            onClick = {
                if (currentRoute != Screen.Star.route) {
                    onCurrentRouteChange(Screen.Star.route)
                }
            },
            backdrop = backdrop,
            liquidGlass = liquidGlass
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_time_line),
                contentDescription = stringResource(R.string.calendar_view)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        GlasenseNavigationButton(
            modifier = Modifier.weight(1f),
            isActive = currentRoute == Screen.Settings.route,
            onClick = {
                if (currentRoute != Screen.Settings.route) {
                    onCurrentRouteChange(Screen.Settings.route)
                }
            },
            backdrop = backdrop,
            liquidGlass = liquidGlass
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_magnifying_glass_sparkle),
                contentDescription = stringResource(R.string.insights)
            )
        }
    }
}