package com.nevoit.cresto.ui.components.glasense

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
    title: String,
    modifier: Modifier = Modifier,
    leading: (@Composable GlasenseModalTopBarScope.() -> Unit)? = null,
    trailing: (@Composable GlasenseModalTopBarScope.() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        if (leading != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                ModalTopBarScope.leading()
            }
        }

        Text(
            text = title,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 64.dp),
            color = AppColors.content,
            style = GlasenseTheme.type.smallTitle,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (trailing != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                ModalTopBarScope.trailing()
            }
        }
    }
}