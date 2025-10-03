package com.example.parkkar.ui.home

import android.app.Application
import android.location.Geocoder
import android.location.Location
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.parkkar.data.model.ParkingSpot
import com.example.parkkar.data.repository.ParkingDataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

sealed class SearchResultUiState {
    object Idle : SearchResultUiState()
    object Loading : SearchResultUiState()
    data class Success(val spots: List<ParkingSpot>, val message: String? = null, val isFromGeocoderOnly: Boolean = false) : SearchResultUiState()
    data class Error(val message: String) : SearchResultUiState()
    object GeocoderError : SearchResultUiState()
    object NoResults : SearchResultUiState()
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val parkingRepository = ParkingDataRepository(application)
    private val geocoder = Geocoder(application, Locale.getDefault())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<SearchResultUiState>(SearchResultUiState.Idle)
    val searchResults: StateFlow<SearchResultUiState> = _searchResults.asStateFlow()

    private var allParkingSpots: List<ParkingSpot> = emptyList()

    private val _arrivalDateTime = MutableStateFlow<Calendar>(Calendar.getInstance())
    val arrivalDateTime: StateFlow<Calendar> = _arrivalDateTime.asStateFlow()

    private val _leavingDateTime = MutableStateFlow<Calendar>(Calendar.getInstance().apply {
        add(Calendar.HOUR_OF_DAY, 1)
    })
    val leavingDateTime: StateFlow<Calendar> = _leavingDateTime.asStateFlow()

    private val VERY_CLOSE_THRESHOLD = 0.001 // Approx 111 meters
    private val NEARBY_THRESHOLD = 0.01   // Approx 1.1 km
    private val CITY_MATCH_THRESHOLD = 0.05 // Approx 5.5 km for wider city matching

    private val _isCoordinateSearch = MutableStateFlow(false)
    val isCoordinateSearch: StateFlow<Boolean> = _isCoordinateSearch.asStateFlow()

    init {
        loadAllParkingSpots()
    }

    private fun loadAllParkingSpots() {
        viewModelScope.launch {
            _searchResults.value = SearchResultUiState.Loading
            try {
                allParkingSpots = parkingRepository.getAllParkingSpots()
                Log.d("HomeViewModel", "Loaded ${allParkingSpots.size} parking spots from repository.")
                if (allParkingSpots.isEmpty()) {
                    _searchResults.value = SearchResultUiState.Error("No parking data found in assets.")
                } else {
                    _searchResults.value = SearchResultUiState.Idle // Go to Idle after successful load
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Failed to load parking data: ${e.message}", e)
                _searchResults.value = SearchResultUiState.Error("Failed to load parking data: ${e.message}")
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = SearchResultUiState.Idle
            _isCoordinateSearch.value = false
            return
        }
        if (allParkingSpots.isEmpty()){
            _searchResults.value = SearchResultUiState.Error("Parking data not loaded yet. Please try again shortly.")
            _isCoordinateSearch.value = false
            return
        }
        performSearch(query)
    }

    private fun parseLatLng(query: String): Pair<Double, Double>? {
        try {
            val parts = query.split(",").map { it.trim().toDoubleOrNull() }
            if (parts.size == 2 && parts[0] != null && parts[1] != null) {
                Log.d("HomeViewModel", "Parsed as comma-separated: ${parts[0]}, ${parts[1]}")
                return Pair(parts[0]!!, parts[1]!!)
            }
        } catch (e: Exception) {
            // Not simple comma-separated
        }

        try {
            val cleanedQuery = query.uppercase(Locale.getDefault())
                .replace("°", " ") // Replace degree symbol with space for easier parsing
                .replace(",", " ")
                .replace(Regex("\\s+"), " ") // Corrected: \s+ for one or more spaces
                .trim()

            // Regex for patterns like "19.123 N 72.456 E" or "19.123 72.456" (assuming N/E or simple order)
            // It allows for optional N/S/E/W characters.
            // Using standard strings with escaped backslashes for regex special characters
            val dmsPattern = Regex(
                "(-?\\d+\\.?\\d*)\\s*([NS])?\\s*(-?\\d+\\.?\\d*)\\s*([EW])?" +
                "|(-?\\d+\\.?\\d*)\\s+(-?\\d+\\.?\\d*)"
            )
            val match = dmsPattern.find(cleanedQuery)

            if (match != null) {
                if (match.groups[1] != null && match.groups[3] != null) { // Format with N/S and E/W
                    val latValStr = match.groupValues[1]
                    val latDirStr = match.groupValues[2].ifEmpty { "N" } // Default to N if no direction
                    val lonValStr = match.groupValues[3]
                    val lonDirStr = match.groupValues[4].ifEmpty { "E" } // Default to E if no direction

                    var lat = latValStr.toDoubleOrNull()
                    var lon = lonValStr.toDoubleOrNull()

                    if (lat != null && lon != null) {
                        if (latDirStr == "S") lat *= -1
                        if (lonDirStr == "W") lon *= -1
                        Log.d("HomeViewModel", "Parsed as DMS/Directional: Lat $lat, Lon $lon")
                        return Pair(lat, lon)
                    }
                } else if (match.groups[5] != null && match.groups[6] != null) { // Format with two numbers
                    val latValStr = match.groupValues[5]
                    val lonValStr = match.groupValues[6]
                     var lat = latValStr.toDoubleOrNull()
                    var lon = lonValStr.toDoubleOrNull()
                     if (lat != null && lon != null) {
                        // Basic validation: lat between -90 and 90, lon between -180 and 180
                        if (lat in -90.0..90.0 && lon in -180.0..180.0) {
                            Log.d("HomeViewModel", "Parsed as two numbers: Lat $lat, Lon $lon")
                            return Pair(lat, lon)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Failed to parse LatLng from query '$query': ${e.message}", e)
        }
        return null
    }


    private fun performSearch(query: String) {
        _searchResults.value = SearchResultUiState.Loading
        Log.d("HomeViewModel", "Performing search for: $query")

        val latLng = parseLatLng(query)
        if (latLng != null) {
            _isCoordinateSearch.value = true
            searchByCoordinates(latLng.first, latLng.second, query)
        } else {
            _isCoordinateSearch.value = false
            searchByText(query)
        }
    }

    private fun searchByText(query: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val lowerCaseQuery = query.lowercase(Locale.getDefault())
            val filteredSpots = allParkingSpots.filter { spot ->
                val nameMatch = spot.parkingName?.lowercase(Locale.getDefault())?.contains(lowerCaseQuery) == true
                val addressMatch = spot.address?.lowercase(Locale.getDefault())?.contains(lowerCaseQuery) == true
                val cityMatch = spot.cityName.lowercase(Locale.getDefault()).contains(lowerCaseQuery)
                val zoneMatch = spot.zoneName?.lowercase(Locale.getDefault())?.contains(lowerCaseQuery) == true
                val wardMatch = spot.wardName?.lowercase(Locale.getDefault())?.contains(lowerCaseQuery) == true
                nameMatch || addressMatch || cityMatch || zoneMatch || wardMatch
            }
            Log.d("HomeViewModel", "Text search found ${filteredSpots.size} spots.")

            if (filteredSpots.isNotEmpty()) {
                _searchResults.value = SearchResultUiState.Success(
                    filteredSpots.sortedByRelevanceToTextQuery(query),
                    message = "Found ${filteredSpots.size} locations matching '$query'."
                )
            } else {
                _searchResults.value = SearchResultUiState.NoResults
            }
        }
    }

    private fun List<ParkingSpot>.sortedByRelevanceToTextQuery(query: String): List<ParkingSpot> {
        val lowerCaseQuery = query.lowercase(Locale.getDefault())
        return this.sortedWith(compareByDescending<ParkingSpot> {
            (it.parkingName?.lowercase(Locale.getDefault())?.contains(lowerCaseQuery) == true)
        }.thenByDescending {
            (it.address?.lowercase(Locale.getDefault())?.contains(lowerCaseQuery) == true)
        }.thenByDescending {
            (it.cityName.lowercase(Locale.getDefault())?.contains(lowerCaseQuery) == true)
        })
    }

    private fun searchByCoordinates(latitude: Double, longitude: Double, originalQuery: String) {
        viewModelScope.launch {
            var derivedCityNameFromGeocoder: String? = null
            var geocodedAddress: String? = null
            var geocoderFailed = false

            try {
                val addresses = withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    if (Geocoder.isPresent()) geocoder.getFromLocation(latitude, longitude, 1) else null
                }
                if (addresses?.isNotEmpty() == true) {
                    val firstAddress = addresses[0]
                    derivedCityNameFromGeocoder = firstAddress.locality ?: firstAddress.subAdminArea ?: firstAddress.adminArea
                    geocodedAddress = listOfNotNull(firstAddress.subLocality, firstAddress.locality, firstAddress.subAdminArea, firstAddress.adminArea, firstAddress.countryName)
                        .filter { it.isNotBlank() }
                        .distinct()
                        .joinToString(", ")
                    if (geocodedAddress.isNullOrBlank()) geocodedAddress = firstAddress.getAddressLine(0)

                    Log.d("HomeViewModel", "Geocoder derived city: $derivedCityNameFromGeocoder, Full Address: $geocodedAddress for $latitude, $longitude")
                } else {
                    Log.w("HomeViewModel", "Geocoder returned no addresses for $latitude, $longitude")
                }
            } catch (e: IOException) {
                Log.e("HomeViewModel", "Geocoder IOException for $latitude, $longitude: ${e.message}", e)
                geocoderFailed = true
            } catch (e: IllegalArgumentException) {
                Log.e("HomeViewModel", "Geocoder IllegalArgumentException for $latitude, $longitude: ${e.message}", e)
                _searchResults.value = SearchResultUiState.Error("Invalid coordinates provided.")
                return@launch
            }

            val resultSpots = mutableListOf<ParkingSpot>()
            var searchMessage: String? = null

            val veryCloseMatches = allParkingSpots.filter { spot ->
                spot.latitude != null && spot.longitude != null &&
                        abs(spot.latitude - latitude) < VERY_CLOSE_THRESHOLD &&
                        abs(spot.longitude - longitude) < VERY_CLOSE_THRESHOLD
            }
            resultSpots.addAll(veryCloseMatches)
            Log.d("HomeViewModel", "Found ${veryCloseMatches.size} very close direct matches from dataset.")

            if (derivedCityNameFromGeocoder != null) {
                searchMessage = "Spots near ${geocodedAddress ?: derivedCityNameFromGeocoder}"
                val spotsInOrNearDerivedCity = allParkingSpots.filter { spot ->
                    !resultSpots.any { rs -> rs.id == spot.id } &&
                            (spot.cityName.equals(derivedCityNameFromGeocoder, ignoreCase = true) ||
                                    (spot.latitude != null && spot.longitude != null &&
                                            abs(spot.latitude - latitude) < CITY_MATCH_THRESHOLD &&
                                            abs(spot.longitude - longitude) < CITY_MATCH_THRESHOLD))
                }
                resultSpots.addAll(spotsInOrNearDerivedCity)
                Log.d("HomeViewModel", "Found ${spotsInOrNearDerivedCity.size} spots in/near Geocoded city: $derivedCityNameFromGeocoder.")
            } else if (geocoderFailed) {
                searchMessage = "Geocoder unavailable. Showing best matches from data."
            } else {
                searchMessage = "Showing best matches from data for the coordinates."
            }

            if (resultSpots.size < 3) { // If not enough very close or city-specific matches
                val nearbySpotsFromAll = allParkingSpots.filter { spot ->
                    !resultSpots.any { rs -> rs.id == spot.id } &&
                            spot.latitude != null && spot.longitude != null &&
                            abs(spot.latitude - latitude) < NEARBY_THRESHOLD &&
                            abs(spot.longitude - longitude) < NEARBY_THRESHOLD
                }
                resultSpots.addAll(nearbySpotsFromAll)
                Log.d("HomeViewModel", "Added ${nearbySpotsFromAll.size} general nearby spots from dataset.")
            }

            val finalSpots = resultSpots.distinctBy { it.id }.sortedBy { spot ->
                val spotLocation = Location("spot").apply {
                    this.latitude = spot.latitude ?: 0.0
                    this.longitude = spot.longitude ?: 0.0
                }
                val searchLocation = Location("search").apply {
                    this.latitude = latitude
                    this.longitude = longitude
                }
                spotLocation.distanceTo(searchLocation)
            }

            Log.d("HomeViewModel", "Final ${finalSpots.size} spots for coordinate search. Message: $searchMessage")

            if (finalSpots.isNotEmpty()) {
                _searchResults.value = SearchResultUiState.Success(finalSpots, searchMessage, isFromGeocoderOnly = false)
            } else {
                if (geocodedAddress != null) {
                    val geocodedSpot = ParkingSpot(
                        id = UUID.randomUUID().toString(),
                        cityName = derivedCityNameFromGeocoder ?: "Unknown Area",
                        parkingName = "Location: ${geocodedAddress.take(40)}${if(geocodedAddress.length > 40) "..." else ""}",
                        address = geocodedAddress,
                        latitude = latitude,
                        longitude = longitude,
                        fourWheelerSpots = -1, // Indicates no specific parking data from our system
                        twoWheelerSpots = -1
                    )
                    _searchResults.value = SearchResultUiState.Success(listOf(geocodedSpot), "Location: $geocodedAddress (No specific parking data in our system)", isFromGeocoderOnly = true)
                } else if (geocoderFailed) {
                    _searchResults.value = SearchResultUiState.GeocoderError // Geocoder failed, and no local results
                } else {
                    _searchResults.value = SearchResultUiState.NoResults // No local results, Geocoder didn't find anything usable
                }
            }
        }
    }

    fun updateArrivalDateTime(calendar: Calendar) {
        _arrivalDateTime.value = calendar
        if (_leavingDateTime.value.before(_arrivalDateTime.value)) {
            _leavingDateTime.value = (calendar.clone() as Calendar).apply {
                add(Calendar.HOUR_OF_DAY, 1)
            }
        }
    }

    fun updateLeavingDateTime(calendar: Calendar) {
        if (calendar.after(_arrivalDateTime.value)) {
            _leavingDateTime.value = calendar
        } else {
            _leavingDateTime.value = (_arrivalDateTime.value.clone() as Calendar).apply {
                add(Calendar.HOUR_OF_DAY, 1)
            }
        }
    }
}
