package com.nevoit.cresto.feature.settings.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.effect
import com.kyant.backdrop.effects.runtimeShaderEffect
import com.nevoit.cresto.R
import com.nevoit.cresto.theme.AppButtonColors
import com.nevoit.cresto.theme.AppSpecs
import com.nevoit.cresto.theme.LocalGlasenseSettings
import com.nevoit.cresto.ui.components.glasense.GlasenseButtonAlt
import com.nevoit.cresto.ui.components.glasense.GlasenseModalTopBar
import com.nevoit.cresto.ui.components.glasense.glasenseHighlight
import com.nevoit.cresto.ui.components.glasense.isScrolledPast
import com.nevoit.cresto.ui.components.glasense.material.MaterialRecipes
import com.nevoit.cresto.ui.components.glasense.material.rememberMaterialRenderEffect
import com.nevoit.cresto.ui.components.packed.ConfigItemContainer
import com.nevoit.cresto.ui.modifier.shaderRipple
import com.nevoit.cresto.ui.modifier.tiltOnPress
import com.nevoit.glasense.component.BottomSheet
import com.nevoit.glasense.component.paddingItem
import com.nevoit.glasense.core.component.HGap
import com.nevoit.glasense.core.component.Icon
import com.nevoit.glasense.core.component.Text
import com.nevoit.glasense.core.component.VDivider
import com.nevoit.glasense.core.component.VGap
import com.nevoit.glasense.theme.GlasenseTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun UpdateBottomSheet(
    updateInfo: UpdateInfo,
    onDismissed: () -> Unit
) {
    val context = LocalContext.current
    val languageTag = LocalLocale.current.toLanguageTag()
    val manifest = updateInfo.manifest
    val description = remember(manifest, languageTag) {
        manifest.localizedDescription(languageTag)
    }
    val releaseNotes = remember(manifest, languageTag) {
        manifest.localizedReleaseNotes(languageTag)
    }
    val publishedDate = remember(manifest.publishedAt) {
        formatPublishedAt(manifest.publishedAt)
    }
    val listState = rememberLazyListState()
    val visible by listState.isScrolledPast(0.dp)

    val backgroundColor = GlasenseTheme.colors.elevatedPageBackground
    val backdrop = rememberLayerBackdrop {
        drawRect(
            backgroundColor,
            size = Size(this.size.width * 3, this.size.height * 3),
            topLeft = Offset(-this.size.width, -this.size.height)
        )
        drawContent()
    }
    BottomSheet(
        onDismissed = onDismissed,
        onDismissRequest = { slideOut ->
            if (!updateInfo.isRequired) slideOut()
        }
    ) { slideOut ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    bottom = WindowInsets.navigationBars.asPaddingValues()
                        .calculateBottomPadding() + 72.dp
                )
            ) {
                item {
                    VGap(72.dp)
                }
                item {
                    UpdateHeader(
                        manifest = manifest,
                        publishedDate = publishedDate,
                        description = description
                    )
                }

                releaseNotes.forEachIndexed { index, section ->
                    item(key = "release-note-$index") {
                        ReleaseNoteSection(section = section)
                        if (index != releaseNotes.lastIndex) {
                            VGap(24.dp)
                        }
                    }
                }

                if (releaseNotes.isEmpty()) {
                    item {
                        ConfigItemContainer(
                            backgroundColor = GlasenseTheme.colors.elevatedCardBackground
                        ) {
                            Text(
                                text = stringResource(R.string.no_release_notes),
                                style = GlasenseTheme.type.body,
                                color = GlasenseTheme.colors.contentVariant
                            )
                        }
                    }
                }
                item { VGap(24.dp) }
                paddingItem(listState)
            }

            UpdateTopBar(
                isRequired = updateInfo.isRequired,
                backdrop = backdrop,
                visible = visible,
                onClose = slideOut
            )
            UpdateActions(
                isRequired = updateInfo.isRequired,
                onLater = slideOut,
                backdrop = backdrop,
                onDownload = {
                    if (context.openUpdateUrl(manifest.downloadUrl, manifest.fallbackDownloadUrl)) {
                        if (!updateInfo.isRequired) slideOut()
                    }
                }
            )
        }
    }
}

@Composable
private fun UpdateHeader(
    manifest: UpdateManifest,
    publishedDate: String?,
    description: String?
) {
    UpdateHeroImage(url = manifest.heroImageUrl)

    Text(
        text = updateTitle(manifest),
        style = GlasenseTheme.type.largeTitleEmphasized,
        color = GlasenseTheme.colors.content,
        modifier = Modifier.padding(horizontal = 12.dp)
    )

    publishedDate?.let {
        Text(
            text = it,
            style = GlasenseTheme.type.subHeadline,
            color = GlasenseTheme.colors.contentVariant,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }

    Column(modifier = Modifier.padding(horizontal = 12.dp)) {
        VGap(8.dp)
        VDivider()
    }

    description?.takeIf { it.isNotBlank() }?.let {
        VGap(12.dp)
        Text(
            text = it,
            style = GlasenseTheme.type.body,
            color = GlasenseTheme.colors.content,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
    VGap(24.dp)
}

@Composable
private fun UpdateHeroImage(url: String?) {
    val imageUrl = url?.takeIf { it.isNotBlank() } ?: return
    val shape = AppSpecs.cardShape
    val darkMode = GlasenseTheme.darkTheme

    AsyncImage(
        model = imageUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .tiltOnPress(maxTilt = 10f)
            .clip(AppSpecs.cardShape)
            .drawWithContent {
                val outline = shape.createOutline(
                    size = size,
                    layoutDirection = LayoutDirection.Ltr,
                    density = this
                )
                drawContent()
                drawOutline(
                    outline = outline,
                    style = Stroke(4.dp.toPx()),
                    color = if (darkMode) Color.White.copy(.2f) else Color.Black.copy(
                        .05f
                    ),
                    blendMode = BlendMode.Luminosity
                )
            }
            .shaderRipple()
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
    )
    VGap(20.dp)
}

@Composable
private fun ReleaseNoteSection(section: ReleaseNoteSection) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        section.title?.takeIf { it.isNotBlank() }?.let { title ->
            ReleaseNoteSectionTitle(title = title, type = section.type)
            VGap(8.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
            ) {
                VDivider()
            }
            VGap(8.dp)
        }
        section.items.forEachIndexed { index, item ->
            ReleaseNoteItem(text = item, index = index)
            if (index != section.items.lastIndex) {
                VGap(6.dp)
            }
        }
    }
}

@Composable
private fun ReleaseNoteSectionTitle(title: String, type: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(releaseNoteIcon(type)),
            contentDescription = null,
            tint = GlasenseTheme.colors.contentVariant,
            modifier = Modifier.size(24.dp)
        )
        HGap(4.dp)
        Text(
            text = title,
            style = GlasenseTheme.type.title3Emphasized.copy(fontWeight = FontWeight.Normal),
            color = GlasenseTheme.colors.content
        )
    }
}

@Composable
private fun ReleaseNoteItem(text: String, index: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_sparkle_dot),
            contentDescription = null,
            tint = GlasenseTheme.colors.primary,
            modifier = Modifier
                .size(12.dp)
                .rotate(index * 45f)
        )
        HGap(8.dp)
        Text(
            text = text,
            style = GlasenseTheme.type.body,
            color = GlasenseTheme.colors.contentVariant
        )
    }
}

@Composable
private fun UpdateTopBar(
    isRequired: Boolean,
    backdrop: LayerBackdrop,
    visible: Boolean,
    onClose: () -> Unit
) {
    val surfaceColor = GlasenseTheme.colors.elevatedPageBackground
    val blur = !LocalGlasenseSettings.current.liteMode

    Box(
        modifier = Modifier
            .graphicsLayer {
                alpha = if (visible) 1f else 0f
            }
            .fillMaxWidth()
            .drawPlainBackdrop(
                backdrop = backdrop,
                shape = { RectangleShape },
                effects = {
                    if (blur) blur(3f.dp.toPx())
                    runtimeShaderEffect(
                        "AlphaMask", """
uniform shader content;

uniform float2 size;
layout(color) uniform half4 tint;
uniform float tintIntensity;

half4 main(float2 coord) {
float blurAlpha = smoothstep(size.y, size.y * 0.5, coord.y);
float tintAlpha = smoothstep(size.y, size.y * 0.5, coord.y);
return mix(content.eval(coord) * blurAlpha, tint * tintAlpha, tintIntensity);
}""", "content"
                    ) {
                        apply {
                            setFloatUniform("size", size.width, size.height)
                            setColorUniform("tint", surfaceColor)
                            setFloatUniform("tintIntensity", 0.7f)
                        }
                    }
                }
            )
            .padding(bottom = 32.dp + 48.dp)) {
    }
    GlasenseModalTopBar(
        title = if (isRequired) {
            stringResource(R.string.update_required)
        } else {
            stringResource(R.string.update_available)
        },
        leading = if (isRequired) {
            null
        } else {
            {
                Action(
                    icon = painterResource(id = R.drawable.ic_cross),
                    contentDescription = stringResource(R.string.back),
                    onClick = onClose
                )
            }
        },
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(top = 12.dp)
    )
}

@Composable
private fun BoxScope.UpdateActions(
    isRequired: Boolean,
    backdrop: LayerBackdrop,
    onLater: () -> Unit,
    onDownload: () -> Unit
) {
    val effect = rememberMaterialRenderEffect(MaterialRecipes.thin())
    val blur = !LocalGlasenseSettings.current.liteMode

    Row(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .drawPlainBackdrop(
                backdrop = backdrop,
                shape = { RectangleShape },
                effects = {
                    if (blur) {
                        this.padding = 32.dp.toPx()
                        blur(32.dp.toPx())
                    }
                    effect(effect)
                }
            )
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!isRequired) {
            GlasenseButtonAlt(
                modifier = Modifier.weight(1f),
                enabled = true,
                onClick = onLater,
                colors = AppButtonColors.secondary(),
                shape = AppSpecs.buttonShape
            ) {
                Text(text = stringResource(R.string.later))
            }
        }
        GlasenseButtonAlt(
            modifier = Modifier
                .weight(1f)
                .glasenseHighlight(AppSpecs.buttonCorner),
            enabled = true,
            onClick = onDownload,
            colors = AppButtonColors.primary(),
            shape = AppSpecs.buttonShape
        ) {
            Text(text = stringResource(R.string.download_update))
        }
    }
}

@Composable
private fun updateTitle(manifest: UpdateManifest) = buildAnnotatedString {
    append(manifest.versionName)
    withStyle(
        SpanStyle(
            color = GlasenseTheme.colors.contentVariant,
            fontWeight = FontWeight.Normal
        )
    ) {
        append(" (${manifest.versionCode})")
    }
}

private fun releaseNoteIcon(type: String?): Int {
    return when (type) {
        "fix" -> R.drawable.ic_wrench_and_screwdriver
        "feature" -> R.drawable.ic_star_outline
        "improvement" -> R.drawable.ic_wand_and_rays
        else -> R.drawable.ic_gear
    }
}

private fun Context.openUpdateUrl(primaryUrl: String, fallbackUrl: String?): Boolean {
    return openUrl(primaryUrl) || fallbackUrl?.let { openUrl(it) } == true
}

private fun Context.openUrl(url: String): Boolean {
    if (url.isBlank()) return false
    return try {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                url.toUri()
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}

private fun formatPublishedAt(publishedAt: String?): String? {
    if (publishedAt.isNullOrBlank()) return null

    return runCatching {
        val instant = Instant.parse(publishedAt)
        val formatter = DateTimeFormatter
            .ofPattern("yyyy/MM/dd")
            .withZone(ZoneId.systemDefault())

        formatter.format(instant)
    }.getOrNull()
}
