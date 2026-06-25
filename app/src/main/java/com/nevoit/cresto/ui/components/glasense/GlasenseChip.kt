package com.nevoit.cresto.ui.components.glasense

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.shapes.Capsule
import com.nevoit.cresto.R
import com.nevoit.cresto.theme.AppColors
import com.nevoit.glasense.core.component.Icon
import com.nevoit.glasense.core.component.Text
import com.nevoit.glasense.core.interaction.DimIndication
import com.nevoit.glasense.core.interaction.overscroll.rememberOffsetOverscrollFactory
import com.nevoit.glasense.theme.LocalGlasenseContentColor
import com.nevoit.glasense.theme.LocalGlasenseTextStyle

private val ChipHorizontalPadding = 16.dp
private val ChipVerticalPadding = 8.dp
private const val ChipHeightSample = "Hg"

@Composable
fun <T> GlasenseChipGroup(
    modifier: Modifier = Modifier,
    items: List<T>,
    selectedItem: T,
    itemLabel: (T) -> String = { it.toString() },
    onItemSelected: (T) -> Unit
) {
    val overscrollFactory = rememberOffsetOverscrollFactory()
    val chipMinHeight = rememberChipMinHeight()

    CompositionLocalProvider(
        LocalOverscrollFactory provides overscrollFactory
    ) {
        LazyRow(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(items) { item ->
                val isSelected = item == selectedItem
                GlasenseChipItem(
                    text = itemLabel(item),
                    isSelected = isSelected,
                    minHeight = chipMinHeight,
                    onClick = { onItemSelected(item) }
                )
            }
        }
    }
}

@Composable
private fun GlasenseChipItem(
    text: String,
    isSelected: Boolean,
    minHeight: Dp,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) AppColors.primary else AppColors.cardBackground,
        animationSpec = tween(durationMillis = 100)
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else LocalGlasenseContentColor.current,
        animationSpec = tween(durationMillis = 100)
    )

    Box(
        modifier = Modifier
            .heightIn(min = minHeight)
            .clip(Capsule())
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = DimIndication()
            ) { onClick() }
            .padding(horizontal = ChipHorizontalPadding, vertical = ChipVerticalPadding),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
fun FolderChipButton(
    onClick: () -> Unit
) {
    val chipMinHeight = rememberChipMinHeight()

    Box(
        modifier = Modifier
            .clip(Capsule())
            .background(AppColors.cardBackground)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = DimIndication()
            ) { onClick() }
            .height(chipMinHeight)
            .width(64.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_folder),
            contentDescription = null,
            tint = LocalGlasenseContentColor.current,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun rememberChipMinHeight(): Dp {
    val textStyle = LocalGlasenseTextStyle.current
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val lineHeightPx = textMeasurer.measure(
        text = AnnotatedString(ChipHeightSample),
        style = textStyle,
        maxLines = 1
    ).size.height

    return with(density) { lineHeightPx.toDp() } + ChipVerticalPadding * 2
}
