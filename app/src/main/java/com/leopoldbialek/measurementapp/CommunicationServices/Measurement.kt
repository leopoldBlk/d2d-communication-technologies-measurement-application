package com.leopoldbialek.measurementapp.CommunicationServices

import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class Measurement(
    val measurementId: UUID,
    val batteryLevel: Float,
    val coordinates: Coordinates,
    val timestamp: LocalDateTime
) {
}