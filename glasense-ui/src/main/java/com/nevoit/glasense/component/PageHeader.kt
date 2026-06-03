package com.nevoit.glasense.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nevoit.glasense.core.component.Text
import com.nevoit.glasense.theme.GlasenseTheme

@Composable
fun PageHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = GlasenseTheme.type.largeTitleEmphasized,
        modifier = modifier
            .statusBarsPadding()
            .height(160.dp)
            .fillMaxWidth()
            .wrapContentHeight(Alignment.Bottom)
            .padding(start = 12.dp, bottom = 16.dp, end = 12.dp)
    )
}

fun ListScope.PageHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    item(
        key = "page_header",
        contentType = "page_header"
    ) {
        Text(
            text = title,
            style = GlasenseTheme.type.largeTitleEmphasized,
            modifier = modifier
                .statusBarsPadding()
                .height(160.dp)
                .fillMaxWidth()
                .wrapContentHeight(Alignment.Bottom)
                .padding(
                    start = horizontalPadding + 12.dp,
                    bottom = 16.dp,
                    end = horizontalPadding + 12.dp
                )
        )
    }
}
