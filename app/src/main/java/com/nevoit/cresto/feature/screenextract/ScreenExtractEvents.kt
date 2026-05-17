package com.nevoit.cresto.feature.screenextract

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object ScreenExtractEvents {
    const val EXTRA_SHOW_ERROR_DIALOG = "com.nevoit.cresto.extra.SHOW_SCREEN_EXTRACT_ERROR_DIALOG"
    const val EXTRA_ERROR_MESSAGE = "com.nevoit.cresto.extra.SCREEN_EXTRACT_ERROR_MESSAGE"

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errors = _errors.asSharedFlow()

    fun emitError(message: String) {
        _errors.tryEmit(message)
    }
}
