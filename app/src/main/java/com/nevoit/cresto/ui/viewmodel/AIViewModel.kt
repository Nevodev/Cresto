package com.nevoit.cresto.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.nevoit.cresto.data.utils.EventResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import kotlin.coroutines.cancellation.CancellationException

/**
 * UI状态 (State)
 * 只描述UI的持久外观，不携带一次性数据。
 * UI根据这个状态来决定是显示加载圈还是显示主内容。
 */
sealed interface UiState {
    object Initial : UiState
    object Loading : UiState
}

/**
 * 一次性事件 (Side Effect / Event)
 * 用于从ViewModel向UI发送需要被处理一次的命令。
 */
sealed interface AiSideEffect {
    data class ShowError(val message: String) : AiSideEffect
    data class ProcessSuccess(val response: EventResponse) : AiSideEffect
}


class AiViewModel : ViewModel() {

    // StateFlow用于管理持久的UI状态
    private val _uiState = MutableStateFlow<UiState>(UiState.Initial)
    val uiState = _uiState.asStateFlow()

    // 【核心改动】SharedFlow用于发送一次性的事件
    private val _sideEffect = MutableSharedFlow<AiSideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()

    private var generationJob: Job? = null

    // --- 省略了 getSystemInstruction 和 cleanJsonString 方法，它们保持不变 ---
    private fun getSystemInstruction(): String {
        val date = LocalDateTime.now().toString()
        return """
            现在时间是${date}。你是一个信息提取AI助手。你的任务是分析我发送给你的文本或图片内容，并严格按照以下要求提取待办事项信息，最后以指定的JSON格式返回。

提取规则:

1.  识别待办事项: 从内容中找出所有独立的待办事项。
2.  提取title(待办事项名称):
    *   准确提取每个待办事项的内容作为title，包含完整信息。
    *   如果原文中明确提及了具体的时间点或时间段（例如 "14:30" 或 "9:00-10:00"），必须将该时间信息完整地附加在title字符串的末尾。
3.  提取date(截止日期):
    *   提取每个待办事项对应的日期。
    *   必须将提取到的日期统一格式化为yyyy-MM-dd。忽略具体时间。
4.  如果无法提取任何日程，返回Error: No tasks

输出格式要求:

*   返回结果必须是一个结构完整的JSON对象。
*   JSON对象的最外层应包含一个quantity字段，其值为提取到的待办事项总数。
*   所有待办事项应收录在名为items的数组中。
*   数组中的每一个元素都是一个独立的对象，包含title和date两个字段，其值分别对应按上述规则提取和格式化后的结果。

示例:

如果输入内容为：“学生事务办公室通知：请各宿舍于10月20日开展宿舍卫生大扫除并确保用电安全。另外，学生会将于2025年10月21日上午7:50-8:20配合开展宿舍卫生安全检查，请同学们留人配合。”

你应该返回如下所示的JSON：
{
  "quantity": 2,
  "items": [
    {
      "title": "配合开展宿舍卫生安全检查 7:50-8:20",
      "date": "2025-10-21"
    },
    {
      "title": "开展宿舍卫生大扫除并确保用电安全",
      "date": "2025-10-20"
    }
  ]
}

从现在开始处理我发送给你的信息，并仅返回符合上述要求的JSON对象，不要包含任何额外的解释或文字。
         """.trimIndent()
    }

    private fun cleanJsonString(rawText: String): String {
        val trimmedText = rawText.trim()

        if (trimmedText.startsWith("```json") && trimmedText.endsWith("```")) {
            return trimmedText
                .removePrefix("```json")
                .removeSuffix("```")
                .trim()
        }

        if (trimmedText.startsWith("```") && trimmedText.endsWith("```")) {
            return trimmedText
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
        }

        return trimmedText
    }
    // -------------------------------------------------------------------------


    fun generateContent(prompt: String, apiKey: String) {
        generationJob?.cancel()

        generationJob = viewModelScope.launch {
            val generativeModel = GenerativeModel(
                modelName = "gemini-2.0-flash", // 建议使用最新的稳定模型
                apiKey = apiKey,
                systemInstruction = content(role = "system") { text(getSystemInstruction()) }
            )
            // 1. 设置加载状态
            _uiState.value = UiState.Loading
            try {
                val response = generativeModel.generateContent(prompt)
                val rawResponseText = response.text ?: ""
                val cleanedJsonText = cleanJsonString(rawResponseText)

                if (cleanedJsonText.isEmpty()) {
                    throw Exception("模型返回了空内容")
                }

                val eventResponse = Json.decodeFromString<EventResponse>(cleanedJsonText)

                // 【核心改动】成功时，发送ProcessSuccess事件
                _sideEffect.emit(AiSideEffect.ProcessSuccess(eventResponse))

            } catch (e: Exception) {
                // 如果不是主动取消协程导致的异常
                if (e !is CancellationException) {
                    // 【核心改动】失败时，发送ShowError事件
                    val errorMessage = e.localizedMessage ?: "发生未知错误，可能是JSON格式不正确"
                    _sideEffect.emit(AiSideEffect.ShowError(errorMessage))
                }
                // 如果是CancellationException，则静默处理，不发送错误事件
            } finally {
                // 【核心改动】无论成功、失败还是取消，最后都将状态重置为Initial
                _uiState.value = UiState.Initial
                generationJob = null
            }
        }
    }

    fun cancelRequest() {
        generationJob?.cancel()
    }

    fun clearState() {
        _uiState.value = UiState.Initial
    }
}