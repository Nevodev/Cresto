package com.nevoit.cresto.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.dp
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.LocalGlasenseSettings
import com.nevoit.cresto.ui.components.glasense.GlasenseSwitch
import com.nevoit.glasense.component.ListRowScope
import com.nevoit.glasense.component.ListScope
import com.nevoit.glasense.component.SectionScope
import com.nevoit.glasense.theme.GlasenseColors

fun ListScope.CustomSwitchRow(
    key: Any? = null,
    contentType: Any? = null,
    separator: Boolean = true,
    enabled: Boolean = true,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    backgroundColor: Color? = null,
    colors: GlasenseColors? = null,
    leading: (@Composable ListRowScope.() -> Unit)? = null,
    destructive: Boolean = false,
    trailing: (@Composable ListRowScope.() -> Unit)? = null,
    content: @Composable ListRowScope.() -> Unit
) {
    Row(
        key = key,
        contentType = contentType ?: customSwitchRowContentType(
            leading = leading,
            trailing = trailing
        ),
        separator = separator,
        enabled = enabled,
        onClick = { onCheckedChange(!checked) },
        leading = leading,
        trailing = {
            CustomSwitchRowTrailingLayout(
                trailing = trailing?.let { trailingContent ->
                    { trailingContent(this) }
                }
            ) {
                GlasenseSwitch(
                    backgroundColor = backgroundColor ?: AppColors.cardBackground,
                    enabled = enabled,
                    checked = checked,
                    colors = colors ?: AppColors,
                    onCheckedChange = onCheckedChange
                )
            }
        },
        destructive = destructive,
        content = content
    )
}

fun SectionScope.CustomSwitchRow(
    key: Any? = null,
    contentType: Any? = null,
    separator: Boolean = true,
    enabled: Boolean = true,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    backgroundColor: Color? = null,
    colors: GlasenseColors? = null,
    glass: Boolean = false,
    leading: (@Composable ListRowScope.() -> Unit)? = null,
    destructive: Boolean = false,
    trailing: (@Composable ListRowScope.() -> Unit)? = null,
    content: @Composable ListRowScope.() -> Unit
) {
    Row(
        key = key,
        contentType = contentType ?: customSwitchRowContentType(
            leading = leading,
            trailing = trailing
        ),
        separator = separator,
        enabled = enabled,
        onClick = { onCheckedChange(!checked) },
        leading = leading,
        trailing = {
            CustomSwitchRowTrailingLayout(
                trailing = trailing?.let { trailingContent ->
                    { trailingContent(this) }
                }
            ) {
                if (glass) {
                    CompositionLocalProvider(
                        LocalGlasenseSettings provides LocalGlasenseSettings.current.copy(
                            liquidGlass = true
                        )
                    ) {
                        GlasenseSwitch(
                            backgroundColor = backgroundColor ?: AppColors.cardBackground,
                            enabled = enabled,
                            checked = checked,
                            colors = colors ?: AppColors,
                            onCheckedChange = onCheckedChange
                        )
                    }
                } else {
                    GlasenseSwitch(
                        backgroundColor = backgroundColor ?: AppColors.cardBackground,
                        enabled = enabled,
                        checked = checked,
                        colors = colors ?: AppColors,
                        onCheckedChange = onCheckedChange
                    )
                }
            }
        },
        destructive = destructive,
        content = content
    )
}

@Composable
private fun CustomSwitchRowTrailingLayout(
    trailing: (@Composable () -> Unit)?,
    switch: @Composable () -> Unit
) {
    Layout(
        content = {
            if (trailing != null) {
                Box(contentAlignment = Alignment.CenterEnd) {
                    trailing()
                }
            }

            Box(contentAlignment = Alignment.CenterEnd) {
                switch()
            }
        }
    ) { measurables, constraints ->
        val switchPlaceable = measurables.last().measure(
            constraints.copy(
                minWidth = 0,
                minHeight = 0
            )
        )

        val spacing = if (trailing != null) {
            DefaultSwitchTrailingSpacing.roundToPx()
        } else {
            0
        }

        val trailingPlaceable = if (trailing != null) {
            measurables[0].measure(
                constraints.copy(
                    minWidth = 0,
                    minHeight = 0,
                    maxWidth = (constraints.maxWidth - switchPlaceable.width - spacing)
                        .coerceAtLeast(0)
                )
            )
        } else {
            null
        }

        val contentWidth = (trailingPlaceable?.width ?: 0) + spacing + switchPlaceable.width
        val width = contentWidth.coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = maxOf(
            constraints.minHeight,
            trailingPlaceable?.height ?: 0,
            switchPlaceable.height
        )

        layout(
            width = width,
            height = height
        ) {
            val contentStartX = width - contentWidth

            trailingPlaceable?.placeRelative(
                x = contentStartX,
                y = (height - trailingPlaceable.height) / 2
            )

            switchPlaceable.placeRelative(
                x = width - switchPlaceable.width,
                y = (height - switchPlaceable.height) / 2
            )
        }
    }
}

private fun customSwitchRowContentType(
    leading: Any?,
    trailing: Any?
): String {
    return when {
        leading != null && trailing != null -> "leading-trailing-custom-switch-row"
        leading != null -> "leading-custom-switch-row"
        trailing != null -> "trailing-custom-switch-row"
        else -> "custom-switch-row"
    }
}

private val DefaultSwitchTrailingSpacing = 8.dp
