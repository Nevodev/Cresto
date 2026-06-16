// Package declaration for the settings screen
package com.nevoit.cresto.feature.settings

// Import necessary libraries and components
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush.Companion.sweepGradient
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.nevoit.cresto.R
import com.nevoit.cresto.feature.settings.util.AISettingsViewModel
import com.nevoit.cresto.theme.AppButtonColors
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.AppSpecs
import com.nevoit.cresto.theme.harmonize
import com.nevoit.cresto.ui.components.glasense.GlasenseButton
import com.nevoit.cresto.ui.components.glasense.GlasenseDynamicSmallTitle
import com.nevoit.cresto.ui.components.glasense.isScrolledPast
import com.nevoit.cresto.ui.components.packed.ConfigInfoHeader
import com.nevoit.cresto.ui.components.packed.ConfigTextField
import com.nevoit.cresto.ui.components.packed.TopBarSpacer
import com.nevoit.glasense.component.ListStack
import com.nevoit.glasense.core.component.Icon
import com.nevoit.glasense.core.component.Text
import com.nevoit.glasense.core.component.VGap
import com.nevoit.glasense.theme.tokens.Blue500
import com.nevoit.glasense.theme.tokens.Pink400
import com.nevoit.glasense.theme.tokens.Purple500

@Composable
fun AIScreen(aiSettingsViewModel: AISettingsViewModel = viewModel()) {
    // Get the current activity instance to allow finishing the screen
    val activity = LocalActivity.current

    // Calculate the height of the status bar to adjust layout
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // Get colors from the app's custom theme
    val backgroundColor = AppColors.pageBackground
    val hierarchicalSurfaceColor = AppColors.cardBackground

    val backdrop = rememberLayerBackdrop {
        drawRect(
            color = backgroundColor,
            size = Size(this.size.width * 3, this.size.height * 3),
            topLeft = Offset(-this.size.width, -this.size.height)
        )
        drawContent()
    }

    // Remember the state for the lazy list to control scrolling
    val lazyListState = rememberLazyListState()

    // Determine if the small title should be visible based on the scroll position
    val isSmallTitleVisible by lazyListState.isScrolledPast(statusBarHeight + 24.dp)
    val apiUrl by aiSettingsViewModel.apiUrl
    val apiKey by aiSettingsViewModel.apiKey
    val textModel by aiSettingsViewModel.textModel
    val multimodalModel by aiSettingsViewModel.multimodalModel

    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Root container for the screen, filling the entire available space
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        ListStack(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .imePadding(),
            cornerRadius = AppSpecs.cardCorner,
            contentPadding = PaddingValues(bottom = navigationBarHeight)
        ) {
            TopBarSpacer()
            // Header item for the AI section with a gradient brush and glow effect
            item {
                ConfigInfoHeader(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    brush = sweepGradient(
                        colorStops = arrayOf(
                            0f to harmonize(Pink400),
                            0.33f to harmonize(Purple500),
                            0.66f to harmonize(Blue500),
                            1f to harmonize(Pink400)
                        )
                    ),
                    backgroundColor = hierarchicalSurfaceColor,
                    icon = painterResource(R.drawable.ic_twotone_sparkles),
                    title = stringResource(R.string.ai),
                    enableGlow = true,
                    info = stringResource(R.string.boost_your_experience_with_intelligent_cresto_function_calling)
                )
            }
            item { VGap(24.dp) }
            // Item container for API-related settings
            item {
                ConfigTextField(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    title = stringResource(R.string.url),
                    value = apiUrl,
                    onValueChange = aiSettingsViewModel::onApiUrlChanged,
                    backgroundColor = hierarchicalSurfaceColor,
                    singleLine = false,
                    decorateText = "https://",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    )
                )
                VGap(24.dp)
            }
            item {
                ConfigTextField(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    title = stringResource(R.string.api_key),
                    value = apiKey,
                    onValueChange = aiSettingsViewModel::onApiKeyChanged,
                    backgroundColor = hierarchicalSurfaceColor,
                    singleLine = false,
                    decorateText = "API key",
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                )
                VGap(24.dp)
            }
            // Item container for testing the AI functionality
            item {
                ConfigTextField(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    title = stringResource(R.string.text_processing_model),
                    value = textModel,
                    onValueChange = aiSettingsViewModel::onTextModelChanged,
                    backgroundColor = hierarchicalSurfaceColor,
                    singleLine = false,
                    decorateText = "Model Code",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    )
                )
                VGap(24.dp)
            }
            item {
                ConfigTextField(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    title = stringResource(R.string.multimodal_model),
                    value = multimodalModel,
                    onValueChange = aiSettingsViewModel::onMultimodalModelChanged,
                    backgroundColor = hierarchicalSurfaceColor,
                    singleLine = false,
                    decorateText = "Model Code",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    )
                )
                VGap(24.dp)
            }
            Section() {
                Row(
                    destructive = true,
                    onClick = aiSettingsViewModel::restoreDefaults
                ) {
                    Text(stringResource(R.string.reset))
                }
            }
            item { VGap() }
        }
        // A small title that dynamically appears at the top when the user scrolls down
        GlasenseDynamicSmallTitle(
            modifier = Modifier.align(Alignment.TopCenter),
            title = stringResource(R.string.ai),
            statusBarHeight = statusBarHeight,
            isVisible = isSmallTitleVisible,
            backdrop = backdrop,
            surfaceColor = backgroundColor
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
