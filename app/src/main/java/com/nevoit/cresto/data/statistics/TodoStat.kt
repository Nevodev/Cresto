package com.nevoit.cresto.data.statistics

import java.time.LocalDate

data class TodoStat(
    val totalCount: Int = 0,
    val completedCount: Int = 0,
) {
    val progress: Float
        get() = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
}

data class DailyStat(
    val date: LocalDate,
    val count: Int
)