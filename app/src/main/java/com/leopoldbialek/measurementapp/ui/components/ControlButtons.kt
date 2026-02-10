package com.leopoldbialek.measurementapp.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.leopoldbialek.measurementapp.Enums.BluetoothLeReceiverPhy
import com.leopoldbialek.measurementapp.Enums.BluetoothLeSenderPhy
import com.leopoldbialek.measurementapp.Enums.Role
import com.leopoldbialek.measurementapp.Enums.Technology
import com.leopoldbialek.measurementapp.Enums.TestType
import com.leopoldbialek.measurementapp.Services.Communication.BluetoothLe.BluetoothLeReceiver
import com.leopoldbialek.measurementapp.Services.Communication.BluetoothLe.BluetoothLeSender
import com.leopoldbialek.measurementapp.Services.Communication.WiFi.Aware.WifiAwareReceiverService
import com.leopoldbialek.measurementapp.Services.Communication.WiFi.Aware.WifiAwareSenderService
import com.leopoldbialek.measurementapp.Services.Location.LocationService

@Composable
fun ControlButtons(
    context: Context,
    selectedTechnology: Technology,
    selectedRole: Role,
    selectedTestType: TestType,
    activeMeasurement: Boolean,
    changedMeasurementState: () -> Unit,
    selectedBluetoothLeSenderPrimaryPhy: BluetoothLeSenderPhy,
    selectedBluetoothLeSenderSecondaryPhy: BluetoothLeSenderPhy,
    selectedBluetoothLeReceiverPhy: BluetoothLeReceiverPhy
) {
    Column(
        Modifier.padding(end = 25.dp), Arrangement.spacedBy(10.dp)
    ) {
        Button(
            {
                changedMeasurementState()

                startServices(
                    context,
                    selectedTechnology,
                    selectedRole,
                    selectedTestType,
                    selectedBluetoothLeSenderPrimaryPhy,
                    selectedBluetoothLeSenderSecondaryPhy,
                    selectedBluetoothLeReceiverPhy
                )
            },
            Modifier.fillMaxWidth(),
            !activeMeasurement
        ) {
            Text("Start")
        }
        Button(
            {
                changedMeasurementState()

                stopServices(
                    context,
                    selectedTechnology,
                    selectedRole,
                    selectedTestType
                )
            },
            Modifier.fillMaxWidth(),
            activeMeasurement
        ) {
            Text("Stop")
        }
    }
}

private fun startServices(
    context: Context,
    selectedTechnology: Technology,
    selectedRole: Role,
    selectedTestType: TestType,
    selectedBluetoothLeSenderPrimaryPhy: BluetoothLeSenderPhy,
    selectedBluetoothLeConfigSecondaryPhy: BluetoothLeSenderPhy,
    selectedBluetoothLeReceiverPhy: BluetoothLeReceiverPhy
) {
    if (selectedTestType == TestType.DISTANCE) {
        context.startService(
            Intent(context, LocationService::class.java)
        )
    }

    if (selectedTechnology == Technology.BLUETOOTH_LE && selectedRole == Role.SENDER) {
        context.startService(
            Intent(context, BluetoothLeSender::class.java)
                .putExtra("PrimarySenderPhy", selectedBluetoothLeSenderPrimaryPhy.name)
                .putExtra("SecondarySenderPhy", selectedBluetoothLeConfigSecondaryPhy.name)
        )
    } else if (selectedTechnology == Technology.BLUETOOTH_LE && selectedRole == Role.RECEIVER) {
        context.startService(
            Intent(context, BluetoothLeReceiver::class.java)
                .putExtra("TestType", selectedTestType.name)
                .putExtra("ReceiverPhy", selectedBluetoothLeReceiverPhy.name)
        )
    } else if (selectedTechnology == Technology.WIFI_AWARE && selectedRole == Role.SENDER) {
        context.startService(
            Intent(context, WifiAwareSenderService::class.java)
        )
    } else if (selectedTechnology == Technology.WIFI_AWARE && selectedRole == Role.RECEIVER) {
        context.startService(
            Intent(context, WifiAwareReceiverService::class.java)
                .putExtra("TestType", selectedTestType.name)
        )
    }
}

private fun stopServices(
    context: Context,
    selectedTechnology: Technology,
    selectedRole: Role,
    selectedTestType: TestType
) {
    if (selectedTestType == TestType.DISTANCE) {
        context.stopService(
            Intent(context, LocationService::class.java)
        )
    }

    if (selectedTechnology == Technology.BLUETOOTH_LE && selectedRole == Role.SENDER) {
        context.stopService(
            Intent(context, BluetoothLeSender::class.java)
        )
    } else if (selectedTechnology == Technology.BLUETOOTH_LE && selectedRole == Role.RECEIVER) {
        context.stopService(
            Intent(context, BluetoothLeReceiver::class.java)
        )
    } else if (selectedTechnology == Technology.WIFI_AWARE && selectedRole == Role.SENDER) {
        context.stopService(
            Intent(context, WifiAwareSenderService::class.java)
        )
    } else if (selectedTechnology == Technology.WIFI_AWARE && selectedRole == Role.RECEIVER) {
        context.stopService(
            Intent(context, WifiAwareReceiverService::class.java)
        )
    }
}