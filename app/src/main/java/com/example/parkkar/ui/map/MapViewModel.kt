package com.example.parkkar.ui.map

import android.app.Application
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.parkkar.model.ParkingLocation
import com.example.parkkar.repository.ParkingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.maplibre.android.geometry.LatLng
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

sealed class SearchResultUiState {
    object Idle : SearchResultUiState()
    object Loading : SearchResultUiState()
    data class Success(val spots: List<ParkingLocation>) : SearchResultUiState()
    data class Error(val message: String) : SearchResultUiState()
    object NoResults : SearchResultUiState()
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class MapViewModel(private val app: Application) : AndroidViewModel(app) {

    private val geocoder = Geocoder(app, Locale.getDefault())

    private val _allParkingSpots = MutableStateFlow<List<ParkingLocation>>(emptyList())
    val allParkingSpots: StateFlow<List<ParkingLocation>> = _allParkingSpots.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedParkingSpot = MutableStateFlow<ParkingLocation?>(null)
    val selectedParkingSpot: StateFlow<ParkingLocation?> = _selectedParkingSpot.asStateFlow()

    private val _geocodedLocation = MutableStateFlow<LatLng?>(null)
    val geocodedLocation: StateFlow<LatLng?> = _geocodedLocation.asStateFlow()

    val searchResults: StateFlow<SearchResultUiState> = _searchQuery
        .debounce(300)
        .combine(_allParkingSpots) { query, spots ->
            Pair(query, spots)
        }
        .flatMapLatest { (query, spots) ->
            flow {
                if (query.isBlank()) {
                    emit(SearchResultUiState.Idle)
                    return@flow
                }
                // This check is the definitive fix for the race condition.
                // It ensures we don't proceed until the parking spot data is loaded.
                if (spots.isEmpty()) {
                    emit(SearchResultUiState.Loading)
                    return@flow
                }

                emit(SearchResultUiState.Loading)
                val localResults = performSearch(query, spots)
                if (localResults.isNotEmpty()) {
                    emit(SearchResultUiState.Success(localResults))
                } else {
                    // Only now is it safe to attempt geocoding as a fallback.
                    val geocoded = geocodeQuery(query)
                    if (geocoded != null) {
                        _geocodedLocation.value = geocoded
                        emit(SearchResultUiState.NoResults) // For the toast message
                    } else {
                        emit(SearchResultUiState.NoResults)
                    }
                }
            }.flowOn(Dispatchers.IO)
        }
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = SearchResultUiState.Idle
        )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _allParkingSpots.value = ParkingRepository.getAllParkingSpots(app)
        }
    }

    private suspend fun geocodeQuery(query: String): LatLng? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCoroutine { continuation ->
                try {
                    geocoder.getFromLocationName(query, 1) { addresses ->
                        val location = addresses.firstOrNull()
                        continuation.resume(location?.let { LatLng(it.latitude, it.longitude) })
                    }
                } catch (e: IOException) {
                    continuation.resume(null)
                }
            }
        } else {
            withContext(Dispatchers.IO) {
                try {
                    @Suppress("DEPRECATION")
                    val addresses: List<Address>? = geocoder.getFromLocationName(query, 1)
                    val location = addresses?.firstOrNull()
                    location?.let { LatLng(it.latitude, it.longitude) }
                } catch (e: IOException) {
                    null
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onParkingSpotSelected(spot: ParkingLocation?) {
        _selectedParkingSpot.value = spot
    }

    fun onGeocodedLocationConsumed() {
        _geocodedLocation.value = null
    }

    private fun performSearch(query: String, spots: List<ParkingLocation>): List<ParkingLocation> {
        val coordinates = parseCoordinates(query)
        if (coordinates != null) {
            return spots.filter { spot ->
                spot.latitude != null && spot.longitude != null &&
                        calculateDistance(coordinates.first, coordinates.second, spot.latitude, spot.longitude) < 2000 // 2km radius
            }
        }

        val lowerCaseQuery = query.lowercase(Locale.getDefault())
        return spots.filter { spot ->
            (spot.name?.lowercase(Locale.getDefault())?.contains(lowerCaseQuery) == true) ||
                    (spot.address?.lowercase(Locale.getDefault())?.contains(lowerCaseQuery) == true) ||
                    (spot.cityName?.lowercase(Locale.getDefault())?.contains(lowerCaseQuery) == true)
        }
    }

    private fun parseCoordinates(query: String): Pair<Double, Double>? {
        val regex = """([\d.]+°?[NSns]?)[\s,]+([\d.]+°?[EWew]?)?""".toRegex()
        val matchResult = regex.find(query)
        if (matchResult != null) {
            try {
                val latStr = matchResult.groupValues[1]
                val lonStr = matchResult.groupValues[2]
                val lat = convertToDecimal(latStr)
                val lon = convertToDecimal(lonStr)
                return Pair(lat, lon)
            } catch (_: NumberFormatException) {
                return null
            }
        }
        return null
    }

    private fun convertToDecimal(coordinate: String): Double {
        val a = coordinate.replace("°", "").lowercase(Locale.getDefault())
        val value = a.filter { it.isDigit() || it == '.' }.toDouble()
        return if (a.contains("s") || a.contains("w")) -value else value
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusMeters = 6371e3 // metres
        val lat1Rad = lat1 * Math.PI / 180
        val lat2Rad = lat2 * Math.PI / 180
        val deltaLatRad = (lat2 - lat1) * Math.PI / 180
        val deltaLonRad = (lon2 - lon1) * Math.PI / 180

        val a = sin(deltaLatRad / 2) * sin(deltaLatRad / 2) +
                cos(lat1Rad) * cos(lat2Rad) *
                sin(deltaLonRad / 2) * sin(deltaLonRad / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadiusMeters * c // in metres
    }
}
