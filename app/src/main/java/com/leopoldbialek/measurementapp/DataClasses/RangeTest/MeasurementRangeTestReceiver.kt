package com.leopoldbialek.measurementapp.DataClasses.RangeTest

import kotlinx.serialization.Serializable

@Serializable
data class MeasurementRangeTestReceiver constructor(
    val packetUuid: String,
    val latReceiver: String,
    val longReceiver: String,
    val timestampReceiving: String,
    val additionalInfoReceiver: String = ""
) {
}