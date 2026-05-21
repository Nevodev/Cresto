package com.nevoit.cresto.data.todo

import com.nevoit.cresto.feature.settings.util.SettingsManager
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
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
                cached?.copy(message = "今日解读更新失败，已显示上次结果")
                    ?: InsightAdviceResult(
                        advice = localAdvice,
                        isAiGenerated = false,
                        generatedAt = now,
                        message = error.localizedMessage ?: "今日解读更新失败，已使用本地解读"
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
            throw IllegalStateException("模型返回的今日解读为空")
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
                summary = "有逾期任务，先重新安排",
                focus = "overdue",
                suggestions = listOf(
                    "有 ${insights.overdueTotal} 件任务已逾期",
                    "先重排仍重要的一件",
                    "不重要的可以归档或删除"
                )
            )

            PressureSource.BACKLOG -> InsightAdvice(
                summary = "积压偏多，适合先整理",
                focus = "backlog",
                suggestions = listOf(
                    "有 ${insights.stalePendingTotal} 件超过 7 天",
                    "先拆小最久的一件",
                    "不重要的可以归档或删除"
                )
            )

            PressureSource.TODAY -> InsightAdvice(
                summary = "今天偏满，先缩小范围",
                focus = "today",
                suggestions = listOf(
                    "今日还有 ${insights.todayRemaining} 件未完成",
                    "先处理最容易开始的一件",
                    "完成后再决定是否加码"
                )
            )

            PressureSource.WEEK -> InsightAdvice(
                summary = "本周节奏放缓，轻量恢复",
                focus = "week",
                suggestions = listOf(
                    "先恢复一个很小的动作",
                    "不要急着补完所有计划",
                    "保持今天可完成就好"
                )
            )

            PressureSource.NONE -> {
                if (insights.todayTotal > 0 && insights.todayRemaining == 0) {
                    InsightAdvice(
                        summary = "今日已清空，保持留白",
                        focus = "clear",
                        suggestions = listOf(
                            "今天 ${insights.todayCompleted}/${insights.todayTotal} 已完成",
                            "当前没有明显压力来源",
                            "不必强行新增任务"
                        )
                    )
                } else if (insights.pendingTotal == 0) {
                    InsightAdvice(
                        summary = "当前很清爽，轻松收尾",
                        focus = "clear",
                        suggestions = listOf(
                            "现在没有待处理任务",
                            "可以保留一点空白时间",
                            "需要时再添加下一件事"
                        )
                    )
                } else {
                    InsightAdvice(
                        summary = "当前压力较轻，顺手推进",
                        focus = "clear",
                        suggestions = listOf(
                            "还有 ${insights.pendingTotal} 件待处理",
                            "按顺手程度处理一件",
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
        你是 Cresto 的「任务压力雷达」文案生成器。
        Cresto 不是普通 Todo List，而是一个帮助用户看清今日负载、积压压力和任务节奏的温和待办工具。

        你的任务：
        - 根据输入的待办统计，生成顶部状态横幅 summary 和下方状态解读 suggestions。
        - summary 只负责给出一句整体状态判断。
        - suggestions 负责解释压力来源，并给出轻量、可执行的下一步。

        产品性格：
        - 温和、清醒、克制
        - 不催促、不责备、不制造羞耻感
        - 不鸡血、不焦虑、不夸张
        - 像一个冷静的任务整理助手，而不是监督者

        绝对不要写：
        - “你应该更自律”
        - “你太拖延了”
        - “必须马上完成”
        - “失败”
        - “严重”
        - “危险”
        - “保持良好状态”
        - “适时调整”
        - “关注未来计划”
        - “继续努力”
        - “加油”
        - “提升效率”

        数据使用规则：
        - 只能使用输入中提供的统计数据，不要编造任务名称、任务内容或用户状态。
        - 如果今日剩余为 0，不要建议“先完成一件任务”。
        - 如果逾期任务为 0，不要把逾期说成压力来源。
        - 如果超过 7 天未处理为 0，不要建议清理旧任务。
        - 如果全部待处理为 0，不要建议继续清理任务。
        - 如果压力指数很低，重点是留白、收尾、不要加码。
        - 如果压力指数中等，重点是先做最小的一件、避免堆积。
        - 如果压力指数较高，重点是缩减范围、重排、推迟、拆分或归档。
        - 如果主要压力来源是今天剩余任务，建议优先处理最小/最容易开始的一件。
        - 如果主要压力来源是逾期任务，建议先决定：重排、推迟、归档或删除。
        - 如果主要压力来源是老化任务，建议拆小、归档或删除一件。
        - 如果主要压力来源是本周节奏，建议恢复一个很小的动作，不要补偿式加码。
        - 如果暂无明显压力来源，建议保持空白时间或轻量规划，不要强行新增任务。

        文案要求：
        - summary：一句话，10 到 24 个中文字符，适合显示在顶部横幅。
        - summary 要包含「状态 + 建议」，例如“今日无压力，保持留白”。
        - suggestions：必须输出 3 条。
        - 每条 suggestion 不超过 30 个中文字符。
        - suggestions 尽量具体，优先引用数字。
        - 每条 suggestion 只能表达一个意思。
        - 不要长篇分析。
        - 不要 Markdown。
        - 不要解释你是 AI。
        - 不要输出 JSON 之外的任何文字。

        输出必须是严格 JSON：
        {
          "summary": "一句顶部状态横幅",
          "focus": "today | overdue | backlog | week | clear",
          "suggestions": [
            "状态解读 1",
            "状态解读 2",
            "状态解读 3"
          ]
        }
    """.trimIndent()
    }

    private fun userPrompt(
        insights: InsightsUiState,
        pressureIndex: PressureIndex
    ): String {
        return """
        请根据下面的待办统计，生成 Cresto 的今日状态文案。

        这些文案会显示在 Insights 页面：
        - summary 显示在顶部彩色横幅，只能是一句整体判断。
        - suggestions 显示在下方「状态解读」卡片，需要解释原因和给出行动建议。
        - 右上角已经显示了压力指数，所以 summary 不要重复写“压力指数 xx”。

        今日数据：
        - 今日到期总数：${insights.todayTotal}
        - 今日已完成：${insights.todayCompleted}
        - 今日剩余：${insights.todayRemaining}
        - 今日完成率：${formatPercentValue(insights.todayProgress)}%

        本周数据：
        - 本周到期总数：${insights.weekDueTotal}
        - 本周到期已完成：${insights.weekDueCompleted}
        - 本周完成率：${formatPercentValue(insights.weekDueProgress)}%
        - 本周实际完成总数：${insights.weekCompletedTotal}
        - 近 7 天完成趋势：${
            insights.weeklyCompletedTrend.joinToString(
                prefix = "[",
                postfix = "]"
            ) { it.count.toString() }
        }

        积压数据：
        - 全部待处理：${insights.pendingTotal}
        - 逾期任务：${insights.overdueTotal}
        - 超过 7 天未处理：${insights.stalePendingTotal}
        - 最久待处理天数：${insights.oldestPendingAgeDays ?: "暂无"}

        压力判断：
        - 压力指数：${pressureIndex.score}/100
        - 压力等级：${pressureLevelText(pressureIndex.score)}
        - 主要压力来源：${pressureSourceText(pressureIndex.primarySource)}

        生成策略：
        1. 先判断今天是否已经清空。
           - 如果今日总数 > 0 且今日剩余 = 0，summary 应强调“已完成、可收尾、保持留白”。
           - 不要建议继续完成今日任务。

        2. 再判断是否存在明显压力来源。
           - 逾期任务 > 0：优先建议重新安排仍重要的逾期任务。
           - 超过 7 天未处理 > 0：建议拆小、归档或删除其中一件。
           - 今日剩余较多：建议先处理最小的一件。
           - 本周完成率偏低：建议恢复一个小动作，不要补偿式加码。

        3. 如果压力指数为 0 到 24：
           - 语气应是清爽、收尾、留白。
           - 不要制造新的任务压力。
           - 可以建议轻量回顾或规划，但不要说“关注未来计划”。

        4. 如果压力指数为 25 到 49：
           - 语气应是轻负载、可推进。
           - 建议只做一件最容易开始的事。

        5. 如果压力指数为 50 到 74：
           - 语气应是偏满但可控。
           - 建议缩小范围，优先处理最关键的一件。

        6. 如果压力指数为 75 到 100：
           - 语气应是先降压。
           - 建议重排、推迟、拆分、归档，不要鼓励硬撑。

        focus 选择规则：
        - 如果主要压力来源是“今天剩余任务”，focus 用 "today"。
        - 如果主要压力来源是“逾期任务”，focus 用 "overdue"。
        - 如果主要压力来源是“老化任务”，focus 用 "backlog"。
        - 如果主要压力来源是“本周节奏”，focus 用 "week"。
        - 如果暂无明显压力来源，focus 用 "clear"。

        输出格式：
        {
          "summary": "10 到 24 个中文字符，状态 + 建议",
          "focus": "today | overdue | backlog | week | clear",
          "suggestions": [
            "不超过 30 个中文字符",
            "不超过 30 个中文字符",
            "不超过 30 个中文字符"
          ]
        }

        好的输出示例：
        {
          "summary": "今日已清空，保持留白",
          "focus": "clear",
          "suggestions": [
            "今天 2/2 已完成，可以收尾",
            "当前没有明显压力来源",
            "不必强行新增任务"
          ]
        }

        {
          "summary": "今天偏满，先缩小范围",
          "focus": "today",
          "suggestions": [
            "今日还有 4 件未完成",
            "先处理最容易开始的一件",
            "其余任务可完成后再决定"
          ]
        }

        {
          "summary": "积压偏多，适合先整理",
          "focus": "backlog",
          "suggestions": [
            "有 5 件任务等待超过 7 天",
            "先拆小最久的一件",
            "不重要的可以归档或删除"
          ]
        }

        坏的输出示例，不要模仿：
        - 保持良好状态
        - 关注未来计划
        - 适时调整
        - 继续努力提高效率
        - 你需要更自律
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
        private const val DEFAULT_AI_API_URL =
            "https://open.bigmodel.cn/api/paas/v4/chat/completions"
        private const val DEFAULT_AI_MODEL = "glm-4-flash"
        private val AUTO_REFRESH_COOLDOWN: Duration = Duration.ofMinutes(30)
        private val MANUAL_REFRESH_COOLDOWN: Duration = Duration.ofMinutes(5)
    }
}