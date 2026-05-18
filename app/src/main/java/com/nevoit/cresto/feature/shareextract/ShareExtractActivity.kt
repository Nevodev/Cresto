package com.nevoit.cresto.feature.shareextract

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import com.nevoit.cresto.R

class ShareExtractActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sourceIntent = intent
        val sharedText = sourceIntent.extractSharedText()
        val imageUris = sourceIntent.extractImageUris()

        if (sharedText.isBlank() && imageUris.isEmpty()) {
            Toast.makeText(this, R.string.share_extract_no_supported_content, Toast.LENGTH_SHORT)
                .show()
            finishSilently()
            return
        }

        imageUris.forEach { uri ->
            grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val serviceIntent = Intent(this, ShareExtractService::class.java).apply {
            putExtra(ShareExtractService.EXTRA_SHARED_TEXT, sharedText)
            putParcelableArrayListExtra(
                ShareExtractService.EXTRA_IMAGE_URIS,
                ArrayList(imageUris)
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = imageUris.toClipData()
        }

        startService(serviceIntent)
        Toast.makeText(this, R.string.share_extract_started, Toast.LENGTH_SHORT).show()
        finishSilently()
    }

    @Suppress("DEPRECATION")
    private fun finishSilently() {
        finish()
        overridePendingTransition(0, 0)
    }

    private fun Intent.extractSharedText(): String {
        return getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()?.trim()
            ?: getStringExtra(Intent.EXTRA_HTML_TEXT)?.trim()
            ?: ""
    }

    private fun Intent.extractImageUris(): List<Uri> {
        val uris = buildList {
            getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)?.let(::add)
            getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)?.let(::addAll)

            val clip = clipData
            if (clip != null) {
                repeat(clip.itemCount) { index ->
                    clip.getItemAt(index).uri?.let(::add)
                }
            }
        }

        return uris.distinctBy(Uri::toString)
    }

    private fun List<Uri>.toClipData(): ClipData? {
        val firstUri = firstOrNull() ?: return null
        return ClipData.newUri(contentResolver, "shared_image_0", firstUri).apply {
            drop(1).forEachIndexed { _, uri -> //index, uri
                addItem(ClipData.Item(uri))
            }
        }
    }
}