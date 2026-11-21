package com.nevoit.cresto.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.ui.components.popup.PopupCaller
import com.nevoit.cresto.ui.components.popup.PopupLayer
import com.nevoit.cresto.ui.components.popup.PopupMode
import com.nevoit.cresto.ui.components.popup.PopupTransition
import com.nevoit.glasense.core.interaction.DimIndication
import com.nevoit.glasense.theme.tokens.Amber500
import com.nevoit.glasense.theme.tokens.Blue500
import com.nevoit.glasense.theme.tokens.Cyan500
import com.nevoit.glasense.theme.tokens.Emerald500
import com.nevoit.glasense.theme.tokens.Fuchsia500
import com.nevoit.glasense.theme.tokens.Green500
import com.nevoit.glasense.theme.tokens.Indigo500
import com.nevoit.glasense.theme.tokens.Lime500
import com.nevoit.glasense.theme.tokens.Orange500
import com.nevoit.glasense.theme.tokens.Pink500
import com.nevoit.glasense.theme.tokens.Purple500
import com.nevoit.glasense.theme.tokens.Red500
import com.nevoit.glasense.theme.tokens.Rose500
import com.nevoit.glasense.theme.tokens.Sky500
import com.nevoit.glasense.theme.tokens.Teal500
import com.nevoit.glasense.theme.tokens.Violet500
import com.nevoit.glasense.theme.tokens.Yellow500
import kotlinx.coroutines.delay

private const val AccentColorsPerRow = 6

@Composable
internal fun ThemeColorPickerPopup(
    caller: PopupCaller,
    current: Int,
    onPick: (Int) -> Unit,
    onDismissed: () -> Unit,
) {
    var dismissRequest by remember { mutableIntStateOf(0) }
    val panelShape = remember { RoundedRectangle(28.dp) }
    val containerColor by rememberUpdatedState(AppColors.elevatedCardBackground)
    val colorProducer = remember { ColorProducer { containerColor } }
    PopupLayer(
        onDismissed = onDismissed,
        transition = PopupTransition.FromAnchor(
            caller = caller,
            mode = PopupMode.Menu,
            shape = panelShape,
            containerColor = colorProducer,
            anchoredToCaller = true,
            expandUpwards = true,
        ),
    ) { dismiss ->
        val latestDismiss by rememberUpdatedState(dismiss)
        LaunchedEffect(dismissRequest) {
            if (dismissRequest == 0) return@LaunchedEffect
            delay(100)
            latestDismiss()
        }
        AccentColorPicker(
            current = current,
            onPick = { color ->
                onPick(color)
                dismissRequest++
            },
        )
    }
}

@Composable
private fun AccentColorPicker(current: Int, onPick: (Int) -> Unit) {
    val haptic = LocalHapticFeedback.current
    val colors = remember {
        listOf(
            Rose500, Red500, Orange500, Amber500,
            Yellow500, Lime500, Green500, Emerald500,
            Teal500, Cyan500, Sky500, Blue500,
            Indigo500, Violet500, Purple500, Fuchsia500, Pink500,
        )
    }
    Column(
        Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
            .padding(12.dp),
    ) {
        colors.chunked(AccentColorsPerRow).forEachIndexed { index, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (index == 0) 0.dp else 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { color ->
                    AccentSwatch(
                        color = color,
                        selected = color.toArgb() == current,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                            onPick(color.toArgb())
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(AccentColorsPerRow - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AccentSwatch(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = remember { Capsule() }
    Box(
        modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(color)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = DimIndication(shape = shape),
                onClick = onClick,
            )
            .then(
                if (selected) Modifier.drawBehind {
                    drawCircle(
                        color = Color.White.copy(alpha = .6f),
                        radius = size.minDimension / 4f,
                    )
                } else Modifier,
            ),
    )
}
