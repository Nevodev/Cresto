package com.nevoit.cresto.data.todo.liveactivity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LiveActivityViewModel(
    private val activityDao: ActivityDao
) : ViewModel() {
    val activities: StateFlow<List<LiveActivityEntity>> = activityDao.getAllActivities()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addMockFoodPickup() {
        viewModelScope.launch {
            val payload = FoodPickupPayload(
                platform = "饿了么",
                storeName = "茶颜悦色",
                pickupCode = "C-120",
                state = "MAKING"
            )

            activityDao.insertActivity(LiveActivityEntity(content = payload))
        }
    }

    fun deleteActivity(id: String) {
        viewModelScope.launch {
            activityDao.deleteActivityById(id)
        }
    }
}