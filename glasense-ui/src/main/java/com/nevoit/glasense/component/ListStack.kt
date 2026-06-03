@file:Suppress("FunctionName")

package com.nevoit.glasense.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.kyant.shapes.RoundedRectangle
import com.kyant.shapes.UnevenRoundedRectangle
import com.nevoit.glasense.core.component.Text
import com.nevoit.glasense.core.component.VDivider
import com.nevoit.glasense.core.interaction.rememberFlingBehavior
import com.nevoit.glasense.core.modifier.clickable
import com.nevoit.glasense.theme.GlasenseTheme

@DslMarker
annotation class GlasenseListDsl

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
        onClick: (() -> Unit)? = null,
        content: @Composable () -> Unit
    ) {
        val currentRowIndex = rowIndex
        rowIndex += 1

        lazyListScope.item(
            key = key,
            contentType = contentType
        ) {
            ListRowFrame(
                separator = separator,
                onClick = onClick,
                content = content,
                rowIndex = currentRowIndex,
                isFirst = true,
                isLast = true,
                style = style,
                colors = colors
            )
        }
    }

    fun LeadingRow(
        key: Any? = null,
        contentType: Any? = null,
        separator: Boolean = true,
        onClick: (() -> Unit)? = null,
        leading: @Composable () -> Unit,
        content: @Composable () -> Unit
    ) {
        val currentRowIndex = rowIndex
        rowIndex += 1

        lazyListScope.item(
            key = key,
            contentType = contentType
        ) {
            ListLeadingRowFrame(
                separator = separator,
                onClick = onClick,
                leading = leading,
                content = content,
                rowIndex = currentRowIndex,
                isFirst = true,
                isLast = true,
                style = style,
                colors = colors
            )
        }
    }

    fun <T> ForEachRow(
        data: List<T>,
        id: ((T) -> Any)? = null,
        contentType: ((T) -> Any?)? = null,
        onClick: ((T) -> Unit)? = null,
        content: @Composable (T) -> Unit
    ) {
        data.forEachIndexed { index, item ->
            Row(
                key = id?.invoke(item) ?: index,
                contentType = contentType?.invoke(item),
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
    private var pendingOnClick: (() -> Unit)? = null
    private var pendingContent: (@Composable () -> Unit)? = null
    private var pendingLeading: (@Composable () -> Unit)? = null
    private var pendingRowIndex = 0
    private var rowIndex = 0

    fun Row(
        key: Any? = null,
        contentType: Any? = null,
        separator: Boolean = true,
        onClick: (() -> Unit)? = null,
        content: @Composable () -> Unit
    ) {
        flushPendingRow(isLast = false)

        pendingKey = key
        pendingContentType = contentType
        pendingSeparator = separator
        pendingOnClick = onClick
        pendingContent = content
        pendingLeading = null
        pendingRowIndex = rowIndex
        rowIndex += 1
    }

    fun LeadingRow(
        key: Any? = null,
        contentType: Any? = null,
        separator: Boolean = true,
        onClick: (() -> Unit)? = null,
        leading: @Composable () -> Unit,
        content: @Composable () -> Unit
    ) {
        flushPendingRow(isLast = false)

        pendingKey = key
        pendingContentType = contentType
        pendingSeparator = separator
        pendingOnClick = onClick
        pendingContent = content
        pendingLeading = leading
        pendingRowIndex = rowIndex
        rowIndex += 1
    }

    internal fun flushLastRow() {
        flushPendingRow(isLast = true)
        pendingKey = null
        pendingContentType = null
        pendingSeparator = true
        pendingOnClick = null
        pendingContent = null
        pendingLeading = null
    }

    private fun flushPendingRow(isLast: Boolean) {
        pendingContent?.let { rowContent ->
            val rowLeading = pendingLeading
            if (rowLeading != null) {
                lazyListScope.renderSectionLeadingRow(
                    key = pendingKey,
                    contentType = pendingContentType,
                    separator = pendingSeparator,
                    onClick = pendingOnClick,
                    leading = rowLeading,
                    content = rowContent,
                    sectionIndex = sectionIndex,
                    rowIndex = pendingRowIndex,
                    isFirst = pendingRowIndex == 0,
                    isLast = isLast,
                    style = style,
                    colors = colors
                )
            } else {
                lazyListScope.renderSectionRow(
                    key = pendingKey,
                    contentType = pendingContentType,
                    separator = pendingSeparator,
                    onClick = pendingOnClick,
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
    }

    fun <T> ForEachRow(
        data: List<T>,
        id: ((T) -> Any)? = null,
        contentType: ((T) -> Any?)? = null,
        onClick: ((T) -> Unit)? = null,
        content: @Composable (T) -> Unit
    ) {
        data.forEachIndexed { index, item ->
            Row(
                key = id?.invoke(item) ?: index,
                contentType = contentType?.invoke(item),
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
    onClick: (() -> Unit)?,
    content: @Composable () -> Unit,
    sectionIndex: Int,
    rowIndex: Int,
    isFirst: Boolean,
    isLast: Boolean,
    style: ListStyle,
    colors: ListColors
) {
    item(
        key = key ?: "section-$sectionIndex-row-$rowIndex",
        contentType = contentType ?: "row"
    ) {
        ListRowFrame(
            separator = separator,
            onClick = onClick,
            content = content,
            rowIndex = rowIndex,
            isFirst = isFirst,
            isLast = isLast,
            style = style,
            colors = colors
        )
    }
}

private fun LazyListScope.renderSectionLeadingRow(
    key: Any?,
    contentType: Any?,
    separator: Boolean,
    onClick: (() -> Unit)?,
    leading: @Composable () -> Unit,
    content: @Composable () -> Unit,
    sectionIndex: Int,
    rowIndex: Int,
    isFirst: Boolean,
    isLast: Boolean,
    style: ListStyle,
    colors: ListColors
) {
    item(
        key = key ?: "section-$sectionIndex-row-$rowIndex",
        contentType = contentType ?: "leading-row"
    ) {
        ListLeadingRowFrame(
            separator = separator,
            onClick = onClick,
            leading = leading,
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
    onClick: (() -> Unit)?,
    content: @Composable () -> Unit,
    rowIndex: Int,
    isFirst: Boolean,
    isLast: Boolean,
    style: ListStyle,
    colors: ListColors
) {
    ListRowContainer(
        rowPadding = DefaultRowPadding,
        separator = separator,
        separatorPaddingStart = 0.dp,
        onClick = onClick,
        rowIndex = rowIndex,
        isFirst = isFirst,
        isLast = isLast,
        style = style,
        colors = colors
    ) {
        content()
    }
}

@Composable
private fun ListLeadingRowFrame(
    separator: Boolean,
    onClick: (() -> Unit)?,
    leading: @Composable () -> Unit,
    content: @Composable () -> Unit,
    rowIndex: Int,
    isFirst: Boolean,
    isLast: Boolean,
    style: ListStyle,
    colors: ListColors
) {
    ListRowContainer(
        rowPadding = LeadingRowPadding,
        separator = separator,
        separatorPaddingStart = DefaultLeadingSize + DefaultLeadingSpacing - 4.dp,
        onClick = onClick,
        rowIndex = rowIndex,
        isFirst = isFirst,
        isLast = isLast,
        style = style,
        colors = colors
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(DefaultLeadingSize),
                contentAlignment = Alignment.Center
            ) {
                leading()
            }
            Spacer(Modifier.width(DefaultLeadingSpacing))
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                content()
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
                        Modifier.clickable(onClick = onClick)
                    } else {
                        Modifier
                    }
                )
                .defaultMinSize(minHeight = DefaultRowMinHeight)
                .padding(rowPadding),
            contentAlignment = Alignment.CenterStart
        ) {
            content()
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
private val LeadingRowPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 16.dp, bottom = 8.dp)
private val DefaultRowMinHeight = 52.dp
private val DefaultSeparatorPadding = 16.dp
private val DefaultLeadingSize = 32.dp
private val DefaultLeadingSpacing = 12.dp

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
