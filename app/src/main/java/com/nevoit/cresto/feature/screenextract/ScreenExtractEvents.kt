package com.nevoit.cresto.feature.screenextract

import com.nevoit.cresto.data.utils.EventItem
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

enum class AiExtractSource {
    Screen,
    Share,
    InApp
}

data class PendingAiTodos(
    val items: List<EventItem>,
    val source: AiExtractSource
)

object ScreenExtractEvents {
    const val EXTRA_SHOW_ERROR_DIALOG = "com.nevoit.cresto.extra.SHOW_SCREEN_EXTRACT_ERROR_DIALOG"
    const val EXTRA_ERROR_MESSAGE = "com.nevoit.cresto.extra.SCREEN_EXTRACT_ERROR_MESSAGE"

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errors = _errors.asSharedFlow()

    private val _pendingTodos = MutableStateFlow<PendingAiTodos?>(null)
    val pendingTodos = _pendingTodos.asStateFlow()

    private val mainUiOpen = AtomicBoolean(false)

    fun emitError(message: String) {
        _errors.tryEmit(message)
    }

    fun setMainUiOpen(isOpen: Boolean) {
        mainUiOpen.set(isOpen)
    }

    fun isMainUiOpen(): Boolean = mainUiOpen.get()

    fun emitPendingTodos(items: List<EventItem>, source: AiExtractSource): Boolean {
        _pendingTodos.value = PendingAiTodos(items, source)
        return true
    }

    fun clearPendingTodos() {
        _pendingTodos.value = null
    }
}
