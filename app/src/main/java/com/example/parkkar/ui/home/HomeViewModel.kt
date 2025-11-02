
package com.example.parkkar.ui.home

import android.annotation.SuppressLint
import android.app.Application
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.parkkar.model.ParkingLocation
import com.example.parkkar.recommendation.RecommendationEngine
import com.example.parkkar.repository.ParkingRepository
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Calendar
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
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val geocoder = Geocoder(application, Locale.getDefault())
    private val recommendationEngine = RecommendationEngine()
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _allParkingSpots = MutableStateFlow<List<ParkingLocation>>(emptyList())

    private val _recommendedSpots = MutableStateFlow<List<ParkingLocation>>(emptyList())
    val recommendedSpots: StateFlow<List<ParkingLocation>> = _recommendedSpots.asStateFlow()

    val searchResults: StateFlow<SearchResultUiState> = _searchQuery
        .debounce(300) // Don't search on every keystroke
        .flatMapLatest { query ->
            flow {
                if (query.isBlank()) {
                    emit(SearchResultUiState.Idle)
                    return@flow
                }
                emit(SearchResultUiState.Loading)
                val spots = _allParkingSpots.value
                if (spots.isEmpty()) {
                    emit(SearchResultUiState.Loading)
                } else {
                    val filteredList = performSearch(query, spots)
                    if (filteredList.isNotEmpty()) {
                        emit(SearchResultUiState.Success(filteredList))
                    } else {
                        emit(SearchResultUiState.NoResults)
                    }
                }
            }.flowOn(Dispatchers.Default)
        }
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = SearchResultUiState.Idle
        )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _allParkingSpots.value = ParkingRepository.getAllParkingSpots(getApplication())
            // Generate initial recommendations when data is loaded
            generateRecommendations()
        }
    }

    @SuppressLint("MissingPermission")
    fun generateRecommendations() {
        viewModelScope.launch {
            try {
                val location: Location? = fusedLocationClient.lastLocation.await()
                if (location != null) {
                    val recommended = recommendationEngine.generateRecommendations(
                        userLat = location.latitude,
                        userLon = location.longitude,
                        allParkingLocations = _allParkingSpots.value
                    )
                    _recommendedSpots.value = recommended
                } else {
                    // Handle case where location is null (e.g., location services disabled)
                    _recommendedSpots.value = emptyList()
                }
            } catch (e: SecurityException) {
                // Handle permission not being granted
                _recommendedSpots.value = emptyList()
            } catch (e: Exception) {
                // Handle other exceptions
                _recommendedSpots.value = emptyList()
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    suspend fun geocodeQuery(query: String): Pair<Double, Double>? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCoroutine { continuation ->
                try {
                    geocoder.getFromLocationName(query, 1) { addresses ->
                        val location = addresses.firstOrNull()
                        continuation.resume(location?.let { Pair(it.latitude, it.longitude) })
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
                    location?.let { Pair(it.latitude, it.longitude) }
                } catch (e: IOException) {
                    null
                }
            }
        }
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
        val regex = Regex("([/d.]+°?[NSns]?)[/s,]+([/d.]+°?[EWew]?)")
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

    // Unchanged date/time logic
    private val _arrivalDateTime = MutableStateFlow<Calendar>(Calendar.getInstance())
    val arrivalDateTime: StateFlow<Calendar> = _arrivalDateTime.asStateFlow()

    private val _leavingDateTime = MutableStateFlow<Calendar>(Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) })
    val leavingDateTime: StateFlow<Calendar> = _leavingDateTime.asStateFlow()

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
