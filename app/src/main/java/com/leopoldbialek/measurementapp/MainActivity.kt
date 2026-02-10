package com.leopoldbialek.measurementapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leopoldbialek.measurementapp.CommunicationServices.BluetoothLe.BluetoothLeReceiver
import com.leopoldbialek.measurementapp.CommunicationServices.BluetoothLe.BluetoothLeSender
import com.leopoldbialek.measurementapp.CommunicationServices.WiFi.Aware.WifiAwareReceiverService
import com.leopoldbialek.measurementapp.CommunicationServices.WiFi.Aware.WifiAwareSenderService
import com.leopoldbialek.measurementapp.ui.theme.MeasurementAppTheme
import java.util.UUID

class MainActivity : ComponentActivity() {
    companion object {
        val deviceUUID: UUID = UUID.randomUUID()
    }

    val technologies: List<String> = listOf<String>(
        "Bluetooth LE", "Wi-Fi Aware"
    )

    val tests: List<String> = listOf<String>(
        "Distance Test", "Battery Test"
    )

    val bluetoothLeSendConfigPrimaryPhy: List<String> = listOf("1M", "LE Coded")
    val bluetoothLeSendConfigSecondaryPhy: List<String> = listOf("1M", "LE Coded")
    val bluetoothLeReceiveConfigPhy: List<String> = listOf("1M", "LE Coded", "All Supported")

    val roles: List<String> = listOf<String>("Sender", "Receiver")

    override fun onCreate(savedInstanceState: Bundle?) {
        requestPermissions(
            arrayOf(
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_ADVERTISE,
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.ACCESS_WIFI_STATE,
                android.Manifest.permission.CHANGE_WIFI_STATE,
                android.Manifest.permission.CHANGE_NETWORK_STATE,
                android.Manifest.permission.INTERNET,
                android.Manifest.permission.NEARBY_WIFI_DEVICES,
            ), 1
        )

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MeasurementAppTheme {
                MeasurementSelection(
                    applicationContext,
                    technologies,
                    bluetoothLeSendConfigPrimaryPhy,
                    bluetoothLeSendConfigSecondaryPhy,
                    bluetoothLeReceiveConfigPhy,
                    roles,
                    tests
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()

        //applicationContext.startService(Intent(applicationContext, WifiDirectSenderService::class.java))

        //applicationContext.startService(Intent(applicationContext, WifiDirectReceiverService::class.java))
    }
}

@Composable
fun MeasurementSelection(
    context: Context,
    technologies: List<String>,
    bluetoothLeSendConfigPrimaryPhy: List<String>,
    bluetoothLeSendConfigSecondaryPhy: List<String>,
    bluetoothLeReceiveConfigPhy: List<String>,
    roles: List<String>,
    tests: List<String>
) {
    val (selectedTechnology, onTechnologyChanged) = remember { mutableStateOf(technologies[0]) }
    val (selectedBluetoothLeSendConfigPrimaryPhy, onBluetoothLeSendConfigPrimaryPhyChanged) = remember {
        mutableStateOf(
            bluetoothLeSendConfigPrimaryPhy[0]
        )
    }
    val (selectedBluetoothLeSendConfigSecondaryPhy, onBluetoothLeSendConfigSecondaryPhyChanged) = remember {
        mutableStateOf(
            bluetoothLeSendConfigSecondaryPhy[0]
        )
    }
    val (selectedBluetoothLeReceiveConfigPhy, onBluetoothLeReceiveConfigPhyChanged) = remember {
        mutableStateOf(
            bluetoothLeReceiveConfigPhy[0]
        )
    }

    val (selectedRole, onRoleChanged) = remember { mutableStateOf(roles[0]) }

    val (selectedTest, onTestSelectionChanged) = remember { mutableStateOf(tests[0]) }

    val (runningMeasurement, onMeasurementStateChanged) = remember {
        mutableStateOf(
            listOf<Boolean>(
                false, true
            )[0]
        )
    }

    Column(
        Modifier.padding(start = 25.dp), Arrangement.spacedBy(50.dp)
    ) {
        Spacer(
            Modifier.height(75.dp)
        )
        TechnologySelection(
            technologies,
            selectedTechnology,
            onTechnologyChanged,
            selectedRole,
            bluetoothLeSendConfigPrimaryPhy,
            selectedBluetoothLeSendConfigPrimaryPhy,
            onBluetoothLeSendConfigPrimaryPhyChanged,
            bluetoothLeSendConfigSecondaryPhy,
            selectedBluetoothLeSendConfigSecondaryPhy,
            onBluetoothLeSendConfigSecondaryPhyChanged,
            bluetoothLeReceiveConfigPhy,
            selectedBluetoothLeReceiveConfigPhy,
            onBluetoothLeReceiveConfigPhyChanged,
            runningMeasurement
        )
        RoleSelection(roles, selectedRole, onRoleChanged, runningMeasurement)
        TestSelection(tests, selectedTest, onTestSelectionChanged, runningMeasurement)
        ControlButtons(
            context,
            selectedRole,
            selectedTechnology,
            selectedBluetoothLeSendConfigPrimaryPhy,
            selectedBluetoothLeSendConfigSecondaryPhy,
            selectedBluetoothLeReceiveConfigPhy,
            selectedTest,
            runningMeasurement,
            onMeasurementStateChanged
        )
    }
}


@Composable
fun TechnologySelection(
    technologies: List<String>,
    selectedTechnology: String,
    onTechnologyChanged: (String) -> Unit,
    selectedRole: String,
    bluetoothLeConfigPrimaryPhy: List<String>,
    selectedBluetoothLeConfigPrimaryPhy: String,
    onBluetoothLeConfigPrimaryPhyChanged: (String) -> Unit,
    bluetoothLeConfigSecondaryPhy: List<String>,
    selectedBluetoothLeConfigSecondaryPhy: String,
    onBluetoothLeConfigSecondaryPhyChanged: (String) -> Unit,
    bluetoothLeReceiveConfigPhy: List<String>,
    selectedBluetoothLeReceiveConfigPhy: String,
    onBluetoothLeReceiveConfigPhyChanged: (String) -> Unit,
    runningMeasurement: Boolean
) {


    Column() {
        Text("Technology for the Measurement", fontSize = 20.sp, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(10.dp))

        BluetoothLeConfig(
            technologies[0],
            selectedTechnology,
            onTechnologyChanged,
            selectedRole,
            bluetoothLeConfigPrimaryPhy,
            selectedBluetoothLeConfigPrimaryPhy,
            onBluetoothLeConfigPrimaryPhyChanged,
            bluetoothLeConfigSecondaryPhy,
            selectedBluetoothLeConfigSecondaryPhy,
            onBluetoothLeConfigSecondaryPhyChanged,
            bluetoothLeReceiveConfigPhy,
            selectedBluetoothLeReceiveConfigPhy,
            onBluetoothLeReceiveConfigPhyChanged,
            runningMeasurement
        )

        Spacer(Modifier.height(10.dp))

        WifiAwareConfig(
            technologies[1],
            selectedTechnology,
            onTechnologyChanged,
            selectedRole,
            runningMeasurement
        )
    }
}

@Composable
fun BluetoothLeConfig(
    technology: String,
    selectedTechnology: String,
    onTechnologyChanged: (String) -> Unit,
    selectedRole: String,
    bluetoothLeConfigPrimaryPhy: List<String>,
    selectedBluetoothLeConfigPrimaryPhy: String,
    onBluetoothLeConfigPrimaryPhyChanged: (String) -> Unit,
    bluetoothLeConfigSecondaryPhy: List<String>,
    selectedBluetoothLeConfigSecondaryPhy: String,
    onBluetoothLeConfigSecondaryPhyChanged: (String) -> Unit,
    bluetoothLeReceiveConfigPhy: List<String>,
    selectedBluetoothLeReceiveConfigPhy: String,
    onBluetoothLeReceiveConfigPhyChanged: (String) -> Unit,
    runningMeasurement: Boolean
) {
    Column() {
        Row(
        ) {
            RadioButton(
                technology == selectedTechnology,
                { onTechnologyChanged(technology) },
                enabled = !runningMeasurement
            )


            Column() {
                Spacer(Modifier.height(12.dp))

                Text(
                    technology, fontSize = 18.sp, fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(15.dp))

                if (selectedRole == "Sender") {
                    Row() {
                        Column() {
                            Text("Primary PHY", fontSize = 15.sp, fontWeight = FontWeight.Bold)

                            bluetoothLeConfigPrimaryPhy.forEach { phyConfig ->
                                Row() {
                                    RadioButton(
                                        phyConfig == selectedBluetoothLeConfigPrimaryPhy,
                                        {
                                            onBluetoothLeConfigPrimaryPhyChanged(phyConfig)
                                        },
                                        enabled = !runningMeasurement && technology == selectedTechnology
                                    )
                                    Text(
                                        phyConfig, Modifier.align(Alignment.CenterVertically)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.width(30.dp))

                        Column() {
                            Text("Secondary PHY", fontSize = 15.sp, fontWeight = FontWeight.Bold)

                            bluetoothLeConfigSecondaryPhy.forEach { phyConfig ->
                                Row() {
                                    RadioButton(
                                        phyConfig == selectedBluetoothLeConfigSecondaryPhy,
                                        {
                                            onBluetoothLeConfigSecondaryPhyChanged(phyConfig)
                                        },
                                        enabled = !runningMeasurement && technology == selectedTechnology
                                    )
                                    Text(
                                        phyConfig, Modifier.align(Alignment.CenterVertically)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    bluetoothLeReceiveConfigPhy.forEach { phyConfig ->
                        Row() {
                            RadioButton(
                                phyConfig == selectedBluetoothLeReceiveConfigPhy, {
                                    onBluetoothLeReceiveConfigPhyChanged(phyConfig)
                                }, enabled = !runningMeasurement && technology == selectedTechnology
                            )
                            Text(
                                phyConfig, Modifier.align(Alignment.CenterVertically)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WifiAwareConfig(
    technology: String,
    selectedTechnology: String,
    onTechnologyChanged: (String) -> Unit,
    selectedRole: String,
    runningMeasurement: Boolean
) {
    Row() {
        RadioButton(
            technology == selectedTechnology,
            { onTechnologyChanged(technology) },
            enabled = !runningMeasurement
        )

        Text(
            technology,
            Modifier.align(Alignment.CenterVertically),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun RoleSelection(
    roles: List<String>,
    selectedRole: String,
    onRoleChanged: (String) -> Unit,
    runningMeasurement: Boolean
) {
    Column() {
        Text("Role for the Measurement", fontSize = 20.sp, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(5.dp))

        roles.forEach { role ->
            Row() {
                RadioButton(
                    role == selectedRole, {
                        onRoleChanged(role)
                    }, enabled = !runningMeasurement
                )
                Text(
                    role, Modifier.align(Alignment.CenterVertically)
                )
            }
        }
    }
}

@Composable
fun TestSelection(
    tests: List<String>,
    selectedTest: String,
    onTestSelectionChanged: (String) -> Unit,
    runningMeasurement: Boolean
) {
    Column() {
        Text("Test to Perform", fontSize = 20.sp, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(5.dp))

        tests.forEach { test ->
            Row() {
                RadioButton(
                    test == selectedTest, {
                        onTestSelectionChanged(test)
                    }, enabled = !runningMeasurement
                )
                Text(
                    test, Modifier.align(Alignment.CenterVertically)
                )
            }
        }
    }

}

@Composable
fun ControlButtons(
    context: Context,
    selectedRole: String,
    selectedTechnology: String,
    selectedBluetoothLeConfigPrimaryPhy: String,
    selectedBluetoothLeConfigSecondaryPhy: String,
    selectedBluetoothLeReceiveConfigPhy: String,
    selectedTest: String,
    runningMeasurement: Boolean,
    onMeasurementStateChanged: (Boolean) -> Unit
) {
    Column(
        Modifier.padding(end = 25.dp), Arrangement.spacedBy(10.dp)
    ) {
        Button(
            {
                if (selectedRole == "Sender") {
                    if (selectedTechnology == "Bluetooth LE") {
                        context.startService(
                            Intent(
                                context, BluetoothLeSender::class.java
                            )
                                .putExtra(
                                    "TestType", selectedTest
                                )
                                .putExtra(
                                    "PrimaryPhyConfig", selectedBluetoothLeConfigPrimaryPhy
                                ).putExtra(
                                    "SecondaryPhyConfig", selectedBluetoothLeConfigSecondaryPhy
                                )
                        )
                    } else {
                        context.startService(
                            Intent(
                                context, WifiAwareSenderService::class.java
                            )
                                .putExtra(
                                    "TestType", selectedTest
                                )
                                .putExtra(
                                    "DeviceUUID", MainActivity.deviceUUID
                                )
                        )
                    }

                } else {
                    if (selectedTechnology == "Bluetooth LE") {
                        context.startService(
                            Intent(
                                context, BluetoothLeReceiver::class.java
                            )
                                .putExtra(
                                    "TestType", selectedTest
                                )
                                .putExtra(
                                    "DeviceUUID", MainActivity.deviceUUID
                                ).putExtra(
                                    "PhyConfig", selectedBluetoothLeReceiveConfigPhy
                                )
                        )
                    } else {
                        context.startService(
                            Intent(
                                context, WifiAwareReceiverService::class.java
                            )
                                .putExtra(
                                    "TestType", selectedTest
                                )
                                .putExtra(
                                    "DeviceUUID", MainActivity.deviceUUID
                                )
                        )
                    }
                }

                onMeasurementStateChanged(true)
                //Toast.makeText(context, "Measurement Started", Toast.LENGTH_SHORT).show()
            }, Modifier.fillMaxWidth(), !runningMeasurement
        ) {
            Text("Start Test")
        }
        Button(
            {
                if (selectedRole == "Sender") {
                    if (selectedTechnology == "Bluetooth LE") {
                        context.stopService(
                            Intent(
                                context, BluetoothLeSender::class.java
                            )
                        )
                    } else {
                        context.stopService(
                            Intent(
                                context, WifiAwareSenderService::class.java
                            )
                        )
                    }
                } else {
                    if (selectedTechnology == "Bluetooth LE") {
                        context.stopService(
                            Intent(
                                context, BluetoothLeReceiver::class.java
                            )
                        )
                    } else {
                        context.stopService(
                            Intent(
                                context, WifiAwareReceiverService::class.java
                            )
                        )
                    }
                }

                onMeasurementStateChanged(false)
                //Toast.makeText(context, "Measurement Stopped", Toast.LENGTH_SHORT).show()
            }, Modifier.fillMaxWidth(), runningMeasurement
        ) {
            Text("Stop")
        }
    }
}