package com.example.parkkar.data.model

import com.example.parkkar.model.OpeningTime
import com.example.parkkar.model.PriceInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MumbaiRoot(
    val type: String?,
    val name: String?,
    val features: List<MumbaiFeature>
)

@Serializable
data class MumbaiFeature(
    val type: String?,
    val properties: MumbaiProperties?,
    val geometry: MumbaiGeometry?
)

@Serializable
data class MumbaiProperties(
    @SerialName("Name")
    val name: String?,
    @SerialName("Description")
    val description: String?, // Can be used if needed, or ignored
    val prices: List<PriceInfo>?,
    val openingTimes: List<OpeningTime>?,
    @SerialName("CoverageType")
    val coverageType: String?
    // address, cityName, twoWheelerCapacity, fourWheelerCapacity are missing
)

@Serializable
data class MumbaiGeometry(
    val type: String?,
    val coordinates: List<Double>? // [longitude, latitude]
)
