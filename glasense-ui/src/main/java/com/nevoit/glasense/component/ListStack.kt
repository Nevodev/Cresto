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
import androidx.compose.foundation.lazy.LazyItemScope
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceAtLeast
import com.kyant.shapes.RoundedRectangle
import com.kyant.shapes.UnevenRoundedRectangle
import com.nevoit.glasense.R
import com.nevoit.glasense.core.component.Icon
import com.nevoit.glasense.core.component.Text
import com.nevoit.glasense.core.interaction.DimIndication
import com.nevoit.glasense.core.interaction.rememberFlingBehavior
import com.nevoit.glasense.theme.GlasenseTheme
import com.nevoit.glasense.theme.LocalGlasenseColors
import com.nevoit.glasense.theme.LocalGlasenseContentColor
import com.nevoit.glasense.theme.tokens.Red500

@DslMarker
annotation class GlasenseListDsl

enum class ListRowPosition {
    Single,
    First,
    Middle,
    Last
}

@GlasenseListDsl
class ListRowScope internal constructor(
    val interactionSource: MutableInteractionSource,
    val enabled: Boolean,
    val indexInSection: Int,
    val position: ListRowPosition,
    private val lazyItemScope: LazyItemScope
) : LazyItemScope by lazyItemScope

sealed interface ListStyle {
    data object Plain : ListStyle
    data object InsetGrouped : ListStyle
}

enum class ListRowAccessory {
    None,
    Chevron,
    SelectIndicator
}

@GlasenseListDsl
class ListScope internal constructor(
    private val lazyListScope: LazyListScope,
    private val style: ListStyle,
    private val colors: ListColors,
    private val cornerRadius: Dp
) {
    val horizontalPadding: Dp
        get() = ListDefaults.horizontalPadding(style)

    private var sectionIndex = 0

    fun item(
        key: Any? = null,
        contentType: Any? = null,
        content: @Composable LazyItemScope.() -> Unit
    ) {
        lazyListScope.item(
            key = key,
            contentType = contentType
        ) {
            content()
        }
    }

    fun Section(
        header: (@Composable () -> String?)? = null,
        footer: (@Composable () -> String?)? = null,
        key: Any? = null,
        topSpacing: Dp? = null,
        content: SectionScope.() -> Unit
    ) {
        renderSection(
            header = header,
            footer = footer,
            key = key,
            topSpacing = topSpacing,
            rowPadding = null,
            separatorHorizontalPadding = DefaultSeparatorPadding,
            separatorPaddingStart = null,
            content = content
        )
    }

    fun NoPaddingSection(
        header: (@Composable () -> String?)? = null,
        footer: (@Composable () -> String?)? = null,
        key: Any? = null,
        topSpacing: Dp? = null,
        content: SectionScope.() -> Unit
    ) {
        renderSection(
            header = header,
            footer = footer,
            key = key,
            topSpacing = topSpacing,
            rowPadding = NoRowPadding,
            separatorHorizontalPadding = 0.dp,
            separatorPaddingStart = 0.dp,
            content = content
        )
    }

    private fun renderSection(
        header: (@Composable () -> String?)?,
        footer: (@Composable () -> String?)?,
        key: Any?,
        topSpacing: Dp?,
        rowPadding: PaddingValues?,
        separatorHorizontalPadding: Dp,
        separatorPaddingStart: Dp?,
        content: SectionScope.() -> Unit
    ) {
        val currentSectionIndex = sectionIndex
        sectionIndex += 1

        lazyListScope.renderSectionHeader(
            key = key,
            header = header,
            sectionIndex = currentSectionIndex,
            topSpacing = topSpacing,
            style = style,
            colors = colors
        )

        val scope = SectionScope(
            lazyListScope = lazyListScope,
            sectionIndex = currentSectionIndex,
            style = style,
            colors = colors,
            cornerRadius = cornerRadius,
            rowPadding = rowPadding,
            separatorHorizontalPadding = separatorHorizontalPadding,
            separatorPaddingStart = separatorPaddingStart
        )
        scope.content()

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
        accessory: ListRowAccessory = ListRowAccessory.None,
        destructive: Boolean = false,
        content: @Composable ListRowScope.() -> Unit
    ) {
        lazyListScope.item(
            key = key,
            contentType = contentType ?: rowContentType(
                leading = leading,
                trailing = trailing,
                accessory = accessory
            )
        ) {
            ListRowFrame(
                separator = separator,
                enabled = enabled,
                onClick = onClick,
                leading = leading,
                trailing = trailing,
                accessory = accessory,
                destructive = destructive,
                content = content,
                lazyItemScope = this,
                indexInSection = 0,
                position = ListRowPosition.Single,
                style = style,
                colors = colors,
                cornerRadius = cornerRadius
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
        trailing: (@Composable ListRowScope.() -> Unit)? = null,
        content: @Composable ListRowScope.() -> Unit
    ) {
        Row(
            key = key,
            contentType = contentType ?: switchRowContentType(
                leading = leading,
                trailing = trailing
            ),
            separator = separator,
            enabled = enabled,
            onClick = { onCheckedChange(!checked) },
            leading = leading,
            trailing = {
                SwitchRowTrailingLayout(
                    rowScope = this,
                    trailing = trailing,
                    switch = {
                        Switch(
                            enabled = enabled,
                            interactionSource = remember { MutableInteractionSource() },
                            checked = checked,
                            onCheckedChange = onCheckedChange,
                            disabledAlpha = 1f
                        )
                    }
                )
            },
            destructive = destructive,
            content = content
        )
    }
}

@GlasenseListDsl
class SectionScope internal constructor(
    private val lazyListScope: LazyListScope,
    private val sectionIndex: Int,
    private val style: ListStyle,
    private val colors: ListColors,
    private val cornerRadius: Dp,
    private val rowPadding: PaddingValues?,
    private val separatorHorizontalPadding: Dp,
    private val separatorPaddingStart: Dp?
) {
    private val rows = SectionRows()

    fun Row(
        key: Any? = null,
        contentType: Any? = null,
        separator: Boolean = true,
        enabled: Boolean = true,
        onClick: (() -> Unit)? = null,
        leading: (@Composable ListRowScope.() -> Unit)? = null,
        trailing: (@Composable ListRowScope.() -> Unit)? = null,
        accessory: ListRowAccessory = ListRowAccessory.None,
        destructive: Boolean = false,
        content: @Composable ListRowScope.() -> Unit
    ) {
        val currentRowIndex = rows.count
        rows.count += 1

        lazyListScope.renderSectionRow(
            key = key,
            contentType = contentType,
            separator = separator,
            enabled = enabled,
            onClick = onClick,
            leading = leading,
            trailing = trailing,
            accessory = accessory,
            destructive = destructive,
            content = content,
            rows = rows,
            sectionIndex = sectionIndex,
            rowIndex = currentRowIndex,
            style = style,
            colors = colors,
            cornerRadius = cornerRadius,
            rowPadding = rowPadding,
            separatorHorizontalPadding = separatorHorizontalPadding,
            separatorPaddingStart = separatorPaddingStart
        )
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
        trailing: (@Composable ListRowScope.() -> Unit)? = null,
        content: @Composable ListRowScope.() -> Unit
    ) {
        Row(
            key = key,
            contentType = contentType ?: switchRowContentType(
                leading = leading,
                trailing = trailing
            ),
            separator = separator,
            enabled = enabled,
            onClick = { onCheckedChange(!checked) },
            leading = leading,
            trailing = {
                SwitchRowTrailingLayout(
                    rowScope = this,
                    trailing = trailing,
                    switch = {
                        Switch(
                            enabled = enabled,
                            interactionSource = remember { MutableInteractionSource() }, // don't use rowscope.interactionsource
                            checked = checked,
                            onCheckedChange = onCheckedChange,
                            disabledAlpha = 1f
                        )
                    }
                )
            },
            destructive = destructive,
            content = content
        )
    }
}

@Composable
fun ListStack(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    style: ListStyle = ListStyle.InsetGrouped,
    colors: ListColors? = null,
    contentPadding: PaddingValues = PaddingValues(),
    cornerRadius: Dp = ListDefaults.cornerRadius(style),
    content: ListScope.() -> Unit
) {
    val resolvedColors = colors ?: ListDefaults.colors(style)

    LazyColumn(
        modifier = modifier.background(resolvedColors.background),
        state = state,
        contentPadding = contentPadding,
        flingBehavior = rememberFlingBehavior()
    ) {
        ListScope(
            lazyListScope = this,
            style = style,
            colors = resolvedColors,
            cornerRadius = cornerRadius
        ).content()
        paddingItem(state)
    }
}

private fun LazyListScope.renderSectionHeader(
    key: Any?,
    header: (@Composable () -> String?)?,
    sectionIndex: Int,
    topSpacing: Dp?,
    style: ListStyle,
    colors: ListColors
) {
    val resolvedTopSpacing = topSpacing ?: if (sectionIndex == 0) 0.dp else DefaultSectionSpacing
    if (resolvedTopSpacing == 0.dp && header == null) return

    item(
        key = key?.let { "$it-header" } ?: "section-$sectionIndex-header",
        contentType = "section-header"
    ) {
        Column {
            Spacer(Modifier.height(resolvedTopSpacing))

            header?.invoke()?.let { header ->
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
    accessory: ListRowAccessory,
    destructive: Boolean,
    content: @Composable ListRowScope.() -> Unit,
    rows: SectionRows,
    sectionIndex: Int,
    rowIndex: Int,
    style: ListStyle,
    colors: ListColors,
    cornerRadius: Dp,
    rowPadding: PaddingValues? = null,
    separatorHorizontalPadding: Dp = DefaultSeparatorPadding,
    separatorPaddingStart: Dp? = null
) {
    item(
        key = key ?: "section-$sectionIndex-row-$rowIndex",
        contentType = contentType ?: rowContentType(
            leading = leading,
            trailing = trailing,
            accessory = accessory
        )
    ) {
        val position = sectionRowPosition(
            rowCount = rows.count,
            indexInSection = rowIndex
        )

        ListRowFrame(
            separator = separator,
            enabled = enabled,
            onClick = onClick,
            leading = leading,
            trailing = trailing,
            accessory = accessory,
            destructive = destructive,
            content = content,
            lazyItemScope = this,
            indexInSection = rowIndex,
            position = position,
            style = style,
            colors = colors,
            cornerRadius = cornerRadius,
            rowPadding = rowPadding,
            separatorHorizontalPadding = separatorHorizontalPadding,
            separatorPaddingStart = separatorPaddingStart
        )
    }
}

private fun LazyListScope.renderSectionFooter(
    key: Any?,
    footer: (@Composable () -> String?)?,
    sectionIndex: Int,
    style: ListStyle,
    colors: ListColors
) {
    if (footer != null) {
        item(
            key = key?.let { "$it-footer" } ?: "section-$sectionIndex-footer",
            contentType = "section-footer"
        ) {
            footer()?.let { footerText ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ListDefaults.horizontalPadding(style))
                ) {
                    Text(
                        text = footerText,
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
}

@Composable
private fun ListRowFrame(
    separator: Boolean,
    enabled: Boolean,
    onClick: (() -> Unit)?,
    leading: (@Composable ListRowScope.() -> Unit)? = null,
    trailing: (@Composable ListRowScope.() -> Unit)? = null,
    accessory: ListRowAccessory = ListRowAccessory.None,
    destructive: Boolean = false,
    content: @Composable ListRowScope.() -> Unit,
    lazyItemScope: LazyItemScope,
    indexInSection: Int,
    position: ListRowPosition,
    style: ListStyle,
    colors: ListColors,
    cornerRadius: Dp,
    rowPadding: PaddingValues? = null,
    separatorHorizontalPadding: Dp = DefaultSeparatorPadding,
    separatorPaddingStart: Dp? = null
) {
    val hasLeading = leading != null
    val hasTrailing = trailing != null
    val hasAccessory = hasTrailing || accessory != ListRowAccessory.None
    val interactionSource = remember { MutableInteractionSource() }
    val rowScope = remember(
        interactionSource,
        enabled,
        lazyItemScope,
        indexInSection,
        position
    ) {
        ListRowScope(
            interactionSource = interactionSource,
            enabled = enabled,
            indexInSection = indexInSection,
            position = position,
            lazyItemScope = lazyItemScope
        )
    }
    val contentColor = if (destructive) {
        Red500
    } else {
        LocalGlasenseContentColor.current
    }

    ListRowContainer(
        rowPadding = rowPadding ?: when {
            hasLeading -> RichRowPadding
            else -> DefaultRowPadding
        },
        separator = separator,
        separatorHorizontalPadding = separatorHorizontalPadding,
        separatorPaddingStart = separatorPaddingStart ?: if (hasLeading) {
            DefaultLeadingSize + DefaultLeadingSpacing - 4.dp
        } else {
            0.dp
        },
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        isFirst = position == ListRowPosition.Single || position == ListRowPosition.First,
        isLast = position == ListRowPosition.Single || position == ListRowPosition.Last,
        style = style,
        colors = colors,
        cornerRadius = cornerRadius
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
                accessory = accessory,
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
    accessory: ListRowAccessory,
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

            when (accessory) {
                ListRowAccessory.Chevron -> {
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_right_compact),
                        contentDescription = null,
                        tint = GlasenseTheme.colors.contentVariant.copy(alpha = .2f),
                        modifier = Modifier.size(
                            width = DefaultAccessoryWidth,
                            height = DefaultAccessoryHeight
                        )
                    )
                }

                ListRowAccessory.SelectIndicator -> {
                    Icon(
                        painter = painterResource(R.drawable.ic_select_indicator),
                        contentDescription = null,
                        tint = GlasenseTheme.colors.contentVariant.copy(alpha = .2f),
                        modifier = Modifier.size(
                            width = DefaultAccessoryWidth,
                            height = DefaultAccessoryHeight
                        )
                    )
                }

                ListRowAccessory.None -> Unit
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

        val accessoryPlaceable = if (accessory != ListRowAccessory.None) {
            measurables.last().measure(
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

        val trailingSpacing = if (trailing != null || accessoryPlaceable != null) {
            DefaultTrailingSpacing.roundToPx()
        } else {
            0
        }

        val accessorySpacing = if (trailing != null && accessoryPlaceable != null) {
            DefaultAccessorySpacing.roundToPx()
        } else {
            0
        }

        val contentIndex = if (leading != null) 1 else 0
        val dynamicWidth = (
                constraints.maxWidth -
                        (leadingPlaceable?.width ?: 0) -
                        leadingSpacing -
                        trailingSpacing -
                        accessorySpacing -
                        (accessoryPlaceable?.width ?: 0)
                ).coerceAtLeast(0)
        val trailingPlaceable = if (trailing != null) {
            measurables[
                measurables.lastIndex - if (accessory != ListRowAccessory.None) 1 else 0
            ].measure(
                constraints.copy(
                    minWidth = 0,
                    minHeight = 0,
                    maxWidth = dynamicWidth / 2
                )
            )
        } else {
            null
        }

        val contentPlaceable = measurables[contentIndex].measure(
            constraints.copy(
                minWidth = 0,
                minHeight = 0,
                maxWidth = dynamicWidth - (trailingPlaceable?.width ?: 0)
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
            trailingPlaceable?.height ?: 0,
            accessoryPlaceable?.height ?: 0
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
                            accessorySpacing -
                            (accessoryPlaceable?.width ?: 0),
                    y = (height - it.height) / 2
                )
            }

            accessoryPlaceable?.let {
                it.placeRelative(
                    x = constraints.maxWidth - it.width,
                    y = (height - it.height) / 2
                )
            }
        }
    }
}

@Composable
private fun SwitchRowTrailingLayout(
    rowScope: ListRowScope,
    trailing: (@Composable ListRowScope.() -> Unit)?,
    switch: @Composable () -> Unit
) {
    Layout(
        content = {
            if (trailing != null) {
                Box(contentAlignment = Alignment.CenterEnd) {
                    rowScope.trailing()
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

@Composable
private fun ListRowContainer(
    rowPadding: PaddingValues,
    separator: Boolean,
    separatorHorizontalPadding: Dp,
    separatorPaddingStart: Dp,
    onClick: (() -> Unit)?,
    enabled: Boolean,
    interactionSource: MutableInteractionSource,
    isFirst: Boolean,
    isLast: Boolean,
    style: ListStyle,
    colors: ListColors,
    cornerRadius: Dp,
    content: @Composable () -> Unit
) {
    val separatorColor =
        if (style == ListStyle.InsetGrouped) LocalGlasenseContentColor.current.copy(.1f) else LocalGlasenseColors.current.scrimMedium
    val shape = when (style) {
        ListStyle.Plain -> RectangleShape

        ListStyle.InsetGrouped -> when {
            isFirst && isLast -> RoundedRectangle(cornerRadius)
            isFirst -> UnevenRoundedRectangle(
                topStart = cornerRadius,
                topEnd = cornerRadius
            )

            isLast -> UnevenRoundedRectangle(
                bottomStart = cornerRadius,
                bottomEnd = cornerRadius
            )

            else -> RectangleShape
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ListDefaults.horizontalPadding(style))
            .then(
                if (separator && !isLast) {
                    Modifier.drawWithCache {
                        val strokeWidth = DefaultSeparatorWidth.toPx()
                        val horizontalPadding = separatorHorizontalPadding.toPx()
                        val startX = horizontalPadding + separatorPaddingStart.toPx()
                        val endX = (size.width - horizontalPadding)
                            .coerceAtLeast(startX)
                        val y = size.height - strokeWidth / 2f

                        onDrawWithContent {
                            drawContent()
                            drawLine(
                                color = separatorColor,
                                start = Offset(x = startX, y = y),
                                end = Offset(x = endX, y = y),
                                strokeWidth = strokeWidth
                            )
                        }
                    }
                } else {
                    Modifier
                }
            )
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
                            indication = DefaultDimIndication,
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
    }
}

private class SectionRows {
    var count: Int = 0
}

private fun sectionRowPosition(
    rowCount: Int,
    indexInSection: Int
): ListRowPosition {
    return when {
        rowCount <= 1 -> ListRowPosition.Single
        indexInSection == 0 -> ListRowPosition.First
        indexInSection == rowCount - 1 -> ListRowPosition.Last
        else -> ListRowPosition.Middle
    }
}

@Immutable
data class ListColors(
    val background: Color,
    val rowBackground: Color,
    val headerText: Color,
    val footerText: Color
)

private val DefaultSectionSpacing = 24.dp
private val DefaultRowPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
private val DefaultRowMinHeight = 52.dp
private val DefaultSeparatorPadding = 16.dp
private val DefaultSeparatorWidth = 1.dp
private val DefaultLeadingSize = 32.dp
private val DefaultLeadingSpacing = 12.dp
private val DefaultTrailingSpacing = 12.dp
private val DefaultSwitchTrailingSpacing = 8.dp
private val DefaultAccessorySpacing = 8.dp
private val DefaultAccessoryWidth = 14.dp
private val DefaultAccessoryHeight = 20.dp
private const val DisabledContentAlpha = 0.5f
private val RichRowPadding =
    PaddingValues(start = 12.dp, top = 8.dp, end = 16.dp, bottom = 8.dp)
private val NoRowPadding = PaddingValues(0.dp)
private val DefaultDimIndication = DimIndication()

private fun rowContentType(
    leading: Any?,
    trailing: Any?,
    accessory: ListRowAccessory
): String {
    val hasAccessory = accessory != ListRowAccessory.None

    return when {
        leading != null && trailing != null && hasAccessory -> "leading-trailing-accessory-row"
        leading != null && trailing != null -> "leading-trailing-row"
        leading != null && hasAccessory -> "leading-accessory-row"
        trailing != null && hasAccessory -> "trailing-accessory-row"
        leading != null -> "leading-row"
        trailing != null -> "trailing-row"
        hasAccessory -> "accessory-row"
        else -> "row"
    }
}

private fun switchRowContentType(
    leading: Any?,
    trailing: Any?
): String {
    return when {
        leading != null && trailing != null -> "leading-trailing-switch-row"
        leading != null -> "leading-switch-row"
        trailing != null -> "trailing-switch-row"
        else -> "switch-row"
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

    fun cornerRadius(style: ListStyle): Dp {
        return when (style) {
            ListStyle.Plain -> 0.dp
            ListStyle.InsetGrouped -> 12.dp
        }
    }

}

fun LazyListScope.paddingItem(state: LazyListState) {
    item(contentType = LazyListPaddingItem) {
        Box(
            Modifier.layout { _, constraints ->
                val layoutInfo = state.layoutInfo
                val viewportBottom = layoutInfo.viewportEndOffset - layoutInfo.afterContentPadding
                val lastContentItem =
                    layoutInfo.visibleItemsInfo.lastOrNull { it.contentType !== LazyListPaddingItem }
                val contentBottom = lastContentItem?.let { it.offset + it.size } ?: 0
                val height = (viewportBottom - contentBottom + 1).fastCoerceAtLeast(0)
                layout(constraints.maxWidth, height) {}
            }
        )
    }
}

private data object LazyListPaddingItem
