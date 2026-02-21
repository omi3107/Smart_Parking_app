package com.example.parkkar

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.parkkar.data.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainApplication : Application() {

    lateinit var userPreferencesRepository: UserPreferencesRepository
    var isDarkTheme by mutableStateOf(false)
        private set

    private val applicationScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        userPreferencesRepository = UserPreferencesRepository.getInstance(this)
        applicationScope.launch {
            userPreferencesRepository.isDarkTheme.collect {
                isDarkTheme = it
            }
        }
    }
}
