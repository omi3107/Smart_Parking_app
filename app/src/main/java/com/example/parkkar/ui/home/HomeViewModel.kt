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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
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

    private val parkingRepository = ParkingDataRepository.getInstance(application)
    private val geocoder = Geocoder(application, Locale.getDefault())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Start with a null list to explicitly track whether the initial data has been loaded.
    private val allParkingSpots: StateFlow<List<ParkingSpot>?> = parkingRepository.getAllParkingSpotsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<SearchResultUiState> =
        combine(_searchQuery.debounce(300L), allParkingSpots) { query, spots ->
            query to spots // Pair the latest query and spots list
        }.flatMapLatest { (query, spots) ->
            if (query.isBlank()) {
                flowOf(SearchResultUiState.Idle)
            } else {
                if (spots == null) {
                    // If we have a query but spots are null, the initial data is still loading.
                    flowOf(SearchResultUiState.Loading)
                } else {
                    // Once we have a non-null list (even if empty), we can perform the search.
                    flow {
                        emit(SearchResultUiState.Loading) // Show loading for the search operation itself
                        val result = performSearch(query, spots)
                        emit(result)
                    }
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SearchResultUiState.Idle
        )

    private val _arrivalDateTime = MutableStateFlow<Calendar>(Calendar.getInstance())
    val arrivalDateTime: StateFlow<Calendar> = _arrivalDateTime.asStateFlow()

    private val _leavingDateTime = MutableStateFlow<Calendar>(Calendar.getInstance().apply {
        add(Calendar.HOUR_OF_DAY, 1)
    })
    val leavingDateTime: StateFlow<Calendar> = _leavingDateTime.asStateFlow()

    private val veryCloseThreshold = 0.001
    private val nearbyThreshold = 0.01
    private val cityMatchThreshold = 0.05

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    private fun parseLatLng(query: String): Pair<Double, Double>? {
        try {
            val parts = query.split(",").map { it.trim().toDoubleOrNull() }
            if (parts.size == 2 && parts[0] != null && parts[1] != null) {
                return Pair(parts[0]!!, parts[1]!!)
            }
        } catch (_: Exception) { /* Ignore */ }

        try {
            val cleanedQuery = query.uppercase(Locale.getDefault()).replace("°", " ").replace(",", " ").replace(Regex("\\s+"), " ").trim()
            val dmsPattern = Regex("(-?\\d+\\.?\\d*)\\s*([NS])?\\s*(-?\\d+\\.?\\d*)\\s*([EW])?|(-?\\d+\\.?\\d*)\\s+(-?\\d+\\.?\\d*)")
            val match = dmsPattern.find(cleanedQuery)

            if (match != null) {
                if (match.groups[1] != null && match.groups[3] != null) {
                    var lat = match.groupValues[1].toDoubleOrNull()
                    val latDir = match.groupValues[2].ifEmpty { "N" }
                    var lon = match.groupValues[3].toDoubleOrNull()
                    val lonDir = match.groupValues[4].ifEmpty { "E" }
                    if (lat != null && lon != null) {
                        if (latDir == "S") lat *= -1
                        if (lonDir == "W") lon *= -1
                        return Pair(lat, lon)
                    }
                } else if (match.groups[5] != null && match.groups[6] != null) {
                    val lat = match.groupValues[5].toDoubleOrNull()
                    val lon = match.groupValues[6].toDoubleOrNull()
                    if (lat != null && lon != null && lat in -90.0..90.0 && lon in -180.0..180.0) {
                        return Pair(lat, lon)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Failed to parse LatLng from query '$query': ${e.message}", e)
        }
        return null
    }

    private suspend fun performSearch(query: String, spots: List<ParkingSpot>): SearchResultUiState {
        val latLng = parseLatLng(query)
        return if (latLng != null) {
            searchByCoordinates(latLng.first, latLng.second, spots)
        } else {
            searchByText(query, spots)
        }
    }

    private fun searchByText(query: String, spots: List<ParkingSpot>): SearchResultUiState {
        val lowerCaseQuery = query.lowercase(Locale.getDefault())
        val filteredSpots = spots.filter { spot ->
            val nameMatch = spot.parkingName?.lowercase(Locale.getDefault())?.contains(lowerCaseQuery) == true
            val addressMatch = spot.address?.lowercase(Locale.getDefault())?.contains(lowerCaseQuery) == true
            val cityMatch = spot.cityName.lowercase(Locale.getDefault()).contains(lowerCaseQuery)
            val zoneMatch = spot.zoneName?.lowercase(Locale.getDefault())?.contains(lowerCaseQuery) == true
            val wardMatch = spot.wardName?.lowercase(Locale.getDefault())?.contains(lowerCaseQuery) == true
            nameMatch || addressMatch || cityMatch || zoneMatch || wardMatch
        }

        return if (filteredSpots.isNotEmpty()) {
            SearchResultUiState.Success(filteredSpots.sortedByRelevanceToTextQuery(query), message = "Found ${filteredSpots.size} locations matching '$query'.")
        } else {
            SearchResultUiState.NoResults
        }
    }

    private fun List<ParkingSpot>.sortedByRelevanceToTextQuery(query: String): List<ParkingSpot> {
        val lowerCaseQuery = query.lowercase(Locale.getDefault())
        return this.sortedWith(compareByDescending<ParkingSpot> { it.parkingName?.lowercase(Locale.getDefault())?.contains(lowerCaseQuery) == true }
            .thenByDescending { it.address?.lowercase(Locale.getDefault())?.contains(lowerCaseQuery) == true }
            .thenByDescending { it.cityName.lowercase(Locale.getDefault()).contains(lowerCaseQuery) })
    }

    private suspend fun searchByCoordinates(latitude: Double, longitude: Double, spots: List<ParkingSpot>): SearchResultUiState {
        return withContext(Dispatchers.IO) {
            var derivedCityNameFromGeocoder: String? = null
            var geocodedAddress: String? = null
            var geocoderFailed = false

            try {
                @Suppress("DEPRECATION")
                val addresses = if (Geocoder.isPresent()) geocoder.getFromLocation(latitude, longitude, 1) else null
                if (!addresses.isNullOrEmpty()) {
                    val firstAddress = addresses[0]
                    derivedCityNameFromGeocoder = firstAddress.locality ?: firstAddress.subAdminArea ?: firstAddress.adminArea
                    geocodedAddress = listOfNotNull(firstAddress.subLocality, firstAddress.locality, firstAddress.subAdminArea, firstAddress.adminArea, firstAddress.countryName)
                        .filter { it.isNotBlank() }.distinct().joinToString(", ")
                    if (geocodedAddress.isNullOrBlank()) geocodedAddress = firstAddress.getAddressLine(0)
                } else {
                    Log.w("HomeViewModel", "Geocoder returned no addresses for $latitude, $longitude")
                }
            } catch (_: IOException) {
                geocoderFailed = true
            } catch (_: IllegalArgumentException) {
                return@withContext SearchResultUiState.Error("Invalid coordinates provided.")
            }

            val resultSpots = mutableListOf<ParkingSpot>()
            var searchMessage: String? = null

            val veryCloseMatches = spots.filter {
                it.latitude != null && it.longitude != null && abs(it.latitude - latitude) < veryCloseThreshold && abs(it.longitude - longitude) < veryCloseThreshold
            }
            resultSpots.addAll(veryCloseMatches)

            if (derivedCityNameFromGeocoder != null) {
                searchMessage = "Spots near ${geocodedAddress ?: derivedCityNameFromGeocoder}"
                val spotsInOrNearDerivedCity = spots.filter { spot ->
                    !resultSpots.any { rs -> rs.id == spot.id } && (spot.cityName.equals(derivedCityNameFromGeocoder, ignoreCase = true) ||
                            (spot.latitude != null && spot.longitude != null && abs(spot.latitude - latitude) < cityMatchThreshold && abs(spot.longitude - longitude) < cityMatchThreshold))
                }
                resultSpots.addAll(spotsInOrNearDerivedCity)
            }

            if (resultSpots.size < 3) {
                val nearbySpotsFromAll = spots.filter { spot ->
                    !resultSpots.any { rs -> rs.id == spot.id } && spot.latitude != null && spot.longitude != null &&
                            abs(spot.latitude - latitude) < nearbyThreshold && abs(spot.longitude - longitude) < nearbyThreshold
                }
                resultSpots.addAll(nearbySpotsFromAll)
            }

            val finalSpots = resultSpots.distinctBy { it.id }.sortedBy { spot ->
                Location("spot").apply { this.latitude = spot.latitude ?: 0.0; this.longitude = spot.longitude ?: 0.0 }
                    .distanceTo(Location("search").apply { this.latitude = latitude; this.longitude = longitude })
            }

            if (finalSpots.isNotEmpty()) {
                SearchResultUiState.Success(finalSpots, searchMessage, isFromGeocoderOnly = false)
            } else {
                if (geocodedAddress != null) {
                    val geocodedSpot = ParkingSpot(UUID.randomUUID().toString(), derivedCityNameFromGeocoder ?: "Unknown Area", "Location: ${geocodedAddress.take(40)}${if (geocodedAddress.length > 40) "..." else ""}", geocodedAddress, latitude, longitude, -1, -1)
                    SearchResultUiState.Success(listOf(geocodedSpot), "Location: $geocodedAddress (No specific parking data in our system)", isFromGeocoderOnly = true)
                } else if (geocoderFailed) {
                    SearchResultUiState.GeocoderError
                } else {
                    SearchResultUiState.NoResults
                }
            }
        }
    }

    fun updateArrivalDateTime(calendar: Calendar) {
        _arrivalDateTime.value = calendar
        if (_leavingDateTime.value.before(_arrivalDateTime.value)) {
            _leavingDateTime.value = (calendar.clone() as Calendar).apply { add(Calendar.HOUR_OF_DAY, 1) }
        }
    }

    fun updateLeavingDateTime(calendar: Calendar) {
        if (calendar.after(_arrivalDateTime.value)) {
            _leavingDateTime.value = calendar
        } else {
            _leavingDateTime.value = (_arrivalDateTime.value.clone() as Calendar).apply { add(Calendar.HOUR_OF_DAY, 1) }
        }
    }
}
