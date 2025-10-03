package com.example.parkkar.data.repository

import android.content.Context
import android.util.Log
import com.example.parkkar.data.model.ParkingSpot // Expected return type
import com.example.parkkar.data.model.PriceInfo as DataPriceInfo // For mapping target
import com.example.parkkar.data.model.OpeningTimeInfo as DataOpeningTimeInfo // For mapping target
import com.example.parkkar.data.sources.*
import com.example.parkkar.model.ParkingLocation // Type from new data sources
import com.example.parkkar.model.PriceInfo as ModelPriceInfo // Type from new data sources
import com.example.parkkar.model.OpeningTime as ModelOpeningTime // Type from new data sources

class ParkingDataRepository(private val context: Context) {

    // Initialize individual city data sources
    private val thaneDataSource = ThaneParkingDataSource(context)
    private val suratDataSource = SuratParkingDataSource(context)
    private val mumbaiDataSource = MumbaiParkingDataSource(context)
    private val panajiDataSource = PanajiParkingDataSource(context)
    // TODO: Initialize other data sources (e.g., Chandigarh, Bengaluru) when mappers are ready

    // Initialize the combined data source
    private val combinedParkingDataSource: ParkingDataSource = CombinedParkingDataSource(
        thaneDataSource,
        suratDataSource,
        mumbaiDataSource,
        panajiDataSource
        // TODO: Add other data sources here once available
    )

    private var allMappedParkingSpots: List<ParkingSpot>? = null
    private suspend fun fetchAndCacheAllParkingSpots(): List<ParkingSpot> {
        if (allMappedParkingSpots == null) {
            Log.d("ParkingRepo", "Fetching and caching all parking spots from CombinedParkingDataSource.")
            val parkingLocations: List<ParkingLocation> = combinedParkingDataSource.getParkingLocations()
            Log.d("ParkingRepo", "Fetched ${parkingLocations.size} locations from CombinedParkingDataSource.")

            allMappedParkingSpots = parkingLocations.map { location ->
                val mappedPrices: List<DataPriceInfo>? = location.prices?.map { price -> mapModelPriceToDataPrice(price) }
                val mappedOpeningTimes: List<DataOpeningTimeInfo>? = location.openingTimes?.map { time -> mapModelOpeningTimeToDataOpeningTime(time) }

                ParkingSpot(
                    id = location.id,
                    cityName = location.cityName ?: "Unknown City",
                    parkingName = location.name,
                    address = location.address,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    fourWheelerSpots = location.fourWheelerCapacity ?: 0,
                    twoWheelerSpots = location.twoWheelerCapacity ?: 0,
                    zoneName = location.zoneName,
                    wardName = location.wardName,
                    prices = mappedPrices,
                    openingTimes = mappedOpeningTimes,
                    coverageType = location.coverageType
                )
            }.also {
                Log.d("ParkingRepo", "Mapped and cached ${it.size} ParkingSpot objects.")
            }
        }
        return allMappedParkingSpots ?: emptyList()
    }

    suspend fun getAllParkingSpots(): List<ParkingSpot> {
        return fetchAndCacheAllParkingSpots()
    }

    suspend fun getParkingSpotById(spotId: String): ParkingSpot? {
        val spots = fetchAndCacheAllParkingSpots()
        return spots.find { it.id == spotId }.also {
            if (it != null) {
                Log.d("ParkingRepo", "Found spot with ID $spotId: ${it.parkingName}")
            } else {
                Log.w("ParkingRepo", "Could not find spot with ID $spotId")
            }
        }
    }

    private fun mapModelPriceToDataPrice(modelPrice: ModelPriceInfo): DataPriceInfo {
        return DataPriceInfo(
            days = modelPrice.days,
            timeRange = modelPrice.timeRange,
            rateType = modelPrice.rateType,
            duration = modelPrice.duration,
            amount = modelPrice.amount,
            currency = modelPrice.currency
        )
    }

    private fun mapModelOpeningTimeToDataOpeningTime(modelOpeningTime: ModelOpeningTime): DataOpeningTimeInfo {
        return DataOpeningTimeInfo(
            days = modelOpeningTime.days,
            timeRange = modelOpeningTime.timeRange
        )
    }
}
