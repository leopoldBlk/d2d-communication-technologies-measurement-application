package com.leopoldbialek.measurementapp.DataClasses.BatteryTest

import kotlinx.serialization.Serializable

@Serializable
data class MeasurementBatteryTest(
    val batteryLevel: String,
    val timestamp: String,
    val additionalInfo: String = ""
) {
}