@file:OptIn(ExperimentalSerializationApi::class, kotlinx.serialization.InternalSerializationApi::class)
package com.example.parkkar.repository

import android.content.Context
import android.util.Log
import com.example.parkkar.model.OpeningTime
import com.example.parkkar.model.ParkingLocation
import com.example.parkkar.model.PriceInfo
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException

object ParkingRepository {

    private var allParkingSpots: List<ParkingLocation>? = null
    private val json = Json { ignoreUnknownKeys = true }

    @Synchronized
    fun getAllParkingSpots(context: Context): List<ParkingLocation> {
        if (allParkingSpots == null) {
            Log.d("ParkingRepository", "Parking data is not cached. Loading from JSON assets...")
            allParkingSpots = loadAndParseAllData(context)
        }
        return allParkingSpots!!
    }

    fun getSpotById(context: Context, id: String): ParkingLocation? {
        getAllParkingSpots(context)
        return allParkingSpots?.firstOrNull { it.id == id }
    }

    private fun loadAndParseAllData(context: Context): List<ParkingLocation> {
        val combinedList = mutableListOf<ParkingLocation>()
        combinedList.addAll(parseJson(context, "mumbai_parking.json", ::parseMumbaiParking))
        combinedList.addAll(parseJson(context, "thane_sample.json", ::parseThaneParking))
        combinedList.addAll(parseJson(context, "surat_parking.json", ::parseSuratParking))
        combinedList.addAll(parseJson(context, "panaji_parking.json", ::parsePanajiParking))
        combinedList.addAll(parseJson(context, "bengaluru_parking.json", ::parseBengaluruParking))
        Log.d("ParkingRepository", "Loaded and parsed a total of ${combinedList.size} parking spots.")
        return combinedList
    }

    private fun parseJson(context: Context, fileName: String, parser: (String) -> List<ParkingLocation>): List<ParkingLocation> {
        return try {
            val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
            parser(jsonString)
        } catch (e: IOException) {
            Log.e("ParkingRepository", "Error reading $fileName", e)
            emptyList()
        } catch (e: Exception) {
            Log.e("ParkingRepository", "Error parsing $fileName", e)
            emptyList()
        }
    }

    private fun parseMumbaiParking(jsonString: String): List<ParkingLocation> {
        val root = json.decodeFromString<MumbaiRoot>(jsonString)
        return root.features.mapNotNull { feature ->
            val props = feature.properties ?: return@mapNotNull null
            val name = props.name ?: return@mapNotNull null
            ParkingLocation(
                id = "mumbai_${name.toSafeId()}",
                name = name,
                address = props.description,
                latitude = feature.geometry?.coordinates?.getOrNull(1),
                longitude = feature.geometry?.coordinates?.getOrNull(0),
                cityName = "Mumbai",
                twoWheelerCapacity = null,
                fourWheelerCapacity = null,
                coverageType = null,
                prices = props.prices,
                openingTimes = props.openingTimes
            )
        }
    }

    private fun parseThaneParking(jsonString: String): List<ParkingLocation> {
        val root = json.decodeFromString<ThaneRoot>(jsonString)
        return root.parkingData.parkingLocation.mapNotNull { location ->
            val name = location.name ?: return@mapNotNull null
            ParkingLocation(
                id = "thane_${name.toSafeId()}",
                name = name,
                address = location.address,
                latitude = location.latitude,
                longitude = location.longitude,
                cityName = "Thane",
                twoWheelerCapacity = location.twoWheelerParking,
                fourWheelerCapacity = location.fourWheelerParking,
                coverageType = null,
                prices = location.prices,
                openingTimes = location.openingTimes
            )
        }
    }

    private fun parseSuratParking(jsonString: String): List<ParkingLocation> {
        val root = json.decodeFromString<SuratRoot>(jsonString)
        return root.dataset.values.mapNotNull { row ->
            val name = row.nameOfParking ?: return@mapNotNull null
            ParkingLocation(
                id = "surat_${name.toSafeId()}",
                name = name,
                address = row.parkingAddress,
                latitude = row.latitude,
                longitude = row.longitude,
                cityName = row.cityName ?: "Surat",
                twoWheelerCapacity = row.noOf2WheelerParking,
                fourWheelerCapacity = row.noOf4WheelerParking,
                coverageType = null,
                prices = row.prices,
                openingTimes = row.openingTimes
            )
        }
    }

    private fun parsePanajiParking(jsonString: String): List<ParkingLocation> {
        val root = json.decodeFromString<PanajiRoot>(jsonString)
        return root.features.mapNotNull { feature ->
            val props = feature.properties ?: return@mapNotNull null
            val name = props.nameOfParking ?: return@mapNotNull null
            ParkingLocation(
                id = "panaji_${name.toSafeId()}",
                name = name,
                address = props.parkingAddress,
                latitude = feature.geometry?.coordinates?.getOrNull(1),
                longitude = feature.geometry?.coordinates?.getOrNull(0),
                cityName = props.cityName ?: "Panaji",
                twoWheelerCapacity = props.noOf2WheelerParking?.toIntOrNull(),
                fourWheelerCapacity = props.noOf4WheelerParking,
                coverageType = null,
                prices = props.prices,
                openingTimes = props.openingTimes
            )
        }
    }
    
    private fun parseBengaluruParking(jsonString: String): List<ParkingLocation> {
        val root = json.decodeFromString<BengaluruRoot>(jsonString)
        return root.features.mapNotNull { feature ->
            val props = feature.properties ?: return@mapNotNull null
            val name = props.name ?: return@mapNotNull null
            ParkingLocation(
                id = "bengaluru_${name.toSafeId()}",
                name = name,
                address = props.description,
                latitude = feature.geometry?.coordinates?.getOrNull(1),
                longitude = feature.geometry?.coordinates?.getOrNull(0),
                cityName = "Bengaluru",
                twoWheelerCapacity = null,
                fourWheelerCapacity = null,
                coverageType = null,
                prices = null,
                openingTimes = null
            )
        }
    }

    private fun String.toSafeId(): String = this.lowercase().replace(Regex("[^a-zA-Z0-9]"), "_")
}

// --- Self-Contained, Private Parsing Models ---

@Serializable private data class MumbaiRoot(val features: List<MumbaiFeature>)
@Serializable private data class MumbaiFeature(val properties: MumbaiProperties?, val geometry: MumbaiGeometry?)
@Serializable private data class MumbaiProperties(@SerialName("Name") val name: String?, @SerialName("Description") val description: String?, val prices: List<PriceInfo>?, val openingTimes: List<OpeningTime>?)
@Serializable private data class MumbaiGeometry(val coordinates: List<Double>?)

@Serializable private data class ThaneRoot(@SerialName("parking_data") val parkingData: ThaneParkingData)
@Serializable private data class ThaneParkingData(@SerialName("parking_location") val parkingLocation: List<ThaneParkingLocation>)
@Serializable private data class ThaneParkingLocation(val name: String?, val address: String?, val latitude: Double?, val longitude: Double?, @SerialName("two_wheeler_parking") val twoWheelerParking: Int?, @SerialName("four_wheeler_parking") val fourWheelerParking: Int?, val prices: List<PriceInfo>?, val openingTimes: List<OpeningTime>?)

@Serializable private data class SuratRoot(@SerialName("DATASET") val dataset: Map<String, SuratRow>)
@Serializable private data class SuratRow(@SerialName("CITYNAME") val cityName: String?, @SerialName("NAME_OF_PARKING") val nameOfParking: String?, @SerialName("PARKING_ADDRESS") val parkingAddress: String?, @SerialName("LATITUDE") val latitude: Double?, @SerialName("LONGITUDE") val longitude: Double?, @SerialName("NO._OF_4_WHEELER_PARKING") val noOf4WheelerParking: Int?, @SerialName("NO._OF_2_WHEELER_PARKING") val noOf2WheelerParking: Int?, val prices: List<PriceInfo>?, val openingTimes: List<OpeningTime>?)

@Serializable private data class PanajiRoot(val features: List<PanajiFeature>)
@Serializable private data class PanajiFeature(val properties: PanajiProperties?, val geometry: PanajiGeometry?)
@Serializable private data class PanajiProperties(@SerialName("CityName") val cityName: String?, @SerialName("NameOfParking") val nameOfParking: String?, @SerialName("ParkingAddress") val parkingAddress: String?, @SerialName("NoOf4WheelerParking") val noOf4WheelerParking: Int?, @SerialName("NoOf2WheelerParking") val noOf2WheelerParking: String?, val prices: List<PriceInfo>?, val openingTimes: List<OpeningTime>?)
@Serializable private data class PanajiGeometry(val coordinates: List<Double>?)

@Serializable private data class BengaluruRoot(val features: List<BengaluruFeature>)
@Serializable private data class BengaluruFeature(val properties: BengaluruProperties?, val geometry: BengaluruGeometry?)
@Serializable private data class BengaluruProperties(@SerialName("Name") val name: String?, @SerialName("Description") val description: String?)
@Serializable private data class BengaluruGeometry(val coordinates: List<Double>?)
