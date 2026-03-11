package com.leopoldbialek.measurementapp.DataClasses.RangeTest

import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
data class TransmissionPacketRangeTest @OptIn(ExperimentalUuidApi::class) constructor(
    val packetUuid: String,
    val coordinatesSender: String,
    val timestampSending: String,
    val additionalInfoSender: String = ""
) {
}