package com.nevoit.cresto.feature.screenextract

import java.util.Base64

private const val DEFAULT_MAX_SCREENSHOT_BYTES = 5 * 1024 * 1024

fun ByteArray.toPngDataUrl(maxSizeBytes: Int = DEFAULT_MAX_SCREENSHOT_BYTES): String {
    require(isNotEmpty()) { "截图为空" }
    require(size <= maxSizeBytes) { "截图过大，请重试" }

    val base64 = Base64.getEncoder().encodeToString(this)
    return "data:image/png;base64,$base64"
}
