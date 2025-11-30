package com.nevoit.cresto.ui.screens.settings.util

import com.tencent.mmkv.MMKV

/**
 * A singleton object for managing app settings using MMKV.
 * This provides a centralized and efficient way to store and retrieve user preferences.
 */
object SettingsManager {

    // Get the default MMKV instance for data storage.
    private val mmkv = MMKV.defaultMMKV()

    // Define constant keys for storing and retrieving settings to avoid typos.
    private const val KEY_CUSTOM_PRIMARY_COLOR_ENABLED = "custom_primary_color_enabled"
    private const val KEY_USE_DYNAMIC_COLOR = "use_dynamic_color_enabled"
    private const val KEY_LITE_MODE = "lite_mode_enabled"
    private const val KEY_LIQUID_GLASS = "liquid_glass_enabled"
    private const val KEY_COLOR_MODE = "color_mode"

    var isCustomPrimaryColorEnabled: Boolean
        get() = mmkv.decodeBool(KEY_CUSTOM_PRIMARY_COLOR_ENABLED, false)
        set(value) {
            mmkv.encode(KEY_CUSTOM_PRIMARY_COLOR_ENABLED, value)
        }

    var isUseDynamicColor: Boolean
        get() = mmkv.decodeBool(KEY_USE_DYNAMIC_COLOR, false)
        set(value) {
            mmkv.encode(KEY_USE_DYNAMIC_COLOR, value)
        }

    var isLiteMode: Boolean
        get() = mmkv.decodeBool(KEY_LITE_MODE, false)
        set(value) {
            mmkv.encode(KEY_LITE_MODE, value)
        }

    var isLiquidGlass: Boolean
        get() = mmkv.decodeBool(KEY_LIQUID_GLASS, false)
        set(value) {
            mmkv.encode(KEY_LIQUID_GLASS, value)
        }

    var colorMode: Int
        get() = mmkv.decodeInt(KEY_COLOR_MODE, 0)
        set(value) {
            mmkv.encode(KEY_COLOR_MODE, value)
        }


    // Example 3: String - Username
    /*var username: String
        get() = mmkv.decodeString(KEY_USERNAME, "") ?: "" // Provides a default value of an empty string
        set(value) = mmkv.encode(KEY_USERNAME, value)

    // Example 4: Int - Sync Frequency (minutes)
    var syncFrequency: Int
        get() = mmkv.decodeInt(KEY_SYNC_FREQUENCY, 15) // Provides a default value of 15
        set(value) = mmkv.encode(KEY_SYNC_FREQUENCY, value)*/

}
