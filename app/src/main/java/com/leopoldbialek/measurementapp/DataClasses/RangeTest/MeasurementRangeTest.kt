package com.leopoldbialek.measurementapp.DataClasses.RangeTest

import kotlinx.serialization.Serializable

@Serializable
data class MeasurementRangeTest(
    val coordinatesSender: String,
    val timestampSending: String,
    val additionalInfoSender: String,
    val coordinatesReceiver: String,
    val timestampReceiving: String,
    val additionalInfoReceiver: String = ""
) {
}