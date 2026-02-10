package com.leopoldbialek.measurementapp.TransmissionData

import kotlinx.serialization.Serializable

@Serializable
data class TransmissionPaket(
    val uuidSender: String,
    val coordinatesSender: String,
    val batteryLevelSender: String,
    val timestampSending: String
) {
}