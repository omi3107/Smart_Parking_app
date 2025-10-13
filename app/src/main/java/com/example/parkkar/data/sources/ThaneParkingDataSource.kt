package com.example.parkkar.data.sources

import android.content.Context
import com.example.parkkar.data.mappers.toParkingLocationList // Assuming this will be created
import com.example.parkkar.data.model.ThaneParkingRoot
import com.example.parkkar.model.ParkingLocation
import kotlinx.serialization.json.Json
import java.io.IOException

interface ParkingDataSource {
    suspend fun getParkingLocations(): List<ParkingLocation>
}

class ThaneParkingDataSource(private val context: Context) : ParkingDataSource {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getParkingLocations(): List<ParkingLocation> {
        return try {
            val jsonString = context.assets.open("thane_sample.json").bufferedReader().use { it.readText() }
            val thaneRoot = json.decodeFromString<ThaneParkingRoot>(jsonString)
            thaneRoot.toParkingLocationList() // This extension function will be in ThaneParkingMapper.kt
        } catch (e: IOException) {
            // Handle exception (e.g., log error, return empty list)
            e.printStackTrace()
            emptyList()
        } catch (e: Exception) {
            // Handle other exceptions like SerializationException
            e.printStackTrace()
            emptyList()
        }
    }
}
