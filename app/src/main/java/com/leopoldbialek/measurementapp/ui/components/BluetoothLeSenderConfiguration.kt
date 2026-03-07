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
import com.leopoldbialek.measurementapp.Enums.BluetoothLeSenderPhy
import com.leopoldbialek.measurementapp.Enums.Technology

@Composable
fun BluetoothLeSenderConfiguration(
    selectedTechnology: Technology,
    selectedBluetoothLeSenderPrimaryPhy: BluetoothLeSenderPhy,
    changeSelectedBluetoothLeSenderPrimaryPhy: (BluetoothLeSenderPhy) -> Unit,
    selectedBluetoothLeConfigSecondaryPhy: BluetoothLeSenderPhy,
    changeSelectedBluetoothLeSenderSecondaryPhy: (BluetoothLeSenderPhy) -> Unit,
    activeMeasurement: Boolean
) {
    Row(
        Modifier.padding(
            start = 50.dp
        )
    ) {
        Column() {
            Text("Primary PHY", fontSize = 15.sp, fontWeight = FontWeight.Bold)

            BluetoothLeSenderPhy.entries.forEach { bluetoothLeSenderPhy ->
                Row() {
                    RadioButton(
                        bluetoothLeSenderPhy == selectedBluetoothLeSenderPrimaryPhy,
                        {
                            changeSelectedBluetoothLeSenderPrimaryPhy(bluetoothLeSenderPhy)
                        },
                        enabled = !activeMeasurement && selectedTechnology == Technology.BLUETOOTH_LE
                    )
                    Text(
                        bluetoothLeSenderPhy.toString().replace("PHY_", ""), Modifier.align(Alignment.CenterVertically)
                    )
                }
            }
        }

        Spacer(Modifier.width(30.dp))

        Column() {
            Text("Secondary PHY", fontSize = 15.sp, fontWeight = FontWeight.Bold)

            BluetoothLeSenderPhy.entries.forEach { bluetoothLeSenderPhy ->
                Row() {
                    RadioButton(
                        bluetoothLeSenderPhy == selectedBluetoothLeConfigSecondaryPhy,
                        {
                            changeSelectedBluetoothLeSenderSecondaryPhy(bluetoothLeSenderPhy)
                        },
                        enabled = !activeMeasurement && selectedTechnology == Technology.BLUETOOTH_LE
                    )
                    Text(
                        bluetoothLeSenderPhy.toString().replace("PHY_", ""), Modifier.align(Alignment.CenterVertically)
                    )
                }
            }
        }
    }
}