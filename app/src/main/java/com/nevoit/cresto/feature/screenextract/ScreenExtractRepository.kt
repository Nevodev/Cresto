package com.nevoit.cresto.feature.screenextract

import com.nevoit.cresto.data.todo.TodoRepository

class ScreenExtractRepository(
    private val todoRepository: TodoRepository,
    private val screenshotCapturer: ShizukuScreenshotCapturer = ShizukuScreenshotCapturer(),
    private val aiTodoExtractor: AiTodoExtractor = AiTodoExtractor()
) {
    suspend fun captureExtractAndInsert(): Int {
        val screenshot = screenshotCapturer.collapsePanelsAndCapturePng()
        val imageDataUrl = screenshot.toPngDataUrl()
        val response = aiTodoExtractor.extractFromImage(imageDataUrl)
        todoRepository.insertAiGeneratedTodosWithSubTasks(response.items)
        return response.items.size
    }
}
