package com.nevoit.cresto.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nevoit.cresto.data.utils.EventResponse
import com.nevoit.cresto.feature.screenextract.AiTodoExtractor
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

sealed interface UiState {
    object Initial : UiState
    object Loading : UiState
}

sealed interface AiSideEffect {
    data class ShowError(val message: String) : AiSideEffect
    data class ProcessSuccess(val response: EventResponse) : AiSideEffect
}

class AiViewModel(
    private val aiTodoExtractor: AiTodoExtractor = AiTodoExtractor()
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Initial)
    val uiState = _uiState.asStateFlow()

    private val _sideEffect = MutableSharedFlow<AiSideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()

    private var generationJob: Job? = null

    fun generateContent(prompt: String) {
        launchExtraction {
            aiTodoExtractor.extractFromText(prompt)
        }
    }

    fun generateContentFromImage(imageDataUrl: String, prompt: String = "请提取图片中的待办事项") {
        launchExtraction {
            aiTodoExtractor.extractFromImage(imageDataUrl, prompt)
        }
    }

    fun cancelRequest() {
        generationJob?.cancel()
    }

    fun clearState() {
        _uiState.value = UiState.Initial
    }

    private fun launchExtraction(block: suspend () -> EventResponse) {
        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                _sideEffect.emit(AiSideEffect.ProcessSuccess(block()))
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    val errorMessage = e.localizedMessage ?: "发生未知错误，可能是JSON格式不正确"
                    _sideEffect.emit(AiSideEffect.ShowError(errorMessage))
                }
            } finally {
                _uiState.value = UiState.Initial
                generationJob = null
            }
        }
    }
}
