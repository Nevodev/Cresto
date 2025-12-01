package com.nevoit.cresto.ui.components.packed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.capsule.ContinuousRoundedRectangle
import com.nevoit.cresto.util.g2

/**
 * A composable function that displays a header for a configuration screen.
 * It includes an icon, a title, and some informational text, all within a styled container.
 *
 * @param brush An optional Brush to be used as the background for the icon.
 * @param color An optional Color to be used as the background for the icon. This takes precedence over `brush`.
 * @param backgroundColor The background color for the entire header container.
 * @param icon The Painter for the icon to be displayed.
 * @param title The main title text of the header.
 * @param info A short informational text displayed below the title.
 * @param enableGlow If true, a glowing effect is added to the icon for emphasis. Defaults to false.
 */
@Composable
fun ConfigInfoHeader(
    brush: Brush? = null,
    color: Color? = null,
    backgroundColor: Color,
    icon: Painter,
    title: String,
    info: String,
    enableGlow: Boolean? = false
) {
    // The main container column.
    Column(
        modifier = Modifier
            .background(color = backgroundColor, shape = ContinuousRoundedRectangle(12.dp, g2))
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Box to display the icon with a circular background and optional glow.
        Box(
            modifier = Modifier
                .padding(top = 24.dp)
                .size(48.dp)
                // Apply either a brush or a solid color as the background, or no background if both are null.
                .then(
                    if (brush != null) Modifier.background(
                        brush = brush,
                        shape = CircleShape
                    ) else if (color != null) Modifier.background(
                        color = color,
                        shape = CircleShape
                    ) else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            // If enabled, draw a blurred version of the icon behind the main icon to create a glow effect.
            if (enableGlow == true) {
                Icon(
                    painter = icon,
                    tint = Color.White.copy(.5f),
                    contentDescription = null,
                    modifier = Modifier
                        .graphicsLayer { blendMode = BlendMode.Plus }
                        .fillMaxSize()
                        .blur(2.dp)
                )
            }
            // The main icon itself.
            Icon(
                painter = icon,
                tint = Color.White,
                contentDescription = null,
                modifier = Modifier
                    .graphicsLayer { blendMode = BlendMode.Plus }
                    .fillMaxSize()
            )
        }
        // The main title text.
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth(),
            maxLines = 1,
            textAlign = TextAlign.Center
        )
        // The secondary informational text, with reduced opacity.
        Text(
            text = info,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(start = 24.dp, top = 0.dp, end = 24.dp, bottom = 20.dp)
                .fillMaxWidth()
                .alpha(.5f)
        )
    }
}