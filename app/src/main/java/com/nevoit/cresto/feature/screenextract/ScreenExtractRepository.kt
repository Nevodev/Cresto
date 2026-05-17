package com.nevoit.cresto.feature.screenextract

import com.nevoit.cresto.data.todo.TodoRepository

class ScreenExtractRepository(
    private val todoRepository: TodoRepository,
    private val screenshotCapturer: ShizukuScreenshotCapturer = ShizukuScreenshotCapturer(),
    private val aiTodoExtractor: AiTodoExtractor = AiTodoExtractor()
) {
    suspend fun captureExtractAndInsert(
        onProgress: (ScreenExtractPhase) -> Unit = {}
    ): Int {
        onProgress(ScreenExtractPhase.Capturing)
        val screenshot = screenshotCapturer.collapsePanelsAndCapturePng()
        val imageDataUrl = screenshot.toCompressedScreenshotDataUrl()

        onProgress(ScreenExtractPhase.Extracting)
        val response = aiTodoExtractor.extractFromImage(imageDataUrl)

        onProgress(ScreenExtractPhase.Importing)
        todoRepository.insertAiGeneratedTodosWithSubTasks(response.items)
        return response.items.size
    }
}
