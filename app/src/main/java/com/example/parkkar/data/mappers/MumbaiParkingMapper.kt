package com.example.parkkar.data.mappers

import com.example.parkkar.data.model.MumbaiRoot
import com.example.parkkar.model.ParkingLocation

fun MumbaiRoot.toParkingLocationList(): List<ParkingLocation> {
    return this.features.mapNotNull { feature ->
        val properties = feature.properties
        val geometry = feature.geometry
        val name = properties?.name ?: return@mapNotNull null
        val city = "Mumbai"

        val longitude = geometry?.coordinates?.getOrNull(0)
        val latitude = geometry?.coordinates?.getOrNull(1)

        ParkingLocation(
            id = "${city.toSafeId()}_${name.toSafeId()}", // Create a stable ID
            name = name,
            address = properties.description, // Use description as address
            latitude = latitude,
            longitude = longitude,
            cityName = city,
            // Capacities are missing in the Mumbai JSON, so they will be null
            twoWheelerCapacity = null, 
            fourWheelerCapacity = null, 
            coverageType = properties.coverageType,
            prices = properties.prices, // Assuming direct mapping is fine
            openingTimes = properties.openingTimes // Assuming direct mapping is fine
        )
    }
}
