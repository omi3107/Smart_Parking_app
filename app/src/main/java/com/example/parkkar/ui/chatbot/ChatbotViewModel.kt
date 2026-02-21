package com.example.parkkar.ui.chatbot

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parkkar.BuildConfig
import com.example.parkkar.model.ParkingLocation
import com.example.parkkar.repository.ParkingRepository
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class Message(val text: String, val isFromUser: Boolean)

data class UiState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false
)

class ChatbotViewModel(application: Application, private val parkingSpotId: String?) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private lateinit var generativeModel: GenerativeModel
    private val gson = Gson() // For serializing data

    init {
        viewModelScope.launch {
            val allParkingSpots = ParkingRepository.getAllParkingSpots(application)
            val selectedSpot = parkingSpotId?.let { id -> allParkingSpots.find { it.id == id } }
            val context = buildContext(allParkingSpots, selectedSpot)

            generativeModel = GenerativeModel(
                modelName = "gemini-2.5-flash",
                apiKey = BuildConfig.GEMINI_API_KEY,
                systemInstruction = content {
                    text(context)
                }
            )
        }
    }

    fun sendMessage(userInput: String) {
        _uiState.update {
            it.copy(messages = it.messages + Message(userInput, true), isLoading = true)
        }

        viewModelScope.launch {
            try {
                // Create a new chat session for each message to apply the system prompt correctly.
                val chat = generativeModel.startChat()
                val response = chat.sendMessage(userInput)
                _uiState.update {
                    it.copy(
                        messages = it.messages + Message(response.text ?: "Sorry, I couldn't process that.", false),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        messages = it.messages + Message("Error: ${e.message}", false),
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun buildContext(allSpots: List<ParkingLocation>, selectedSpot: ParkingLocation?): String {
        val allSpotsJson = gson.toJson(allSpots) // Serialize the entire list

        val basePrompt = """
        You are 'Park-Kar Assistant', a friendly and precise AI helper for a parking app. 
        Your knowledge is based ONLY on the data provided below in JSON format. 
        When asked a question, find the relevant information from the JSON data and present it clearly and concisely. 
        Do not mention that you are working with a JSON file, just provide the information as if you know it. 
        If the user asks about traffic, check the provided data for a nearby alternative and advise them to use a map application for real-time traffic information. 
        If a question cannot be answered from the provided data, say "I do not have information on that."
        You can also answer questions related to "Help and Support".

        When the user asks for parking locations near them, you should ask for their current location.

        When the user asks for parking locations near a selected location, you should provide a list of parking spots within a 5km radius of the selected location.

        Here is the full list of available parking spots:
        ${'$'}allSpotsJson
        """

        return if (selectedSpot != null) {
            val selectedSpotJson = gson.toJson(selectedSpot)
            basePrompt + "The user is currently viewing the details for the following parking spot:\n$selectedSpotJson"
        } else {
            basePrompt
        }
    }
}
