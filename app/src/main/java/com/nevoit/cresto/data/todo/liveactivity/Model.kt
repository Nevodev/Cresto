package com.nevoit.cresto.data.todo.liveactivity

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed interface ActivityPayload

@Serializable
@SerialName("food_pickup")
data class FoodPickupPayload(
    val platform: String,
    val storeName: String,
    val pickupCode: String,
    val state: String = "PENDING"
) : ActivityPayload

@Serializable
@SerialName("parcel_pickup")
data class ParcelPickupPayload(
    val logisticsCompany: String,
    val pickupCode: String,
    val location: String
) : ActivityPayload