
package com.example.parkkar

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.provider.CalendarContract
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.parkkar.data.DatabaseHelper
import com.example.parkkar.data.UserPreferencesRepository
import com.example.parkkar.model.Booking
import com.example.parkkar.model.ParkingLocation
import com.example.parkkar.ui.theme.ParkkarTheme
import com.example.parkkar.utils.NotificationHelper
import com.example.parkkar.worker.TimeToLeaveWorker
import com.google.gson.Gson
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class BookingConfirmationActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PARKING_SPOT = "extra_parking_spot"
        const val EXTRA_ARRIVAL_TIME = "extra_arrival_time"
        const val EXTRA_LEAVING_TIME = "extra_leaving_time"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createNotificationChannel(this)
        val parkingSpotJson = intent.getStringExtra(EXTRA_PARKING_SPOT)
        val arrivalTime = intent.getLongExtra(EXTRA_ARRIVAL_TIME, 0)
        val leavingTime = intent.getLongExtra(EXTRA_LEAVING_TIME, 0)
        val parkingSpot = Gson().fromJson(parkingSpotJson, ParkingLocation::class.java)

        setContent {
            val isDarkTheme = (application as MainApplication).isDarkTheme
            ParkkarTheme(darkTheme = isDarkTheme) {
                BookingConfirmationScreen(
                    parkingSpot = parkingSpot,
                    arrivalTime = arrivalTime,
                    leavingTime = leavingTime,
                    onNavigateBack = { onBackPressedDispatcher.onBackPressed() },
                    onNavigateToProfile = { startActivity(Intent(this, ProfileActivity::class.java)) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingConfirmationScreen(
    parkingSpot: ParkingLocation,
    arrivalTime: Long,
    leavingTime: Long,
    onNavigateBack: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val context = LocalContext.current
    val bookingTimestamp = System.currentTimeMillis()
    val dbHelper = DatabaseHelper(context)
    val userPreferencesRepository = UserPreferencesRepository.getInstance(context)
    val notificationsEnabled by userPreferencesRepository.notificationsEnabled.collectAsState(initial = true)


    val qrCodeBitmap = remember(parkingSpot, arrivalTime, leavingTime) {
        generateQrCode(parkingSpot.id, arrivalTime, leavingTime)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Booking Confirmation") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("PARK-KAR", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Image(painter = painterResource(id = R.drawable.logo), contentDescription = "Park-Kar Logo", modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = onNavigateToProfile) {
                            Icon(Icons.Default.Person, contentDescription = "Profile")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Parking Ticket", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
                    Spacer(modifier = Modifier.height(16.dp))
                    if (qrCodeBitmap != null) {
                        Image(
                            bitmap = qrCodeBitmap.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier
                                .size(200.dp)
                                .align(Alignment.CenterHorizontally)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    TicketDetailRow("Location:", parkingSpot.name ?: "N/A")
                    TicketDetailRow("Address:", parkingSpot.address ?: "N/A")
                    TicketDetailRow("Arrival Time:", formatTimestamp(arrivalTime))
                    TicketDetailRow("Leaving Time:", formatTimestamp(leavingTime))
                    TicketDetailRow("Booked On:", formatTimestamp(bookingTimestamp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Payment Summary", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    TicketDetailRow("Base Price:", "${parkingSpot.prices?.firstOrNull()?.currency ?: "₹"}${parkingSpot.prices?.firstOrNull()?.amount ?: "N/A"}")
                    TicketDetailRow("Taxes & Fees:", "Calculated at payment")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Total: ${parkingSpot.prices?.firstOrNull()?.currency ?: "₹"}${parkingSpot.prices?.firstOrNull()?.amount ?: "N/A"}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.End))
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = {
                        val durationInMinutes = ((leavingTime - arrivalTime) / (1000 * 60)).toInt()
                        val sdf = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
                        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                        val calendar = Calendar.getInstance()
                        calendar.timeInMillis = bookingTimestamp

                        val newBooking = Booking(
                            id = 0, // DB will auto-increment
                            userId = 1, // Replace with actual user ID
                            parkingName = parkingSpot.name ?: "N/A",
                            address = parkingSpot.address ?: "N/A",
                            bookingDate = sdf.format(calendar.time),
                            bookingTime = timeFormat.format(calendar.time),
                            duration = durationInMinutes,
                            totalCost = parkingSpot.prices?.firstOrNull()?.amount ?: 0.0,
                            status = "Confirmed"
                        )

                        val bookingId = dbHelper.addBooking(newBooking)

                        if (bookingId != -1L) {
                            if (notificationsEnabled) {
                                NotificationHelper.showBookingConfirmationNotification(context, newBooking.parkingName)

                                val expiryTime = formatTimestamp(leavingTime)
                                val notificationTime = leavingTime - TimeUnit.MINUTES.toMillis(15)
                                val currentTime = System.currentTimeMillis()

                                if (notificationTime > currentTime) {
                                    val delay = notificationTime - currentTime
                                    val data = Data.Builder()
                                        .putString("parkingName", newBooking.parkingName)
                                        .putString("expiryTime", expiryTime)
                                        .build()

                                    val timeToLeaveWorkRequest = OneTimeWorkRequestBuilder<TimeToLeaveWorker>()
                                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                                        .setInputData(data)
                                        .build()

                                    WorkManager.getInstance(context).enqueue(timeToLeaveWorkRequest)
                                    Toast.makeText(context, "Time to leave alert scheduled.", Toast.LENGTH_SHORT).show()
                                }
                            }
                            Toast.makeText(context, "Booking successful!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Booking failed.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Book Now")
                }
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_INSERT)
                            .setData(CalendarContract.Events.CONTENT_URI)
                            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, arrivalTime)
                            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, leavingTime)
                            .putExtra(CalendarContract.Events.TITLE, "Parking at ${parkingSpot.name}")
                            .putExtra(CalendarContract.Events.EVENT_LOCATION, parkingSpot.address)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Add to Calendar")
                }
            }
        }
    }
}

@Composable
fun TicketDetailRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, fontWeight = FontWeight.Bold, modifier = Modifier.width(120.dp))
        Text(text = value)
    }
}

private fun generateQrCode(spotId: String, arrival: Long, leaving: Long): Bitmap? {
    val content = "parkkar://booking?spotId=$spotId&arrival=$arrival&leaving=$leaving"
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        bmp
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("EEE, MMM d, yyyy, hh:mm a", Locale.getDefault())
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timestamp
    return sdf.format(calendar.time)
}
