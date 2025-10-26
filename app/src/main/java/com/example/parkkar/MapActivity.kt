package com.example.parkkar

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.parkkar.model.ParkingLocation
import com.example.parkkar.ui.map.MapViewModel
import com.example.parkkar.ui.map.SearchResultUiState
import com.example.parkkar.ui.theme.ParkkarTheme
import com.example.parkkar.utils.MarkerUtils
import com.google.gson.Gson
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.plugins.annotation.SymbolManager
import org.maplibre.android.plugins.annotation.SymbolOptions

class MapActivity : ComponentActivity() {

    private val viewModel: MapViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)

        val latitude = intent.getDoubleExtra("latitude", 0.0)
        val longitude = intent.getDoubleExtra("longitude", 0.0)
        val query = intent.getStringExtra("query")

        if (query != null) {
            viewModel.onSearchQueryChanged(query)
        }

        setContent {
            ParkkarTheme {
                MapScreen(
                    viewModel = viewModel,
                    initialLatitude = latitude,
                    initialLongitude = longitude,
                    onNavigateBack = { onBackPressedDispatcher.onBackPressed() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel,
    initialLatitude: Double,
    initialLongitude: Double,
    onNavigateBack: () -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResultsUiState by viewModel.searchResults.collectAsState()
    val allParkingSpots by viewModel.allParkingSpots.collectAsState()
    val selectedParkingSpot by viewModel.selectedParkingSpot.collectAsState()
    val geocodedLocation by viewModel.geocodedLocation.collectAsState()
    val mapView = rememberMapViewWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(searchResultsUiState) {
        if (searchResultsUiState is SearchResultUiState.NoResults) {
            Toast.makeText(context, "No parking facilities found in this area.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(geocodedLocation) {
        geocodedLocation?.let { latLng ->
            mapView.getMapAsync { map ->
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14.0))
            }
            viewModel.onGeocodedLocationConsumed()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("PARK-KAR", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF4A4A4A))
                        Spacer(modifier = Modifier.width(8.dp))
                        Image(painter = painterResource(id = R.drawable.logo), contentDescription = "Park-Kar Logo", modifier = Modifier.height(20.dp))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
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
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)) {
            MapContentView(
                initialLatitude = initialLatitude,
                initialLongitude = initialLongitude,
                parkingSpots = allParkingSpots,
                mapView = mapView,
                onSymbolClick = { spot -> viewModel.onParkingSpotSelected(spot) },
                onMapClick = { viewModel.onParkingSpotSelected(null) }
            )

            // Floating Search Bar and Results
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search Here") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                }
                if (searchResultsUiState is SearchResultUiState.Success) {
                    Card(
                        modifier = Modifier.padding(top = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        LazyColumn {
                            items((searchResultsUiState as SearchResultUiState.Success).spots) { spot ->
                                Column(modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.onParkingSpotSelected(spot)
                                        viewModel.onSearchQueryChanged("") // Clear search
                                    }
                                    .padding(16.dp)) {
                                    Text(spot.name ?: "", fontWeight = FontWeight.Bold)
                                    Text(spot.address ?: "")
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }

            // Floating Map Controls
            Card(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(16.dp),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column {
                    IconButton(onClick = { mapView.getMapAsync { it.moveCamera(CameraUpdateFactory.zoomIn()) } }) {
                        Icon(Icons.Default.Add, contentDescription = "Zoom In")
                    }
                    IconButton(onClick = { mapView.getMapAsync { it.moveCamera(CameraUpdateFactory.zoomOut()) } }) {
                        Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
                    }
                }
            }

            // Bottom-aligned details sheet
            AnimatedVisibility(
                visible = selectedParkingSpot != null,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                selectedParkingSpot?.let {
                    ParkingDetailsSheet(parkingSpot = it)
                    if (it.latitude != null && it.longitude != null) {
                        mapView.getMapAsync { map ->
                            map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 16.0))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MapContentView(
    initialLatitude: Double,
    initialLongitude: Double,
    parkingSpots: List<ParkingLocation>,
    mapView: MapView,
    onSymbolClick: (ParkingLocation) -> Unit,
    onMapClick: () -> Unit
) {
    val context = LocalContext.current
    val gson = remember { Gson() }

    AndroidView({ mapView }) { view ->
        view.getMapAsync { maplibreMap ->
            maplibreMap.setStyle(BuildConfig.MAP_STYLE_URL) { style ->
                if (initialLatitude != 0.0 && initialLongitude != 0.0) {
                    maplibreMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(initialLatitude, initialLongitude), 15.0))
                }

                val symbolManager = SymbolManager(view, maplibreMap, style)

                parkingSpots.forEach { spot ->
                    if (spot.latitude != null && spot.longitude != null) {
                        val price = spot.prices?.firstOrNull()?.amount ?: 0.0
                        val currency = spot.prices?.firstOrNull()?.currency ?: ""
                        val priceText = if (price > 0) "$currency$price" else "Free"

                        // Add image to style
                        val imageId = "marker-icon-${spot.id}"
                        style.addImage(imageId, MarkerUtils.createBitmapFromView(context, priceText))

                        val symbolOptions = SymbolOptions()
                            .withLatLng(LatLng(spot.latitude, spot.longitude))
                            .withIconImage(imageId)
                            .withData(gson.toJsonTree(spot)) // Attach spot data

                        symbolManager.create(symbolOptions)
                    }
                }

                symbolManager.addClickListener { symbol ->
                    val spotJson = symbol.data.toString()
                    val spot = gson.fromJson(spotJson, ParkingLocation::class.java)
                    onSymbolClick(spot)
                    true
                }
                maplibreMap.addOnMapClickListener {
                    onMapClick()
                    true
                }
            }
        }
    }
}

@Composable
fun ParkingDetailsSheet(parkingSpot: ParkingLocation) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = parkingSpot.name ?: "", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Price: ${parkingSpot.prices?.firstOrNull()?.currency ?: ""}${parkingSpot.prices?.firstOrNull()?.amount ?: "N/A"}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Duration: ${parkingSpot.prices?.firstOrNull()?.duration ?: "N/A"}", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { /* TODO: Implement Pay Now */ }, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Pay Now")
            }
        }
    }
}

@Composable
fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    DisposableEffect(lifecycle, mapView) {
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(Bundle())
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycle.addObserver(lifecycleObserver)
        onDispose { lifecycle.removeObserver(lifecycleObserver) }
    }

    return mapView
}
