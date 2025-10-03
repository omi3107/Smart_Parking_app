package com.example.parkkar.data.model

import com.example.parkkar.model.OpeningTime
import com.example.parkkar.model.PriceInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PanajiRoot(
    val type: String?,
    val name: String?,
    // val crs: Map<String, Map<String, String>>?, // CRS can be ignored for mapping
    val features: List<PanajiFeature>
)

@Serializable
data class PanajiFeature(
    val type: String?,
    val properties: PanajiProperties?,
    val geometry: PanajiGeometry?
)

@Serializable
data class PanajiProperties(
    @SerialName("CityName")
    val cityName: String?,
    @SerialName("ZoneName")
    val zoneName: String?, // Can be ignored or used for details
    @SerialName("WardName")
    val wardName: String?, // Can be ignored or used for details
    @SerialName("NameOfParking")
    val nameOfParking: String?,
    @SerialName("ParkingAddress")
    val parkingAddress: String?,
    @SerialName("Latitude")
    val latitudeProp: Double?, // Fallback, geometry.coordinates preferred
    @SerialName("Longitude")
    val longitudeProp: Double?, // Fallback, geometry.coordinates preferred
    @SerialName("NoOf4WheelerParking")
    val fourWheelerCapacityString: String?,
    @SerialName("NoOf2WheelerParking")
    val twoWheelerCapacityString: String?,
    @SerialName("Description")
    val description: String?, // Can be ignored or used for details
    val prices: List<PriceInfo>?,
    val openingTimes: List<OpeningTime>?,
    @SerialName("CoverageType")
    val coverageType: String?
)

@Serializable
data class PanajiGeometry(
    val type: String?,
    val coordinates: List<Double>? // [longitude, latitude]
)
