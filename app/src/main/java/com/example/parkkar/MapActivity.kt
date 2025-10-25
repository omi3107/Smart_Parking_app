package com.example.parkkar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.example.parkkar.ui.theme.ParkkarTheme
import com.example.parkkar.utils.MarkerUtils
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView

class MapActivity : ComponentActivity() {

    private val viewModel: MapViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)

        val latitude = intent.getDoubleExtra("latitude", 0.0)
        val longitude = intent.getDoubleExtra("longitude", 0.0)

        setContent {
            ParkkarTheme {
                MapScreen(
                    viewModel = viewModel,
                    initialLatitude = latitude,
                    initialLongitude = longitude
                )
            }
        }
    }
}

@Composable
fun MapScreen(
    viewModel: MapViewModel,
    initialLatitude: Double,
    initialLongitude: Double
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val allParkingSpots by viewModel.allParkingSpots.collectAsState()
    val selectedParkingSpot by viewModel.selectedParkingSpot.collectAsState()
    val mapView = rememberMapViewWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        MapContentView(initialLatitude, initialLongitude, allParkingSpots, mapView)

        // Floating Header
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("PARK-KAR", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF4A4A4A))
            Spacer(modifier = Modifier.width(8.dp))
            Image(painter = painterResource(id = R.drawable.logo), contentDescription = "Park-Kar Logo", modifier = Modifier.height(20.dp))
        }

        // Floating Search Bar
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(16.dp),
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

        // Floating Map Controls
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(16.dp)
        ) {
            IconButton(onClick = { mapView.getMapAsync { it.moveCamera(CameraUpdateFactory.zoomIn()) } }) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In")
            }
            IconButton(onClick = { mapView.getMapAsync { it.moveCamera(CameraUpdateFactory.zoomOut()) } }) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
            }
        }

        // Bottom-aligned details sheet
        selectedParkingSpot?.let {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                ParkingDetailsSheet(parkingSpot = it)
            }
        }
    }
}

@Composable
fun MapContentView(
    latitude: Double,
    longitude: Double,
    parkingSpots: List<com.example.parkkar.model.ParkingLocation>,
    mapView: MapView
) {
    val context = LocalContext.current

    AndroidView({ mapView }) { view ->
        view.getMapAsync { maplibreMap ->
            maplibreMap.setStyle(BuildConfig.MAP_STYLE_URL) { _ ->
                if (latitude != 0.0 && longitude != 0.0) {
                    maplibreMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), 15.0))
                }

                parkingSpots.forEach { spot ->
                    if (spot.latitude != null && spot.longitude != null) {
                        val price = spot.prices?.firstOrNull()?.amount ?: 0.0
                        val currency = spot.prices?.firstOrNull()?.currency ?: ""
                        val priceText = if (price > 0) "$currency$price" else "Free"

                        val icon = IconFactory.getInstance(context)
                            .fromBitmap(MarkerUtils.createBitmapFromView(context, priceText))

                        maplibreMap.addMarker(
                            MarkerOptions()
                                .position(LatLng(spot.latitude, spot.longitude))
                                .icon(icon)
                        )
                    }
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
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = parkingSpot.name ?: "", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Text(text = "Price: ", fontWeight = FontWeight.Bold)
                Text(text = "${parkingSpot.prices?.firstOrNull()?.currency ?: ""}${parkingSpot.prices?.firstOrNull()?.amount ?: 0.0}")
            }
            Row {
                Text(text = "Duration: ", fontWeight = FontWeight.Bold)
                Text(text = parkingSpot.prices?.firstOrNull()?.duration ?: "N/A")
            }
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
