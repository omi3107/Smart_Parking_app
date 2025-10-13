package com.example.parkkar.ui.parkingresults

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.parkkar.data.model.ParkingSpot
import com.example.parkkar.data.repository.ParkingDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ParkingSpotUiState {
    object Loading : ParkingSpotUiState()
    data class Success(val parkingSpot: ParkingSpot) : ParkingSpotUiState()
    data class Error(val message: String) : ParkingSpotUiState()
    object NotFound : ParkingSpotUiState()
}

class ParkingResultsViewModel(application: Application) : AndroidViewModel(application) {

    // Use the singleton instance of the repository
    private val parkingRepository = ParkingDataRepository.getInstance(application)

    private val _parkingSpotUiState = MutableStateFlow<ParkingSpotUiState>(ParkingSpotUiState.Loading)
    val parkingSpotUiState: StateFlow<ParkingSpotUiState> = _parkingSpotUiState.asStateFlow()

    fun fetchParkingSpotDetails(spotId: String) {
        _parkingSpotUiState.value = ParkingSpotUiState.Loading
        viewModelScope.launch {
            try {
                val spot = parkingRepository.getParkingSpotById(spotId)
                if (spot != null) {
                    _parkingSpotUiState.value = ParkingSpotUiState.Success(spot)
                } else {
                    _parkingSpotUiState.value = ParkingSpotUiState.NotFound
                }
            } catch (e: Exception) {
                _parkingSpotUiState.value = ParkingSpotUiState.Error("Failed to load parking spot details: ${e.message}")
            }
        }
    }
}
