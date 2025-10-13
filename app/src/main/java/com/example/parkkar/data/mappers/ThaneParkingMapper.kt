package com.example.parkkar.data.mappers

import com.example.parkkar.data.model.ThaneOpeningTimeRaw
import com.example.parkkar.data.model.ThaneParkingLocationRaw
import com.example.parkkar.data.model.ThanePriceInfoRaw
// FIX: Corrected the class name to match the actual data class
import com.example.parkkar.data.model.ThaneParkingRoot 

import com.example.parkkar.model.OpeningTime
import com.example.parkkar.model.ParkingLocation
import com.example.parkkar.model.PriceInfo

// FIX: Corrected the extension function receiver to use the correct class
fun ThaneParkingRoot.toParkingLocationList(): List<ParkingLocation> {
    // The rest of the code now works because the root type is correct
    return this.parkingData?.parkingLocations?.mapNotNull { rawLocation ->
        val name = rawLocation.name ?: return@mapNotNull null
        ParkingLocation(
            id = "thane_${name.toSafeId()}", // Create a stable ID
            name = name,
            address = rawLocation.address,
            latitude = rawLocation.latitude,
            longitude = rawLocation.longitude,
            cityName = "Thane", // Set city explicitly
            twoWheelerCapacity = rawLocation.twoWheelerParking,
            fourWheelerCapacity = rawLocation.fourWheelerParking,
            coverageType = rawLocation.coverageType,
            prices = rawLocation.prices?.map { priceInfo: ThanePriceInfoRaw ->
                priceInfo.toDomainModel()
            },
            openingTimes = rawLocation.openingTimes?.map { openingTime: ThaneOpeningTimeRaw ->
                openingTime.toDomainModel()
            }
        )
    } ?: emptyList()
}

fun ThanePriceInfoRaw.toDomainModel(): PriceInfo {
    return PriceInfo(
        days = this.days,
        timeRange = this.timeRange,
        rateType = this.rateType,
        duration = this.duration,
        amount = this.amount,
        currency = this.currency
    )
}

fun ThaneOpeningTimeRaw.toDomainModel(): OpeningTime {
    return OpeningTime(
        days = this.days,
        timeRange = this.timeRange
    )
}
