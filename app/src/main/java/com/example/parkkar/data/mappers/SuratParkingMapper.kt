package com.example.parkkar.data.mappers

import com.example.parkkar.data.model.SuratParkingLocationRaw
import com.example.parkkar.data.model.SuratRoot
import com.example.parkkar.model.ParkingLocation

fun SuratRoot.toParkingLocationList(): List<ParkingLocation> {
    return this.dataset.values.mapNotNull { rawLocation ->
        val name = rawLocation.nameOfParking ?: return@mapNotNull null
        val city = rawLocation.cityName ?: "Surat"

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
            id = "${city.toSafeId()}_${name.toSafeId()}", // Create a stable ID
            name = name,
            address = rawLocation.parkingAddress,
            latitude = rawLocation.latitudeString.toDoubleOrNullNA(),
            longitude = rawLocation.longitudeString.toDoubleOrNullNA(),
            cityName = city,
            zoneName = rawLocation.zoneName,
            wardName = rawLocation.wardName,
            twoWheelerCapacity = rawLocation.twoWheelerCapacityString.toIntOrNullNA(),
            fourWheelerCapacity = rawLocation.fourWheelerCapacityString.toIntOrNullNA(),
            coverageType = rawLocation.coverageType,
            prices = rawLocation.prices, // Assuming direct mapping is fine
            openingTimes = rawLocation.openingTimes // Assuming direct mapping is fine
        )
    }
}
