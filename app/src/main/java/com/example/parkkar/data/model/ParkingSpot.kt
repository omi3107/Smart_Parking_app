package com.example.parkkar.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class PriceInfo(
    val days: String? = null,
    @SerialName("timeRange") val timeRange: String? = null,
    @SerialName("rateType") val rateType: String? = null,
    val duration: String? = null,
    val amount: Double? = null,
    val currency: String? = null
)

@Serializable
data class OpeningTimeInfo(
    val days: String? = null,
    @SerialName("timeRange") val timeRange: String? = null
)

@Serializable
data class ParkingSpot(
    val id: String = UUID.randomUUID().toString(), // Not typically from JSON, generated locally
    
    // SerialName annotations are examples. Ensure they match your actual JSON keys
    // or that your repository's custom parsing logic handles the mapping.
    @SerialName("CityName") // For Panaji, Mumbai, Bengaluru (deduced)
                            // For Surat: CITYNAME
                            // For Chandigarh: Relies on custom logic (index-based) or a default.
                            // For Thane: Relies on custom logic (fileName-based) or a default.
    val cityName: String, 
    
    @SerialName("NameOfParking") // For Panaji
                                 // For Surat: NAME_OF_PARKING
                                 // For Mumbai/Bengaluru: Name (properties.Name)
                                 // For Thane: name (parking_location.name)
                                 // For Chandigarh: Relies on custom logic
    val parkingName: String?,
    
    @SerialName("ParkingAddress") // For Panaji
                                   // For Surat: PARKING_ADDRESS
                                   // For Mumbai/Bengaluru: Description (properties.Description can be used as address if specific address field is missing)
                                   // For Thane: address (parking_location.address)
                                   // For Chandigarh: Relies on custom logic
    val address: String?,
    
    @SerialName("Latitude") // For Panaji
                            // For Surat: LATITUDE
                            // For Mumbai/Bengaluru: geometry.coordinates[1]
                            // For Thane: latitude
                            // For Chandigarh: Relies on custom logic
    val latitude: Double?,
    
    @SerialName("Longitude") // For Panaji
                             // For Surat: LONGITUDE
                             // For Mumbai/Bengaluru: geometry.coordinates[0]
                             // For Thane: longitude
                             // For Chandigarh: Relies on custom logic
    val longitude: Double?,
    
    // For capacities, if JSON keys are consistently "NO._OF_4_WHEELER_PARKING", etc.
    // and you want direct serialization, use @SerialName.
    // Otherwise, ensure repository logic correctly maps.
    // Current structure assumes repository handles mapping:
    // Panaji: NoOf4WheelerParking, NoOf2WheelerParking
    // Surat: NO._OF_4_WHEELER_PARKING, NO._OF_2_WHEELER_PARKING
    // Mumbai/Bengaluru: NO._OF_4_WHEELER_PARKING, NO._OF_2_WHEELER_PARKING (added by user)
    // Thane: four_wheeler_parking, two_wheeler_parking
    // Chandigarh: Relies on custom logic
    val fourWheelerSpots: Int = 0,
    val twoWheelerSpots: Int = 0,
    
    @SerialName("ZoneName") // For Panaji
                            // For Surat: ZONE_NAME
                            // Others: May not be present or named differently
    val zoneName: String? = null,
    
    @SerialName("WardName") // For Panaji
                            // For Surat: WARD_NAME
                            // Others: May not be present or named differently
    val wardName: String? = null,
    
    // Assuming "CoverageType" is the consistent key in your updated JSONs
    @SerialName("CoverageType") 
    val coverageType: String? = null,
    
    // Assumes "prices" is the consistent key for the array in JSON
    val prices: List<PriceInfo>? = emptyList(),
    
    // Assumes "openingTimes" is the consistent key for the array in JSON
    val openingTimes: List<OpeningTimeInfo>? = emptyList()
)
