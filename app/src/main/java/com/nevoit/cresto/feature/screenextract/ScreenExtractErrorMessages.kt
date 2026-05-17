package com.nevoit.cresto.feature.screenextract

import java.net.SocketException
import java.net.SocketTimeoutException

fun Throwable.toScreenExtractErrorMessage(defaultMessage: String): String {
    val message = localizedMessage.orEmpty()
    return when {
        this is SocketTimeoutException -> "AI 请求超时，请检查网络后重试"
        this is SocketException && message.contains("connection abort", ignoreCase = true) -> {
            "AI 请求连接中断，可能是网络不稳定或截图过大，请重试"
        }
        message.contains("connection abort", ignoreCase = true) -> {
            "AI 请求连接中断，可能是网络不稳定或截图过大，请重试"
        }
        message.contains("timeout", ignoreCase = true) -> "AI 请求超时，请检查网络后重试"
        message.isNotBlank() -> message
        else -> defaultMessage
    }
}
