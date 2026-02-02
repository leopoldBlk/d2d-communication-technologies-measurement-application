package com.leopoldbialek.measurementapp.CommunicationServices

import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class Measurement(
    val measurementId: String,
    val batteryLevel: Float,
    val coordinates: String,
    val timestamp: String,
) {
}