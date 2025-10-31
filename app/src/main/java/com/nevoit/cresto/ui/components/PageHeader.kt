package com.nevoit.cresto.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A simple header for a page, displaying a title.
 *
 * @param title The title to be displayed.
 * @param statusBarHeight The height of the system status bar, used for padding.
 */
@Composable
fun PageHeader(
    title: String,
    statusBarHeight: Dp
) {
    // A box that provides padding for the status bar and sets a fixed height.
    Box(
        modifier = Modifier
            .padding(top = statusBarHeight)
            .height(128.dp + statusBarHeight)
            .fillMaxWidth()
    ) {
        // The title text, aligned to the bottom start of the box.
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier
                .padding(start = 12.dp, bottom = 16.dp)
                .align(Alignment.BottomStart)
        )
    }
}
