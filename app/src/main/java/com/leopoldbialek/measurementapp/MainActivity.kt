package com.leopoldbialek.measurementapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.leopoldbialek.measurementapp.ui.components.ControlButtons
import com.leopoldbialek.measurementapp.ui.components.RoleSelection
import com.leopoldbialek.measurementapp.ui.components.SendingReceivingIndicator
import com.leopoldbialek.measurementapp.ui.components.TechnologySelection
import com.leopoldbialek.measurementapp.ui.components.TestTypeSelection
import com.leopoldbialek.measurementapp.ui.theme.MeasurementAppTheme

class MainActivity : ComponentActivity() {
    private val measurementViewModel: MeasurementViewModel by viewModels<MeasurementViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissions(
            arrayOf(
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_ADVERTISE,
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.ACCESS_WIFI_STATE,
                android.Manifest.permission.CHANGE_WIFI_STATE,
                android.Manifest.permission.CHANGE_NETWORK_STATE,
                android.Manifest.permission.INTERNET,
                android.Manifest.permission.NEARBY_WIFI_DEVICES,
            ), 1
        )

        enableEdgeToEdge()
        setContent {
            MeasurementAppTheme {
                Column(
                    Modifier.padding(start = 25.dp), Arrangement.spacedBy(35.dp)
                ) {
                    Spacer(
                        Modifier.height(20.dp)
                    )

                    TechnologySelection(
                        measurementViewModel.selectedTechnology.value,
                        measurementViewModel::changeSelectedTechnology,
                        measurementViewModel.selectedRole.value,
                        measurementViewModel.selectedBluetoothLeSenderPrimaryPhy.value,
                        measurementViewModel::changeSelectedBluetoothLeSenderPrimaryPhy,
                        measurementViewModel.selectedBluetoothLeSenderSecondaryPhy.value,
                        measurementViewModel::changeSelectedBluetoothLeSenderSecondaryPhy,
                        measurementViewModel.selectedBluetoothLeReceiverPhy.value,
                        measurementViewModel::changeSelectedBluetoothLeReceiverPhy,
                        measurementViewModel.activeMeasurement.value
                    )

                    RoleSelection(
                        measurementViewModel.selectedRole.value,
                        measurementViewModel::changeSelectedRole,
                        measurementViewModel.activeMeasurement.value
                    )

                    TestTypeSelection(
                        measurementViewModel.selectedTestType.value,
                        measurementViewModel::changeSelectedTestType,
                        measurementViewModel.activeMeasurement.value
                    )

                    ControlButtons(
                        applicationContext,
                        measurementViewModel.selectedTechnology.value,
                        measurementViewModel.selectedRole.value,
                        measurementViewModel.selectedTestType.value,
                        measurementViewModel.activeMeasurement.value,
                        measurementViewModel::changeMeasurementState,
                        measurementViewModel.selectedBluetoothLeSenderPrimaryPhy.value,
                        measurementViewModel.selectedBluetoothLeSenderSecondaryPhy.value,
                        measurementViewModel.selectedBluetoothLeReceiverPhy.value
                    )

                    SendingReceivingIndicator(
                        measurementViewModel.selectedRole.value,
                        measurementViewModel.activeDataTransmission.value,
                        measurementViewModel.activeMeasurement.value
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context?,
                    intent: Intent?
                ) {
                    measurementViewModel.changeActiveDataTransmission(
                        intent!!.getBooleanExtra(
                            "activeDataTransmission",
                            false
                        )
                    )
                }
            }, IntentFilter("MEASUREMENT_BROADCAST"), RECEIVER_EXPORTED
        )


        //applicationContext.startService(Intent(applicationContext, WifiDirectSenderService::class.java))

        //applicationContext.startService(Intent(applicationContext, WifiDirectReceiverService::class.java))
    }
}