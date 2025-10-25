package com.example.parkkar.ui.parkingresults

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.parkkar.model.ParkingLocation
import com.example.parkkar.repository.ParkingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ParkingSpotUiState {
    object Loading : ParkingSpotUiState()
    data class Success(val parkingSpot: ParkingLocation) : ParkingSpotUiState()
    data class Error(val message: String) : ParkingSpotUiState()
    object NotFound : ParkingSpotUiState()
}

class ParkingResultsViewModel(application: Application) : AndroidViewModel(application) {

    private val _parkingSpotUiState = MutableStateFlow<ParkingSpotUiState>(ParkingSpotUiState.Loading)
    val parkingSpotUiState: StateFlow<ParkingSpotUiState> = _parkingSpotUiState.asStateFlow()

    fun fetchParkingSpotDetails(spotId: String) {
        viewModelScope.launch {
            val spot = ParkingRepository.getSpotById(getApplication(), spotId)
            if (spot != null) {
                _parkingSpotUiState.value = ParkingSpotUiState.Success(spot)
            } else {
                _parkingSpotUiState.value = ParkingSpotUiState.NotFound
            }
        }
    }
}
