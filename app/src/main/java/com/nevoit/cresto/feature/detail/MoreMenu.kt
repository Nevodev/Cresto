package com.nevoit.cresto.feature.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.nevoit.cresto.R
import com.nevoit.cresto.ui.components.glasense.GlasenseMenuItem
import com.nevoit.cresto.ui.components.glasense.MenuDivider
import com.nevoit.cresto.ui.components.glasense.MenuItemData

@Composable
fun rememberMoreMenuItems(
    onDuplicateSelected: () -> Unit,
    onAddToCalendarSelected: () -> Unit,
    onShareSelected: () -> Unit
): List<GlasenseMenuItem> {
    val shareIcon = painterResource(R.drawable.ic_share)
    val duplicateIcon = painterResource(R.drawable.ic_duplicate)
    val calendarIcon = painterResource(R.drawable.ic_calendar_add)
    val duplicateText = stringResource(R.string.duplicate_todo)
    val addToCalendarText = stringResource(R.string.add_to_calendar)
    val shareText = stringResource(R.string.share)

    return remember(
        onDuplicateSelected,
        onAddToCalendarSelected,
        onShareSelected,
        duplicateText,
        addToCalendarText,
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
            add(MenuDivider)
            add(
                MenuItemData(
                    addToCalendarText,
                    calendarIcon,
                    onClick = onAddToCalendarSelected
                )
            )
            add(MenuDivider)
            add(
                MenuItemData(
                    shareText,
                    shareIcon,
                    onClick = onShareSelected
                )
            )
        }
    }
}