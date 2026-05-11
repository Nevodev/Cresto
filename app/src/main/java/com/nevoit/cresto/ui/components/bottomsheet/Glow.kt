package com.nevoit.cresto.ui.components.bottomsheet

import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.shapes.Capsule
import com.nevoit.cresto.R
import com.nevoit.cresto.theme.AppButtonColors
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.defaultEnterTransition
import com.nevoit.cresto.theme.defaultExitTransition
import com.nevoit.cresto.theme.gradientColorsDark
import com.nevoit.cresto.theme.gradientColorsLight
import com.nevoit.cresto.theme.highlightColorsDark
import com.nevoit.cresto.theme.highlightColorsLight
import com.nevoit.cresto.theme.isAppInDarkTheme
import com.nevoit.cresto.ui.components.CustomAnimatedVisibility
import com.nevoit.cresto.ui.components.glasense.GlasenseButton
import com.nevoit.cresto.ui.components.glasense.RotatingGlow
import com.nevoit.cresto.ui.components.glasense.RotatingGlowBorder
import com.nevoit.cresto.ui.components.glasense.glasenseHighlight
import com.nevoit.cresto.ui.viewmodel.AiViewModel

@Composable
fun GlowContainer(
    modifier: Modifier = Modifier,
    glowColors: List<Color>,
    strongerHighlight: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = isAppInDarkTheme()

    val backdrop = rememberLayerBackdrop {
        drawContent()
    }

    val highlightColors = if (darkTheme) {
        highlightColorsDark
    } else {
        highlightColorsLight
    }
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        RotatingGlow(
            modifier = Modifier
                .height(64.dp)
                .padding(horizontal = 8.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp)),
            blurRadius = 16.dp,
            shape = RectangleShape,
            colors = glowColors,
            timeMillis = 5000,
            backdrop = backdrop
        )
        Box(
            modifier = Modifier
                .height(56.dp)
                .padding(horizontal = 12.dp)
                .fillMaxWidth()
                .drawPlainBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule() },
                    effects = {
                        blur(64f.dp.toPx(), TileMode.Clamp)
                        colorControls(saturation = 1.1f)
                    }, onDrawSurface = {
                        // The drawing logic is different for light and dark themes.
                        if (!darkTheme) {
                            drawRect(
                                brush = SolidColor(Color(0xFF272727).copy(alpha = 0.2f)),
                                style = Fill,
                                blendMode = BlendMode.Luminosity,
                            )
                            drawRect(
                                brush = SolidColor(Color(0xFF252525).copy(alpha = 1f)),
                                style = Fill,
                                blendMode = BlendMode.Plus,
                            )
                            drawRect(
                                brush = SolidColor(Color(0xFF555555).copy(alpha = 0.5f)),
                                style = Fill,
                                blendMode = BlendMode.ColorDodge,
                            )
                            drawRect(
                                brush = SolidColor(Color(0xFFFFFFFF).copy(alpha = 0.2f)),
                                style = Fill,
                                blendMode = BlendMode.SrcOver,
                            )
                        } else {
                            drawRect(
                                brush = SolidColor(Color(0xFF000000).copy(alpha = 0.5f)),
                                style = Fill,
                                blendMode = BlendMode.Luminosity,
                            )
                            drawRect(
                                brush = SolidColor(Color(0xFF252525).copy(alpha = 1f)),
                                style = Fill,
                                blendMode = BlendMode.Plus,
                            )
                            drawRect(
                                brush = SolidColor(Color(0xFF4B4B4B).copy(alpha = 0.5f)),
                                style = Fill,
                                blendMode = BlendMode.ColorDodge,
                            )
                        }
                    })
                .glasenseHighlight(56.dp)
                .clip(Capsule())
        ) {
            RotatingGlowBorder(
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 4.dp,
                blurRadius = 4.dp,
                shape = Capsule(),
                colors = highlightColors,
                timeMillis = 3000
            )
            if (strongerHighlight) {
                RotatingGlowBorder(
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 8.dp,
                    blurRadius = 8.dp,
                    shape = Capsule(),
                    colors = highlightColors,
                    timeMillis = 3000
                )
            }
            content()
        }
    }
}

@Composable
fun AiInputBox(
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    textFieldState: TextFieldState,
    aiViewModel: AiViewModel,
    imagePickerLauncher: ManagedActivityResultLauncher<String, *>
) {
    val darkTheme = isAppInDarkTheme()
    val gradientColors = if (darkTheme) {
        gradientColorsDark
    } else {
        gradientColorsLight
    }
    GlowContainer(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        glowColors = gradientColors
    ) {
        if (!isLoading) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 16.dp)
                    ) {
                        BasicTextField(
                            state = textFieldState,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            onKeyboardAction = {
                                aiViewModel.generateContent(textFieldState.text.toString())
                            },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = AppColors.content
                            ),
                            cursorBrush = SolidColor(AppColors.primary)
                        )
                        if (textFieldState.text.isBlank()) {
                            Text(
                                stringResource(R.string.extract_from_text),
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        alpha = 0.5f
                                        blendMode =
                                            if (darkTheme) BlendMode.Plus else BlendMode.Luminosity
                                    },
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (!darkTheme) Color(0xFF545454) else MaterialTheme.typography.bodyLarge.color
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier.size(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CustomAnimatedVisibility(
                        visible = !textFieldState.text.isBlank(),
                        enter = defaultEnterTransition,
                        exit = defaultExitTransition
                    ) {
                        GlasenseButton(
                            enabled = true,
                            shape = CircleShape,
                            onClick = {
                                aiViewModel.generateContent(textFieldState.text.toString())
                            },
                            modifier = Modifier
                                .width(40.dp)
                                .height(40.dp)
                                .align(Alignment.Center),
                            colors = AppButtonColors.primary()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .glasenseHighlight(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_arrow_up),
                                    contentDescription = stringResource(R.string.extract),
                                    modifier = Modifier.width(28.dp)
                                )
                            }
                        }
                    }
                    CustomAnimatedVisibility(
                        visible = textFieldState.text.isBlank(),
                        enter = defaultEnterTransition,
                        exit = defaultExitTransition
                    ) {
                        GlasenseButton(
                            enabled = true,
                            shape = CircleShape,
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier
                                .width(40.dp)
                                .height(40.dp)
                                .align(Alignment.Center),
                            colors = AppButtonColors.primary().copy(
                                containerColor = Color.Transparent,
                                contentColor = AppColors.content
                            )
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_photo),
                                contentDescription = stringResource(R.string.extract_from_image),
                                modifier = Modifier
                                    .size(32.dp)
                                    .graphicsLayer {
                                        alpha = 0.5f
                                    }
                            )
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.extracting),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .graphicsLayer {
                            alpha = 0.5f
                            blendMode =
                                if (darkTheme) BlendMode.Plus else BlendMode.Luminosity
                        },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (!darkTheme) Color(0xFF545454) else MaterialTheme.typography.bodyLarge.color
                )
            }
        }
    }
}