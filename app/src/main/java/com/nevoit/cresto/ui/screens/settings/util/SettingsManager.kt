package com.nevoit.cresto.ui.screens.settings.util

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
    private const val KEY_IS_FIRST_RUN = "is_first_run"
    private const val KEY_SORT_OPTION = "sort_option"
    private const val KEY_SORT_ORDER = "sort_order"

    const val MODE_LIGHT = 0
    const val MODE_DARK = 1
    const val MODE_SYSTEM = 2

    val colorModeState = mutableIntStateOf(mmkv.decodeInt(KEY_COLOR_MODE, MODE_SYSTEM))
    val isCustomPrimaryColorEnabledState =
        mutableStateOf(mmkv.decodeBool(KEY_CUSTOM_PRIMARY_COLOR_ENABLED, false))
    val isUseDynamicColorState = mutableStateOf(mmkv.decodeBool(KEY_USE_DYNAMIC_COLOR, false))
    val isLiteModeState = mutableStateOf(mmkv.decodeBool(KEY_LITE_MODE, false))
    val isLiquidGlassState = mutableStateOf(mmkv.decodeBool(KEY_LIQUID_GLASS, false))
    val sortOptionState =
        mutableIntStateOf(mmkv.decodeInt(KEY_SORT_OPTION, SortOption.DEFAULT.ordinal))
    val sortOrderState =
        mutableIntStateOf(mmkv.decodeInt(KEY_SORT_ORDER, SortOrder.DESCENDING.ordinal))

    var isCustomPrimaryColorEnabled: Boolean
        get() = mmkv.decodeBool(KEY_CUSTOM_PRIMARY_COLOR_ENABLED, false)
        set(value) {
            mmkv.encode(KEY_CUSTOM_PRIMARY_COLOR_ENABLED, value)
            isCustomPrimaryColorEnabledState.value = value
        }

    var isUseDynamicColor: Boolean
        get() = mmkv.decodeBool(KEY_USE_DYNAMIC_COLOR, false)
        set(value) {
            mmkv.encode(KEY_USE_DYNAMIC_COLOR, value)
            isUseDynamicColorState.value = value
        }

    var isLiteMode: Boolean
        get() = mmkv.decodeBool(KEY_LITE_MODE, false)
        set(value) {
            mmkv.encode(KEY_LITE_MODE, value)
            isLiteModeState.value = value
        }

    var isLiquidGlass: Boolean
        get() = mmkv.decodeBool(KEY_LIQUID_GLASS, false)
        set(value) {
            mmkv.encode(KEY_LIQUID_GLASS, value)
            isLiquidGlassState.value = value
        }

    var colorMode: Int
        get() = mmkv.decodeInt(KEY_COLOR_MODE, MODE_SYSTEM)
        set(value) {
            mmkv.encode(KEY_COLOR_MODE, value)
            colorModeState.intValue = value
        }

    var isFirstRun: Boolean
        get() = mmkv.decodeBool(KEY_IS_FIRST_RUN, true)
        set(value) {
            mmkv.encode(KEY_IS_FIRST_RUN, value)
        }

    var sortOption: SortOption
        get() {
            val ordinal = mmkv.decodeInt(KEY_SORT_OPTION, SortOption.DEFAULT.ordinal)
            return SortOption.entries.getOrElse(ordinal) { SortOption.DEFAULT }
        }
        set(value) {
            mmkv.encode(KEY_SORT_OPTION, value.ordinal)
            sortOptionState.intValue = value.ordinal
        }

    var sortOrder: SortOrder
        get() {
            val ordinal = mmkv.decodeInt(KEY_SORT_ORDER, SortOrder.DESCENDING.ordinal)
            return SortOrder.entries.getOrElse(ordinal) { SortOrder.DESCENDING }
        }
        set(value) {
            mmkv.encode(KEY_SORT_ORDER, value.ordinal)
            sortOrderState.intValue = value.ordinal
        }
}

enum class SortOption {
    DEFAULT,
    DUE_DATE,
    FLAG,
    TITLE
}

enum class SortOrder {
    ASCENDING,
    DESCENDING
}
