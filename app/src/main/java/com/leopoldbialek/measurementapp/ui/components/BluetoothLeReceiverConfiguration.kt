package com.leopoldbialek.measurementapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leopoldbialek.measurementapp.Enums.BluetoothLeReceiverPhy
import com.leopoldbialek.measurementapp.Enums.BluetoothLeSenderPhy
import com.leopoldbialek.measurementapp.Enums.Technology

@Composable
fun BluetoothLeReceiverConfiguration(
    selectedTechnology: Technology,
    selectedBluetoothLeReceiverPhy: BluetoothLeReceiverPhy,
    changeSelectedBluetoothLeReceiverPhy: (BluetoothLeReceiverPhy) -> Unit,
    activeMeasurement: Boolean
) {
    Column(
        Modifier.padding(
            start = 50.dp
        )
    ) {
        BluetoothLeReceiverPhy.entries.forEach { bluetoothLeReceiverPhy ->
            Row() {
                RadioButton(
                    bluetoothLeReceiverPhy == selectedBluetoothLeReceiverPhy,
                    {
                        changeSelectedBluetoothLeReceiverPhy(bluetoothLeReceiverPhy)
                    },
                    enabled = !activeMeasurement && selectedTechnology == Technology.BLUETOOTH_LE
                )
                Text(
                    bluetoothLeReceiverPhy.toString(), Modifier.align(Alignment.CenterVertically)
                )
            }
        }
    }
}