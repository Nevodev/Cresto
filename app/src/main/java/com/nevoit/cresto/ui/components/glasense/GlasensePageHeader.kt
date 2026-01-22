package com.nevoit.cresto.ui.components.glasense

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A simple header for a page, displaying a title.
 *
 * @param title The title to be displayed.
 */
@Composable
fun GlasensePageHeader(
    title: String
) {
    // A box that provides padding for the status bar and sets a fixed height.
    Box(
        modifier = Modifier
            .statusBarsPadding()
            .height(160.dp)
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
