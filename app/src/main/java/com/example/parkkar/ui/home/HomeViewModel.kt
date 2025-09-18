package com.example.parkkar.ui.home

import android.app.Application
import android.location.Geocoder
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

// Sealed class for search result states
sealed class SearchResultUiState {
    object Idle : SearchResultUiState()
    object Loading : SearchResultUiState()
    data class Success(val spots: List<ParkingSpot>) : SearchResultUiState()
    data class Error(val message: String) : SearchResultUiState()
    object GeocoderError : SearchResultUiState() // Specific error for Geocoder issues
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

    // Arriving Date and Time
    private val _arrivalDateTime = MutableStateFlow<Calendar>(Calendar.getInstance())
    val arrivalDateTime: StateFlow<Calendar> = _arrivalDateTime.asStateFlow()

    // Leaving Date and Time
    private val _leavingDateTime = MutableStateFlow<Calendar>(Calendar.getInstance().apply {
        add(Calendar.HOUR_OF_DAY, 1) // Default leaving time 1 hour after arrival
    })
    val leavingDateTime: StateFlow<Calendar> = _leavingDateTime.asStateFlow()

    init {
        loadAllParkingSpots()
    }

    private fun loadAllParkingSpots() {
        viewModelScope.launch {
            _searchResults.value = SearchResultUiState.Loading
            try {
                allParkingSpots = parkingRepository.getAllParkingSpots()
                // Initially, you might want to show some default spots or just be Idle
                // If allParkingSpots is empty after loading, it could also be an error or just no data.
                if (allParkingSpots.isEmpty()) {
                     _searchResults.value = SearchResultUiState.NoResults // Or Error("No parking data found in assets")
                } else {
                    // Decide what to show initially. For now, Idle.
                    // Or, if you want to show all spots initially (not recommended for large lists):
                    // _searchResults.value = SearchResultUiState.Success(allParkingSpots)
                   _searchResults.value = SearchResultUiState.Idle
                }
            } catch (e: Exception) {
                _searchResults.value = SearchResultUiState.Error("Failed to load parking data: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = SearchResultUiState.Idle // Or clear results
            return
        }
        performSearch(query)
    }

    private fun performSearch(query: String) {
        _searchResults.value = SearchResultUiState.Loading

        // Try to parse as LatLng first
        val latLng = parseLatLng(query)
        if (latLng != null) {
            searchByCoordinates(latLng.first, latLng.second)
        } else {
            searchByText(query)
        }
    }

    private fun parseLatLng(query: String): Pair<Double, Double>? {
        return try {
            val parts = query.split(",").map { it.trim().toDoubleOrNull() }
            if (parts.size == 2 && parts[0] != null && parts[1] != null) {
                Pair(parts[0]!!, parts[1]!!)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun searchByText(query: String) {
        viewModelScope.launch(Dispatchers.Default) { // Use Default dispatcher for CPU-bound filtering
            val lowerCaseQuery = query.lowercase(Locale.getDefault())
            val filteredSpots = allParkingSpots.filter { spot ->
                spot.parkingName?.lowercase(Locale.getDefault())?.contains(lowerCaseQuery) == true ||
                spot.address?.lowercase(Locale.getDefault())?.contains(lowerCaseQuery) == true ||
                spot.cityName.lowercase(Locale.getDefault()).contains(lowerCaseQuery) ||
                spot.zoneName?.lowercase(Locale.getDefault())?.contains(lowerCaseQuery) == true ||
                spot.wardName?.lowercase(Locale.getDefault())?.contains(lowerCaseQuery) == true
                // Add zipCode and area here if they are added to ParkingSpot
            }

            if (filteredSpots.isNotEmpty()) {
                _searchResults.value = SearchResultUiState.Success(filteredSpots)
            } else {
                _searchResults.value = SearchResultUiState.NoResults
            }
        }
    }

    private fun searchByCoordinates(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            var derivedCityName: String? = null
            try {
                // Geocoder runs on IO dispatcher
                val addresses = withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION") // For older API levels if minSdk < 33
                    geocoder.getFromLocation(latitude, longitude, 1)
                }
                if (addresses?.isNotEmpty() == true) {
                    derivedCityName = addresses[0].locality ?: addresses[0].subAdminArea // locality is city, subAdminArea as fallback
                }
            } catch (e: IOException) {
                // Geocoder can fail due to network or other issues
                 _searchResults.value = SearchResultUiState.GeocoderError
                e.printStackTrace()
                // Proceed to search local data even if Geocoder fails
            } catch (e: IllegalArgumentException) {
                // Invalid lat/lon
                _searchResults.value = SearchResultUiState.Error("Invalid coordinates.")
                e.printStackTrace()
                return@launch
            }

            val matchedSpots = mutableListOf<ParkingSpot>()

            // 1. Filter by derived city name if available
            if (derivedCityName != null) {
                val citySpots = allParkingSpots.filter {
                    it.cityName.equals(derivedCityName, ignoreCase = true)
                }
                // You might want to further refine citySpots by proximity to the input lat/lon here
                matchedSpots.addAll(citySpots)
            }

            // 2. Add spots from "parking_lat_lon.json" (which have generic city name "Parking lat lon")
            // that are very close to the input coordinates.
            // Define a threshold for "close" (e.g., 0.01 degrees for lat/lon is roughly 1.1km)
            val latLngThreshold = 0.01 
            val parkingLatLonFileSpots = allParkingSpots.filter {
                it.cityName.equals("Parking lat lon", ignoreCase = true) &&
                it.latitude != null && it.longitude != null &&
                (kotlin.math.abs(it.latitude - latitude) < latLngThreshold) &&
                (kotlin.math.abs(it.longitude - longitude) < latLngThreshold)
            }
            // Add without duplicates
            parkingLatLonFileSpots.forEach { spot ->
                if (!matchedSpots.any { it.id == spot.id }) {
                    matchedSpots.add(spot)
                }
            }
            
            // 3. If no city derived but local "parking_lat_lon" spots found, show them.
            // Or, if Geocoder failed, we might only have these.

            if (matchedSpots.isNotEmpty()) {
                _searchResults.value = SearchResultUiState.Success(matchedSpots)
            } else if (derivedCityName == null && _searchResults.value !is SearchResultUiState.GeocoderError) {
                _searchResults.value = SearchResultUiState.NoResults // No city from Geocoder, no local matches
            } else if (matchedSpots.isEmpty() && _searchResults.value !is SearchResultUiState.GeocoderError) {
                 _searchResults.value = SearchResultUiState.NoResults // City derived, but no spots found in that city or locally
            }
            // If GeocoderError was already set and no local spots found, it remains GeocoderError
        }
    }

    fun updateArrivalDateTime(calendar: Calendar) {
        _arrivalDateTime.value = calendar
        // Ensure leaving time is after arrival time
        if (_leavingDateTime.value.before(_arrivalDateTime.value)) {
            _leavingDateTime.value = (calendar.clone() as Calendar).apply {
                add(Calendar.HOUR_OF_DAY, 1)
            }
        }
    }

    fun updateLeavingDateTime(calendar: Calendar) {
        // Ensure leaving time is after arrival time
        if (calendar.after(_arrivalDateTime.value)) {
            _leavingDateTime.value = calendar
        } else {
            // Optionally, show a toast or message to the user that leaving time must be after arrival
            // For now, just set it to 1 hour after arrival if an invalid time is chosen
             _leavingDateTime.value = (_arrivalDateTime.value.clone() as Calendar).apply {
                add(Calendar.HOUR_OF_DAY, 1)
            }
        }
    }
}