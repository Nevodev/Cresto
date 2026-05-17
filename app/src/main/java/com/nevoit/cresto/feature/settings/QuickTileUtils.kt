package com.nevoit.cresto.feature.settings

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import com.nevoit.cresto.R
import com.nevoit.cresto.feature.screenextract.ExtractScreenTileService

internal fun requestAddExtractScreenTile(context: Context) {
    val statusBarManager = context.getSystemService(StatusBarManager::class.java) ?: return
    statusBarManager.requestAddTileService(
        ComponentName(context, ExtractScreenTileService::class.java),
        context.getString(R.string.extract_screen_tile_label),
        Icon.createWithResource(context, R.drawable.ic_sparkle_viewfinder),
        context.mainExecutor
    ) {
        // No-op: the setting only controls availability; the system owns actual placement.
    }
}
