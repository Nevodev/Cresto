package com.nevoit.cresto.ui.components.packed

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.capsule.ContinuousRoundedRectangle
import com.nevoit.cresto.R
import com.nevoit.cresto.util.g2
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A composable that represents a single entry item in a configuration or settings screen.
 * It includes an icon, a title, and an optional forward arrow or switch.
 *
 * @param brush An optional Brush to be used as the background for the icon.
 * @param color An optional Color to be used as the background for the icon. Takes precedence over `brush`.
 * @param icon The Painter for the icon to be displayed.
 * @param title The main text label for the configuration item.
 * @param isSwitch If true, the forward arrow is hidden, implying a switch might be placed here. Defaults to false.
 * @param enableGlow If true, a glow effect is added to the icon. Defaults to false.
 * @param onClick The lambda function to be executed when the item is clicked.
 */
@Composable
fun ConfigEntryItem(
    brush: Brush? = null,
    color: Color? = null,
    icon: Painter,
    title: String,
    isSwitch: Boolean? = false,
    enableGlow: Boolean? = false,
    onClick: () -> Unit
) {
    // Interaction source to track press state.
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    // State for managing the alpha value for press animations.
    var alphaValue by remember { mutableFloatStateOf(1f) }
    val animatedAlphaValue by animateFloatAsState(
        targetValue = alphaValue,
        animationSpec = tween(200)
    )
    val scope = rememberCoroutineScope()

    // Animate alpha on press and release.
    LaunchedEffect(isPressed) {
        alphaValue = if (isPressed) {
            0.5f
        } else {
            1f
        }
    }

    // The main row layout for the item.
    Row(
        modifier = Modifier
            .graphicsLayer { alpha = animatedAlphaValue } // Apply the alpha animation.
            .clickable(
                enabled = true,
                onClick = {
                    // Custom click feedback animation.
                    scope.launch {
                        alphaValue = 0.5f
                        delay(400)
                        alphaValue = 1f
                    }
                    onClick()
                },
                interactionSource = interactionSource,
                indication = null // Disable default ripple effect.
            )
            .fillMaxWidth()
            .height(40.dp)
    ) {
        // Box to contain the icon with its background and optional glow.
        Box(
            modifier = Modifier
                .size(40.dp)
                .then(
                    // Apply brush or color background if provided.
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
            // Optional glow effect for the icon.
            if (enableGlow == true) {
                Icon(
                    painter = icon,
                    tint = Color.White.copy(.5f),
                    contentDescription = "$title config entry",
                    modifier = Modifier
                        .graphicsLayer { blendMode = BlendMode.Plus }
                        .fillMaxSize()
                        .blur(2.dp)
                )
            }
            // The main icon.
            Icon(
                painter = icon,
                tint = Color.White,
                contentDescription = "$title config entry",
                modifier = Modifier
                    .graphicsLayer { blendMode = BlendMode.Plus }
                    .fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        // The title text.
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically)
        )
        Spacer(modifier = Modifier.width(12.dp))
        // Display a forward arrow if it's not a switch item.
        if (isSwitch == null || !isSwitch) {
            Icon(
                painter = painterResource(R.drawable.ic_forward),
                tint = MaterialTheme.colorScheme.onBackground.copy(.2f),
                contentDescription = "Enter icon",
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .height(40.dp)
                    .width(20.dp)
            )
        }
    }
}

/**
 * A composable that represents an entry item specifically for "About" screen
 * It displays the app icon, name, developer info, and version.
 *
 * @param icon The Painter for the app icon.
 * @param onClick The lambda function to be executed when the item is clicked.
 */
@Composable
fun AboutEntryItem(
    icon: Painter,
    onClick: () -> Unit
) {
    // Interaction source to track press state.
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    // State for managing the alpha value for press animations.
    var alphaValue by remember { mutableFloatStateOf(1f) }
    val animatedAlphaValue by animateFloatAsState(
        targetValue = alphaValue,
        animationSpec = tween(200)
    )
    val scope = rememberCoroutineScope()

    val context = LocalContext.current

    // Retrieve package information to get the app version name.
    val packageInfo: PackageInfo? = remember {
        try {
            val packageName = context.packageName
            context.packageManager.getPackageInfo(packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            null
        }
    }

    // Animate alpha on press and release.
    LaunchedEffect(isPressed) {
        alphaValue = if (isPressed) {
            0.5f
        } else {
            1f
        }
    }
    // The main row layout for the item.
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .graphicsLayer { alpha = animatedAlphaValue } // Apply the alpha animation.
            .clickable(
                enabled = true,
                onClick = {
                    // Custom click feedback animation.
                    scope.launch {
                        alphaValue = 0.5f
                        delay(400)
                        alphaValue = 1f
                    }
                    onClick()
                },
                interactionSource = interactionSource,
                indication = null // Disable default ripple effect.
            )
            .fillMaxWidth()
    ) {
        // Box containing the app icon.
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(ContinuousRoundedRectangle(12.dp, g2))
        ) {
            Image(painter = icon, contentDescription = "App Icon")
        }
        Spacer(modifier = Modifier.width(12.dp))
        // Column for the app information text.
        Column(
            modifier = Modifier
                .weight(1f)
        ) {
            Text(
                text = "Cresto",
                fontSize = 20.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.W500,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Developed by Nevoit",
                fontSize = 12.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.W400,
                color = MaterialTheme.colorScheme.onBackground.copy(.5f),
            )
            Text(
                text = "Version ${packageInfo?.versionName}",
                fontSize = 12.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.W400,
                color = MaterialTheme.colorScheme.onBackground.copy(.5f),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        // Forward arrow icon.
        Icon(
            painter = painterResource(R.drawable.ic_forward),
            tint = MaterialTheme.colorScheme.onBackground.copy(.2f),
            contentDescription = "Enter icon",
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .height(40.dp)
                .width(20.dp)
        )
    }
}
