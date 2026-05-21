package com.nevoit.cresto.data.todo

import kotlin.math.roundToInt

data class PressureIndex(
    val score: Int,
    val primarySource: PressureSource
)

enum class PressureSource {
    NONE,
    TODAY,
    OVERDUE,
    BACKLOG,
    WEEK
}

enum class PressureLevel {
    CLEAR,
    LOW,
    MEDIUM,
    HIGH
}

fun calculatePressureIndex(insights: InsightsUiState): PressureIndex {
    val todayScore = (insights.todayRemaining / 6f).coerceIn(0f, 1f) * 35f
    val overdueScore = (insights.overdueTotal / 3f).coerceIn(0f, 1f) * 30f
    val backlogScore = (insights.stalePendingTotal / 5f).coerceIn(0f, 1f) * 20f
    val weekScore = if (insights.weekDueTotal > 0) {
        ((1f - insights.weekDueProgress) * 15f).coerceIn(0f, 15f)
    } else {
        0f
    }

    val scoreBySource = listOf(
        PressureSource.TODAY to todayScore,
        PressureSource.OVERDUE to overdueScore,
        PressureSource.BACKLOG to backlogScore,
        PressureSource.WEEK to weekScore
    )

    val totalScore = scoreBySource.sumOf { it.second.toDouble() }.roundToInt().coerceIn(0, 100)
    val primarySource = scoreBySource.maxByOrNull { it.second }?.takeIf { it.second > 0f }?.first
        ?: PressureSource.NONE

    return PressureIndex(
        score = totalScore,
        primarySource = primarySource
    )
}

fun pressureLevelOf(score: Int): PressureLevel {
    return when {
        score >= 80 -> PressureLevel.HIGH
        score >= 55 -> PressureLevel.MEDIUM
        score >= 30 -> PressureLevel.LOW
        else -> PressureLevel.CLEAR
    }
}