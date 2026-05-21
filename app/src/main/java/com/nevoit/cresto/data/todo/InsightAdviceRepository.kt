package com.nevoit.cresto.data.todo

import com.nevoit.cresto.feature.settings.util.SettingsManager
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Serializable
data class InsightAdvice(
    val summary: String,
    val focus: String,
    val suggestions: List<String>
)

data class InsightAdviceResult(
    val advice: InsightAdvice,
    val isAiGenerated: Boolean,
    val generatedAt: LocalDateTime?,
    val message: String? = null
)

data class InsightAdviceRefreshDecision(
    val shouldRefresh: Boolean,
    val message: String? = null
)

class InsightAdviceRepository(
    private val mmkv: MMKV = MMKV.defaultMMKV()
) {
    @Serializable
    private data class ChatMessage(
        val role: String,
        val content: JsonElement
    )

    @Serializable
    private data class ChatRequest(
        val model: String,
        val messages: List<ChatMessage>,
        val temperature: Double = 0.2
    )

    @Serializable
    private data class ChatChoice(
        val message: ChatMessage? = null
    )

    @Serializable
    private data class ChatResponse(
        val choices: List<ChatChoice> = emptyList()
    )

    @Serializable
    private data class AiAdviceResponse(
        val summary: String = "",
        val focus: String = "clear",
        val suggestions: List<String> = emptyList()
    )

    @Serializable
    private data class AdviceCache(
        val date: String,
        val generatedAt: String,
        val pressureScore: Int,
        val pressureLevel: String,
        val pressureSource: String,
        val todayRemaining: Int,
        val overdueTotal: Int,
        val stalePendingTotal: Int,
        val advice: InsightAdvice,
        val isAiGenerated: Boolean
    )

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    fun previewAdvice(
        insights: InsightsUiState,
        pressureIndex: PressureIndex
    ): InsightAdviceResult {
        if (!insights.hasAnyData) {
            return InsightAdviceResult(
                advice = buildEmptyAdvice(),
                isAiGenerated = false,
                generatedAt = null
            )
        }

        val cache = readCache()
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        if (cache != null && cache.date == today) {
            return cache.toResult()
        }

        return InsightAdviceResult(
            advice = buildLocalAdvice(insights, pressureIndex),
            isAiGenerated = false,
            generatedAt = null
        )
    }

    fun shouldRefresh(
        insights: InsightsUiState,
        pressureIndex: PressureIndex,
        manual: Boolean
    ): InsightAdviceRefreshDecision {
        if (!insights.isReady || !insights.hasAnyData) {
            return InsightAdviceRefreshDecision(shouldRefresh = false)
        }

        val cache = readCache() ?: return InsightAdviceRefreshDecision(shouldRefresh = true)
        val now = LocalDateTime.now()
        val generatedAt = cache.generatedAt.toLocalDateTimeOrNull()
            ?: return InsightAdviceRefreshDecision(shouldRefresh = true)
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

        if (cache.date != today) {
            return InsightAdviceRefreshDecision(shouldRefresh = true)
        }

        val hasSignificantChange = hasSignificantChange(
            cache = cache,
            insights = insights,
            pressureIndex = pressureIndex
        )
        if (hasSignificantChange) {
            return InsightAdviceRefreshDecision(shouldRefresh = true)
        }

        if (manual) {
            val elapsed = Duration.between(generatedAt, now)
            return if (elapsed < MANUAL_REFRESH_COOLDOWN) {
                InsightAdviceRefreshDecision(
                    shouldRefresh = false,
                    message = "刚刚更新过，稍后再试"
                )
            } else {
                InsightAdviceRefreshDecision(shouldRefresh = true)
            }
        }

        val elapsed = Duration.between(generatedAt, now)
        if (elapsed < AUTO_REFRESH_COOLDOWN) {
            return InsightAdviceRefreshDecision(shouldRefresh = false)
        }

        return InsightAdviceRefreshDecision(
            shouldRefresh = !cache.isAiGenerated && SettingsManager.aiApiKey.isNotBlank()
        )
    }

    suspend fun refreshAdvice(
        insights: InsightsUiState,
        pressureIndex: PressureIndex
    ): InsightAdviceResult = withContext(Dispatchers.IO) {
        val now = LocalDateTime.now()
        val localAdvice = buildLocalAdvice(insights, pressureIndex)

        if (SettingsManager.aiApiKey.isBlank()) {
            return@withContext InsightAdviceResult(
                advice = localAdvice,
                isAiGenerated = false,
                generatedAt = now,
                message = "未配置 AI，已使用本地解读"
            )
        }

        runCatching {
            requestAiAdvice(insights, pressureIndex)
        }.fold(
            onSuccess = { advice ->
                val result = InsightAdviceResult(
                    advice = advice,
                    isAiGenerated = true,
                    generatedAt = now
                )
                writeCache(insights, pressureIndex, result)
                result
            },
            onFailure = { error ->
                val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                val cached = readCache()?.takeIf { it.date == today }?.toResult()
                cached?.copy(message = "压力解读更新失败，已显示上次结果")
                    ?: InsightAdviceResult(
                        advice = localAdvice,
                        isAiGenerated = false,
                        generatedAt = now,
                        message = error.localizedMessage ?: "压力解读更新失败，已使用本地解读"
                    ).also { writeCache(insights, pressureIndex, it) }
            }
        )
    }

    private fun requestAiAdvice(
        insights: InsightsUiState,
        pressureIndex: PressureIndex
    ): InsightAdvice {
        val endpoint = resolveEndpoint(SettingsManager.aiApiUrl.trim())
        val model = SettingsManager.aiTextModel.trim().ifEmpty { DEFAULT_AI_MODEL }
        val rawResponse = requestChatCompletion(
            endpoint = endpoint,
            apiKey = SettingsManager.aiApiKey.trim(),
            model = model,
            messages = listOf(
                ChatMessage("system", JsonPrimitive(systemPrompt())),
                ChatMessage("user", JsonPrimitive(userPrompt(insights, pressureIndex)))
            )
        )
        return parseAdvice(rawResponse)
    }

    private fun parseAdvice(rawResponse: String): InsightAdvice {
        val assistantText = extractAssistantContent(rawResponse)
        val decoded = json.decodeFromString<AiAdviceResponse>(cleanJsonString(assistantText))
        val suggestions = decoded.suggestions
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .take(3)

        if (decoded.summary.isBlank() || suggestions.isEmpty()) {
            throw IllegalStateException("模型返回的压力解读为空")
        }

        return InsightAdvice(
            summary = decoded.summary.trim(),
            focus = normalizeFocus(decoded.focus),
            suggestions = suggestions
        )
    }

    private fun buildLocalAdvice(
        insights: InsightsUiState,
        pressureIndex: PressureIndex
    ): InsightAdvice {
        if (!insights.hasAnyData) {
            return buildEmptyAdvice()
        }

        return when (pressureIndex.primarySource) {
            PressureSource.OVERDUE -> InsightAdvice(
                summary = "压力主要来自逾期任务",
                focus = "overdue",
                suggestions = listOf(
                    "先重排仍然重要的一项",
                    "不重要的逾期可以归档",
                    "今天不要一次清太多"
                )
            )

            PressureSource.BACKLOG -> InsightAdvice(
                summary = "有些任务已经等待太久",
                focus = "backlog",
                suggestions = listOf(
                    "先拆小最久的一项",
                    "不再重要的任务可以删除",
                    "只保留下一步动作"
                )
            )

            PressureSource.TODAY -> InsightAdvice(
                summary = "今天任务量有点占注意力",
                focus = "today",
                suggestions = listOf(
                    "先完成一件最小的任务",
                    "把大任务拆成下一步",
                    "完成后再决定是否加码"
                )
            )

            PressureSource.WEEK -> InsightAdvice(
                summary = "本周节奏适合轻轻恢复",
                focus = "week",
                suggestions = listOf(
                    "先恢复一个很小的动作",
                    "不要急着补完所有计划",
                    "保持今天可完成就好"
                )
            )

            PressureSource.NONE -> {
                if (insights.todayRemaining == 0) {
                    InsightAdvice(
                        summary = "今天已经很清爽",
                        focus = "clear",
                        suggestions = listOf(
                            "保护一段空白时间",
                            "不用强行新增任务",
                            "保持现在的节奏就好"
                        )
                    )
                } else {
                    InsightAdvice(
                        summary = "当前压力比较轻",
                        focus = "clear",
                        suggestions = listOf(
                            "按顺手程度处理一项",
                            "先做容易开始的任务",
                            "保持节奏比加速更重要"
                        )
                    )
                }
            }
        }
    }

    private fun buildEmptyAdvice(): InsightAdvice {
        return InsightAdvice(
            summary = "还没有足够的任务数据",
            focus = "clear",
            suggestions = listOf(
                "先记录今天最想处理的一件事",
                "完成几项后会生成更准确解读",
                "不用急着填满计划"
            )
        )
    }

    private fun writeCache(
        insights: InsightsUiState,
        pressureIndex: PressureIndex,
        result: InsightAdviceResult
    ) {
        val generatedAt = result.generatedAt ?: LocalDateTime.now()
        val cache = AdviceCache(
            date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            generatedAt = generatedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            pressureScore = pressureIndex.score,
            pressureLevel = pressureLevelOf(pressureIndex.score).name,
            pressureSource = pressureIndex.primarySource.name,
            todayRemaining = insights.todayRemaining,
            overdueTotal = insights.overdueTotal,
            stalePendingTotal = insights.stalePendingTotal,
            advice = result.advice,
            isAiGenerated = result.isAiGenerated
        )
        mmkv.encode(KEY_INSIGHT_ADVICE_CACHE, json.encodeToString(cache))
    }

    private fun readCache(): AdviceCache? {
        val raw = mmkv.decodeString(KEY_INSIGHT_ADVICE_CACHE, null) ?: return null
        return runCatching { json.decodeFromString<AdviceCache>(raw) }.getOrNull()
    }

    private fun hasSignificantChange(
        cache: AdviceCache,
        insights: InsightsUiState,
        pressureIndex: PressureIndex
    ): Boolean {
        val pressureLevel = pressureLevelOf(pressureIndex.score).name
        return cache.pressureLevel != pressureLevel ||
            cache.pressureSource != pressureIndex.primarySource.name ||
            kotlin.math.abs(cache.todayRemaining - insights.todayRemaining) >= 2 ||
            cache.overdueTotal != insights.overdueTotal ||
            cache.stalePendingTotal != insights.stalePendingTotal
    }

    private fun AdviceCache.toResult(): InsightAdviceResult {
        return InsightAdviceResult(
            advice = advice,
            isAiGenerated = isAiGenerated,
            generatedAt = generatedAt.toLocalDateTimeOrNull()
        )
    }

    private fun systemPrompt(): String {
        return """
            你是 Cresto 的压力解读助手，负责根据用户的待办统计生成温和、清醒、可执行的建议。

            你的目标不是催促用户完成更多任务，而是帮助用户看清当前任务压力来自哪里，并给出一个轻量的下一步行动。

            语气要求：
            - 温和、克制、清醒
            - 不责备用户
            - 不使用鸡血、焦虑、夸张表达
            - 不说“你太拖延了”“你应该更自律”
            - 不输出长篇分析
            - 不提到你是 AI
            - 不编造输入数据里没有的信息

            建议原则：
            - 如果今天剩余任务较多，建议先完成最小的一项
            - 如果有逾期任务，建议先重新安排仍然重要的逾期任务
            - 如果有超过 7 天未处理的任务，建议拆分、归档或删除
            - 如果今天已经清空，建议保护空白时间，不要强行新增任务
            - 如果本周节奏偏慢，建议恢复一个很小的动作
            - 如果压力较高，优先建议减压，而不是增加计划

            输出必须是简体中文。
            输出必须是严格 JSON，不要 Markdown，不要额外解释。
        """.trimIndent()
    }

    private fun userPrompt(
        insights: InsightsUiState,
        pressureIndex: PressureIndex
    ): String {
        return """
            请根据下面的待办统计，生成 Cresto 的压力解读。

            统计数据：
            - 今日到期总数：${insights.todayTotal}
            - 今日已完成：${insights.todayCompleted}
            - 今日剩余：${insights.todayRemaining}
            - 今日完成率：${formatPercentValue(insights.todayProgress)}%
            - 本周到期总数：${insights.weekDueTotal}
            - 本周到期已完成：${insights.weekDueCompleted}
            - 本周完成率：${formatPercentValue(insights.weekDueProgress)}%
            - 本周实际完成总数：${insights.weekCompletedTotal}
            - 全部待处理：${insights.pendingTotal}
            - 逾期任务：${insights.overdueTotal}
            - 超过 7 天未处理：${insights.stalePendingTotal}
            - 最久待处理天数：${insights.oldestPendingAgeDays ?: "暂无"}
            - 压力指数：${pressureIndex.score}/100
            - 压力等级：${pressureLevelText(pressureIndex.score)}
            - 主要压力来源：${pressureSourceText(pressureIndex.primarySource)}
            - 近 7 天完成趋势：${insights.weeklyCompletedTrend.joinToString(prefix = "[", postfix = "]") { it.count.toString() }}

            请输出以下 JSON 结构：

            {
              "summary": "一句整体判断，不超过 32 个中文字符",
              "focus": "today | overdue | backlog | week | clear",
              "suggestions": [
                "建议 1，不超过 28 个中文字符",
                "建议 2，不超过 28 个中文字符",
                "建议 3，不超过 28 个中文字符"
              ]
            }

            要求：
            - suggestions 数量为 1 到 3 条
            - 每条建议必须具体、轻量、可执行
            - 不要建议用户一次处理太多任务
            - 不要输出输入数据里没有的任务名称
            - 如果压力指数低，建议保持节奏或保护空白时间
            - 如果压力指数高，建议先减压、重排、拆分或归档
        """.trimIndent()
    }

    private fun requestChatCompletion(
        endpoint: String,
        apiKey: String,
        model: String,
        messages: List<ChatMessage>
    ): String {
        val requestBody = json.encodeToString(
            ChatRequest(
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

    private fun resolveEndpoint(rawUrl: String): String {
        if (rawUrl.isBlank()) return DEFAULT_AI_API_URL

        val normalized = rawUrl.trim().trimEnd('/')
        return if (normalized.endsWith("/chat/completions")) {
            normalized
        } else {
            "$normalized/v1/chat/completions"
        }
    }

    private fun extractAssistantContent(rawResponse: String): String {
        val decoded = json.decodeFromString<ChatResponse>(rawResponse)
        val content = decoded.choices.firstOrNull()?.message?.content ?: return ""
        return if (content is JsonPrimitive) content.content.trim() else content.toString().trim()
    }

    private fun cleanJsonString(rawText: String): String {
        val trimmedText = rawText.trim()
        if (trimmedText.startsWith("```json") && trimmedText.endsWith("```")) {
            return trimmedText.removePrefix("```json").removeSuffix("```").trim()
        }

        if (trimmedText.startsWith("```") && trimmedText.endsWith("```")) {
            return trimmedText.removePrefix("```").removeSuffix("```").trim()
        }

        return trimmedText
    }

    private fun InputStream?.readAllText(): String {
        if (this == null) return ""
        return bufferedReader().use { it.readText() }
    }

    private fun normalizeFocus(value: String): String {
        return when (value.trim().lowercase()) {
            "today", "overdue", "backlog", "week", "clear" -> value.trim().lowercase()
            else -> "clear"
        }
    }

    private fun pressureLevelText(score: Int): String {
        return when (pressureLevelOf(score)) {
            PressureLevel.HIGH -> "过载"
            PressureLevel.MEDIUM -> "偏满"
            PressureLevel.LOW -> "轻量"
            PressureLevel.CLEAR -> "清爽"
        }
    }

    private fun pressureSourceText(source: PressureSource): String {
        return when (source) {
            PressureSource.TODAY -> "今天剩余任务"
            PressureSource.OVERDUE -> "逾期任务"
            PressureSource.BACKLOG -> "老化任务"
            PressureSource.WEEK -> "本周节奏"
            PressureSource.NONE -> "暂无明显压力来源"
        }
    }

    private fun formatPercentValue(progress: Float): Int {
        return (progress.coerceIn(0f, 1f) * 100).toInt()
    }

    private fun String.toLocalDateTimeOrNull(): LocalDateTime? {
        return runCatching { LocalDateTime.parse(this, DateTimeFormatter.ISO_LOCAL_DATE_TIME) }
            .getOrNull()
    }

    private companion object {
        private const val KEY_INSIGHT_ADVICE_CACHE = "insight_advice_cache"
        private const val DEFAULT_AI_API_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions"
        private const val DEFAULT_AI_MODEL = "glm-4-flash"
        private val AUTO_REFRESH_COOLDOWN: Duration = Duration.ofMinutes(30)
        private val MANUAL_REFRESH_COOLDOWN: Duration = Duration.ofMinutes(5)
    }
}