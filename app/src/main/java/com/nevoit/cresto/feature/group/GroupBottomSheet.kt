package com.nevoit.cresto.feature.group

import androidx.compose.runtime.Composable
import com.nevoit.glasense.component.BottomSheet

@Composable
fun GroupBottomSheet(
    onDismissed: () -> Unit
) {
    BottomSheet(
        onDismissed = onDismissed
    ) { slideOut ->

    }
}