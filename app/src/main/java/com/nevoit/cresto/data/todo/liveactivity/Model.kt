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
    val storeName: String,
    val pickupCode: String,
    val foodName: String,
    val foodType: FoodPickupType = FoodPickupType.la_unspecified
) : ActivityPayload

@Serializable
enum class FoodPickupType {
    la_unspecified,
    la_takeaway,
    la_hot,
    la_lemonade,
    la_iced_ganlu,
    la_iced_grape,
    la_iced_mango,
    la_iced_americano,
    la_iced_latte,
    la_iced_tea,
    la_iced_milktea,
    la_iced_lemon_tea,
    la_iced_matcha_latte,
    la_iced_bubble_tea
}

@Serializable
@SerialName("parcel_pickup")
data class ParcelPickupPayload(
    val logisticsCompany: String,
    val pickupCode: String,
    val location: String
) : ActivityPayload