package com.nevoit.cresto.data.todo.liveactivity

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    @Query("SELECT * FROM live_activities ORDER BY createTime DESC")
    fun getAllActivities(): Flow<List<LiveActivityEntity>>

    @Query("SELECT * FROM live_activities WHERE id = :id")
    suspend fun getActivityById(id: String): LiveActivityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: LiveActivityEntity)

    @Delete
    suspend fun deleteActivity(activity: LiveActivityEntity)

    @Query("DELETE FROM live_activities WHERE id = :id")
    suspend fun deleteActivityById(id: String)

    @Query("DELETE FROM live_activities")
    suspend fun clearAll()
}