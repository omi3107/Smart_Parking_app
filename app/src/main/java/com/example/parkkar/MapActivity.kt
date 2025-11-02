
package com.example.parkkar

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.parkkar.model.ParkingLocation
import com.example.parkkar.network.Route
import com.example.parkkar.ui.map.MapViewModel
import com.example.parkkar.ui.map.SearchResultUiState
import com.example.parkkar.ui.theme.ParkkarTheme
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView
import org.maplibre.android.plugins.annotation.SymbolManager
import org.maplibre.android.plugins.annotation.SymbolOptions
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import java.net.URLEncoder

class MapActivity : ComponentActivity() {

    private val viewModel: MapViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)

        val latitude = intent.getDoubleExtra("latitude", 0.0)
        val longitude = intent.getDoubleExtra("longitude", 0.0)
        val userLatitude = intent.getDoubleExtra("user_latitude", 0.0)
        val userLongitude = intent.getDoubleExtra("user_longitude", 0.0)
        val locationName = intent.getStringExtra("locationName")

        setContent {
            ParkkarTheme {
                MapScreen(
                    viewModel = viewModel,
                    initialLatitude = latitude,
                    initialLongitude = longitude,
                    userLatitude = userLatitude,
                    userLongitude = userLongitude,
                    locationName = locationName,
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
    userLatitude: Double,
    userLongitude: Double,
    locationName: String?,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResultsUiState by viewModel.searchResults.collectAsState()
    val allParkingSpots by viewModel.allParkingSpots.collectAsState()
    val selectedParkingSpot by viewModel.selectedParkingSpot.collectAsState()
    val route by viewModel.route.collectAsState()
    val mapView = rememberMapViewWithLifecycle()

    val onSpotSelected = { spot: ParkingLocation ->
        viewModel.onParkingSpotSelected(spot)
        if (userLatitude != 0.0 && userLongitude != 0.0 && spot.latitude != null && spot.longitude != null) {
            viewModel.fetchRoute(userLatitude, userLongitude, spot.latitude, spot.longitude)
        }
    }

    LaunchedEffect(allParkingSpots, initialLatitude, initialLongitude) {
        if (allParkingSpots.isNotEmpty() && initialLatitude != 0.0) {
            val spot = allParkingSpots.find { it.latitude == initialLatitude && it.longitude == initialLongitude }
            if (spot != null) {
                viewModel.onParkingSpotSelected(spot)
                viewModel.onSearchQueryChanged(spot.name ?: "")
                if (userLatitude != 0.0 && userLongitude != 0.0) {
                    viewModel.fetchRoute(userLatitude, userLongitude, initialLatitude, initialLongitude)
                }
            } else if (locationName != null) {
                viewModel.onSearchQueryChanged(locationName)
                Toast.makeText(mapView.context, "This location does not provide parking facility", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 16.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("PARK-KAR", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = androidx.compose.ui.graphics.Color(0xFF4A4A4A))
                        Spacer(modifier = Modifier.width(8.dp))
                        Image(painter = painterResource(id = R.drawable.logo), contentDescription = "Park-Kar Logo", modifier = Modifier.size(24.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                val intent = Intent(context, ChatbotActivity::class.java).apply {
                    putExtra("parking_spot_id", selectedParkingSpot?.id)
                }
                context.startActivity(intent)
            }) {
                Icon(Icons.Filled.Chat, contentDescription = "Chatbot")
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)) {
            MapContentView(
                initialLatitude = initialLatitude,
                initialLongitude = initialLongitude,
                userLatitude = userLatitude,
                userLongitude = userLongitude,
                parkingSpots = allParkingSpots,
                mapView = mapView,
                route = route,
                onSymbolClick = onSpotSelected,
                onMapClick = { viewModel.onParkingSpotSelected(null) }
            )

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                        ),
                        singleLine = true
                    )
                }

                val currentResults = searchResultsUiState
                if (currentResults is SearchResultUiState.Success) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                            items(currentResults.spots) { spot ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSpotSelected(spot) }
                                        .padding(16.dp)
                                ) {
                                    Text(spot.name ?: "", fontWeight = FontWeight.Bold)
                                    spot.address?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(16.dp),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)
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
    userLatitude: Double,
    userLongitude: Double,
    parkingSpots: List<ParkingLocation>,
    mapView: MapView,
    route: Route?,
    onSymbolClick: (ParkingLocation) -> Unit,
    onMapClick: () -> Unit
) {
    val context = LocalContext.current
    val gson = remember { Gson() }

    val homeIcon = remember(context) {
        val vectorDrawable = ContextCompat.getDrawable(context, R.drawable.ic_home)
        val bitmap = Bitmap.createBitmap(
            vectorDrawable!!.intrinsicWidth,
            vectorDrawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
        vectorDrawable.draw(canvas)
        bitmap
    }

    val markerIcon = remember(context) {
        val vectorDrawable = ContextCompat.getDrawable(context, R.drawable.ic_marker)
        val bitmap = Bitmap.createBitmap(
            vectorDrawable!!.intrinsicWidth,
            vectorDrawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
        vectorDrawable.draw(canvas)
        bitmap
    }

    AndroidView({ mapView }) { view ->
        view.getMapAsync { maplibreMap ->
            maplibreMap.setStyle("https://api.maptiler.com/maps/019a1af3-3396-72e5-8331-011b55745b4c/style.json?key=EnaRQtianLhrW7vEu1Z8") { style ->
                if (initialLatitude != 0.0 && initialLongitude != 0.0 && maplibreMap.cameraPosition.zoom < 1.0) {
                    maplibreMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(initialLatitude, initialLongitude), 15.0))
                }

                val symbolManager = SymbolManager(view, maplibreMap, style)

                style.addImage("home-icon", homeIcon)
                style.addImage("marker-icon", markerIcon)

                if(userLatitude != 0.0 && userLongitude != 0.0){
                    symbolManager.create(
                        SymbolOptions()
                        .withLatLng(LatLng(userLatitude, userLongitude))
                        .withIconImage("home-icon")
                        .withIconSize(1.5f)
                    )
                }

                parkingSpots.forEach { spot ->
                    if (spot.latitude != null && spot.longitude != null) {
                        symbolManager.create(
                            SymbolOptions()
                                .withLatLng(LatLng(spot.latitude, spot.longitude))
                                .withIconImage("marker-icon")
                                .withIconSize(1.5f)
                                .withData(gson.toJsonTree(spot))
                        )
                    }
                }


                route?.let { currentRoute ->
                    val routeCoordinates = currentRoute.geometry.coordinates.map { Point.fromLngLat(it[0], it[1]) }
                    val lineString = LineString.fromLngLats(routeCoordinates)
                    val sourceId = "route-source"
                    val source = style.getSourceAs<GeoJsonSource>(sourceId)
                    if (source != null) {
                        source.setGeoJson(lineString.toJson())
                    } else {
                        style.addSource(GeoJsonSource(sourceId, lineString.toJson()))
                    }

                    val casingLayerId = "route-layer-casing"
                    style.getLayer(casingLayerId) ?: style.addLayerBelow(
                        LineLayer(casingLayerId, sourceId).withProperties(
                            PropertyFactory.lineColor(Color.parseColor("#00FFFF")),
                            PropertyFactory.lineWidth(12f),
                            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                            PropertyFactory.lineBlur(0.3f)
                        ), "route-layer"
                    )

                    val layerId = "route-layer"
                    style.getLayer(layerId) ?: style.addLayer(
                        LineLayer(layerId, sourceId).withProperties(
                            PropertyFactory.lineColor(Color.parseColor("#00BFFF")),
                            PropertyFactory.lineWidth(7f),
                            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
                        )
                    )

                    if (routeCoordinates.isNotEmpty()) {
                        val latLngs = routeCoordinates.map { LatLng(it.latitude(), it.longitude()) }
                        val bounds = LatLngBounds.Builder().includes(latLngs).build()
                        maplibreMap.easeCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100), 2000)
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
    val context = LocalContext.current
    val firebaseAnalytics = Firebase.analytics

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { 
                        firebaseAnalytics.logEvent("book_now_clicked") {
                            param("spot_id", parkingSpot.id)
                        }
                        Toast.makeText(context, "Booking feature not yet implemented", Toast.LENGTH_SHORT).show()
                     },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Book Now")
                }
                Button(
                    onClick = {
                        val lat = parkingSpot.latitude
                        val lon = parkingSpot.longitude
                        if (lat != null && lon != null) {
                            val encodedName = URLEncoder.encode(parkingSpot.name, "UTF-8")
                            val mapUri = Uri.parse("geo:0,0?q=$lat,$lon($encodedName)")
                            val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)
                            try {
                                context.startActivity(mapIntent)
                            } catch (e: ActivityNotFoundException) {
                                Toast.makeText(context, "No map application found", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "360° View")
                }
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
