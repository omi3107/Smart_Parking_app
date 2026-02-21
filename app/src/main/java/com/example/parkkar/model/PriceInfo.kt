package com.example.parkkar.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PriceInfo(
    val days: String? = null,
    @SerialName("timeRange") val timeRange: String? = null,
    @SerialName("rateType") val rateType: String? = null,
    val duration: String? = null,
    val amount: Double? = null,
    val currency: String? = "₹"
)
