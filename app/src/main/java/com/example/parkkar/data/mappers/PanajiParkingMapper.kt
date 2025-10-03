package com.example.parkkar.data.mappers

import com.example.parkkar.data.model.PanajiRoot
import com.example.parkkar.model.ParkingLocation
import java.util.UUID

fun PanajiRoot.toParkingLocationList(): List<ParkingLocation> {
    return this.features.mapNotNull { feature ->
        val properties = feature.properties
        val geometry = feature.geometry

        // Helper to parse Int, returning null for "NA" or invalid numbers
        fun String?.toIntOrNullNA(): Int? {
            if (this == null || this.equals("NA", ignoreCase = true)) return null
            return this.toIntOrNull()
        }

        val longitude = geometry?.coordinates?.getOrNull(0) ?: properties?.longitudeProp
        val latitude = geometry?.coordinates?.getOrNull(1) ?: properties?.latitudeProp

        ParkingLocation(
            id = UUID.randomUUID().toString(),
            name = properties?.nameOfParking,
            address = properties?.parkingAddress,
            latitude = latitude,
            longitude = longitude,
            cityName = properties?.cityName,
            zoneName = properties?.zoneName, // Added
            wardName = properties?.wardName, // Added
            twoWheelerCapacity = properties?.twoWheelerCapacityString.toIntOrNullNA(),
            fourWheelerCapacity = properties?.fourWheelerCapacityString.toIntOrNullNA(),
            coverageType = properties?.coverageType,
            prices = properties?.prices,
            openingTimes = properties?.openingTimes
        )
    }
}
