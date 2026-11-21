package com.nevoit.cresto.feature.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.kyant.shapes.RoundedRectangle
import com.nevoit.cresto.R
import com.nevoit.cresto.data.todo.TodoItemWithSubTodos
import com.nevoit.cresto.navigation.NavigationCaller
import com.nevoit.cresto.navigation.navigationCaller
import com.nevoit.cresto.navigation.rememberNavigationCaller
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.AppSpecs
import com.nevoit.cresto.ui.components.glasense.SwipeableListState
import com.nevoit.cresto.ui.components.packed.SwipeableTodoItem
import com.nevoit.glasense.core.component.Icon
import com.nevoit.glasense.core.component.Text
import com.nevoit.glasense.core.interaction.DimIndication
import com.nevoit.glasense.theme.GlasenseTheme
import com.nevoit.glasense.theme.tokens.Springs

@Composable
fun LazyItemScope.TodoListItemRow(
    modifier: Modifier = Modifier,
    item: TodoItemWithSubTodos,
    showDate: Boolean = true,
    isDueTodayMarkerEnabled: Boolean,
    isOverdueMarkerEnabled: Boolean,
    isSelected: Boolean,
    isSelectionModeActive: Boolean,
    overlayInteractionSource: MutableInteractionSource,
    swipeListState: SwipeableListState,
    onEnterSelection: () -> Unit,
    onToggleSelection: () -> Unit,
    onOpenDetail: (NavigationCaller) -> Unit,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onCheckboxTapPosition: ((Offset) -> Unit)? = null,
    onTogglePinned: (() -> Unit)? = null,
) {
    val alpha = remember { Animatable(if (isSelected) 1f else 0f) }
    val rowInteractionSource = remember { MutableInteractionSource() }
    val selectionOutline = AppColors.primary
    val cardCorner = AppSpecs.cardCorner
    val caller =
        rememberNavigationCaller(RoundedRectangle(cardCorner), AppColors.cardBackground, item.todoItem.id)

    LaunchedEffect(isSelected) {
        if (isSelected) {
            alpha.animateTo(1f, tween(100))
        } else {
            alpha.animateTo(0f, tween(100))
        }
    }

    Box(
        modifier = modifier
            .animateItem(placementSpec = Springs.crisp())
    ) {
        SwipeableTodoItem(
            item = item,
            showDate = showDate,
            isDueTodayMarkerEnabled = isDueTodayMarkerEnabled,
            isOverdueMarkerEnabled = isOverdueMarkerEnabled,
            onCheckboxTapPosition = onCheckboxTapPosition ?: {},
            onCheckedChange = onCheckedChange,
            onDelete = onDelete,
            cardModifier = Modifier
                .navigationCaller(caller, clipContent = false)
                .combinedClickable(
                    interactionSource = rowInteractionSource,
                    indication = DimIndication(shape = AppSpecs.cardShape),
                    onLongClick = {
                        if (!isSelectionModeActive) {
                            onEnterSelection()
                        } else {
                            onToggleSelection()
                        }
                    },
                    onClick = {
                        if (isSelectionModeActive) {
                            onToggleSelection()
                        } else {
                            onOpenDetail(caller)
                        }
                    }
                ),
            modifier = Modifier.drawBehind {
                if (alpha.value > 0) {
                    val outline = RoundedRectangle(cardCorner - 3.dp / 2).createOutline(
                        size = Size(size.width - 3.dp.toPx(), size.height - 3.dp.toPx()),
                        layoutDirection = LayoutDirection.Ltr,
                        density = this
                    )
                    translate(1.5.dp.toPx(), 1.5.dp.toPx()) {
                        drawOutline(
                            outline = outline,
                            color = selectionOutline,
                            alpha = alpha.value,
                            style = Stroke(width = 3.dp.toPx()),
                        )
                    }
                }
            },
            listState = swipeListState,
            onTogglePinned = onTogglePinned
        )

        if (isSelectionModeActive) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .combinedClickable(
                        interactionSource = overlayInteractionSource,
                        indication = null,
                        onClick = { onToggleSelection() }
                    )
            ) {}
        }
    }
}

@Composable
fun LazyItemScope.TodoListSectionHead(
    title: String,
    horizontalPadding: Boolean = true,
    isExpanded: Boolean = true,
    showExpandIcon: Boolean = true,
    onExpandedChange: (() -> Unit)? = null
) {
    val degree = remember { Animatable(if (isExpanded) 90f else 180f) }
    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            degree.animateTo(90f, tween(200))
        } else {
            degree.animateTo(180f, tween(200))
        }
    }
    val interactionSource = remember { MutableInteractionSource() }
    val expandableModifier = if (onExpandedChange != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onExpandedChange
        )
    } else {
        Modifier
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .padding(horizontal = if (horizontalPadding) 12.dp else 0.dp)
            .zIndex(-1f)
            .animateItem(placementSpec = Springs.crisp())
            .fillMaxWidth()
            .then(expandableModifier)
            .padding(top = 8.dp, bottom = 8.dp, start = 12.dp)
    ) {
        Text(
            text = title,
            style = GlasenseTheme.type.subHeadline.copy(lineHeight = 14.sp),
            fontWeight = FontWeight.Normal,
            color = AppColors.contentVariant
        )
        if (showExpandIcon) {
            Icon(
                painter = painterResource(R.drawable.ic_forward_nav),
                contentDescription = stringResource(R.string.expand),
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(20.dp)
                    .alpha(.5f)
                    .graphicsLayer {
                        rotationZ = degree.value
                    }
            )
        }
    }
}
