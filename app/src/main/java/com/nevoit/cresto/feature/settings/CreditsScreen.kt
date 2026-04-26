package com.nevoit.cresto.feature.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.nevoit.cresto.R
import com.nevoit.cresto.theme.AppButtonColors
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.harmonize
import com.nevoit.cresto.ui.components.glasense.GlasenseButton
import com.nevoit.cresto.ui.components.glasense.GlasenseDynamicSmallTitle
import com.nevoit.cresto.ui.components.glasense.ZeroHeightDivider
import com.nevoit.cresto.ui.components.glasense.extend.overscrollSpacer
import com.nevoit.cresto.ui.components.glasense.isScrolledPast
import com.nevoit.cresto.ui.components.packed.ConfigInfoHeader
import com.nevoit.cresto.ui.components.packed.ConfigItemContainer
import com.nevoit.cresto.ui.components.packed.PageContent
import com.nevoit.cresto.ui.components.packed.VGap
import com.nevoit.glasense.theme.Slate500
import com.nevoit.glasense.theme.Springs
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/**
 * This composable function defines the Credits screen.
 * It displays a list of open-source libraries used in the app.
 * It uses experimental APIs for Material 3 and Haze effects.
 */
@OptIn(ExperimentalHazeApi::class)
@Composable
fun CreditsScreen() {
    val activity = LocalActivity.current

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val hazeState = rememberHazeState()

    val lazyListState = rememberLazyListState()

    val isSmallTitleVisible by lazyListState.isScrolledPast(statusBarHeight + 24.dp)

    val libraries by produceLibraries(R.raw.aboutlibraries)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.pageBackground)
    ) {
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
                    color = harmonize(Slate500),
                    backgroundColor = AppColors.cardBackground,
                    icon = painterResource(R.drawable.ic_twotone_info),
                    title = stringResource(R.string.credits),
                    info = stringResource(R.string.credits_info)
                )
                VGap()
            }
            libraries?.let { libs ->
                item(key = "libraries") {
                    Box(modifier = Modifier.animateItem(placementSpec = Springs.crisp())) {
                        ConfigItemContainer(
                            backgroundColor = AppColors.cardBackground
                        ) {
                            Column {
                                libs.libraries.forEachIndexed { index, library ->
                                    LibraryItem(library = library)
                                    if (index < libs.libraries.size - 1) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        ZeroHeightDivider()
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                VGap()
            }
            overscrollSpacer(lazyListState)
        }
        GlasenseDynamicSmallTitle(
            modifier = Modifier.align(Alignment.TopCenter),
            title = stringResource(R.string.credits),
            statusBarHeight = statusBarHeight,
            isVisible = isSmallTitleVisible,
            hazeState = hazeState,
            surfaceColor = AppColors.pageBackground
        ) {

        }
        GlasenseButton(
            enabled = true,
            shape = CircleShape,
            onClick = { activity?.finish() },
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

@Composable
fun LibraryItem(library: Library) {
    val uriHandler = LocalUriHandler.current
    val interactionSource = remember { MutableInteractionSource() }

    val alphaAni = remember { Animatable(1f) }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    alphaAni.animateTo(0.5f, tween(100))
                }

                is PressInteraction.Release -> {
                    alphaAni.animateTo(1f, tween(200))
                }

                is PressInteraction.Cancel -> {
                    alphaAni.animateTo(1f, tween(200))
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = alphaAni.value
            }
            .padding(vertical = 4.dp)
            .clickable(interactionSource = interactionSource, indication = null) {
                val url = library.website ?: library.scm?.url
                url?.let { uriHandler.openUri(it) }
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = library.name,
                style = MaterialTheme.typography.bodyLarge,
                color = AppColors.content,
                modifier = Modifier.weight(1f)
            )
            library.artifactVersion?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.contentVariant
                )
            }
        }
        val developers = library.developers.joinToString(", ") { it.name ?: "" }
        if (developers.isNotEmpty()) {
            Text(
                text = developers,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.contentVariant
            )
        }
    }
}
