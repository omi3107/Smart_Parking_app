package com.example.parkkar.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import com.example.parkkar.model.PriceInfo
import com.example.parkkar.model.OpeningTime

@Serializable
data class SuratRoot(
    @SerialName("DATASET")
    val dataset: Map<String, SuratParkingLocationRaw>
)

@Serializable
data class SuratParkingLocationRaw(
    @SerialName("CITYNAME")
    val cityName: String?,
    @SerialName("ZONE_NAME")
    val zoneName: String?, // Not directly in ParkingLocation, can be ignored or used for details
    @SerialName("WARD_NAME")
    val wardName: String?, // Not directly in ParkingLocation, can be ignored or used for details
    @SerialName("NAME_OF_PARKING")
    val nameOfParking: String?,
    @SerialName("PARKING_ADDRESS")
    val parkingAddress: String?,
    @SerialName("LATITUDE")
    val latitudeString: String?,
    @SerialName("LONGITUDE")
    val longitudeString: String?,
    @SerialName("NO._OF_4_WHEELER_PARKING")
    val fourWheelerCapacityString: String?,
    @SerialName("NO._OF_2_WHEELER_PARKING")
    val twoWheelerCapacityString: String?,
    @SerialName("prices")
    val prices: List<PriceInfo>?,
    @SerialName("openingTimes")
    val openingTimes: List<OpeningTime>?,
    @SerialName("CoverageType")
    val coverageType: String?
)
