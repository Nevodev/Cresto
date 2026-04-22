package com.nevoit.cresto.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.nevoit.cresto.R
import com.nevoit.cresto.feature.settings.util.SettingsManager
import com.nevoit.cresto.feature.settings.util.SortOption
import com.nevoit.cresto.feature.settings.util.SortOrder
import com.nevoit.cresto.ui.components.glasense.GlasenseMenuItem
import com.nevoit.cresto.ui.components.glasense.MenuDivider
import com.nevoit.cresto.ui.components.glasense.SelectiveMenuItemData

@Composable
fun rememberSortMenuItems(): List<GlasenseMenuItem> {
    val defaultText = stringResource(R.string.filter_default)
    val dueDateText = stringResource(R.string.due_date)
    val flagText = stringResource(R.string.flag)
    val titleText = stringResource(R.string.title)
    val ascText = stringResource(R.string.ascending)
    val descText = stringResource(R.string.descending)

    val rankIcon = painterResource(R.drawable.ic_rank)
    val calendarAltIcon = painterResource(R.drawable.ic_calendar)
    val flagIcon = painterResource(R.drawable.ic_flag)
    val characterIcon = painterResource(R.drawable.ic_character)
    val ascIcon = painterResource(R.drawable.ic_chevron_up)
    val descIcon = painterResource(R.drawable.ic_chevron_down)

    return remember(
        defaultText,
        rankIcon,
        calendarAltIcon,
        flagIcon,
        characterIcon,
        dueDateText,
        flagText,
        titleText,
        ascText,
        descText,
        ascIcon,
        descIcon
    ) {
        listOf(
            SelectiveMenuItemData(
                defaultText,
                rankIcon,
                isSelected = { SettingsManager.sortOptionState.intValue == SortOption.DEFAULT.ordinal },
                onClick = { SettingsManager.sortOption = SortOption.DEFAULT }
            ),
            SelectiveMenuItemData(
                dueDateText,
                calendarAltIcon,
                isSelected = { SettingsManager.sortOptionState.intValue == SortOption.DUE_DATE.ordinal },
                onClick = { SettingsManager.sortOption = SortOption.DUE_DATE }
            ),
            SelectiveMenuItemData(
                flagText,
                flagIcon,
                isSelected = { SettingsManager.sortOptionState.intValue == SortOption.FLAG.ordinal },
                onClick = { SettingsManager.sortOption = SortOption.FLAG }
            ),
            SelectiveMenuItemData(
                titleText,
                characterIcon,
                isSelected = { SettingsManager.sortOptionState.intValue == SortOption.TITLE.ordinal },
                onClick = { SettingsManager.sortOption = SortOption.TITLE }
            ),
            MenuDivider,
            SelectiveMenuItemData(
                ascText,
                ascIcon,
                isSelected = { SettingsManager.sortOrderState.intValue == SortOrder.ASCENDING.ordinal },
                onClick = { SettingsManager.sortOrder = SortOrder.ASCENDING }
            ),
            SelectiveMenuItemData(
                descText,
                descIcon,
                isSelected = { SettingsManager.sortOrderState.intValue == SortOrder.DESCENDING.ordinal },
                onClick = { SettingsManager.sortOrder = SortOrder.DESCENDING }
            )
        )
    }
}