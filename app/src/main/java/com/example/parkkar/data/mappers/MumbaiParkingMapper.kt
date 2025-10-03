package com.example.parkkar.data.mappers

import com.example.parkkar.data.model.MumbaiRoot
import com.example.parkkar.model.ParkingLocation
import java.util.UUID

fun MumbaiRoot.toParkingLocationList(): List<ParkingLocation> {
    return this.features.mapNotNull { feature ->
        val properties = feature.properties
        val geometry = feature.geometry

        val longitude = geometry?.coordinates?.getOrNull(0)
        val latitude = geometry?.coordinates?.getOrNull(1)

        ParkingLocation(
            id = UUID.randomUUID().toString(),
            name = properties?.name,
            address = null, // Not available in Mumbai JSON
            latitude = latitude,
            longitude = longitude,
            cityName = null, // Not available in Mumbai JSON, could be set to "Mumbai" if appropriate
            twoWheelerCapacity = null, // Not available in Mumbai JSON
            fourWheelerCapacity = null, // Not available in Mumbai JSON
            coverageType = properties?.coverageType,
            prices = properties?.prices,
            openingTimes = properties?.openingTimes
        )
    }
}
