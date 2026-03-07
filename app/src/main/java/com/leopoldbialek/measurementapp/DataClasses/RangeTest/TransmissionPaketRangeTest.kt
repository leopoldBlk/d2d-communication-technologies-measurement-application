package com.leopoldbialek.measurementapp.DataClasses.RangeTest

import kotlinx.serialization.Serializable

@Serializable
data class TransmissionPaketRangeTest(
    val coordinatesSender: String,
    val timestampSending: String,
    val additionalInfoSender: String = ""
) {
}