package com.leopoldbialek.measurementapp.DataClasses

import kotlinx.serialization.Serializable

@Serializable
data class TransmissionPaket(
    val uuidSender: String,
    val coordinatesSender: String,
    val batteryLevelSender: String,
    val timestampSending: String
) {
}