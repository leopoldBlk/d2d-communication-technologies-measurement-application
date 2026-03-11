package com.leopoldbialek.measurementapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leopoldbialek.measurementapp.Enums.BluetoothLeReceiverPhy
import com.leopoldbialek.measurementapp.Enums.BluetoothLeSenderPrimaryPhy
import com.leopoldbialek.measurementapp.Enums.BluetoothLeSenderSecondaryPhy
import com.leopoldbialek.measurementapp.Enums.Role
import com.leopoldbialek.measurementapp.Enums.Technology

@Composable
fun TechnologySelection(
    selectedTechnology: Technology,
    changeSelectedTechnology: (Technology) -> Unit,
    selectedRole: Role,
    selectedBluetoothLeSenderPrimaryPhy: BluetoothLeSenderPrimaryPhy,
    changeSelectedBluetoothLeSenderPrimaryPhy: (BluetoothLeSenderPrimaryPhy) -> Unit,
    selectedBluetoothLeSenderSecondaryPhy: BluetoothLeSenderSecondaryPhy,
    changeSelectedBluetoothLeSenderSecondaryPhy: (BluetoothLeSenderSecondaryPhy) -> Unit,
    selectedBluetoothLeReceiverPhy: BluetoothLeReceiverPhy,
    changeSelectedBluetoothLeReceiverPhy: (BluetoothLeReceiverPhy) -> Unit,
    activeMeasurement: Boolean
) {
    Column(
        Modifier.height(275.dp)
    ) {
        Text("Technology for the Measurement", fontSize = 20.sp, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(10.dp))
        Technology.entries.forEach { technology ->
            TechnologySelectable(
                technology,
                selectedTechnology,
                changeSelectedTechnology,
                activeMeasurement
            )

            if (technology == Technology.BLUETOOTH_LE) {
                when (selectedRole) {
                    Role.SENDER -> {
                        BluetoothLeSenderConfiguration(
                            selectedTechnology,
                            selectedBluetoothLeSenderPrimaryPhy,
                            changeSelectedBluetoothLeSenderPrimaryPhy,
                            selectedBluetoothLeSenderSecondaryPhy,
                            changeSelectedBluetoothLeSenderSecondaryPhy,
                            activeMeasurement
                        )
                    }

                    Role.RECEIVER -> {
                        BluetoothLeReceiverConfiguration(
                            selectedTechnology,
                            selectedBluetoothLeReceiverPhy,
                            changeSelectedBluetoothLeReceiverPhy,
                            activeMeasurement
                        )
                    }
                }
            }
        }
    }
}