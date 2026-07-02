package com.nevoit.cresto.feature.group

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nevoit.cresto.R
import com.nevoit.cresto.data.todo.HomeGroupFilter
import com.nevoit.cresto.data.todo.TodoGroup
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.AppSpecs
import com.nevoit.cresto.ui.components.glasense.GlasenseModalTopBar
import com.nevoit.cresto.ui.components.glasense.GlasenseSwipeable
import com.nevoit.cresto.ui.components.glasense.SwipeableActionButton
import com.nevoit.cresto.ui.components.glasense.SwipeableListState
import com.nevoit.cresto.ui.components.glasense.rememberSwipeableListState
import com.nevoit.glasense.component.BottomSheet
import com.nevoit.glasense.component.ListColors
import com.nevoit.glasense.component.ListScope
import com.nevoit.glasense.component.ListStack
import com.nevoit.glasense.core.component.HGap
import com.nevoit.glasense.core.component.Icon
import com.nevoit.glasense.core.component.Text
import com.nevoit.glasense.core.component.VDivider
import com.nevoit.glasense.core.component.VGap
import com.nevoit.glasense.core.modifier.clickable
import com.nevoit.glasense.theme.GlasenseTheme
import com.nevoit.glasense.theme.tokens.Springs
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import androidx.compose.animation.Animatable as ColorAnimatable

@Composable
fun GroupBottomSheet(
    groups: List<TodoGroup>,
    groupTodoCounts: Map<Int?, Int>,
    selectedFilter: HomeGroupFilter,
    onFilterSelected: (HomeGroupFilter) -> Unit,
    onCreateGroup: (String) -> Unit,
    onDeleteGroup: (TodoGroup) -> Unit,
    onDismissed: () -> Unit,
    showAllFilter: Boolean = true,
    showRecentlyDeleted: Boolean = true
) {
    val listState = rememberLazyListState()
    val elevatedPageBackground = AppColors.elevatedPageBackground
    val elevatedCardBackground = AppColors.elevatedCardBackground
    val contentVariant = AppColors.contentVariant
    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val swipeableListState = rememberSwipeableListState()
    val actions = persistentListOf(
        SwipeableActionButton(
            index = 0,
            color = AppColors.error,
            iconColor = AppColors.onError,
            icon = painterResource(R.drawable.ic_trash),
            contentDescription = stringResource(R.string.delete),
            isDestructive = true,
            triggerOnDeepSwipe = true
        )
    )

    BottomSheet(
        onDismissed = onDismissed
    ) { slideOut ->
        fun selectFilter(filter: HomeGroupFilter) {
            onFilterSelected(filter)
            slideOut()
        }

        ListStack(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
            colors = ListColors(
                background = elevatedPageBackground,
                rowBackground = elevatedCardBackground,
                headerText = contentVariant,
                footerText = contentVariant.copy(alpha = .3f)
            ),
            cornerRadius = AppSpecs.cardCorner,
            contentPadding = PaddingValues(bottom = navigationBarHeight)
        ) {
            item(key = "top_padding") { VGap(72.dp) }
            if (showAllFilter) {
                item(key = "all") {
                    GroupRow(
                        modifier = Modifier.animateItem(placementSpec = Springs.crisp()),
                        title = stringResource(R.string.all),
                        trailingText = groupTodoCounts.values.sum().toString(),
                        iconType = FolderRowIconType.TODOS,
                        selected = selectedFilter == HomeGroupFilter.All,
                        onClick = {
                            selectFilter(HomeGroupFilter.All)
                        })
                    VGap(8.dp)
                }
            }
            item(key = "ungrouped") {
                GroupRow(
                    modifier = Modifier.animateItem(placementSpec = Springs.crisp()),
                    title = stringResource(R.string.ungrouped_todos),
                    trailingText = (groupTodoCounts[null] ?: 0).toString(),
                    iconType = FolderRowIconType.FOLDER_QUESTION_MARK,
                    selected = selectedFilter == HomeGroupFilter.Ungrouped,
                    onClick = {
                        selectFilter(HomeGroupFilter.Ungrouped)
                    })
                VGap(8.dp)
            }
            if (showRecentlyDeleted) {
                item(key = "recently_deleted") {
                    GroupRow(
                        modifier = Modifier.animateItem(placementSpec = Springs.crisp()),
                        title = stringResource(R.string.recently_deleted),
                        iconType = FolderRowIconType.TRASH,
                        onClick = {})
                    VGap(8.dp)
                }
            }
            item(key = "divider") {
                VDivider(
                    modifier = Modifier
                        .padding(horizontal = horizontalPadding + 16.dp)
                        .animateItem(placementSpec = Springs.crisp())
                )
                VGap(8.dp)
            }
            groups.forEach { group ->
                item(key = "group_${group.id}") {
                    GroupRow(
                        swipeableState = swipeableListState,
                        actions = actions,
                        modifier = Modifier.animateItem(placementSpec = Springs.crisp()),
                        title = group.name,
                        trailingText = (groupTodoCounts[group.id] ?: 0).toString(),
                        selected = selectedFilter == HomeGroupFilter.Group(group.id),
                        onClick = {
                            selectFilter(HomeGroupFilter.Group(group.id))
                        },
                        onDelete = {
                            onDeleteGroup(group)
                        }
                    )
                    VGap(8.dp)
                }
            }
            item(key = "new_group") {
                NewGroupRow(
                    modifier = Modifier.animateItem(placementSpec = Springs.crisp()),
                    title = stringResource(R.string.new_group),
                    onCreateGroup = onCreateGroup
                )
            }
            item { VGap() }
        }
        GlasenseModalTopBar(
            leading = {
                Action(
                    icon = painterResource(id = R.drawable.ic_forward_nav),
                    contentDescription = stringResource(R.string.back),
                    onClick = slideOut,
                    iconSize = 32.dp
                )
            },
            title = stringResource(R.string.groups),
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
fun ListScope.GroupRow(
    modifier: Modifier = Modifier,
    title: String,
    trailingText: String? = null,
    iconType: FolderRowIconType = FolderRowIconType.FOLDER,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    val selectedBackground = AppColors.elevatedCardBackground
    val backgroundColor = remember {
        ColorAnimatable(if (selected) selectedBackground else selectedBackground.copy(0f))
    }

    LaunchedEffect(selected, selectedBackground) {
        backgroundColor.animateTo(
            targetValue = if (selected) selectedBackground else selectedBackground.copy(0f),
            animationSpec = tween(durationMillis = 200)
        )
    }

    Row(
        modifier = modifier
            .padding(horizontal = horizontalPadding)
            .fillMaxWidth()
            .clip(AppSpecs.cardShape)
            .background(backgroundColor.value)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(
                id = when (iconType) {
                    FolderRowIconType.FOLDER -> R.drawable.ic_folder
                    FolderRowIconType.FOLDER_QUESTION_MARK -> R.drawable.ic_folder_questionmark
                    FolderRowIconType.FOLDER_PLUS -> R.drawable.ic_folder_plus
                    FolderRowIconType.TRASH -> R.drawable.ic_trash
                    FolderRowIconType.TODOS -> R.drawable.ic_checklist
                }
            ),
            contentDescription = null,
            modifier = Modifier
                .padding(end = 12.dp)
                .size(24.dp)
        )
        Text(
            text = title,
            style = GlasenseTheme.type.body,
            modifier = Modifier.weight(1f)
        )
        if (trailingText?.isNotBlank() == true) {
            HGap()
            Text(
                text = trailingText,
                style = GlasenseTheme.type.body,
                color = AppColors.contentVariant
            )
        }
    }
}

@Composable
fun ListScope.GroupRow(
    modifier: Modifier = Modifier,
    swipeableState: SwipeableListState,
    actions: ImmutableList<SwipeableActionButton>,
    title: String,
    trailingText: String? = null,
    iconType: FolderRowIconType = FolderRowIconType.FOLDER,
    selected: Boolean = false,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val selectedBackground = AppColors.elevatedCardBackground
    val backgroundColor = remember {
        ColorAnimatable(if (selected) selectedBackground else selectedBackground.copy(0f))
    }

    LaunchedEffect(selected, selectedBackground) {
        backgroundColor.animateTo(
            targetValue = if (selected) selectedBackground else selectedBackground.copy(0f),
            animationSpec = tween(durationMillis = 200)
        )
    }

    GlasenseSwipeable(
        key = title,
        modifier = modifier
            .padding(horizontal = horizontalPadding)
            .fillMaxWidth(),
        listState = swipeableState,
        actions = actions,
        onAction = { index ->
            when (index) {
                0 -> onDelete()
            }
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppSpecs.cardShape)
                .background(backgroundColor.value)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(
                    id = when (iconType) {
                        FolderRowIconType.FOLDER -> R.drawable.ic_folder
                        FolderRowIconType.FOLDER_QUESTION_MARK -> R.drawable.ic_folder_questionmark
                        FolderRowIconType.FOLDER_PLUS -> R.drawable.ic_folder_plus
                        FolderRowIconType.TRASH -> R.drawable.ic_trash
                        FolderRowIconType.TODOS -> R.drawable.ic_checklist
                    }
                ),
                contentDescription = null,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(24.dp)
            )
            Text(
                text = title,
                style = GlasenseTheme.type.body,
                modifier = Modifier.weight(1f)
            )
            if (trailingText?.isNotBlank() == true) {
                HGap()
                Text(
                    text = trailingText,
                    style = GlasenseTheme.type.body,
                    color = AppColors.contentVariant
                )
            }
        }
    }
}

@Composable
fun ListScope.NewGroupRow(
    modifier: Modifier = Modifier,
    title: String,
    selected: Boolean = false,
    onCreateGroup: (String) -> Unit
) {
    var groupName by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val inputTextStyle = GlasenseTheme.type.body
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val inputHeight = remember(textMeasurer, inputTextStyle, density) {
        val measuredHeight = textMeasurer.measure(
            text = AnnotatedString("Hg"),
            style = inputTextStyle,
            maxLines = 1
        ).size.height

        with(density) { measuredHeight.toDp() }
    }
    val submitGroupName = {
        val normalizedName = groupName.trim()
        if (normalizedName.isNotEmpty()) {
            onCreateGroup(normalizedName)
            groupName = ""
        }
    }

    Row(
        modifier = modifier
            .padding(horizontal = horizontalPadding)
            .fillMaxWidth()
            .clip(AppSpecs.cardShape)
            .then(if (selected) Modifier.background(AppColors.elevatedCardBackground) else Modifier)
            .clickable(onClick = { focusRequester.requestFocus() })
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_folder_plus),
            contentDescription = null,
            tint = GlasenseTheme.colors.primary,
            modifier = Modifier
                .padding(end = 12.dp)
                .size(24.dp)
        )
        BasicTextField(
            value = groupName,
            onValueChange = { groupName = it },
            modifier = Modifier
                .fillMaxWidth()
                .fixedHeightCenterVertically(inputHeight)
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { event ->
                    if (event.key == Key.Enter) {
                        if (event.type == KeyEventType.KeyUp) {
                            submitGroupName()
                        }
                        true
                    } else {
                        false
                    }
                },
            textStyle = inputTextStyle.copy(color = AppColors.content),
            cursorBrush = SolidColor(AppColors.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submitGroupName() }),
            singleLine = true,
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (groupName.isEmpty()) {
                        Text(
                            text = title,
                            color = AppColors.primary,
                            style = GlasenseTheme.type.body
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

private fun Modifier.fixedHeightCenterVertically(height: Dp): Modifier =
    layout { measurable, constraints ->
        val targetHeight = height.roundToPx()
        val placeable = measurable.measure(
            constraints.copy(
                minHeight = 0,
                maxHeight = Constraints.Infinity
            )
        )
        val width = placeable.width.coerceIn(constraints.minWidth, constraints.maxWidth)
        layout(width, targetHeight) {
            placeable.placeRelative(0, (targetHeight - placeable.height) / 2)
        }
    }

enum class FolderRowIconType {
    FOLDER,
    FOLDER_QUESTION_MARK,
    FOLDER_PLUS,
    TRASH,
    TODOS
}
