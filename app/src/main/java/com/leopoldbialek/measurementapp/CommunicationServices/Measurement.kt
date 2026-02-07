package com.leopoldbialek.measurementapp.CommunicationServices

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.LongAsStringSerializer
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class Measurement(
    val technologyForDataTransmission: String,
    val coordinatesSender: String,
    val batteryLevelSender: String,
    val timestampSending: String,
    val coordinatesReceiver: String,
    val batteryLevelReceiver: String,
    val timestampReceiving: String,
    val rssiValue: String,
    val additionalInfo: String = ""
) {
}