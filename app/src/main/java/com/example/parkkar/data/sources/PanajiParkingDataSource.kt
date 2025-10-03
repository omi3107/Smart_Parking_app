package com.example.parkkar.data.sources

import android.content.Context
import com.example.parkkar.data.mappers.toParkingLocationList // Ensure this import is correct
import com.example.parkkar.data.model.PanajiRoot
import com.example.parkkar.model.ParkingLocation
import kotlinx.serialization.json.Json
import java.io.IOException

class PanajiParkingDataSource(private val context: Context) : ParkingDataSource {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getParkingLocations(): List<ParkingLocation> {
        return try {
            val jsonString = context.assets.open("panaji_parking.json").bufferedReader().use { it.readText() }
            val panajiRoot = json.decodeFromString<PanajiRoot>(jsonString)
            panajiRoot.toParkingLocationList()
        } catch (e: IOException) {
            e.printStackTrace()
            emptyList()
        } catch (e: Exception) { // Catching generic Exception for other parsing/mapping errors
            e.printStackTrace()
            emptyList()
        }
    }
}
