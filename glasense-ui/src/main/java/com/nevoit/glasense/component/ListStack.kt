@file:Suppress("FunctionName")

package com.nevoit.glasense.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.kyant.shapes.RoundedRectangle
import com.kyant.shapes.UnevenRoundedRectangle
import com.nevoit.glasense.R
import com.nevoit.glasense.core.component.Icon
import com.nevoit.glasense.core.component.Text
import com.nevoit.glasense.core.component.VDivider
import com.nevoit.glasense.core.interaction.DimIndication
import com.nevoit.glasense.core.interaction.rememberFlingBehavior
import com.nevoit.glasense.theme.GlasenseTheme
import com.nevoit.glasense.theme.LocalGlasenseContentColor
import com.nevoit.glasense.theme.tokens.Red500

@DslMarker
annotation class GlasenseListDsl

@GlasenseListDsl
class ListRowScope internal constructor(
    val interactionSource: MutableInteractionSource,
    val enabled: Boolean
)

sealed interface ListStyle {
    data object Plain : ListStyle
    data object InsetGrouped : ListStyle
}

@GlasenseListDsl
class ListScope internal constructor(
    private val lazyListScope: LazyListScope,
    private val style: ListStyle,
    private val colors: ListColors
) {
    internal val horizontalPadding: Dp
        get() = ListDefaults.horizontalPadding(style)

    private var sectionIndex = 0
    private var rowIndex = 0

    fun item(
        key: Any? = null,
        contentType: Any? = null,
        content: @Composable () -> Unit
    ) {
        lazyListScope.item(
            key = key,
            contentType = contentType
        ) {
            content()
        }
    }

    fun Section(
        header: String? = null,
        footer: String? = null,
        key: Any? = null,
        content: SectionScope.() -> Unit
    ) {
        val currentSectionIndex = sectionIndex
        sectionIndex += 1

        lazyListScope.renderSectionHeader(
            key = key,
            header = header,
            sectionIndex = currentSectionIndex,
            style = style,
            colors = colors
        )

        val scope = SectionScope(
            lazyListScope = lazyListScope,
            sectionIndex = currentSectionIndex,
            style = style,
            colors = colors
        )
        scope.content()
        scope.flushLastRow()

        lazyListScope.renderSectionFooter(
            key = key,
            footer = footer,
            sectionIndex = currentSectionIndex,
            style = style,
            colors = colors
        )
    }

    fun Row(
        key: Any? = null,
        contentType: Any? = null,
        separator: Boolean = true,
        enabled: Boolean = true,
        onClick: (() -> Unit)? = null,
        leading: (@Composable ListRowScope.() -> Unit)? = null,
        trailing: (@Composable ListRowScope.() -> Unit)? = null,
        chevron: Boolean = false,
        destructive: Boolean = false,
        content: @Composable ListRowScope.() -> Unit
    ) {
        val currentRowIndex = rowIndex
        rowIndex += 1

        lazyListScope.item(
            key = key,
            contentType = contentType ?: rowContentType(
                leading = leading,
                trailing = trailing,
                chevron = chevron
            )
        ) {
            ListRowFrame(
                separator = separator,
                enabled = enabled,
                onClick = onClick,
                leading = leading,
                trailing = trailing,
                chevron = chevron,
                destructive = destructive,
                content = content,
                rowIndex = currentRowIndex,
                isFirst = true,
                isLast = true,
                style = style,
                colors = colors
            )
        }
    }

    fun SwitchRow(
        key: Any? = null,
        contentType: Any? = null,
        separator: Boolean = true,
        enabled: Boolean = true,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        leading: (@Composable ListRowScope.() -> Unit)? = null,
        destructive: Boolean = false,
        content: @Composable ListRowScope.() -> Unit
    ) {
        Row(
            key = key,
            contentType = contentType
                ?: if (leading != null) "leading-switch-row" else "switch-row",
            separator = separator,
            enabled = enabled,
            onClick = { onCheckedChange(!checked) },
            leading = leading,
            trailing = {
                Switch(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    disabledAlpha = 1f
                )
            },
            destructive = destructive,
            content = content
        )
    }

    fun <T> ForEachRow(
        data: List<T>,
        id: ((T) -> Any)? = null,
        contentType: ((T) -> Any?)? = null,
        enabled: ((T) -> Boolean)? = null,
        onClick: ((T) -> Unit)? = null,
        content: @Composable ListRowScope.(T) -> Unit
    ) {
        data.forEachIndexed { index, item ->
            Row(
                key = id?.invoke(item) ?: index,
                contentType = contentType?.invoke(item),
                enabled = enabled?.invoke(item) ?: true,
                onClick = onClick?.let { { it(item) } }
            ) {
                content(item)
            }
        }
    }
}

@GlasenseListDsl
class SectionScope internal constructor(
    private val lazyListScope: LazyListScope,
    private val sectionIndex: Int,
    private val style: ListStyle,
    private val colors: ListColors
) {
    private var pendingKey: Any? = null
    private var pendingContentType: Any? = null
    private var pendingSeparator = true
    private var pendingEnabled = true
    private var pendingOnClick: (() -> Unit)? = null
    private var pendingContent: (@Composable ListRowScope.() -> Unit)? = null
    private var pendingLeading: (@Composable ListRowScope.() -> Unit)? = null
    private var pendingTrailing: (@Composable ListRowScope.() -> Unit)? = null
    private var pendingChevron = false
    private var pendingDestructive = false
    private var pendingRowIndex = 0
    private var rowIndex = 0

    fun Row(
        key: Any? = null,
        contentType: Any? = null,
        separator: Boolean = true,
        enabled: Boolean = true,
        onClick: (() -> Unit)? = null,
        leading: (@Composable ListRowScope.() -> Unit)? = null,
        trailing: (@Composable ListRowScope.() -> Unit)? = null,
        chevron: Boolean = false,
        destructive: Boolean = false,
        content: @Composable ListRowScope.() -> Unit
    ) {
        flushPendingRow(isLast = false)

        pendingKey = key
        pendingContentType = contentType
        pendingSeparator = separator
        pendingEnabled = enabled
        pendingOnClick = onClick
        pendingLeading = leading
        pendingTrailing = trailing
        pendingChevron = chevron
        pendingDestructive = destructive
        pendingContent = content
        pendingRowIndex = rowIndex
        rowIndex += 1
    }

    internal fun flushLastRow() {
        flushPendingRow(isLast = true)
        pendingKey = null
        pendingContentType = null
        pendingSeparator = true
        pendingEnabled = true
        pendingOnClick = null
        pendingContent = null
        pendingLeading = null
        pendingTrailing = null
        pendingChevron = false
        pendingDestructive = false
    }

    private fun flushPendingRow(isLast: Boolean) {
        pendingContent?.let { rowContent ->
            lazyListScope.renderSectionRow(
                key = pendingKey,
                contentType = pendingContentType,
                separator = pendingSeparator,
                enabled = pendingEnabled,
                onClick = pendingOnClick,
                leading = pendingLeading,
                trailing = pendingTrailing,
                chevron = pendingChevron,
                destructive = pendingDestructive,
                content = rowContent,
                sectionIndex = sectionIndex,
                rowIndex = pendingRowIndex,
                isFirst = pendingRowIndex == 0,
                isLast = isLast,
                style = style,
                colors = colors
            )
        }
    }

    fun SwitchRow(
        key: Any? = null,
        contentType: Any? = null,
        separator: Boolean = true,
        enabled: Boolean = true,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        leading: (@Composable ListRowScope.() -> Unit)? = null,
        destructive: Boolean = false,
        content: @Composable ListRowScope.() -> Unit
    ) {
        Row(
            key = key,
            contentType = contentType
                ?: if (leading != null) "leading-switch-row" else "switch-row",
            separator = separator,
            enabled = enabled,
            onClick = { onCheckedChange(!checked) },
            leading = leading,
            trailing = {
                Switch(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    disabledAlpha = 1f
                )
            },
            destructive = destructive,
            content = content
        )
    }

    fun <T> ForEachRow(
        data: List<T>,
        id: ((T) -> Any)? = null,
        contentType: ((T) -> Any?)? = null,
        enabled: ((T) -> Boolean)? = null,
        onClick: ((T) -> Unit)? = null,
        content: @Composable ListRowScope.(T) -> Unit
    ) {
        data.forEachIndexed { index, item ->
            Row(
                key = id?.invoke(item) ?: index,
                contentType = contentType?.invoke(item),
                enabled = enabled?.invoke(item) ?: true,
                onClick = onClick?.let { { it(item) } }
            ) {
                content(item)
            }
        }
    }
}

@Composable
fun ListStack(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    style: ListStyle = ListStyle.InsetGrouped,
    contentPadding: PaddingValues = PaddingValues(),
    content: ListScope.() -> Unit
) {
    val colors = ListDefaults.colors(style)

    LazyColumn(
        modifier = modifier.background(colors.background),
        state = state,
        contentPadding = contentPadding,
        flingBehavior = rememberFlingBehavior()
    ) {
        ListScope(
            lazyListScope = this,
            style = style,
            colors = colors
        ).content()
    }
}

private fun LazyListScope.renderSectionHeader(
    key: Any?,
    header: String?,
    sectionIndex: Int,
    style: ListStyle,
    colors: ListColors
) {
    if (sectionIndex == 0 && header == null) return

    item(
        key = key?.let { "$it-header" } ?: "section-$sectionIndex-header",
        contentType = "section-header"
    ) {
        Column {
            Spacer(
                Modifier.height(
                    if (sectionIndex == 0) 0.dp else ListDefaults.sectionSpacing(style)
                )
            )

            header?.let { header ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ListDefaults.horizontalPadding(style))
                ) {
                    Text(
                        text = header.uppercase(),
                        style = GlasenseTheme.type.subHeadline.copy(lineHeight = 14.sp),
                        color = colors.headerText,
                        modifier = Modifier
                            .padding(
                                start = 12.dp,
                                top = 0.dp,
                                end = 12.dp,
                                bottom = 8.dp
                            )
                    )
                }
            }
        }
    }
}

private fun LazyListScope.renderSectionRow(
    key: Any?,
    contentType: Any?,
    separator: Boolean,
    enabled: Boolean,
    onClick: (() -> Unit)?,
    leading: (@Composable ListRowScope.() -> Unit)? = null,
    trailing: (@Composable ListRowScope.() -> Unit)? = null,
    chevron: Boolean,
    destructive: Boolean,
    content: @Composable ListRowScope.() -> Unit,
    sectionIndex: Int,
    rowIndex: Int,
    isFirst: Boolean,
    isLast: Boolean,
    style: ListStyle,
    colors: ListColors
) {
    item(
        key = key ?: "section-$sectionIndex-row-$rowIndex",
        contentType = contentType ?: rowContentType(
            leading = leading,
            trailing = trailing,
            chevron = chevron
        )
    ) {
        ListRowFrame(
            separator = separator,
            enabled = enabled,
            onClick = onClick,
            leading = leading,
            trailing = trailing,
            chevron = chevron,
            destructive = destructive,
            content = content,
            rowIndex = rowIndex,
            isFirst = isFirst,
            isLast = isLast,
            style = style,
            colors = colors
        )
    }
}

private fun LazyListScope.renderSectionFooter(
    key: Any?,
    footer: String?,
    sectionIndex: Int,
    style: ListStyle,
    colors: ListColors
) {
    footer?.let { footer ->
        item(
            key = key?.let { "$it-footer" } ?: "section-$sectionIndex-footer",
            contentType = "section-footer"
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ListDefaults.horizontalPadding(style))
            ) {
                Text(
                    text = footer,
                    style = GlasenseTheme.type.subHeadline,
                    color = colors.footerText,
                    modifier = Modifier
                        .padding(
                            start = 12.dp,
                            top = 8.dp,
                            end = 12.dp,
                            bottom = 0.dp
                        )
                )
            }
        }
    }
}

@Composable
private fun ListRowFrame(
    separator: Boolean,
    enabled: Boolean,
    onClick: (() -> Unit)?,
    leading: (@Composable ListRowScope.() -> Unit)? = null,
    trailing: (@Composable ListRowScope.() -> Unit)? = null,
    chevron: Boolean = false,
    destructive: Boolean = false,
    content: @Composable ListRowScope.() -> Unit,
    rowIndex: Int,
    isFirst: Boolean,
    isLast: Boolean,
    style: ListStyle,
    colors: ListColors
) {
    val hasLeading = leading != null
    val hasTrailing = trailing != null
    val hasAccessory = hasTrailing || chevron
    val interactionSource = remember { MutableInteractionSource() }
    val rowScope = remember(interactionSource, enabled) {
        ListRowScope(
            interactionSource = interactionSource,
            enabled = enabled
        )
    }
    val contentColor = if (destructive) {
        Red500
    } else {
        LocalGlasenseContentColor.current
    }

    ListRowContainer(
        rowPadding = when {
            hasLeading -> RichRowPadding
            else -> DefaultRowPadding
        },
        separator = separator,
        separatorPaddingStart = if (hasLeading) {
            DefaultLeadingSize + DefaultLeadingSpacing - 4.dp
        } else {
            0.dp
        },
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        rowIndex = rowIndex,
        isFirst = isFirst,
        isLast = isLast,
        style = style,
        colors = colors
    ) {
        if (!hasLeading && !hasAccessory) {
            CompositionLocalProvider(
                LocalGlasenseContentColor provides contentColor
            ) {
                rowScope.content()
            }
        } else {
            ListRowLayout(
                rowScope = rowScope,
                leading = leading,
                trailing = trailing,
                chevron = chevron,
                destructive = destructive,
                contentColor = contentColor,
                content = content
            )
        }
    }
}

@Composable
private fun ListRowLayout(
    rowScope: ListRowScope,
    leading: (@Composable ListRowScope.() -> Unit)?,
    trailing: (@Composable ListRowScope.() -> Unit)?,
    chevron: Boolean,
    destructive: Boolean,
    contentColor: Color,
    content: @Composable ListRowScope.() -> Unit
) {
    val trailingContentColor = if (destructive) {
        Red500.copy(alpha = 0.6f)
    } else {
        GlasenseTheme.colors.contentVariant
    }

    Layout(
        content = {
            if (leading != null) {
                Box(
                    modifier = Modifier.size(DefaultLeadingSize),
                    contentAlignment = Alignment.Center
                ) {
                    CompositionLocalProvider(
                        LocalGlasenseContentColor provides contentColor
                    ) {
                        rowScope.leading()
                    }
                }
            }

            Box(
                contentAlignment = Alignment.CenterStart
            ) {
                CompositionLocalProvider(
                    LocalGlasenseContentColor provides contentColor
                ) {
                    rowScope.content()
                }
            }

            if (trailing != null) {
                Box(
                    contentAlignment = Alignment.CenterEnd
                ) {
                    CompositionLocalProvider(
                        LocalGlasenseContentColor provides trailingContentColor
                    ) {
                        rowScope.trailing()
                    }
                }
            }

            if (chevron) {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right_compact),
                    contentDescription = null,
                    tint = GlasenseTheme.colors.contentVariant.copy(alpha = .4f),
                    modifier = Modifier.size(
                        width = DefaultChevronWidth,
                        height = DefaultChevronHeight
                    )
                )
            }
        }
    ) { measurables, constraints ->

        val leadingPlaceable = if (leading != null) {
            measurables[0].measure(
                Constraints.fixed(
                    width = DefaultLeadingSize.roundToPx(),
                    height = DefaultLeadingSize.roundToPx()
                )
            )
        } else {
            null
        }

        val chevronPlaceable = if (chevron) {
            measurables.last().measure(
                constraints.copy(
                    minWidth = 0,
                    minHeight = 0
                )
            )
        } else {
            null
        }

        val trailingPlaceable = if (trailing != null) {
            measurables[measurables.lastIndex - if (chevron) 1 else 0].measure(
                constraints.copy(
                    minWidth = 0,
                    minHeight = 0
                )
            )
        } else {
            null
        }

        val leadingSpacing = if (leadingPlaceable != null) {
            DefaultLeadingSpacing.roundToPx()
        } else {
            0
        }

        val accessorySpacing = if (trailingPlaceable != null || chevronPlaceable != null) {
            DefaultTrailingSpacing.roundToPx()
        } else {
            0
        }

        val chevronSpacing = if (trailingPlaceable != null && chevronPlaceable != null) {
            DefaultChevronSpacing.roundToPx()
        } else {
            0
        }

        val occupiedWidth =
            (leadingPlaceable?.width ?: 0) +
                    leadingSpacing +
                    accessorySpacing +
                    (trailingPlaceable?.width ?: 0) +
                    chevronSpacing +
                    (chevronPlaceable?.width ?: 0)

        val contentIndex = if (leading != null) 1 else 0

        val contentPlaceable = measurables[contentIndex].measure(
            constraints.copy(
                minWidth = 0,
                minHeight = 0,
                maxWidth = (constraints.maxWidth - occupiedWidth)
                    .coerceAtLeast(0)
            )
        )

        val minContentHeight =
            DefaultRowMinHeight.roundToPx() -
                    RichRowPadding.calculateTopPadding().roundToPx() -
                    RichRowPadding.calculateBottomPadding().roundToPx()

        val height = maxOf(
            constraints.minHeight,
            minContentHeight,
            leadingPlaceable?.height ?: 0,
            contentPlaceable.height,
            trailingPlaceable?.height ?: 0
        )

        layout(
            width = constraints.maxWidth,
            height = height
        ) {
            var x = 0

            leadingPlaceable?.let {
                it.placeRelative(
                    x = x,
                    y = (height - it.height) / 2
                )
                x += it.width + leadingSpacing
            }

            contentPlaceable.placeRelative(
                x = x,
                y = (height - contentPlaceable.height) / 2
            )

            trailingPlaceable?.let {
                it.placeRelative(
                    x = constraints.maxWidth -
                            it.width -
                            chevronSpacing -
                            (chevronPlaceable?.width ?: 0),
                    y = (height - it.height) / 2
                )
            }

            chevronPlaceable?.let {
                it.placeRelative(
                    x = constraints.maxWidth - it.width,
                    y = (height - it.height) / 2
                )
            }
        }
    }
}

@Composable
private fun ListRowContainer(
    rowPadding: PaddingValues,
    separator: Boolean,
    separatorPaddingStart: Dp,
    onClick: (() -> Unit)?,
    enabled: Boolean,
    interactionSource: MutableInteractionSource,
    rowIndex: Int,
    isFirst: Boolean,
    isLast: Boolean,
    style: ListStyle,
    colors: ListColors,
    content: @Composable () -> Unit
) {
    val corner = ListDefaults.cornerRadius(style)
    val shape = when (style) {
        ListStyle.Plain -> RectangleShape

        ListStyle.InsetGrouped -> when {
            isFirst && isLast -> RoundedRectangle(corner)
            isFirst -> UnevenRoundedRectangle(
                topStart = corner,
                topEnd = corner
            )

            isLast -> UnevenRoundedRectangle(
                bottomStart = corner,
                bottomEnd = corner
            )

            else -> RectangleShape
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ListDefaults.horizontalPadding(style))
            .zIndex(-rowIndex.toFloat())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.rowBackground)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            enabled = enabled,
                            interactionSource = interactionSource,
                            indication = DimIndication(),
                            onClick = onClick
                        )
                    } else {
                        Modifier
                    }
                )
                .defaultMinSize(minHeight = DefaultRowMinHeight)
                .padding(rowPadding),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier.alpha(if (enabled) 1f else DisabledContentAlpha),
                contentAlignment = Alignment.CenterStart
            ) {
                content()
            }
        }

        if (separator && !isLast) {
            VDivider(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = DefaultSeparatorPadding)
                    .padding(start = separatorPaddingStart)
            )
        }
    }
}

@Immutable
data class ListColors(
    val background: Color,
    val rowBackground: Color,
    val headerText: Color,
    val footerText: Color
)

private val DefaultRowPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
private val DefaultRowMinHeight = 52.dp
private val DefaultSeparatorPadding = 16.dp
private val DefaultLeadingSize = 32.dp
private val DefaultLeadingSpacing = 12.dp
private val DefaultTrailingSpacing = 12.dp
private val DefaultChevronSpacing = 8.dp
private val DefaultChevronWidth = 14.dp
private val DefaultChevronHeight = 20.dp
private const val DisabledContentAlpha = 0.5f
private val RichRowPadding =
    PaddingValues(start = 12.dp, top = 8.dp, end = 16.dp, bottom = 8.dp)

private fun rowContentType(
    leading: Any?,
    trailing: Any?,
    chevron: Boolean
): String {
    return when {
        leading != null && trailing != null && chevron -> "leading-trailing-chevron-row"
        leading != null && trailing != null -> "leading-trailing-row"
        leading != null && chevron -> "leading-chevron-row"
        trailing != null && chevron -> "trailing-chevron-row"
        leading != null -> "leading-row"
        trailing != null -> "trailing-row"
        chevron -> "chevron-row"
        else -> "row"
    }
}

object ListDefaults {
    @Composable
    fun colors(style: ListStyle): ListColors {
        return ListColors(
            background = when (style) {
                ListStyle.Plain -> GlasenseTheme.colors.background
                ListStyle.InsetGrouped -> GlasenseTheme.colors.pageBackground
            },
            rowBackground = when (style) {
                ListStyle.Plain -> Color.Transparent
                ListStyle.InsetGrouped -> GlasenseTheme.colors.cardBackground
            },
            headerText = GlasenseTheme.colors.contentVariant,
            footerText = GlasenseTheme.colors.contentVariant.copy(alpha = .3f)
        )
    }

    fun horizontalPadding(style: ListStyle): Dp {
        return when (style) {
            ListStyle.Plain -> 0.dp
            ListStyle.InsetGrouped -> 12.dp
        }
    }

    fun sectionSpacing(style: ListStyle): Dp {
        return when (style) {
            ListStyle.Plain -> 0.dp
            ListStyle.InsetGrouped -> 24.dp
        }
    }

    fun cornerRadius(style: ListStyle): Dp {
        return when (style) {
            ListStyle.Plain -> 0.dp
            ListStyle.InsetGrouped -> 12.dp
        }
    }

}
