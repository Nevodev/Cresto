package com.nevoit.cresto.ui.components.glasense

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nevoit.glasense.theme.GlasenseTheme

/**
 * A simple header for a page, displaying a title.
 *
 * @param title The title to be displayed.
 */
@Composable
fun GlasensePageHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = GlasenseTheme.type.largeTitle,
        modifier = modifier
            .statusBarsPadding()
            .height(160.dp)
            .fillMaxWidth()
            .wrapContentHeight(Alignment.Bottom)
            .padding(start = 12.dp, bottom = 16.dp, end = 12.dp)
    )
}

@Composable
fun GlasensePageHeaderCompact(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = GlasenseTheme.type.largeTitle,
        modifier = modifier
            .statusBarsPadding()
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 16.dp)
    )
}
