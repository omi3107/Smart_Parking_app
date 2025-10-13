package com.example.parkkar

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.example.parkkar.data.DatabaseHelper
import com.example.parkkar.data.model.ParkingSpot
import com.example.parkkar.ui.home.HomeViewModel
import com.example.parkkar.ui.home.SearchResultUiState
import com.example.parkkar.ui.theme.ParkkarTheme
import com.example.parkkar.utils.showToast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// SharedPreferences Constants
private const val PREFS_NAME = "ParkkarPrefs"
private const val KEY_SAVED_USERNAME = "saved_username"

class HomeActivity : ComponentActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var dbHelper: DatabaseHelper
    private val homeViewModel: HomeViewModel by viewModels()

    private fun navigateToDetails(spotId: String) {
        val intent = Intent(this, ParkingResultsActivity::class.java).apply {
            putExtra(ParkingResultsActivity.EXTRA_PARKING_SPOT_ID, spotId)
        }
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        dbHelper = DatabaseHelper(this)

        setContent {
            ParkkarTheme {
                HomeScreenContent(
                    viewModel = homeViewModel,
                    onNavigateBack = {
                        val savedUsername = sharedPreferences.getString(KEY_SAVED_USERNAME, null)
                        if (!savedUsername.isNullOrEmpty()) {
                            val userId = dbHelper.getUserIdByUsername(savedUsername)
                            dbHelper.addLogEntry(userId, "Successful Logout")
                        }
                        sharedPreferences.edit { remove(KEY_SAVED_USERNAME) }
                        val intent = Intent(this, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    },
                    onNavigateToDetails = ::navigateToDetails,
                    onFindParkingGeneral = {
                        val arrivalCal = homeViewModel.arrivalDateTime.value
                        val leavingCal = homeViewModel.leavingDateTime.value

                        val todayCal = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                        }
                        val arrivalDateCal = (arrivalCal.clone() as Calendar).apply {
                            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                        }
                        val leavingDateCal = (leavingCal.clone() as Calendar).apply {
                            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                        }

                        if (arrivalDateCal.before(todayCal)) {
                            showToast(this, "Arriving date cannot be in the past.")
                            return@HomeScreenContent
                        } 
                        if (leavingDateCal.before(todayCal)) {
                            showToast(this, "Leaving date cannot be in the past.")
                            return@HomeScreenContent
                        }
                        if (leavingCal.before(arrivalCal)) {
                            showToast(this, "Leaving date and time cannot be before arriving date and time.")
                            return@HomeScreenContent
                        }

                        val searchState = homeViewModel.searchResults.value
                        if (searchState is SearchResultUiState.Success && searchState.spots.isNotEmpty()) {
                            val topSpotId = searchState.spots[0].id
                            navigateToDetails(topSpotId)
                        } else {
                            Toast.makeText(this, "Please search and select a parking spot or ensure your search yields results.", Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    viewModel: HomeViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetails: (spotId: String) -> Unit, 
    onFindParkingGeneral: () -> Unit 
) {
    val context = LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResultsUiState by viewModel.searchResults.collectAsState()
    val arrivalDateTime by viewModel.arrivalDateTime.collectAsState()
    val leavingDateTime by viewModel.leavingDateTime.collectAsState()

    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    val showArrivalDatePicker = remember { mutableStateOf(false) }
    val showArrivalTimePicker = remember { mutableStateOf(false) }
    val showLeavingDatePicker = remember { mutableStateOf(false) }
    val showLeavingTimePicker = remember { mutableStateOf(false) }

    if (showArrivalDatePicker.value) {
        ShowDatePicker(context, arrivalDateTime) { calendar ->
            viewModel.updateArrivalDateTime(calendar)
            showArrivalDatePicker.value = false
        }
    }
    if (showArrivalTimePicker.value) {
        ShowTimePicker(context, arrivalDateTime) { calendar ->
            viewModel.updateArrivalDateTime(calendar)
            showArrivalTimePicker.value = false
        }
    }
    if (showLeavingDatePicker.value) {
        ShowDatePicker(context, leavingDateTime) { calendar ->
            viewModel.updateLeavingDateTime(calendar)
            showLeavingDatePicker.value = false
        }
    }
    if (showLeavingTimePicker.value) {
        ShowTimePicker(context, leavingDateTime) { calendar ->
            viewModel.updateLeavingDateTime(calendar)
            showLeavingTimePicker.value = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "PARK-KAR", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF4A4A4A))
                Spacer(modifier = Modifier.width(8.dp))
                Image(painter = painterResource(id = R.drawable.logo), contentDescription = "Park-Kar Logo", modifier = Modifier.height(20.dp))
            }

            Text("Hi!", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold), modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp), textAlign = TextAlign.Start)
            Text("Where are you going?", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), textAlign = TextAlign.Start)

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                label = { Text("City/Area/Address/Zip Code/Lat,Lon") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.fillMaxWidth().weight(0.5f)) {
                when (val state = searchResultsUiState) {
                    is SearchResultUiState.Idle -> {
                        Text("Start typing to search for parking.", textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center))
                    }
                    is SearchResultUiState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    is SearchResultUiState.Success -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(state.spots) { spot ->
                                ParkingSpotItem(spot = spot) {
                                    viewModel.onSearchQueryChanged(spot.parkingName ?: spot.address ?: spot.cityName)
                                    onNavigateToDetails(spot.id)
                                }
                                HorizontalDivider() // FIX: Replaced Divider with HorizontalDivider
                            }
                        }
                    }
                    is SearchResultUiState.NoResults -> {
                        Text("No parking spots found matching your query.", textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center))
                    }
                    is SearchResultUiState.Error -> {
                        Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center))
                    }
                    is SearchResultUiState.GeocoderError -> {
                         Text("Could not determine city from coordinates. Please try a different location or check network.", color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Arriving", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { showArrivalDatePicker.value = true }.padding(vertical = 8.dp)) {
                        Text(dateFormatter.format(arrivalDateTime.time), style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Filled.CalendarToday, contentDescription = "Select Arriving Date")
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { showArrivalTimePicker.value = true }.padding(vertical = 8.dp)) {
                        Text(timeFormatter.format(arrivalDateTime.time), style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Filled.Schedule, contentDescription = "Select Arriving Time")
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Box(modifier = Modifier.width(1.dp).height(120.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)))
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Leaving", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { showLeavingDatePicker.value = true }.padding(vertical = 8.dp)) {
                        Text(dateFormatter.format(leavingDateTime.time), style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Filled.CalendarToday, contentDescription = "Select Leaving Date")
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { showLeavingTimePicker.value = true }.padding(vertical = 8.dp)) {
                        Text(timeFormatter.format(leavingDateTime.time), style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Filled.Schedule, contentDescription = "Select Leaving Time")
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.5f))

            Button(
                onClick = onFindParkingGeneral, 
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF301934))
            ) {
                Text("Find Parking", fontSize = 18.sp, color = Color.White)
            }
        }
    }
}

@Composable
fun ParkingSpotItem(spot: ParkingSpot, onClick: () -> Unit) { 
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = spot.parkingName ?: "Unknown Parking Name", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            spot.address?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium) }
            Text(text = "City: ${spot.cityName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (spot.latitude != null && spot.longitude != null) {
                 Text(text = "Coords: ${String.format("%.4f", spot.latitude)}, ${String.format("%.4f", spot.longitude)}", style = MaterialTheme.typography.bodySmall)
            }
             Text(text = "4-Wheeler: ${spot.fourWheelerSpots}, 2-Wheeler: ${spot.twoWheelerSpots}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun ShowDatePicker(context: Context, initialCalendar: Calendar, onDateSelected: (Calendar) -> Unit) {
    val year = initialCalendar.get(Calendar.YEAR); val month = initialCalendar.get(Calendar.MONTH); val day = initialCalendar.get(Calendar.DAY_OF_MONTH)
    DatePickerDialog(context, { _, selectedYear, selectedMonth, selectedDayOfMonth ->
        val newCalendar = Calendar.getInstance().apply { timeInMillis = initialCalendar.timeInMillis; set(selectedYear, selectedMonth, selectedDayOfMonth) }
        onDateSelected(newCalendar)
    }, year, month, day).show()
}

@Composable
fun ShowTimePicker(context: Context, initialCalendar: Calendar, onTimeSelected: (Calendar) -> Unit) {
    val hour = initialCalendar.get(Calendar.HOUR_OF_DAY); val minute = initialCalendar.get(Calendar.MINUTE)
    TimePickerDialog(context, { _, selectedHour, selectedMinute ->
        val newCalendar = Calendar.getInstance().apply { timeInMillis = initialCalendar.timeInMillis; set(Calendar.HOUR_OF_DAY, selectedHour); set(Calendar.MINUTE, selectedMinute) }
        onTimeSelected(newCalendar)
    }, hour, minute, false).show()
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun DefaultPreviewOfHomeScreenUpdated() {
    ParkkarTheme {
        val dummyViewModel = HomeViewModel(LocalContext.current.applicationContext as android.app.Application)
        HomeScreenContent(viewModel = dummyViewModel, onNavigateBack = {}, onNavigateToDetails = {}, onFindParkingGeneral = {})
    }
}
