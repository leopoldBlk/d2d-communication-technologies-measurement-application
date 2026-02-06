package com.leopoldbialek.measurementapp.CommunicationServices

import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class Measurement(
    val coordinatesSender: String,
    val batteryLevelSender: String,
    val timestampSending: String,
    val coordinatesReceiver: String,
    val batteryLevelReceiver: String,
    val timestampReceiving: String,
    val rssiValue: String
) {
}