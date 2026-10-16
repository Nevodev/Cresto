package com.nevoit.cresto.feature.screenextract

import com.nevoit.cresto.feature.settings.util.SettingsManager
import java.net.SocketException
import java.net.SocketTimeoutException

/**
 * Maps an extraction failure to a user facing message.
 *
 * @param defaultMessage Fallback text used when nothing more specific can be derived.
 * @param timeoutMessage Renders a timeout message that also tells the user how to react; it
 *                       receives the currently configured response timeout in seconds.
 */
fun Throwable.toScreenExtractErrorMessage(
    defaultMessage: String,
    timeoutMessage: (seconds: Int) -> String = { seconds ->
        "AI 请求超时（已等待 $seconds 秒）。可在 设置 > AI 中调大响应超时后重试。"
    }
): String {
    val message = localizedMessage.orEmpty()
    val timeoutSeconds = SettingsManager.aiResponseTimeoutSeconds
    return when {
        this is SocketTimeoutException -> timeoutMessage(timeoutSeconds)
        this is SocketException && message.contains("connection abort", ignoreCase = true) -> {
            "AI 请求连接中断，可能是网络不稳定或截图过大，请重试"
        }
        message.contains("connection abort", ignoreCase = true) -> {
            "AI 请求连接中断，可能是网络不稳定或截图过大，请重试"
        }
        message.contains("timeout", ignoreCase = true) -> timeoutMessage(timeoutSeconds)
        message.isNotBlank() -> message
        else -> defaultMessage
    }
}
