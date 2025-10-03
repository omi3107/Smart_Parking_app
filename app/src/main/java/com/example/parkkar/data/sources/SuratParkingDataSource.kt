package com.example.parkkar.data.sources

import android.content.Context
import com.example.parkkar.data.mappers.toParkingLocationList // Ensure this import is correct
import com.example.parkkar.data.model.SuratRoot
import com.example.parkkar.model.ParkingLocation
import kotlinx.serialization.json.Json
import java.io.IOException

class SuratParkingDataSource(private val context: Context) : ParkingDataSource {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getParkingLocations(): List<ParkingLocation> {
        return try {
            val jsonString = context.assets.open("surat_parking.json").bufferedReader().use { it.readText() }
            val suratRoot = json.decodeFromString<SuratRoot>(jsonString)
            suratRoot.toParkingLocationList()
        } catch (e: IOException) {
            e.printStackTrace()
            emptyList()
        } catch (e: Exception) { // Catching generic Exception for other parsing/mapping errors
            e.printStackTrace()
            emptyList()
        }
    }
}
