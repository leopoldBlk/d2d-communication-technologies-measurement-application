package com.leopoldbialek.measurementapp.DataClasses.RangeTest

import kotlinx.serialization.Serializable

@Serializable
data class MeasurementRangeTestSender constructor(
    val packetUuid: String,
    val latSender: String,
    val longSender: String,
    val timestampSending: String,
    val additionalInfoSender: String = ""
) {
}