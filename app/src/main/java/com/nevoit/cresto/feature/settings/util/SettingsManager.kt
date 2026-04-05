package com.nevoit.cresto.feature.settings.util

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.toArgb
import com.nevoit.glasense.theme.Blue500
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
    private const val KEY_THEME_PRIMARY_COLOR = "theme_primary_color"
    private const val KEY_DUE_TODAY_MARKER = "due_today_marker_enabled"
    private const val KEY_OVERDUE_MARKER = "overdue_marker_enabled"
    private const val KEY_AI_API_URL = "ai_api_url"
    private const val KEY_AI_API_KEY = "ai_api_key"
    private const val KEY_AI_TEXT_MODEL = "ai_text_model"
    private const val KEY_AI_MULTIMODAL_MODEL = "ai_multimodal_model"
    private const val DEFAULT_AI_API_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions"
    private const val DEFAULT_AI_MODEL = "glm-4-flash"

    private val defaultThemePrimaryColor = Blue500.toArgb()

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
    val themePrimaryColorState =
        mutableIntStateOf(mmkv.decodeInt(KEY_THEME_PRIMARY_COLOR, defaultThemePrimaryColor))
    val isDueTodayMarkerState = mutableStateOf(mmkv.decodeBool(KEY_DUE_TODAY_MARKER, true))
    val isOverdueMarkerState = mutableStateOf(mmkv.decodeBool(KEY_OVERDUE_MARKER, true))
    val aiApiUrlState = mutableStateOf(resolveAiApiUrl(mmkv.decodeString(KEY_AI_API_URL, DEFAULT_AI_API_URL)))
    val aiApiKeyState = mutableStateOf(mmkv.decodeString(KEY_AI_API_KEY, "") ?: "")
    val aiTextModelState = mutableStateOf(mmkv.decodeString(KEY_AI_TEXT_MODEL, DEFAULT_AI_MODEL) ?: DEFAULT_AI_MODEL)
    val aiMultimodalModelState =
        mutableStateOf(mmkv.decodeString(KEY_AI_MULTIMODAL_MODEL, DEFAULT_AI_MODEL) ?: DEFAULT_AI_MODEL)

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

    var themePrimaryColor: Int
        get() = mmkv.decodeInt(KEY_THEME_PRIMARY_COLOR, defaultThemePrimaryColor)
        set(value) {
            mmkv.encode(KEY_THEME_PRIMARY_COLOR, value)
            themePrimaryColorState.intValue = value
        }

    var isDueTodayMarker: Boolean
        get() = mmkv.decodeBool(KEY_DUE_TODAY_MARKER, true)
        set(value) {
            mmkv.encode(KEY_DUE_TODAY_MARKER, value)
            isDueTodayMarkerState.value = value
        }

    var isOverdueMarker: Boolean
        get() = mmkv.decodeBool(KEY_OVERDUE_MARKER, true)
        set(value) {
            mmkv.encode(KEY_OVERDUE_MARKER, value)
            isOverdueMarkerState.value = value
        }

    var aiApiUrl: String
        get() = resolveAiApiUrl(mmkv.decodeString(KEY_AI_API_URL, DEFAULT_AI_API_URL))
        set(value) {
            mmkv.encode(KEY_AI_API_URL, value)
            aiApiUrlState.value = value
        }

    private fun resolveAiApiUrl(rawValue: String?): String {
        return rawValue?.trim().takeUnless { it.isNullOrEmpty() } ?: DEFAULT_AI_API_URL
    }

    var aiApiKey: String
        get() = mmkv.decodeString(KEY_AI_API_KEY, "") ?: ""
        set(value) {
            mmkv.encode(KEY_AI_API_KEY, value)
            aiApiKeyState.value = value
        }

    var aiTextModel: String
        get() = mmkv.decodeString(KEY_AI_TEXT_MODEL, DEFAULT_AI_MODEL) ?: DEFAULT_AI_MODEL
        set(value) {
            mmkv.encode(KEY_AI_TEXT_MODEL, value)
            aiTextModelState.value = value
        }

    var aiMultimodalModel: String
        get() = mmkv.decodeString(KEY_AI_MULTIMODAL_MODEL, DEFAULT_AI_MODEL) ?: DEFAULT_AI_MODEL
        set(value) {
            mmkv.encode(KEY_AI_MULTIMODAL_MODEL, value)
            aiMultimodalModelState.value = value
        }

    fun resetAiSettingsToDefaults() {
        aiApiUrl = DEFAULT_AI_API_URL
        aiApiKey = ""
        aiTextModel = DEFAULT_AI_MODEL
        aiMultimodalModel = DEFAULT_AI_MODEL
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
