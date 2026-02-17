package com.leopoldbialek.measurementapp.DataClasses

import kotlinx.serialization.Serializable

@Serializable
data class Measurement(
    val uuidSender: String,
    val coordinatesSender: String,
    val batteryLevelSender: String,
    val timestampSending: String,
    val uuidReceiver: String,
    val coordinatesReceiver: String,
    val batteryLevelReceiver: String,
    val timestampReceiving: String,
    val additionalInfo: String = ""
) {
}