package com.example.parkkar.data.sources

import com.example.parkkar.model.ParkingLocation
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class CombinedParkingDataSource(
    private val thaneDataSource: ThaneParkingDataSource,
    private val suratDataSource: SuratParkingDataSource,
    private val mumbaiDataSource: MumbaiParkingDataSource,
    private val panajiDataSource: PanajiParkingDataSource
) : ParkingDataSource {

    override suspend fun getParkingLocations(): List<ParkingLocation> = coroutineScope {
        val thaneLocationsDeferred = async { thaneDataSource.getParkingLocations() }
        val suratLocationsDeferred = async { suratDataSource.getParkingLocations() }
        val mumbaiLocationsDeferred = async { mumbaiDataSource.getParkingLocations() }
        val panajiLocationsDeferred = async { panajiDataSource.getParkingLocations() }

        val allLocations = mutableListOf<ParkingLocation>()
        allLocations.addAll(thaneLocationsDeferred.await())
        allLocations.addAll(suratLocationsDeferred.await())
        allLocations.addAll(mumbaiLocationsDeferred.await())
        allLocations.addAll(panajiLocationsDeferred.await())
        
        return@coroutineScope allLocations
    }
}
