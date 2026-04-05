package com.nevoit.cresto.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nevoit.cresto.data.utils.EventResponse
import com.nevoit.cresto.feature.settings.util.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import kotlin.coroutines.cancellation.CancellationException

sealed interface UiState {
    object Initial : UiState
    object Loading : UiState
}

sealed interface AiSideEffect {
    data class ShowError(val message: String) : AiSideEffect
    data class ProcessSuccess(val response: EventResponse) : AiSideEffect
}


class AiViewModel : ViewModel() {

    @Serializable
    private data class BigModelMessage(
        val role: String,
        val content: JsonElement
    )

    @Serializable
    private data class BigModelRequest(
        val model: String,
        val messages: List<BigModelMessage>,
        val temperature: Double = 0.1
    )

    @Serializable
    private data class BigModelChoice(
        val message: BigModelMessage? = null
    )

    @Serializable
    private data class BigModelResponse(
        val choices: List<BigModelChoice> = emptyList()
    )

    private val json = Json { ignoreUnknownKeys = true }

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
4.  提取subTasks(子任务，可选):
    *   仅当原文明确出现该待办事项下的步骤、拆分动作或子项时，才生成subTasks。
    *   不要臆造子任务，也不要为了凑格式强行拆分title。
    *   subTasks如出现，必须是字符串数组，每一项是简洁明确的子任务描述。
    *   若无明确子任务，可省略subTasks字段，或返回空数组[]。
    *   若同一待办包含“购买/准备/采购”等动作后跟并列物品（例如“买茄子、土豆、酱油和醋”），必须拆分为多个subTasks。
    *   并列物品即使未使用顿号，也应结合语义进行合理拆分（如“买茄子土豆酱油和醋”）。
    *   购买类subTasks建议保留动作动词，例如“买茄子”“买土豆”。
5.  如果无法提取任何日程，返回Error: No tasks

输出格式要求:

*   返回结果必须是一个结构完整的JSON对象。
*   JSON对象的最外层应包含一个quantity字段，其值为提取到的待办事项总数。
*   所有待办事项应收录在名为items的数组中。
*   数组中的每一个元素都是一个独立的对象。
*   title和date是必填字段。
*   subTasks是可选字段，仅在有明确子任务时返回。

示例:

如果输入内容为：“学生事务办公室通知：请各宿舍于10月20日开展宿舍卫生大扫除并确保用电安全。另外，学生会将于2025年10月21日上午7:50-8:20配合开展宿舍卫生安全检查，请同学们留人配合。”

你应该返回如下所示的JSON：
{
  "quantity": 2,
  "items": [
    {
      "title": "配合开展宿舍卫生安全检查 7:50-8:20",
      "date": "2025-10-21",
      "subTasks": ["宿舍留人配合检查"]
    },
    {
      "title": "开展宿舍卫生大扫除并确保用电安全",
      "date": "2025-10-20"
    }
  ]
}

如果输入内容为：“今天出去买菜，买茄子土豆酱油和醋”

你应该返回如下所示的JSON：
{
  "quantity": 1,
  "items": [
    {
      "title": "出去买菜",
      "date": "2026-04-05",
      "subTasks": ["买茄子", "买土豆", "买酱油", "买醋"]
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


    fun generateContent(prompt: String) {
        generationJob?.cancel()

        generationJob = viewModelScope.launch {
            // 1. 设置加载状态
            _uiState.value = UiState.Loading
            try {
                val apiKey = SettingsManager.aiApiKey.trim()
                val endpoint = resolveEndpoint(SettingsManager.aiApiUrl.trim())
                val model = SettingsManager.aiTextModel.trim().ifEmpty { "glm-4-flash" }

                if (apiKey.isBlank()) {
                    throw IllegalStateException("请先在设置中填写 API Key")
                }

                if (prompt.isBlank()) {
                    throw IllegalArgumentException("请输入要提取的文本")
                }

                val rawResponseText = withContext(Dispatchers.IO) {
                    requestBigModel(
                        endpoint = endpoint,
                        apiKey = apiKey,
                        model = model,
                        messages = listOf(
                            BigModelMessage(
                                role = "system",
                                content = JsonPrimitive(getSystemInstruction())
                            ),
                            BigModelMessage(role = "user", content = JsonPrimitive(prompt))
                        )
                    )
                }
                val assistantText = extractAssistantContent(rawResponseText)
                val cleanedJsonText = cleanJsonString(assistantText)

                if (cleanedJsonText.isEmpty()) {
                    throw Exception("模型返回了空内容")
                }

                val eventResponse = parseEventResponse(cleanedJsonText)

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

    fun generateContentFromImage(imageDataUrl: String, prompt: String = "请提取图片中的待办事项") {
        generationJob?.cancel()

        generationJob = viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val apiKey = SettingsManager.aiApiKey.trim()
                val endpoint = resolveEndpoint(SettingsManager.aiApiUrl.trim())
                val model = SettingsManager.aiMultimodalModel.trim().ifEmpty { "glm-4-flash" }

                if (apiKey.isBlank()) {
                    throw IllegalStateException("请先在设置中填写 API Key")
                }

                if (imageDataUrl.isBlank()) {
                    throw IllegalArgumentException("未读取到有效图片")
                }

                val userContent = buildJsonArray {
                    add(
                        buildJsonObject {
                            put("type", "text")
                            put("text", prompt)
                        }
                    )
                    add(
                        buildJsonObject {
                            put("type", "image_url")
                            putJsonObject("image_url") {
                                put("url", imageDataUrl)
                            }
                        }
                    )
                }

                val rawResponseText = withContext(Dispatchers.IO) {
                    requestBigModel(
                        endpoint = endpoint,
                        apiKey = apiKey,
                        model = model,
                        messages = listOf(
                            BigModelMessage(
                                role = "system",
                                content = JsonPrimitive(getSystemInstruction())
                            ),
                            BigModelMessage(role = "user", content = userContent)
                        )
                    )
                }

                val assistantText = extractAssistantContent(rawResponseText)
                val cleanedJsonText = cleanJsonString(assistantText)
                if (cleanedJsonText.isEmpty()) {
                    throw Exception("模型返回了空内容")
                }

                val eventResponse = parseEventResponse(cleanedJsonText)
                _sideEffect.emit(AiSideEffect.ProcessSuccess(eventResponse))
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

    fun clearState() {
        _uiState.value = UiState.Initial
    }

    private fun parseEventResponse(rawJsonText: String): EventResponse {
        val normalizedJsonText = normalizeEventResponseJson(rawJsonText)
        return json.decodeFromString(normalizedJsonText)
    }

    private fun normalizeEventResponseJson(rawJsonText: String): String {
        val rootObject = json.parseToJsonElement(rawJsonText).jsonObject
        val normalizedItems = rootObject["items"]
            ?.toJsonArrayOrNull()
            .orEmpty()
            .mapNotNull { itemElement ->
                val itemObject = itemElement as? JsonObject ?: return@mapNotNull null
                val title = itemObject["title"].asTrimmedText()
                val date = itemObject["date"].asTrimmedText()

                if (title.isEmpty() || date.isEmpty()) {
                    return@mapNotNull null
                }

                val subTasksElement = itemObject["subTasks"]
                    ?: itemObject["subtasks"]
                    ?: itemObject["sub_tasks"]
                val subTasks = extractSubTaskTexts(subTasksElement)

                buildJsonObject {
                    put("title", title)
                    put("date", date)
                    put("subTasks", buildJsonArray {
                        subTasks.forEach { add(JsonPrimitive(it)) }
                    })
                }
            }

        val normalizedRoot = buildJsonObject {
            put("quantity", normalizedItems.size)
            put("items", JsonArray(normalizedItems))
        }
        return json.encodeToString(JsonObject.serializer(), normalizedRoot)
    }

    private fun JsonElement?.asTrimmedText(): String {
        return (this as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()
    }

    private fun JsonElement?.toJsonArrayOrNull(): JsonArray? {
        return this as? JsonArray
    }

    private fun extractSubTaskTexts(element: JsonElement?): List<String> {
        return when (element) {
            null -> emptyList()
            is JsonArray -> element.flatMap(::extractSubTaskTexts)
            is JsonPrimitive -> {
                element.contentOrNull?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?.let(::listOf)
                    ?: emptyList()
            }

            is JsonObject -> {
                listOf("title", "text", "description", "name")
                    .firstNotNullOfOrNull { key ->
                        (element[key] as? JsonPrimitive)?.contentOrNull?.trim()
                    }
                    ?.takeIf { it.isNotEmpty() }
                    ?.let(::listOf)
                    ?: emptyList()
            }

        }.distinct()
    }

    private fun resolveEndpoint(rawUrl: String): String {
        val defaultEndpoint = "https://open.bigmodel.cn/api/paas/v4/chat/completions"
        if (rawUrl.isBlank()) return defaultEndpoint

        val normalized = rawUrl.trim().trimEnd('/')
        return if (normalized.endsWith("/chat/completions")) {
            normalized
        } else {
            "$normalized/api/paas/v4/chat/completions"
        }
    }

    private fun requestBigModel(
        endpoint: String,
        apiKey: String,
        model: String,
        messages: List<BigModelMessage>
    ): String {
        val requestBody = json.encodeToString(
            BigModelRequest(
                model = model,
                messages = messages
            )
        )

        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 30_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $apiKey")
        }

        return try {
            connection.outputStream.use { output ->
                output.write(requestBody.toByteArray(Charsets.UTF_8))
            }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream.readAllText()

            if (code !in 200..299) {
                throw IllegalStateException("请求失败($code): $text")
            }

            text
        } finally {
            connection.disconnect()
        }
    }

    private fun InputStream?.readAllText(): String {
        if (this == null) return ""
        return bufferedReader().use { it.readText() }
    }

    private fun extractAssistantContent(rawResponse: String): String {
        val decoded = json.decodeFromString<BigModelResponse>(rawResponse)
        val content = decoded.choices.firstOrNull()?.message?.content ?: return ""
        return if (content is JsonPrimitive) content.content.trim() else content.toString().trim()
    }
}