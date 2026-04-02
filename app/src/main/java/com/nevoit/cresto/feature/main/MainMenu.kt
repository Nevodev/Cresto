package com.nevoit.cresto.feature.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.nevoit.cresto.R
import com.nevoit.cresto.glasense.getFlagColor
import com.nevoit.cresto.ui.components.glasense.GlasenseMenuItem
import com.nevoit.cresto.ui.components.glasense.MenuDivider
import com.nevoit.cresto.ui.components.glasense.MenuItemData

@Composable
fun rememberMoreMenuItems(
    onDuplicateSelected: () -> Unit,
    onMergeSelected: () -> Unit,
    canMerge: Boolean
): List<GlasenseMenuItem> {
    val combineIcon = painterResource(R.drawable.ic_combine_as_one)
    val shareIcon = painterResource(R.drawable.ic_share)
    val duplicateIcon = painterResource(R.drawable.ic_duplicate)
    val duplicateText = stringResource(R.string.duplicate_todo)
    val mergeTodosText = stringResource(R.string.merge_todos)
    val shareText = stringResource(R.string.share)

    return remember(
        onDuplicateSelected,
        onMergeSelected,
        canMerge,
        duplicateText,
        mergeTodosText,
        shareText
    ) {
        buildList {
            add(
                MenuItemData(
                    duplicateText,
                    duplicateIcon,
                    onClick = onDuplicateSelected
                )
            )
            if (canMerge) {
                add(MenuDivider)
                add(
                    MenuItemData(
                        mergeTodosText,
                        combineIcon,
                        onClick = onMergeSelected
                    )
                )
            }
            add(MenuDivider)
            add(
                MenuItemData(
                    shareText,
                    shareIcon,
                    onClick = { }
                )
            )
        }
    }
}

@Composable
fun rememberFlagMenuItems(onFlagSelected: (Int) -> Unit): List<GlasenseMenuItem> {
    val flagIcon = painterResource(R.drawable.ic_flag_fill)
    val noFlagIcon = painterResource(R.drawable.ic_flag)
    val flagNames = listOf(
        stringResource(R.string.flag_red),
        stringResource(R.string.flag_orange),
        stringResource(R.string.flag_yellow),
        stringResource(R.string.flag_green),
        stringResource(R.string.flag_blue),
        stringResource(R.string.flag_purple),
        stringResource(R.string.flag_gray)
    )

    val noneText = stringResource(R.string.none)
    return remember(onFlagSelected, flagIcon, noFlagIcon, flagNames, noneText) {
        buildList {
            flagNames.forEachIndexed { index, flagName ->
                val flagIndex = index + 1
                add(
                    MenuItemData(
                        text = flagName,
                        icon = flagIcon,
                        iconColor = getFlagColor(flagIndex),
                        onClick = { onFlagSelected(flagIndex) }
                    )
                )
            }
            add(
                MenuDivider
            )
            add(
                MenuItemData(
                    text = noneText,
                    icon = noFlagIcon,
                    onClick = { onFlagSelected(0) }
                )
            )
        }
    }
}
