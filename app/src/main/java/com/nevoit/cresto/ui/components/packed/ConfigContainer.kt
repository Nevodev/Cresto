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
 * A container composable for configuration items, providing a consistent layout with an optional title and a background.
 *
 * @param title An optional title to be displayed above the container.
 * @param backgroundColor The background color of the main content area.
 * @param content The composable content to be displayed inside the container.
 */
@Composable
fun ConfigContainer(
    title: String? = null,
    backgroundColor: Color,
    content: @Composable () -> Unit,
) {
    // The main column that holds the optional title and the content box.
    Column(modifier = Modifier.fillMaxWidth()) {
        // Display the title only if it is not null.
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
        // The main box that contains the content with a specific background and shape.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = backgroundColor, shape = ContinuousRoundedRectangle(12.dp, g2))
        ) {
            // An inner box to provide padding for the content.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // The actual content provided to the composable.
                content()
            }
        }
    }
}
