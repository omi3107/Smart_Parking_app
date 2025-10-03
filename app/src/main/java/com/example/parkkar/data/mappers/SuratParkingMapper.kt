package com.example.parkkar.data.mappers

import com.example.parkkar.data.model.SuratParkingLocationRaw
import com.example.parkkar.data.model.SuratRoot
import com.example.parkkar.model.ParkingLocation
import java.util.UUID

fun SuratRoot.toParkingLocationList(): List<ParkingLocation> {
    return this.dataset.values.mapNotNull { rawLocation ->
        // Helper to parse Double, returning null for "NA" or invalid numbers
        fun String?.toDoubleOrNullNA(): Double? {
            if (this == null || this.equals("NA", ignoreCase = true)) return null
            return this.toDoubleOrNull()
        }

        // Helper to parse Int, returning null for "NA" or invalid numbers
        fun String?.toIntOrNullNA(): Int? {
            if (this == null || this.equals("NA", ignoreCase = true)) return null
            return this.toIntOrNull()
        }

        ParkingLocation(
            id = UUID.randomUUID().toString(), 
            name = rawLocation.nameOfParking,
            address = rawLocation.parkingAddress,
            latitude = rawLocation.latitudeString.toDoubleOrNullNA(),
            longitude = rawLocation.longitudeString.toDoubleOrNullNA(),
            cityName = rawLocation.cityName,
            zoneName = rawLocation.zoneName, // Added
            wardName = rawLocation.wardName, // Added
            twoWheelerCapacity = rawLocation.twoWheelerCapacityString.toIntOrNullNA(),
            fourWheelerCapacity = rawLocation.fourWheelerCapacityString.toIntOrNullNA(),
            coverageType = rawLocation.coverageType,
            prices = rawLocation.prices,
            openingTimes = rawLocation.openingTimes
        )
    }
}
