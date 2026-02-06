package com.leopoldbialek.measurementapp.CommunicationServices

import kotlinx.serialization.Serializable

@Serializable
data class TransmissionPaket(
    val coordinatesSender: String,
    val batteryLevelSender: String,
    val timestampSending: String
) {
}