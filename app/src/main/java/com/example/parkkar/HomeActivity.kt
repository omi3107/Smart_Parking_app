package com.example.parkkar

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack // Added
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext // Added
import androidx.compose.ui.tooling.preview.Preview
import com.example.parkkar.ui.theme.ParkkarTheme

// SharedPreferences Constants (assuming they are not in a globally accessible file)
private const val PREFS_NAME = "ParkkarPrefs"
private const val KEY_SAVED_USERNAME = "saved_username"

class HomeActivity : ComponentActivity() {
    private lateinit var sharedPreferences: SharedPreferences

    @OptIn(ExperimentalMaterial3Api::class) // Needed for TopAppBar
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        setContent {
            ParkkarTheme {
                val context = LocalContext.current // Get context for Intent
                HomeScreenContent(
                    onNavigateBack = {
                        // Clear saved username
                        sharedPreferences.edit().remove(KEY_SAVED_USERNAME).apply()

                        // Navigate to LoginActivity and clear task
                        val intent = Intent(context, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        context.startActivity(intent)
                        finish() // Finish HomeActivity
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class) // Needed for TopAppBar
@Composable
fun HomeScreenContent(onNavigateBack: () -> Unit) { // Added onNavigateBack parameter
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Logout"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding), // Apply padding from Scaffold
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Welcome to Parkkar!",
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreviewOfHomeScreen() {
    ParkkarTheme {
        HomeScreenContent(onNavigateBack = {}) // Added dummy lambda for preview
    }
}
