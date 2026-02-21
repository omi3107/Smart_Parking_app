package com.example.parkkar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.parkkar.data.DatabaseHelper
import com.example.parkkar.model.FavoriteSpot
import com.example.parkkar.ui.theme.ParkkarTheme

class FavoriteSpotsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val application = application as MainApplication
            val isDarkTheme by application.userPreferencesRepository.isDarkTheme.collectAsState(initial = false)
            val dbHelper = DatabaseHelper(this)
            // A sample user ID, you should replace this with the actual logged-in user's ID
            val userId = 1
            val favoriteSpots = dbHelper.getFavoriteSpots(userId)

            ParkkarTheme(darkTheme = isDarkTheme) {
                FavoriteSpotsScreen(onNavigateBack = { finish() }, favoriteSpots = favoriteSpots)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteSpotsScreen(onNavigateBack: () -> Unit, favoriteSpots: List<FavoriteSpot>) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favorite Spots") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (favoriteSpots.isEmpty()) {
                item {
                    Text(text = "No favorite spots added yet.")
                }
            } else {
                items(favoriteSpots) { spot ->
                    FavoriteSpotItem(spot = spot)
                }
            }
        }
    }
}

@Composable
fun FavoriteSpotItem(spot: FavoriteSpot) {
    Card(
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = spot.parkingName, style = MaterialTheme.typography.titleMedium)
            Text(text = spot.address)
        }
    }
}
