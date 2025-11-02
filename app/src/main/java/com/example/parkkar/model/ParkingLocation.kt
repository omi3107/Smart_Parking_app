
@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)
package com.example.parkkar.model

import kotlinx.serialization.Serializable

@Serializable
data class ParkingLocation(
    val id: String, // Unique identifier for the parking location
    val name: String?,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
    val cityName: String?,
    val zoneName: String? = null, // Added field
    val wardName: String? = null, // Added field
    val twoWheelerCapacity: Int?,
    val fourWheelerCapacity: Int?,
    val coverageType: String?, // e.g., "Multi-level", "Covered", "Not Covered"
    val prices: List<PriceInfo>?,
    val openingTimes: List<OpeningTime>?
) {
    val totalCapacity: Int
        get() = (twoWheelerCapacity ?: 0) + (fourWheelerCapacity ?: 0)
}

@Serializable
data class OpeningTime(
    val days: String?,
    val timeRange: String?
)
