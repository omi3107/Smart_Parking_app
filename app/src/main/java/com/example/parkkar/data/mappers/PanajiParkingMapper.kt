package com.example.parkkar.data.mappers

import com.example.parkkar.data.model.PanajiRoot
import com.example.parkkar.model.ParkingLocation

fun PanajiRoot.toParkingLocationList(): List<ParkingLocation> {
    return this.features.mapNotNull { feature ->
        val properties = feature.properties ?: return@mapNotNull null
        val geometry = feature.geometry
        val name = properties.nameOfParking ?: return@mapNotNull null
        val city = properties.cityName ?: "Panaji"

        // Helper to parse Int, returning null for "NA" or invalid numbers
        fun String?.toIntOrNullNA(): Int? {
            if (this == null || this.equals("NA", ignoreCase = true)) return null
            return this.toIntOrNull()
        }

        val longitude = geometry?.coordinates?.getOrNull(0) ?: properties.longitudeProp
        val latitude = geometry?.coordinates?.getOrNull(1) ?: properties.latitudeProp

        ParkingLocation(
            id = "${city.toSafeId()}_${name.toSafeId()}", // Create a stable ID
            name = name,
            address = properties.parkingAddress,
            latitude = latitude,
            longitude = longitude,
            cityName = city,
            zoneName = properties.zoneName,
            wardName = properties.wardName,
            twoWheelerCapacity = properties.twoWheelerCapacityString.toIntOrNullNA(),
            fourWheelerCapacity = properties.fourWheelerCapacityString.toIntOrNullNA(),
            coverageType = properties.coverageType,
            prices = properties.prices, // Assuming direct mapping is fine
            openingTimes = properties.openingTimes // Assuming direct mapping is fine
        )
    }
}
