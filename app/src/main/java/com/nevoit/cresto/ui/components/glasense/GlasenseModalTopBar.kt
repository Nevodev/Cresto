package com.nevoit.cresto.ui.components.glasense

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtLeast
import com.nevoit.cresto.theme.AppButtonColors
import com.nevoit.cresto.theme.AppColors
import com.nevoit.glasense.core.component.Icon
import com.nevoit.glasense.core.component.Text
import com.nevoit.glasense.theme.GlasenseTheme

interface GlasenseModalTopBarScope {
    @Composable
    fun Action(
        modifier: Modifier = Modifier,
        icon: Painter,
        iconSize: Dp = 28.dp,
        contentDescription: String?,
        onClick: () -> Unit,
        enabled: Boolean = true,
        shape: Shape = CircleShape,
        colors: GlasenseButtonColors = AppButtonColors.action(),
        highlight: Boolean = false
    )
}

private object ModalTopBarScope : GlasenseModalTopBarScope {
    @Composable
    override fun Action(
        modifier: Modifier,
        icon: Painter,
        iconSize: Dp,
        contentDescription: String?,
        onClick: () -> Unit,
        enabled: Boolean,
        shape: Shape,
        colors: GlasenseButtonColors,
        highlight: Boolean
    ) {
        GlasenseButton(
            enabled = enabled,
            shape = shape,
            onClick = onClick,
            modifier = modifier.size(48.dp),
            colors = colors
        ) {
            if (highlight) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .glasenseHighlight(shape)
                )
            }

            Icon(
                painter = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
fun GlasenseModalTopBar(
    modifier: Modifier = Modifier,
    title: String? = null,
    leading: (@Composable GlasenseModalTopBarScope.() -> Unit)? = null,
    trailing: (@Composable GlasenseModalTopBarScope.() -> Unit)? = null
) {
    Layout(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        content = {
            if (leading != null) {
                Box(modifier = Modifier.layoutId("leading")) { ModalTopBarScope.leading() }
            }
            if (trailing != null) {
                Box(modifier = Modifier.layoutId("trailing")) { ModalTopBarScope.trailing() }
            }

            if (title != null) {
                Text(
                    text = title,
                    modifier = Modifier.layoutId("title"),
                    color = AppColors.content,
                    style = GlasenseTheme.type.smallTitle,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    ) { measurables, constraints ->
        val sideConstraints = constraints.copy(
            minWidth = 0,
            minHeight = 0,
            maxWidth = constraints.maxWidth / 2
        )

        val leadingPlaceable =
            measurables.find { it.layoutId == "leading" }?.measure(sideConstraints)
        val trailingPlaceable =
            measurables.find { it.layoutId == "trailing" }?.measure(sideConstraints)

        val leadingWidth = leadingPlaceable?.width ?: 0
        val trailingWidth = trailingPlaceable?.width ?: 0

        val safePaddingPx = 12.dp.roundToPx()
        val maxSideWidth = maxOf(leadingWidth, trailingWidth)
        val sidePadding = if (maxSideWidth > 0) maxSideWidth + safePaddingPx else 24.dp.roundToPx()

        val titleMaxWidth = (constraints.maxWidth - sidePadding * 2).fastCoerceAtLeast(0)

        val titlePlaceable = measurables.find { it.layoutId == "title" }?.measure(
            constraints.copy(
                minWidth = 0,
                maxWidth = titleMaxWidth,
                minHeight = 0
            )
        )

        layout(constraints.maxWidth, constraints.maxHeight) {
            fun centerVertically(childHeight: Int) = (constraints.maxHeight - childHeight) / 2

            leadingPlaceable?.placeRelative(
                x = 0,
                y = centerVertically(leadingPlaceable.height)
            )

            trailingPlaceable?.placeRelative(
                x = constraints.maxWidth - trailingWidth,
                y = centerVertically(trailingPlaceable.height)
            )

            titlePlaceable?.placeRelative(
                x = (constraints.maxWidth - titlePlaceable.width) / 2,
                y = centerVertically(titlePlaceable.height)
            )
        }
    }
}