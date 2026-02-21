package com.example.parkkar.model

data class FavoriteSpot(
    val id: Int,
    val userId: Int,
    val parkingId: String,
    val parkingName: String,
    val address: String,
    val latitude: Double?,
    val longitude: Double?
)
