@file:OptIn(ExperimentalSerializationApi::class, kotlinx.serialization.InternalSerializationApi::class)
package com.example.parkkar.repository

import android.content.Context
import android.util.Log
import com.example.parkkar.model.OpeningTime
import com.example.parkkar.model.ParkingLocation
import com.example.parkkar.model.PriceInfo
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import java.io.IOException

// Custom serializer to handle "NA" or other non-double strings gracefully
object DoubleAsStringSerializer : KSerializer<Double?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("DoubleAsString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Double?) {
        encoder.encodeString(value?.toString() ?: "NA")
    }

    override fun deserialize(decoder: Decoder): Double? {
        return decoder.decodeString().toDoubleOrNull()
    }
}

object ParkingRepository {

    private var allParkingSpots: List<ParkingLocation>? = null
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; }

    @Synchronized
    fun getAllParkingSpots(context: Context): List<ParkingLocation> {
        if (allParkingSpots == null) {
            Log.d("ParkingRepository", "Parking data is not cached. Loading from JSON assets...")
            allParkingSpots = loadAndParseAllData(context)
        }
        return allParkingSpots!!
    }

    fun searchParkingSpots(context: Context, query: String): List<ParkingLocation> {
        val allSpots = getAllParkingSpots(context)
        if (query.isBlank()) {
            return emptyList()
        }
        return allSpots.filter {
            it.name?.contains(query, ignoreCase = true) == true ||
            it.address?.contains(query, ignoreCase = true) == true ||
            it.cityName?.contains(query, ignoreCase = true) == true
        }
    }

    fun getSpotById(context: Context, id: String): ParkingLocation? {
        getAllParkingSpots(context)
        return allParkingSpots?.firstOrNull { it.id == id }
    }

    private fun loadAndParseAllData(context: Context): List<ParkingLocation> {
        val combinedList = mutableListOf<ParkingLocation>()
        combinedList.addAll(parseJson(context, "mumbai_parking.json", ::parseMumbaiParking))
        combinedList.addAll(parseJson(context, "thane_sample.json") { parseStandardParking(it, "Thane") })
        combinedList.addAll(parseJson(context, "surat_parking.json") { parseStandardParking(it, "Surat") })
        combinedList.addAll(parseJson(context, "panaji_parking.json") { parseStandardParking(it, "Panaji") })
        combinedList.addAll(parseJson(context, "bengaluru_parking.json") { parseStandardParking(it, "Bengaluru") })
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
        val locations = mutableListOf<ParkingLocation>()
        root.features.forEach { featureElement ->
            try {
                val feature = json.decodeFromJsonElement<MumbaiFeature>(featureElement)
                val props = feature.properties ?: return@forEach
                val name = props.name ?: return@forEach
                locations.add(ParkingLocation(
                    id = "mumbai_${name.toSafeId()}",
                    name = name,
                    address = props.description,
                    latitude = feature.geometry?.coordinates?.getOrNull(1),
                    longitude = feature.geometry?.coordinates?.getOrNull(0),
                    cityName = "Mumbai",
                    twoWheelerCapacity = null, // These JSONs don't have capacity info
                    fourWheelerCapacity = null,
                    coverageType = props.coverageType,
                    prices = props.prices, 
                    openingTimes = props.openingTimes
                ))
            } catch (e: Exception) {
                Log.w("ParkingRepository", "Skipping malformed feature in Mumbai JSON: $featureElement")
            }
        }
        return locations
    }

    private fun parseStandardParking(jsonString: String, cityName: String): List<ParkingLocation> {
        val root = json.decodeFromString<StandardParkingRoot>(jsonString)
        return root.parkingData.parkingLocation.mapNotNull { location ->
            val name = location.name ?: return@mapNotNull null
            ParkingLocation(
                id = "${cityName.lowercase()}_${name.toSafeId()}",
                name = name,
                address = location.address,
                latitude = location.latitude,
                longitude = location.longitude,
                cityName = cityName,
                twoWheelerCapacity = location.twoWheelerParking,
                fourWheelerCapacity = location.fourWheelerParking,
                coverageType = location.coverageType,
                prices = location.prices,
                openingTimes = location.openingTimes
            )
        }
    }

    private fun String.toSafeId(): String = this.lowercase().replace(Regex("[^a-zA-Z0-9]"), "_")
}

// --- Self-Contained, Private Parsing Models ---

@Serializable private data class MumbaiRoot(val features: List<JsonElement>)
@Serializable private data class MumbaiFeature(val properties: MumbaiProperties?, val geometry: MumbaiGeometry?)
@Serializable private data class MumbaiProperties(
    @SerialName("Name") val name: String?, 
    @SerialName("Description") val description: String?, 
    val prices: List<PriceInfo>?, 
    val openingTimes: List<OpeningTime>?,
    @SerialName("CoverageType") val coverageType: String? = null
)
@Serializable private data class MumbaiGeometry(val coordinates: List<Double>?)

@Serializable private data class StandardParkingRoot(@SerialName("parking_data") val parkingData: StandardParkingData)
@Serializable private data class StandardParkingData(@SerialName("parking_location") val parkingLocation: List<StandardParkingLocation>)
@Serializable private data class StandardParkingLocation(
    val name: String?, 
    val address: String?, 
    val latitude: Double?, 
    val longitude: Double?, 
    @SerialName("two_wheeler_parking") val twoWheelerParking: Int?, 
    @SerialName("four_wheeler_parking") val fourWheelerParking: Int?, 
    val prices: List<PriceInfo>?, 
    val openingTimes: List<OpeningTime>?,
    @SerialName("CoverageType") val coverageType: String? = null
)
