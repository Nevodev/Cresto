package com.nevoit.cresto.ui.screens.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.nevoit.cresto.R
import com.nevoit.cresto.ui.components.glasense.GlasenseMenuItem
import com.nevoit.cresto.ui.components.glasense.MenuDivider
import com.nevoit.cresto.ui.components.glasense.MenuItemData
import com.nevoit.cresto.ui.theme.glasense.getFlagColor

@Composable
fun rememberMoreMenuItems(): List<GlasenseMenuItem> {
    val rankIcon = painterResource(R.drawable.ic_rank)

    return remember() {
        listOf(
            MenuItemData(
                "创建副本",
                rankIcon,
                onClick = { }
            ),
            MenuItemData(
                "设置标签",
                rankIcon,
                onClick = { }
            ),
            MenuDivider,
            MenuItemData(
                "合并任务",
                rankIcon,
                onClick = { }
            ),
            MenuItemData(
                "关联主任务",
                rankIcon,
                onClick = { }
            ),
            MenuDivider,
            MenuItemData(
                "导出为 .json",
                rankIcon,
                onClick = { }
            )
        )
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
            add(
                MenuItemData(
                    text = noneText,
                    icon = noFlagIcon,
                    onClick = { onFlagSelected(0) }
                )
            )
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
        }
    }
}
