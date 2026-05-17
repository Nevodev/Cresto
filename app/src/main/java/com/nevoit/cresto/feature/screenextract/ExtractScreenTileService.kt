package com.nevoit.cresto.feature.screenextract

import android.content.Intent
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.nevoit.cresto.R
import com.nevoit.cresto.feature.settings.util.SettingsManager

class ExtractScreenTileService : TileService() {

    private val screenshotCapturer = ShizukuScreenshotCapturer()

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        if (!SettingsManager.isExtractScreenQuickTileEnabled) {
            Toast.makeText(this, R.string.extract_screen_tile_disabled, Toast.LENGTH_SHORT).show()
            updateTileState()
            return
        }

        if (!screenshotCapturer.hasPermission()) {
            Toast.makeText(this, R.string.shizuku_permission_required, Toast.LENGTH_SHORT).show()
            updateTileState()
            return
        }

        startService(Intent(this, ScreenExtractService::class.java))
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val enabled = SettingsManager.isExtractScreenQuickTileEnabled
        val hasPermission = screenshotCapturer.hasPermission()
        tile.label = getString(R.string.extract_screen_tile_label)
        tile.subtitle = when {
            !enabled -> getString(R.string.disabled)
            hasPermission -> getString(R.string.ready)
            else -> getString(R.string.shizuku_permission_required)
        }
        tile.contentDescription = tile.label
        tile.icon = Icon.createWithResource(this, R.drawable.ic_sparkle_viewfinder)
        tile.state = Tile.STATE_INACTIVE
        tile.updateTile()
    }
}
