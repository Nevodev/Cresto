package com.nevoit.cresto.ui.components.packed

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nevoit.glasense.component.ListScope

fun ListScope.TopBarSpacer() {
    item {
        val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        Spacer(modifier = Modifier.height(48.dp + statusBarHeight + 12.dp))
    }
}