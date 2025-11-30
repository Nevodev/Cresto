package com.nevoit.cresto.ui.components.packed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.capsule.ContinuousRoundedRectangle
import com.nevoit.cresto.util.g2

/**
 * A container for configuration items.
 *
 * @param title An optional title for the container.
 * @param backgroundColor The background color of the container.
 * @param content The content of the container.
 */
@Composable
fun ConfigItemContainer(
    title: String? = null,
    backgroundColor: Color,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Display the title if it's provided.
        if (title != null) {
            Text(
                text = title,
                fontSize = 14.sp,
                lineHeight = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(.5f),
                modifier = Modifier
                    .padding(
                        start = 12.dp,
                        top = 0.dp,
                        end = 12.dp,
                        bottom = 12.dp
                    )
                    .fillMaxWidth()
            )
        }
        // The main container box with background and shape.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = backgroundColor, shape = ContinuousRoundedRectangle(12.dp, g2))
        ) {
            // Inner box with padding for the content.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                content()
            }
        }
    }
}
