package com.example.parkkar.data.mappers

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ThaneParkingRoot(
    @SerialName("parking_data")
    val parkingData: ThaneParkingDataContainer?
)

@Serializable
data class ThaneParkingDataContainer(
    @SerialName("parking_location")
    val parkingLocations: List<ThaneParkingLocationRaw>?
)

@Serializable
data class ThaneParkingLocationRaw(
    val name: String?,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
    @SerialName("two_wheeler_parking")
    val twoWheelerParking: Int?,
    @SerialName("four_wheeler_parking")
    val fourWheelerParking: Int?,
    val prices: List<ThanePriceInfoRaw>?,
    @SerialName("openingTimes")
    val openingTimes: List<ThaneOpeningTimeRaw>?,
    @SerialName("CoverageType")
    val coverageType: String?
    // Note: "?xml" field is ignored as it's likely not needed.
)

@Serializable
data class ThanePriceInfoRaw(
    val days: String?,
    @SerialName("timeRange")
    val timeRange: String?,
    @SerialName("rateType")
    val rateType: String?,
    val duration: String?,
    val amount: Double?,
    val currency: String?
)

@Serializable
data class ThaneOpeningTimeRaw(
    val days: String?,
    @SerialName("timeRange")
    val timeRange: String?
)
