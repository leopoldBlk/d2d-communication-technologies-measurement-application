package com.leopoldbialek.measurementapp.DataClasses.RangeTest

import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
data class MeasurementRangeTest constructor(
    val paketUuid: String,
    val coordinatesSender: String,
    val timestampSending: String,
    val additionalInfoSender: String,
    val coordinatesReceiver: String,
    val timestampReceiving: String,
    val additionalInfoReceiver: String = ""
) {
}