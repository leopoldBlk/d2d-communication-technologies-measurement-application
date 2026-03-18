package com.leopoldbialek.measurementapp.Services.Communication.BluetoothLe

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.os.BatteryManager
import android.os.ParcelUuid
import android.util.Log
import com.google.android.gms.time.TrustedTime
import com.google.android.gms.time.TrustedTimeClient
import com.leopoldbialek.measurementapp.Enums.TestType
import com.leopoldbialek.measurementapp.Utils.LogController
import com.leopoldbialek.measurementapp.Utils.PersistenceController
import java.util.UUID

abstract class BluetoothLeService() : Service() {
    protected lateinit var blManager: BluetoothManager
    protected lateinit var blAdapter: BluetoothAdapter

    protected lateinit var batteryManager: BatteryManager
    protected lateinit var batteryChangeBroadcastReceiver: BroadcastReceiver

    protected lateinit var testType: TestType
    protected lateinit var persistenceController: PersistenceController

    protected val serviceUUID: ParcelUuid =
        ParcelUuid(UUID.fromString("00000000-0000-1000-8000-00805f9b34fb"))
    protected val measurements: MutableList<Any> =
        mutableListOf<Any>()
    protected var trustedTimeClient: TrustedTimeClient? = null
    private val checkSupportedBleFeatures: Boolean = false

    override fun onCreate() {
        super.onCreate()

        blManager = applicationContext.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        blAdapter = blManager.adapter

        batteryManager = applicationContext.getSystemService(BATTERY_SERVICE) as BatteryManager

        TrustedTime.createClient(applicationContext).addOnSuccessListener { trustedTimeClient ->
            this.trustedTimeClient = trustedTimeClient
        }

        if (checkSupportedBleFeatures) {
            val supportedBleFeatures: MutableMap<String, Boolean> = mutableMapOf(
                "isLe2MPhySupported" to blAdapter.isLe2MPhySupported,
                "isLeCodedPhySupported" to blAdapter.isLeCodedPhySupported,
                "isLeExtendedAdvertisingSupported" to blAdapter.isLeExtendedAdvertisingSupported,
                "isLePeriodicAdvertisingSupported" to blAdapter.isLePeriodicAdvertisingSupported
            )

            for (feature in supportedBleFeatures) {
                if (feature.value) {
                    LogController.log(Log.INFO, "Bluetooth-Le", "${feature.key}: ${feature.value}")
                } else {
                    LogController.log(Log.DEBUG, "Bluetooth-Le", "${feature.key}: ${feature.value}")
                }
            }
        }
    }
}