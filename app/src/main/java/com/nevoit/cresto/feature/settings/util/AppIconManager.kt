package com.nevoit.cresto.feature.settings.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object AppIconManager {
    private const val ALIAS_DEFAULT = "com.nevoit.cresto.AppIconDefault"
    private const val ALIAS_APRICOT = "com.nevoit.cresto.AppIconApricot"
    private const val ALIAS_KIWI = "com.nevoit.cresto.AppIconKiwi"
    private const val ALIAS_BLUEBERRY = "com.nevoit.cresto.AppIconBlueberry"
    private const val ALIAS_PEACH = "com.nevoit.cresto.AppIconPeach"


    enum class AppIcon(val alias: String) {
        DEFAULT(ALIAS_DEFAULT),
        APRICOT(ALIAS_APRICOT),
        KIWI(ALIAS_KIWI),
        BLUEBERRY(ALIAS_BLUEBERRY),
        PEACH(ALIAS_PEACH)
    }

    fun setIcon(context: Context, icon: AppIcon) {
        val pm = context.packageManager

        AppIcon.entries.forEach { entry ->
            val component = ComponentName(context, entry.alias)
            val state = if (entry == icon) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            pm.setComponentEnabledSetting(
                component,
                state,
                PackageManager.DONT_KILL_APP
            )
        }
    }

    fun getCurrentIcon(context: Context): AppIcon {
        val pm = context.packageManager
        return AppIcon.entries.firstOrNull { entry ->
            val component = ComponentName(context, entry.alias)
            pm.getComponentEnabledSetting(component) ==
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } ?: AppIcon.DEFAULT
    }
}