package com.nevoit.cresto.feature.shareextract

import android.content.Context
import android.net.Uri
import com.nevoit.cresto.data.todo.TodoRepository
import com.nevoit.cresto.data.todo.reminder.TodoAlarmScheduler
import com.nevoit.cresto.data.utils.EventItem
import com.nevoit.cresto.feature.screenextract.AiTodoExtractor
import com.nevoit.cresto.feature.screenextract.toCompressedSharedImageDataUrl

class ShareExtractRepository(
    private val context: Context,
    private val todoRepository: TodoRepository,
    private val alarmScheduler: TodoAlarmScheduler,
    private val aiTodoExtractor: AiTodoExtractor = AiTodoExtractor()
) {
    suspend fun extractAndInsert(
        sharedText: String,
        imageUris: List<Uri>,
        onProgress: (ShareExtractPhase) -> Unit = {}
    ): Int {
        val text = sharedText.trim()
        require(text.isNotBlank() || imageUris.isNotEmpty()) { "没有可提取的分享内容" }

        val items = if (imageUris.isNotEmpty()) {
            extractFromImages(imageUris, text, onProgress)
        } else {
            onProgress(ShareExtractPhase.Extracting)
            aiTodoExtractor.extractFromText(text).items
        }

        onProgress(ShareExtractPhase.Importing)
        val insertedTodos = todoRepository.insertAiGeneratedTodosWithSubTasks(items)
        insertedTodos.forEach(alarmScheduler::schedule)
        return insertedTodos.size
    }

    private suspend fun extractFromImages(
        imageUris: List<Uri>,
        sharedText: String,
        onProgress: (ShareExtractPhase) -> Unit
    ): List<EventItem> {
        val prompt = if (sharedText.isBlank()) {
            "请提取图片中的待办事项"
        } else {
            "请结合这段分享文字提取图片中的待办事项：\n$sharedText"
        }

        return imageUris.flatMap { uri ->
            onProgress(ShareExtractPhase.Reading)
            val imageBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalArgumentException("无法读取分享图片")
            val imageDataUrl = imageBytes.toCompressedSharedImageDataUrl()

            onProgress(ShareExtractPhase.Extracting)
            aiTodoExtractor.extractFromImage(imageDataUrl, prompt).items
        }
    }
}