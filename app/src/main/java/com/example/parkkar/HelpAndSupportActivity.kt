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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.parkkar.ui.theme.ParkkarTheme

data class FaqItem(val question: String, val answer: String)

class HelpAndSupportActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val application = application as MainApplication
            val isDarkTheme by application.userPreferencesRepository.isDarkTheme.collectAsState(initial = false)

            ParkkarTheme(darkTheme = isDarkTheme) {
                HelpAndSupportScreen(onNavigateBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpAndSupportScreen(onNavigateBack: () -> Unit) {
    val faqs = listOf(
        FaqItem("How do I book a parking spot?", "You can book a parking spot by selecting a location on the map, choosing your arrival and leaving times, and confirming your booking."),
        FaqItem("How do I pay for parking?", "Payment is handled through the app using your saved payment method."),
        FaqItem("How do I cancel a booking?", "You can cancel a booking from the Booking History screen. Please note that cancellation policies may apply."),
        FaqItem("How do I contact customer support?", "You can contact us through the chatbot or by emailing support@parkkar.com.")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Help & Support") },
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
            items(faqs) { faq ->
                Card(modifier = Modifier.padding(vertical = 8.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = faq.question, fontWeight = FontWeight.Bold)
                        Text(text = faq.answer)
                    }
                }
            }
        }
    }
}
