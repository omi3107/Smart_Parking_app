package com.example.parkkar.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpeningTimeInfo(
    val days: String? = null,
    @SerialName("timeRange") val timeRange: String? = null
)
