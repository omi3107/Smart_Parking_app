package com.example.parkkar.ui.chatbot

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ChatbotViewModelFactory(private val application: Application, private val parkingSpotId: String?) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatbotViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatbotViewModel(application, parkingSpotId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
