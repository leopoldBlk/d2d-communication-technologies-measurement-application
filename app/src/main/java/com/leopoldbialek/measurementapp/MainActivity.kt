package com.leopoldbialek.measurementapp

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.leopoldbialek.measurementapp.CommunicationServices.Bluetooth.BluetoothReceiverService
import com.leopoldbialek.measurementapp.CommunicationServices.Bluetooth.BluetoothSenderService
import com.leopoldbialek.measurementapp.CommunicationServices.Bluetooth.BluetoothService
import com.leopoldbialek.measurementapp.ui.theme.MeasurementAppTheme
import java.time.Duration
import java.util.jar.Manifest

class MainActivity : ComponentActivity() {
    val technologies: List<String> = listOf<String>("Bluetooth LE (LE Coded PHY)", "Wi-Fi Aware")
    val roles: List<String> = listOf<String>("Sender", "Receiver")

    override fun onCreate(savedInstanceState: Bundle?) {
        requestPermissions(
            arrayOf(
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_ADVERTISE,
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ),
            1
        )

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MeasurementAppTheme {
                MeasurementSelection(applicationContext, technologies, roles)
            }
        }
    }
}

@Composable
fun MeasurementSelection(context: Context, technologies: List<String>, roles: List<String>) {
    val (selectedTechnology, onTechnologyChanged) = remember { mutableStateOf(technologies[0]) }
    val (selectedRole, onRoleChanged) = remember { mutableStateOf(roles[0]) }
    val (runningMeasurement, onMeasurementStateChanged) = remember { mutableStateOf(listOf<Boolean>(false, true)[0]) }

    Column(
        Modifier
            .padding(start = 25.dp),
        Arrangement.spacedBy(50.dp)
    ) {
        Spacer(
            Modifier
                .height(75.dp)
        )
        TechnologySelection(technologies, selectedTechnology, onTechnologyChanged, runningMeasurement)
        RoleSelection(roles, selectedRole, onRoleChanged, runningMeasurement)
        ControlButtons(context, selectedRole, runningMeasurement, onMeasurementStateChanged)
    }
}


@Composable
fun TechnologySelection(
    technologies: List<String>,
    selectedTechnology: String,
    onTechnologyChanged: (String) -> Unit,
    runningMeasurement: Boolean
) {


    Column() {
        Text("Technology for the Measurement", fontSize = 20.sp, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(5.dp))

        technologies.forEach { technology ->
            Row() {
                RadioButton(
                    technology == selectedTechnology,
                    {
                        onTechnologyChanged(technology)
                    },
                    enabled = !runningMeasurement
                )
                Text(
                    technology,
                    Modifier.align(Alignment.CenterVertically)
                )
            }
        }
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
                    role == selectedRole,
                    {
                        onRoleChanged(role)
                    },
                    enabled = !runningMeasurement
                )
                Text(
                    role,
                    Modifier.align(Alignment.CenterVertically)
                )
            }
        }
    }
}

@Composable
fun ControlButtons(
    context: Context,
    selectedRole: String,
    runningMeasurement: Boolean,
    onMeasurementStateChanged: (Boolean) -> Unit
) {
    Column(
        Modifier.padding(end = 25.dp),
        Arrangement.spacedBy(10.dp)
    ) {
        Button(
            {
                if (selectedRole == "Sender") {
                    context.startService(
                        Intent(
                            context,
                            BluetoothSenderService::class.java
                        )
                    )
                } else {
                    context.startService(
                        Intent(
                            context,
                            BluetoothReceiverService::class.java
                        )
                    )
                }

                onMeasurementStateChanged(true)
                Toast.makeText(context, "Measurement Started", Toast.LENGTH_SHORT).show()
            },
            Modifier.fillMaxWidth(),
            !runningMeasurement
        ) {
            Text("Start")
        }
        Button(
            {
                if (selectedRole == "Sender") {
                    context.stopService(
                        Intent(
                            context,
                            BluetoothSenderService::class.java
                        )
                    )
                } else {
                    context.stopService(
                        Intent(
                            context,
                            BluetoothReceiverService::class.java
                        )
                    )
                }

                onMeasurementStateChanged(false)
                Toast.makeText(context, "Measurement Stopped", Toast.LENGTH_SHORT).show()
            },
            Modifier.fillMaxWidth(),
            runningMeasurement
        ) {
            Text("Stop")
        }
    }
}

