package com.leopoldbialek.measurementapp.Services.Communication.BluetoothLe

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.os.BatteryManager
import android.os.ParcelUuid
import com.leopoldbialek.measurementapp.Enums.TestType
import java.util.UUID

abstract class BluetoothLeService() : Service() {
    protected lateinit var blManager: BluetoothManager
    protected lateinit var blAdapter: BluetoothAdapter
    protected lateinit var batteryManager: BatteryManager
    protected lateinit var testType: TestType

    protected val serviceUUID: ParcelUuid =
        ParcelUuid
            .fromString(
                UUID.fromString("11111111-2222-3333-4444-555555555555")
                    .toString()
            )

    override fun onCreate() {
        super.onCreate()

        blManager = applicationContext.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        blAdapter = blManager.adapter

        batteryManager = applicationContext.getSystemService(BATTERY_SERVICE) as BatteryManager

        val isLe2MPhySupported: Boolean = blAdapter.isLe2MPhySupported
        val isLeCodedPhySupported: Boolean = blAdapter.isLeCodedPhySupported
        val isLeExtendedAdvertisingSupported: Boolean = blAdapter.isLeExtendedAdvertisingSupported
        val isLePeriodicAdvertisingSupported: Boolean = blAdapter.isLePeriodicAdvertisingSupported
    }
}