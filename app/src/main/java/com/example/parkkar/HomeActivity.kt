
package com.example.parkkar

import android.Manifest
import android.app.Application
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.example.parkkar.model.ParkingLocation
import com.example.parkkar.ui.home.HomeViewModel
import com.example.parkkar.ui.home.SearchResultUiState
import com.example.parkkar.ui.theme.ParkkarTheme
import com.example.parkkar.utils.NotificationHelper
import com.example.parkkar.utils.showToast
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private const val PREFS_NAME = "ParkkarPrefs"
private const val KEY_SAVED_USERNAME = "saved_username"

class HomeActivity : ComponentActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private val homeViewModel: HomeViewModel by viewModels()
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        firebaseAnalytics = Firebase.analytics
        NotificationHelper.requestNotificationPermission(this)

        setContent {
            val isDarkTheme = (application as MainApplication).isDarkTheme
            ParkkarTheme(darkTheme = isDarkTheme) {
                val context = LocalContext.current
                var hasLocationPermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                }

                val locationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { isGranted ->
                        if (isGranted) {
                            hasLocationPermission = true
                            homeViewModel.generateRecommendations()
                        } else {
                            Toast.makeText(context, "Location permission is required for recommendations.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                LaunchedEffect(Unit) {
                    if (!hasLocationPermission) {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    } else {
                        homeViewModel.generateRecommendations()
                    }
                }

                HomeScreenContent(
                    viewModel = homeViewModel,
                    hasLocationPermission = hasLocationPermission,
                    onNavigateBack = {
                        sharedPreferences.edit { remove(KEY_SAVED_USERNAME) }
                        val intent = Intent(this, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    },
                    onNavigateToProfile = {
                        startActivity(Intent(this, ProfileActivity::class.java))
                    },
                    onNavigateToDetails = ::navigateToDetails,
                    onRecommendationClicked = {
                        firebaseAnalytics.logEvent("recommendation_clicked") {
                            param("spot_id", it.id)
                        }
                        navigateToDetails(it.id)
                     },
                    onFindParkingGeneral = {
                        val arrivalCal = homeViewModel.arrivalDateTime.value
                        val leavingCal = homeViewModel.leavingDateTime.value

                        if (isDateTimeInvalid(arrivalCal, leavingCal)) return@HomeScreenContent

                        val searchState = homeViewModel.searchResults.value
                        val searchQuery = homeViewModel.searchQuery.value

                        if (searchState is SearchResultUiState.Success && searchState.spots.isNotEmpty()) {
                            navigateToDetails(searchState.spots[0].id)
                        } else if (searchQuery.isNotBlank()) {
                            lifecycleScope.launch {
                                val coords = homeViewModel.geocodeQuery(searchQuery)
                                if (coords != null) {
                                    navigateToMap(coords.first, coords.second)
                                } else {
                                    Toast.makeText(this@HomeActivity, "Could not find location: $searchQuery", Toast.LENGTH_LONG).show()
                                }
                            }
                        } else {
                            Toast.makeText(this, "Please enter a search query.", Toast.LENGTH_LONG).show()
                        }
                    },
                    onChatbotClick = {
                        val intent = Intent(this, ChatbotActivity::class.java)
                        startActivity(intent)
                    }
                )
            }
        }
    }

    private fun navigateToDetails(spotId: String) {
        val intent = Intent(this, ParkingResultsActivity::class.java).apply {
            putExtra(ParkingResultsActivity.EXTRA_PARKING_SPOT_ID, spotId)
        }
        startActivity(intent)
    }

    private fun navigateToMap(latitude: Double, longitude: Double) {
        val intent = Intent(this, MapActivity::class.java).apply {
            putExtra("latitude", latitude)
            putExtra("longitude", longitude)
        }
        startActivity(intent)
    }

    private fun isDateTimeInvalid(arrivalCal: Calendar, leavingCal: Calendar): Boolean {
        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val arrivalDateCal = (arrivalCal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        if (arrivalDateCal.before(todayCal)) {
            showToast(this, "Arriving date cannot be in the past.")
            return true
        }
        if (leavingCal.before(arrivalCal)) {
            showToast(this, "Leaving date and time cannot be before arriving date and time.")
            return true
        }
        return false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    viewModel: HomeViewModel,
    hasLocationPermission: Boolean,
    onNavigateBack: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToDetails: (spotId: String) -> Unit,
    onRecommendationClicked: (spot: ParkingLocation) -> Unit,
    onFindParkingGeneral: () -> Unit,
    onChatbotClick: () -> Unit
) {
    val context = LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResultsUiState by viewModel.searchResults.collectAsState()
    val recommendedSpots by viewModel.recommendedSpots.collectAsState()
    val arrivalDateTime by viewModel.arrivalDateTime.collectAsState()
    val leavingDateTime by viewModel.leavingDateTime.collectAsState()

    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    val showArrivalDatePicker = remember { mutableStateOf(false) }
    val showArrivalTimePicker = remember { mutableStateOf(false) }
    val showLeavingDatePicker = remember { mutableStateOf(false) }
    val showLeavingTimePicker = remember { mutableStateOf(false) }

    if (showArrivalDatePicker.value) {
        ShowDatePicker(context, arrivalDateTime) { viewModel.updateArrivalDateTime(it); showArrivalDatePicker.value = false }
    }
    if (showArrivalTimePicker.value) {
        ShowTimePicker(context, arrivalDateTime) { viewModel.updateArrivalDateTime(it); showArrivalTimePicker.value = false }
    }
    if (showLeavingDatePicker.value) {
        ShowDatePicker(context, leavingDateTime) { viewModel.updateLeavingDateTime(it); showLeavingDatePicker.value = false }
    }
    if (showLeavingTimePicker.value) {
        ShowTimePicker(context, leavingDateTime) { viewModel.updateLeavingDateTime(it); showLeavingTimePicker.value = false }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Logout") } },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("PARK-KAR", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Image(painter = painterResource(id = R.drawable.logo), contentDescription = "Logo", modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = onNavigateToProfile) {
                            Icon(Icons.Default.Person, contentDescription = "Profile")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        },
        floatingActionButton = { FloatingActionButton(onClick = onChatbotClick) { Icon(Icons.Filled.Chat, "Chatbot") } }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Hi!", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
            Text("Where are you going?", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), textAlign = TextAlign.Start)

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                label = { Text("City/Area/Address/Zip Code/Lat,Lon") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Recommendations Section
            if (hasLocationPermission && recommendedSpots.isNotEmpty() && searchQuery.isBlank()) {
                Text(
                    text = "Recommended For You",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(recommendedSpots) { spot ->
                        RecommendationCard(spot = spot) { onRecommendationClicked(spot) }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
            }

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                when (val state = searchResultsUiState) {
                    is SearchResultUiState.Idle -> {
                        if (searchQuery.isBlank()) {
                            // This space is now used by recommendations when available
                             Text("Start typing to search or see recommendations above.", textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center))
                        } else {
                            Text("Searching...", textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center))
                        }
                    }
                    is SearchResultUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    is SearchResultUiState.Success -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(state.spots) { spot ->
                                ParkingSpotItem(spot = spot) {
                                    viewModel.onSearchQueryChanged(spot.name ?: "")
                                    onNavigateToDetails(spot.id)
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                    is SearchResultUiState.NoResults -> Text("No parking spots found.", textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center))
                    is SearchResultUiState.Error -> Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Date and Time Pickers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                DateTimeColumn("Arriving", dateFormatter.format(arrivalDateTime.time), timeFormatter.format(arrivalDateTime.time), {
                    showArrivalDatePicker.value = true
                }, { showArrivalTimePicker.value = true })
                Spacer(modifier = Modifier.width(16.dp))
                Box(modifier = Modifier.width(1.dp).height(120.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)))
                Spacer(modifier = Modifier.width(16.dp))
                DateTimeColumn("Leaving", dateFormatter.format(leavingDateTime.time), timeFormatter.format(leavingDateTime.time), {
                    showLeavingDatePicker.value = true
                }, { showLeavingTimePicker.value = true })
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onFindParkingGeneral,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Find Parking", fontSize = 18.sp)
            }
             Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun RowScope.DateTimeColumn(title: String, date: String, time: String, onDateClick: () -> Unit, onTimeClick: () -> Unit) {
    Column(modifier = Modifier.weight(1f)) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable(onClick = onDateClick).padding(vertical = 8.dp)) {
            Text(date, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Filled.CalendarToday, "Select Date")
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable(onClick = onTimeClick).padding(vertical = 8.dp)) {
            Text(time, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Filled.Schedule, "Select Time")
        }
    }
}

@Composable
fun ParkingSpotItem(spot: ParkingLocation, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(spot.name ?: "Unknown Parking Name", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            spot.address?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            Text("Capacity: ${spot.totalCapacity}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun RecommendationCard(spot: ParkingLocation, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(220.dp).clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(spot.name ?: "Parking Spot", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
            Spacer(modifier = Modifier.height(4.dp))
            spot.address?.let { Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Capacity: ${spot.totalCapacity}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}

@Composable
fun ShowDatePicker(context: Context, initialCalendar: Calendar, onDateSelected: (Calendar) -> Unit) {
    val year = initialCalendar.get(Calendar.YEAR)
    val month = initialCalendar.get(Calendar.MONTH)
    val day = initialCalendar.get(Calendar.DAY_OF_MONTH)
    DatePickerDialog(context, { _, y, m, d ->
        val newCalendar = (initialCalendar.clone() as Calendar).apply { set(y, m, d) }
        onDateSelected(newCalendar)
    }, year, month, day).show()
}

@Composable
fun ShowTimePicker(context: Context, initialCalendar: Calendar, onTimeSelected: (Calendar) -> Unit) {
    val hour = initialCalendar.get(Calendar.HOUR_OF_DAY)
    val minute = initialCalendar.get(Calendar.MINUTE)
    TimePickerDialog(context, { _, h, m ->
        val newCalendar = (initialCalendar.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m) }
        onTimeSelected(newCalendar)
    }, hour, minute, false).show()
}

@Preview(showBackground = true)
@Composable
fun DefaultPreviewOfHomeScreenUpdated() {
    ParkkarTheme {
        val app = LocalContext.current.applicationContext as Application
        HomeScreenContent(
            viewModel = HomeViewModel(app),
            hasLocationPermission = true,
            onNavigateBack = {},
            onNavigateToProfile = {},
            onNavigateToDetails = {},
            onRecommendationClicked = {},
            onFindParkingGeneral = {},
            onChatbotClick = {}
        )
    }
}
