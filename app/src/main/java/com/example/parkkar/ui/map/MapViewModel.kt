package com.example.parkkar.ui.map

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.parkkar.model.ParkingLocation
import com.example.parkkar.repository.ParkingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val _allParkingSpots = MutableStateFlow<List<ParkingLocation>>(emptyList())
    val allParkingSpots: StateFlow<List<ParkingLocation>> = _allParkingSpots.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedParkingSpot = MutableStateFlow<ParkingLocation?>(null)
    val selectedParkingSpot: StateFlow<ParkingLocation?> = _selectedParkingSpot.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _allParkingSpots.value = ParkingRepository.getAllParkingSpots(application)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onParkingSpotSelected(spot: ParkingLocation?) {
        _selectedParkingSpot.value = spot
    }
}
