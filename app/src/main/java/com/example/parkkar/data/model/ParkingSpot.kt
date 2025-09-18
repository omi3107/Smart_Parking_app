package com.example.parkkar.data.model

import java.util.UUID

data class ParkingSpot(
    val id: String = UUID.randomUUID().toString(), // Generate a unique ID by default
    val cityName: String,
    val parkingName: String?,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
    val fourWheelerSpots: Int = 0, // Default to 0 if NA or missing
    val twoWheelerSpots: Int = 0, // Default to 0 if NA or missing
    val zoneName: String? = null,
    val wardName: String? = null
    // Future considerations:
    // val zipCode: String? = null,
    // val area: String? = null,
)