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
                storeName = "喜茶",
                pickupCode = "8237",
                foodName = "多肉葡萄"
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