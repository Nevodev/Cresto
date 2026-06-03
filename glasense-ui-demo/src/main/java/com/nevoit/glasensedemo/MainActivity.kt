package com.nevoit.glasensedemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nevoit.glasense.component.ListStack
import com.nevoit.glasense.component.ListStyle
import com.nevoit.glasense.component.PageHeader
import com.nevoit.glasense.component.ProgressView
import com.nevoit.glasense.component.ProgressViewStyle
import com.nevoit.glasense.core.component.Text
import com.nevoit.glasense.core.interaction.overscroll.rememberOffsetOverscrollFactory
import com.nevoit.glasense.theme.GlasenseTheme
import com.nevoit.glasense.theme.tokens.Blue500
import com.nevoit.glasense.theme.tokens.Cyan500
import com.nevoit.glasense.theme.tokens.Emerald500
import com.nevoit.glasense.theme.tokens.Fuchsia500
import com.nevoit.glasense.theme.tokens.Green500
import com.nevoit.glasense.theme.tokens.Indigo500
import com.nevoit.glasense.theme.tokens.Orange500
import com.nevoit.glasense.theme.tokens.Red500
import com.nevoit.glasense.theme.tokens.Rose500
import com.nevoit.glasense.theme.tokens.Yellow500

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GlasenseDemoApp()
        }
    }
}

@Composable
private fun GlasenseDemoApp() {
    val darkTheme = isSystemInDarkTheme()
    val overscrollFactory = rememberOffsetOverscrollFactory(Orientation.Vertical)
    CompositionLocalProvider(LocalOverscrollFactory provides overscrollFactory) {
        GlasenseTheme(darkTheme = darkTheme) {
            var showSheet by remember { mutableStateOf(false) }
            DemoScreen(onShowSheet = { showSheet = true })
        }
    }
}

@Composable
private fun DemoScreen(onShowSheet: () -> Unit) {
    var progress by remember { mutableFloatStateOf(0.5f) }
    val colors = mapOf(
        "Red" to Red500,
        "Orange" to Orange500,
        "Yellow" to Yellow500,
        "Green" to Green500,
        "Emerald" to Emerald500,
        "Cyan" to Cyan500,
        "Blue" to Blue500,
        "Indigo" to Indigo500,
        "Fuchsia" to Fuchsia500,
        "Rose" to Rose500,
        "Content" to GlasenseTheme.colors.content,
        "Content Variant" to GlasenseTheme.colors.contentVariant,
        "Primary" to GlasenseTheme.colors.primary,
        "On Primary" to GlasenseTheme.colors.onPrimary,
        "Error" to GlasenseTheme.colors.error,
        "On Error" to GlasenseTheme.colors.onError,
        "Highlight Text" to GlasenseTheme.colors.highlightText,
        "Background" to GlasenseTheme.colors.background,
        "Page Background" to GlasenseTheme.colors.pageBackground,
        "Card Background" to GlasenseTheme.colors.cardBackground
    )

    ListStack(
        modifier = Modifier.fillMaxSize(),
        style = ListStyle.InsetGrouped,
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        PageHeader(title = "Gallery")

        Section(header = "Progress") {
            Row {
                ProgressView()
            }
            Row {
                ProgressView(
                    value = progress,
                    modifier = Modifier.fillMaxWidth(),
                    progressViewStyle = ProgressViewStyle.Circular
                )
            }
            Row { ProgressView(value = progress, modifier = Modifier.fillMaxWidth()) }
        }

        Section(header = "Colors", footer = "Colors from Tailwind CSS Color Palette.") {
            for (color in colors) {
                LeadingRow(onClick = {}, leading = { ColorBox(color.value) }) {
                    Text(color.key)
                }
            }
        }

        Section(header = "Typography", footer = "Default typography.") {
            Row(onClick = {}) {
                Text("Large title", style = GlasenseTheme.type.largeTitleEmphasized)
            }
            Row(onClick = {}) {
                Text("Large title", style = GlasenseTheme.type.largeTitle)
            }
            Row(onClick = {}) {
                Text("Title1", style = GlasenseTheme.type.title1Emphasized)
            }
            Row(onClick = {}) {
                Text("Title1", style = GlasenseTheme.type.title1)
            }
            Row(onClick = {}) {
                Text("Title2", style = GlasenseTheme.type.title2Emphasized)
            }
            Row(onClick = {}) {
                Text("Title2", style = GlasenseTheme.type.title2)
            }
            Row(onClick = {}) {
                Text("Title3", style = GlasenseTheme.type.title3Emphasized)
            }
            Row(onClick = {}) {
                Text("Title3", style = GlasenseTheme.type.title3)
            }
            Row(onClick = {}) {
                Text("Headline", style = GlasenseTheme.type.headline)
            }
            Row(onClick = {}) {
                Text("Body", style = GlasenseTheme.type.body)
            }
            Row(onClick = {}) {
                Text("Body", style = GlasenseTheme.type.bodyEmphasized)
            }
            Row(onClick = {}) {
                Text("Callout", style = GlasenseTheme.type.subHeadline)
            }
            Row(onClick = {}) {
                Text("Callout", style = GlasenseTheme.type.subHeadlineEmphasized)
            }
            Row(onClick = {}) {
                Text("Footnote", style = GlasenseTheme.type.footnote)
            }
        }
    }
}

@Composable
private fun ColorBox(color: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(color)
            .border(1.dp, GlasenseTheme.colors.scrimNormal, CircleShape)
    )
}

@Composable
private fun DemoChip(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(GlasenseTheme.colors.scrimNormal)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = GlasenseTheme.type.subHeadline)
    }
}
