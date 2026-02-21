package com.example.parkkar.model

data class Booking(
    val id: Int,
    val userId: Int,
    val parkingName: String,
    val address: String,
    val bookingDate: String,
    val bookingTime: String,
    val duration: Int,
    val totalCost: Double,
    val status: String
)
