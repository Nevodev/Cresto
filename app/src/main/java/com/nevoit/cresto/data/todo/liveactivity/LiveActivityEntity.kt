package com.nevoit.cresto.data.todo.liveactivity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.util.UUID

@Entity(tableName = "live_activities")
data class LiveActivityEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val createTime: LocalDateTime = LocalDateTime.now(),
    val content: ActivityPayload,
    val isDone: Boolean = false
)