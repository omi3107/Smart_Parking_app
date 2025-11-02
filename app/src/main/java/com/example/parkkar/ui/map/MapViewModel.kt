package com.example.parkkar.ui.map

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.parkkar.model.ParkingLocation
import com.example.parkkar.network.DirectionsService
import com.example.parkkar.network.Route
import com.example.parkkar.repository.ParkingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SearchResultUiState {
    object Idle : SearchResultUiState()
    object Loading : SearchResultUiState()
    data class Success(val spots: List<ParkingLocation>) : SearchResultUiState()
    object NoResults : SearchResultUiState()
    data class Error(val message: String) : SearchResultUiState()
}

class MapViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ParkingRepository

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<SearchResultUiState>(SearchResultUiState.Idle)
    val searchResults = _searchResults.asStateFlow()

    private val _allParkingSpots = MutableStateFlow<List<ParkingLocation>>(emptyList())
    val allParkingSpots = _allParkingSpots.asStateFlow()

    private val _selectedParkingSpot = MutableStateFlow<ParkingLocation?>(null)
    val selectedParkingSpot = _selectedParkingSpot.asStateFlow()

    private val _route = MutableStateFlow<Route?>(null)
    val route = _route.asStateFlow()

    init {
        viewModelScope.launch {
            _allParkingSpots.value = repository.getAllParkingSpots(getApplication())
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        if (query.isNotBlank()) {
            _searchResults.value = SearchResultUiState.Loading
            viewModelScope.launch {
                val results = repository.searchParkingSpots(getApplication(), query)
                _searchResults.value = if (results.isNotEmpty()) {
                    SearchResultUiState.Success(results)
                } else {
                    SearchResultUiState.NoResults
                }
            }
        } else {
            _searchResults.value = SearchResultUiState.Idle
        }
    }

    fun onParkingSpotSelected(spot: ParkingLocation?) {
        _selectedParkingSpot.value = spot
    }

    fun fetchRoute(userLat: Double, userLon: Double, destLat: Double, destLon: Double) {
        viewModelScope.launch {
            val coordinates = "$userLon,$userLat;$destLon,$destLat"
            val response = DirectionsService.getDirections(coordinates)
            _route.value = response?.routes?.firstOrNull()
        }
    }
}
