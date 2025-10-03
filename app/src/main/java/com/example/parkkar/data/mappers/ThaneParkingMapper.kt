package com.example.parkkar.data.mappers

import com.example.parkkar.data.model.ThaneParkingLocationRaw
import com.example.parkkar.data.model.ThaneRoot
import com.example.parkkar.model.ParkingLocation
import java.util.UUID

fun ThaneRoot.toParkingLocationList(): List<ParkingLocation> {
    return this.parkingData?.parkingLocations?.mapNotNull { rawLocation ->
        ParkingLocation(
            id = UUID.randomUUID().toString(), // Generate a unique ID
            name = rawLocation.name,
            address = rawLocation.address,
            latitude = rawLocation.latitude,
            longitude = rawLocation.longitude,
            cityName = "Thane", // Assuming all locations in this file are in Thane
            twoWheelerCapacity = rawLocation.twoWheelerCapacity,
            fourWheelerCapacity = rawLocation.fourWheelerCapacity,
            coverageType = rawLocation.coverageType,
            prices = rawLocation.prices,
            openingTimes = rawLocation.openingTimes
        )
    } ?: emptyList()
}
