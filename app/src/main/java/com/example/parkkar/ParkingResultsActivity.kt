package com.example.parkkar

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable // For clickable stars
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarBorder // For empty stars
import androidx.compose.material3.*
import androidx.compose.material3.TextFieldDefaults // Explicit import
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.parkkar.data.model.OpeningTimeInfo
import com.example.parkkar.data.model.ParkingSpot
import com.example.parkkar.data.model.PriceInfo
import com.example.parkkar.ui.parkingresults.ParkingResultsViewModel
import com.example.parkkar.ui.parkingresults.ParkingSpotUiState
import com.example.parkkar.ui.theme.ParkkarTheme
import java.util.Locale
import java.util.UUID

data class ReviewItemData(
    val id: String = UUID.randomUUID().toString(),
    val reviewerName: String,
    val rating: Float,
    val reviewText: String,
    val date: String
)

class ParkingResultsActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PARKING_SPOT_ID = "extra_parking_spot_id"
    }

    private val viewModel: ParkingResultsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val parkingSpotId = intent.getStringExtra(EXTRA_PARKING_SPOT_ID)

        if (parkingSpotId != null) {
            Log.d("ParkingResultsActivity", "Received parking spot ID: $parkingSpotId")
            viewModel.fetchParkingSpotDetails(parkingSpotId)
        } else {
            Log.e("ParkingResultsActivity", "No parking spot ID received. Cannot display details.")
            Toast.makeText(this, "Error: Parking spot details not found.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setContent {
            ParkkarTheme {
                val uiState by viewModel.parkingSpotUiState.collectAsState()

                when (val state = uiState) {
                    is ParkingSpotUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is ParkingSpotUiState.Success -> {
                        ExactParkingResultsScreen(
                            parkingSpot = state.parkingSpot,
                            currentReviews = emptyList(),
                            onCloseScreen = { finish() }
                        )
                    }
                    is ParkingSpotUiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    is ParkingSpotUiState.NotFound -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Parking spot not found.")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExactParkingResultsScreen(
    parkingSpot: ParkingSpot, 
    currentReviews: List<ReviewItemData>,
    onCloseScreen: () -> Unit
) {
    val context = LocalContext.current
    var showWriteReviewForm by remember { mutableStateOf(false) }
    var userRating by remember { mutableStateOf(0f) }
    var userReviewText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            ExactTopBar(
                title = parkingSpot.parkingName ?: "Parking Details",
                subtitle = "${parkingSpot.coverageType ?: ""} - ${parkingSpot.fourWheelerSpots + parkingSpot.twoWheelerSpots} spaces",
                onClose = onCloseScreen
            )
        },
        containerColor = Color(0xFFF0F2F5)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding) 
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            ExactCoreInfoSection(
                price = parkingSpot.prices?.firstOrNull()?.amount ?: 0.0,
                currency = parkingSpot.prices?.firstOrNull()?.currency ?: "₹",
                duration = parkingSpot.prices?.firstOrNull()?.duration ?: "N/A",
                timeToDestination = "Calculating...", 
                locationName = parkingSpot.parkingName ?: "N/A",
                locationArea = parkingSpot.cityName, 
                onGetDirections = {
                    Toast.makeText(context, "Get Directions Clicked for ${parkingSpot.parkingName}", Toast.LENGTH_SHORT).show()
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            ExactPricesSection(prices = parkingSpot.prices ?: emptyList())
            Spacer(modifier = Modifier.height(8.dp))
            ExactOpeningTimesSection(openingTimes = parkingSpot.openingTimes ?: emptyList())
            Spacer(modifier = Modifier.height(8.dp))
            ExactPaymentOptionsSection()
            Spacer(modifier = Modifier.height(8.dp))

            if (!showWriteReviewForm) {
                ExactReviewsSection(
                    currentReviews = currentReviews, 
                    onRateWriteReviewClick = { showWriteReviewForm = true }
                )
            } else {
                ExactWriteReviewForm(
                    currentRating = userRating,
                    reviewText = userReviewText,
                    onRatingChange = { userRating = it },
                    onReviewTextChange = { userReviewText = it },
                    onAddReview = { rating, text ->
                        Toast.makeText(context, "Review: $rating stars, '$text'", Toast.LENGTH_LONG).show()
                        showWriteReviewForm = false
                        userRating = 0f
                        userReviewText = ""
                    },
                    onCancel = {
                        showWriteReviewForm = false
                        userRating = 0f
                        userReviewText = ""
                    }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExactTopBar(title: String, subtitle: String, onClose: () -> Unit) {
    TopAppBar(
        title = {
            Column {
                Text(text = title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        actions = {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Close")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = Modifier.statusBarsPadding()
    )
}

@Composable
fun ExactCoreInfoSection(
    price: Double,
    currency: String,
    duration: String,
    timeToDestination: String,
    locationName: String,
    locationArea: String,
    onGetDirections: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PARK-KAR",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color(0xFF4A4A4A)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Park-Kar Logo",
                modifier = Modifier.height(20.dp)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$currency${String.format(Locale.getDefault(), "%.0f", price)}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(duration, fontSize = 13.sp, color = Color.Gray)
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                        contentDescription = "Time to destination",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(timeToDestination, fontSize = 18.sp, fontWeight = FontWeight.Normal, color = Color.Black)
                }
                Text("to destination", fontSize = 12.sp, color = Color.Gray)
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Button(
            onClick = onGetDirections,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF301934))
        ) {
            Text("Get Directions", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Medium)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                Icons.Filled.LocationOn,
                contentDescription = "Location",
                tint = Color.DarkGray,
                modifier = Modifier.size(20.dp).padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(locationName, fontSize = 16.sp, color = Color.DarkGray, fontWeight = FontWeight.Normal)
                Text(locationArea, fontSize = 14.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun DottedDividerExact(modifier: Modifier = Modifier) {
    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
    Canvas(
        modifier
            .fillMaxWidth()
            .height(1.dp)) {
        drawLine(
            color = Color.LightGray,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            pathEffect = pathEffect
        )
    }
}

@Composable
fun ExactPricesSection(prices: List<PriceInfo>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Text(
            "Prices",
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(10.dp))
        if (prices.isEmpty()) {
            Text("No price information available.", fontSize = 14.sp, color = Color.Gray)
        } else {
            prices.forEachIndexed { index, priceInfo ->
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(0.7f)) {
                            Text(
                                "${priceInfo.days ?: ""} ${priceInfo.timeRange ?: ""}",
                                fontSize = 15.sp,
                                color = Color.DarkGray
                            )
                            Text(
                                priceInfo.rateType ?: "",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        }
                        Text(
                            "${priceInfo.currency ?: "₹"}${String.format(Locale.getDefault(), "%.2f", priceInfo.amount ?: 0.0)}", 
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.DarkGray,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(0.3f)
                        )
                    }
                    if (index < prices.size - 1) {
                        DottedDividerExact(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ExactOpeningTimesSection(openingTimes: List<OpeningTimeInfo>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Text(
            "Opening Times",
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(10.dp))
        if (openingTimes.isEmpty()) {
            Text("No opening time information available.", fontSize = 14.sp, color = Color.Gray)
        } else {
            openingTimes.forEachIndexed { index, timeInfo ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        timeInfo.days ?: "N/A",
                        fontSize = 15.sp,
                        color = Color.DarkGray,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        timeInfo.timeRange ?: "N/A",
                        fontSize = 15.sp,
                        color = Color.DarkGray,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (index < openingTimes.size - 1) {
                    DottedDividerExact(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
fun ExactPaymentOptionsSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Text(
            "Payment Options",
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CreditCard, contentDescription = "Card Payment", tint = Color.DarkGray, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Card/Mobile Payments Accepted", fontSize = 15.sp, color = Color.DarkGray)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.AccountBalanceWallet, contentDescription = "Mobile Payment", tint = Color.DarkGray, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Cash Accepted", fontSize = 15.sp, color = Color.DarkGray)
        }
    }
}

@Composable
fun ExactReviewsSection(
    currentReviews: List<ReviewItemData>, 
    onRateWriteReviewClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Ratings and Reviews",
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            TextButton(onClick = onRateWriteReviewClick) {
                Text("Rate & Write Review", color = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        if (currentReviews.isEmpty()) {
            Text("No reviews yet. Be the first to write one!", fontSize = 14.sp, color = Color.Gray)
        } else {
            currentReviews.forEach { review ->
                Text("${review.reviewerName} (${review.rating} stars): ${review.reviewText} - ${review.date}", modifier = Modifier.padding(bottom=4.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExactWriteReviewForm(
    currentRating: Float,
    reviewText: String,
    onRatingChange: (Float) -> Unit,
    onReviewTextChange: (String) -> Unit,
    onAddReview: (Float, String) -> Unit,
    onCancel: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current // FIX: Get context in the composable scope
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Text("Write a Review", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
        Spacer(modifier = Modifier.height(16.dp))

        Text("Your Rating:", fontSize = 14.sp, color = Color.Gray)
        Row(modifier = Modifier.padding(vertical = 8.dp)) {
            (1..5).forEach { starIndex ->
                Icon(
                    imageVector = if (starIndex <= currentRating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Star $starIndex",
                    tint = if (starIndex <= currentRating) Color(0xFFFFC107) else Color.Gray,
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { onRatingChange(starIndex.toFloat()) }
                )
            }
        }

        TextField(
            value = reviewText,
            onValueChange = onReviewTextChange,
            label = { Text("Your Review (Optional)") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
            ),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { 
                if (currentRating > 0) {
                    onAddReview(currentRating, reviewText)
                } else {
                    // FIX: Use the context variable from the outer scope
                    Toast.makeText(context, "Please select a rating before submitting.", Toast.LENGTH_SHORT).show()
                }
            }) {
                Text("Add Review")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ExactParkingResultsScreenPreview() {
    ParkkarTheme {
        val previewSpot = ParkingSpot(
            id = "preview-id",
            cityName = "Preview City",
            parkingName = "Preview Parking Name",
            address = "123 Preview St, Preview City",
            latitude = 0.0, longitude = 0.0,
            fourWheelerSpots = 100, twoWheelerSpots = 50,
            coverageType = "Covered",
            prices = listOf(PriceInfo(days = "Mon-Fri", timeRange = "All Day", rateType = "Flat", duration = "1 hour", amount = 5.0, currency = "$")),
            openingTimes = listOf(OpeningTimeInfo(days = "Mon-Fri", timeRange = "09:00 - 18:00"))
        )
        ExactParkingResultsScreen(previewSpot, emptyList(), onCloseScreen = {})
    }
}
