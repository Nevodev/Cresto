package com.nevoit.cresto.feature.screenextract

import com.nevoit.cresto.data.todo.TodoRepository
import com.nevoit.cresto.data.todo.reminder.TodoAlarmScheduler

class ScreenExtractRepository(
    private val todoRepository: TodoRepository,
    private val alarmScheduler: TodoAlarmScheduler,
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
        val insertedTodos = todoRepository.insertAiGeneratedTodosWithSubTasks(response.items)
        insertedTodos.forEach(alarmScheduler::schedule)
        return insertedTodos.size
    }
}
