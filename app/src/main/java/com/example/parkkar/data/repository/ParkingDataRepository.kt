package com.example.parkkar.data.repository

import android.content.Context
import com.example.parkkar.data.model.ParkingSpot
import kotlinx.serialization.json.*
import java.io.IOException
import java.util.*

class ParkingDataRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    suspend fun getAllParkingSpots(): List<ParkingSpot> {
        val allSpots = mutableListOf<ParkingSpot>()
        try {
            val assetManager = context.assets
            // Updated to include parking_lat_lon.json
            val fileNames = assetManager.list("")?.filter {
                it.endsWith("_parking.json") || it == "parking_lat_lon.json"
            }

            fileNames?.forEach { fileName ->
                try {
                    val jsonString = assetManager.open(fileName).bufferedReader().use { it.readText() }
                    val cityParkingSpots = parseCityJson(fileName, jsonString) // parseCityJson will route
                    allSpots.addAll(cityParkingSpots)
                } catch (e: IOException) {
                    e.printStackTrace()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return allSpots
    }

    private fun parseCityJson(fileName: String, jsonString: String): List<ParkingSpot> {
        val cityName = fileName.substringBefore(".json").replaceFirstChar { // Adjusted for parking_lat_lon
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }.replace("_", " ") // Make it more readable, e.g., "Parking lat lon"

        val rootElement = json.parseToJsonElement(jsonString)

        return when (cityName.lowercase(Locale.getDefault()).replace(" ", "_")) { // Normalize for when condition
            "chandigarh_parking" -> parseChandigarhJson(cityName, rootElement.jsonObject)
            "panaji_parking" -> parsePanajiJson(cityName, rootElement.jsonObject)
            "thane_parking" -> parseThaneJson(cityName, rootElement.jsonObject)
            "mumbai_parking" -> parseMumbaiOrBengaluruJson(cityName, rootElement.jsonObject)
            "bengaluru_osm_parking_spots" -> parseMumbaiOrBengaluruJson(cityName, rootElement.jsonObject) // Corrected original filename
            "surat_parking_data" -> parseSuratJson(cityName, rootElement.jsonObject) // Corrected original filename
            "parking_lat_lon" -> parseParkingLatLonJson(cityName, rootElement.jsonArray) // New case
            else -> {
                // Fallback for potentially different naming of the city files based on previous logs
                if (fileName.startsWith("bengaluru_osm_parking_spots")) {
                    parseMumbaiOrBengaluruJson("Bengaluru", rootElement.jsonObject)
                } else if (fileName.startsWith("surat_parking_data")) {
                    parseSuratJson("Surat", rootElement.jsonObject)
                }
                else {
                    println("Unknown file format or city for: $fileName")
                    emptyList()
                }
            }
        }
    }

    // Helper to extract string from table-cell like structures
    private fun getStringFromTableCell(cell: JsonElement?): String? {
        return cell?.jsonObject?.get("text:p")?.jsonPrimitive?.contentOrNull?.takeIf { it != "NA" }
    }


    // Helper to safely convert JsonPrimitive to Double
    private fun JsonPrimitive?.toDoubleOrNullSafe(): Double? {
        return if (this != null && !this.isString) {
            this.doubleOrNull
        } else if (this != null && this.isString) {
            val stringVal = this.content.trim()
            if (stringVal.equals("NA", ignoreCase = true)) null else stringVal.toDoubleOrNull()
        } else {
            null
        }
    }

    // Helper to safely convert JsonElement to Double
    private fun JsonElement?.toDoubleOrNullSafe(): Double? {
        return when (this) {
            is JsonPrimitive -> this.toDoubleOrNullSafe()
            else -> null
        }
    }

    // Helper to safely convert JsonPrimitive to Int, defaulting to 0
    private fun JsonPrimitive?.toIntOrDefault(default: Int = 0): Int {
        return if (this != null && !this.isString) {
            this.intOrNull ?: default
        } else if (this != null && this.isString) {
            val stringVal = this.content.trim()
            if (stringVal.equals("NA", ignoreCase = true)) default else stringVal.toIntOrNull() ?: default
        } else {
            default
        }
    }

    // Helper to safely convert JsonElement to Int, defaulting to 0
    private fun JsonElement?.toIntOrDefault(default: Int = 0): Int {
        return when (this) {
            is JsonPrimitive -> this.toIntOrDefault(default)
            else -> default
        }
    }

    private fun parseChandigarhJson(cityName: String, jsonObject: JsonObject): List<ParkingSpot> {
        val spots = mutableListOf<ParkingSpot>()
        jsonObject["Sheet1"]?.jsonArray?.forEach { element ->
            val item = element.jsonObject
            spots.add(
                ParkingSpot(
                    cityName = cityName,
                    parkingName = item["Parking_Name"]?.jsonPrimitive?.contentOrNull,
                    address = item["Parking_Address"]?.jsonPrimitive?.contentOrNull,
                    latitude = item["LATITUDE"].toDoubleOrNullSafe(),
                    longitude = item["LONGITUDE"].toDoubleOrNullSafe(),
                    fourWheelerSpots = item["No_of_4_wheeler_parking"].toIntOrDefault(),
                    twoWheelerSpots = item["No_of_2_wheeler_parking"].toIntOrDefault(),
                    zoneName = item["Zone_Name"]?.jsonPrimitive?.contentOrNull?.takeIf { it != "NA" },
                    wardName = item["Ward_Name"]?.jsonPrimitive?.contentOrNull?.takeIf { it != "NA" }
                )
            )
        }
        return spots
    }

    private fun parsePanajiJson(cityName: String, jsonObject: JsonObject): List<ParkingSpot> {
        val spots = mutableListOf<ParkingSpot>()
        try {
            val rows = jsonObject["office:document"]?.jsonObject
                ?.get("office:body")?.jsonObject
                ?.get("office:spreadsheet")?.jsonObject
                ?.get("table:table")?.jsonObject
                ?.get("table:table-row")?.jsonArray

            rows?.drop(1)?.forEach { rowElement ->
                val cells = rowElement.jsonObject["table:table-cell"]?.jsonArray
                if (cells != null && cells.size >= 9) {
                    val latString = getStringFromTableCell(cells[5])
                    val lonString = getStringFromTableCell(cells[6])
                    val fourWheelerString = getStringFromTableCell(cells[7])
                    val twoWheelerString = getStringFromTableCell(cells[8])

                    spots.add(
                        ParkingSpot(
                            cityName = cityName,
                            parkingName = getStringFromTableCell(cells[3]),
                            address = getStringFromTableCell(cells[4]),
                            latitude = latString?.toDoubleOrNull(),
                            longitude = lonString?.toDoubleOrNull(),
                            fourWheelerSpots = fourWheelerString?.toIntOrNull() ?: 0,
                            twoWheelerSpots = twoWheelerString?.toIntOrNull() ?: 0,
                            zoneName = getStringFromTableCell(cells[1]),
                            wardName = getStringFromTableCell(cells[2])
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return spots
    }

    private fun parseThaneJson(cityName: String, jsonObject: JsonObject): List<ParkingSpot> {
        val spots = mutableListOf<ParkingSpot>()
        jsonObject["parking_data"]?.jsonObject?.get("parking_location")?.jsonArray?.forEach { element ->
            val item = element.jsonObject
            spots.add(
                ParkingSpot(
                    cityName = cityName,
                    parkingName = item["name"]?.jsonPrimitive?.contentOrNull,
                    address = item["address"]?.jsonPrimitive?.contentOrNull,
                    latitude = item["latitude"].toDoubleOrNullSafe(),
                    longitude = item["longitude"].toDoubleOrNullSafe(),
                    fourWheelerSpots = item["four_wheeler_parking"].toIntOrDefault(),
                    twoWheelerSpots = item["two_wheeler_parking"].toIntOrDefault()
                )
            )
        }
        return spots
    }

    private fun parseMumbaiOrBengaluruJson(cityName: String, jsonObject: JsonObject): List<ParkingSpot> {
        val spots = mutableListOf<ParkingSpot>()
        jsonObject["features"]?.jsonArray?.forEach { featureElement ->
            val properties = featureElement.jsonObject["properties"]?.jsonObject
            val geometry = featureElement.jsonObject["geometry"]?.jsonObject
            val coordinates = geometry?.get("coordinates")?.jsonArray

            val lon = coordinates?.get(0).toDoubleOrNullSafe()
            val lat = coordinates?.get(1).toDoubleOrNullSafe()

            spots.add(
                ParkingSpot(
                    cityName = cityName,
                    parkingName = properties?.get("Name")?.jsonPrimitive?.contentOrNull,
                    address = properties?.get("Description")?.jsonPrimitive?.contentOrNull?.ifEmpty { null },
                    latitude = lat,
                    longitude = lon,
                    fourWheelerSpots = 0,
                    twoWheelerSpots = 0
                )
            )
        }
        return spots
    }

    private fun parseSuratJson(cityName: String, jsonObject: JsonObject): List<ParkingSpot> {
        val spots = mutableListOf<ParkingSpot>()
        jsonObject["DATASET"]?.jsonObject?.entries?.forEach { entry ->
            val item = entry.value.jsonObject

            val latElement = item["LATITUDE"]
            val lonElement = item["LONGITUDE"]

            var parsedLat: Double? = null
            if (latElement is JsonPrimitive) {
                parsedLat = latElement.contentOrNull?.let { convertDMSLatToDouble(it) ?: it.toDoubleOrNull() }
            } else {
                parsedLat = latElement.toDoubleOrNullSafe()
            }

            var parsedLon: Double? = null
            if (lonElement is JsonPrimitive) {
                parsedLon = lonElement.contentOrNull?.let { convertDMSLonToDouble(it) ?: it.toDoubleOrNull() }
            } else {
                parsedLon = lonElement.toDoubleOrNullSafe()
            }

            spots.add(
                ParkingSpot(
                    cityName = item["CITYNAME"]?.jsonPrimitive?.contentOrNull ?: cityName,
                    parkingName = item["NAME_OF_PARKING"]?.jsonPrimitive?.contentOrNull,
                    address = item["PARKING_ADDRESS"]?.jsonPrimitive?.contentOrNull,
                    latitude = parsedLat,
                    longitude = parsedLon,
                    fourWheelerSpots = item["NO._OF_4_WHEELER_PARKING"].toIntOrDefault(),
                    twoWheelerSpots = item["NO._OF_2_WHEELER_PARKING"].toIntOrDefault(),
                    zoneName = item["ZONE_NAME"]?.jsonPrimitive?.contentOrNull?.takeIf { it != "NA" },
                    wardName = item["WARD_NAME"]?.jsonPrimitive?.contentOrNull?.takeIf { it != "NA" }
                )
            )
        }
        return spots
    }

    // New parsing function for parking_lat_lon.json
    private fun parseParkingLatLonJson(derivedCityName: String, jsonArray: JsonArray): List<ParkingSpot> {
        val spots = mutableListOf<ParkingSpot>()
        jsonArray.drop(1).forEach { element -> // Drop header row
            // Each element is a JsonObject like {"table:table-cell": [...]}
            val cells = element.jsonObject["table:table-cell"]?.jsonArray
            if (cells != null && cells.size >= 5) { // Need at least up to Longitude index
                val capacityStr = getStringFromTableCell(cells[2]) // Capacity is at index 2
                val latitudeStr = getStringFromTableCell(cells[3]) // Latitude is at index 3
                val longitudeStr = getStringFromTableCell(cells[4]) // Longitude is at index 4
                // val idStr = getStringFromTableCell(cells[0]) // Available if needed
                // val systemCodeStr = getStringFromTableCell(cells[1]) // Available if needed


                spots.add(
                    ParkingSpot(
                        // Use a generic city name or consider making cityName nullable
                        // For now, using the derived name which will be "Parking Lat Lon"
                        cityName = derivedCityName,
                        parkingName = null, // Or use idStr / systemCodeStr if suitable
                        address = null,
                        latitude = latitudeStr?.toDoubleOrNull(),
                        longitude = longitudeStr?.toDoubleOrNull(),
                        fourWheelerSpots = capacityStr?.toIntOrNull() ?: 0,
                        twoWheelerSpots = 0 // No specific 2-wheeler data in this file from user spec
                    )
                )
            }
        }
        return spots
    }


    private fun convertDMSLatToDouble(dms: String?): Double? {
        if (dms == null || dms.equals("NA", ignoreCase = true)) return null
        // Placeholder: Needs proper DMS parsing logic (e.g., regex for D?M'S" H)
        // For "21?11'17.1\" N"
        // Example: val parts = dms.replace("?"," ").replace("'"," ").replace("\""," ").split(" ")
        // if (parts.size >= 4) return (parts[0].toDouble() + parts[1].toDouble()/60 + parts[2].toDouble()/3600) * if(parts[3] == "S") -1 else 1
        return dms.toDoubleOrNull() // Fallback if not DMS or simple number
    }

    private fun convertDMSLonToDouble(dms: String?): Double? {
        if (dms == null || dms.equals("NA", ignoreCase = true)) return null
        // Placeholder: Needs proper DMS parsing logic
        return dms.toDoubleOrNull() // Fallback
    }
}
