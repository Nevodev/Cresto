package com.nevoit.cresto.feature.settings.util

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList

object AppLocaleManager {
    const val SYSTEM = ""
    const val ENGLISH = "en"
    const val SIMPLIFIED_CHINESE = "zh-CN"

    fun getLanguageTag(context: Context): String {
        return context.getSystemService(LocaleManager::class.java)
            .applicationLocales
            .toLanguageTags()
    }

    fun setLanguageTag(context: Context, tag: String) {
        context.getSystemService(LocaleManager::class.java)
            .applicationLocales = if (tag == SYSTEM) {
            LocaleList.getEmptyLocaleList()
        } else {
            LocaleList.forLanguageTags(tag)
        }
    }
}
