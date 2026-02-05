package com.nevoit.cresto.data.todo.liveactivity

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json

class ActivityTypeConverters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromPayload(payload: ActivityPayload): String {
        return json.encodeToString(payload)
    }

    @TypeConverter
    fun toPayload(jsonString: String): ActivityPayload {
        return json.decodeFromString(jsonString)
    }
}