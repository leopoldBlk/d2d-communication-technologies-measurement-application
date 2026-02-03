package com.leopoldbialek.measurementapp.CommunicationServices.Bluetooth

import android.Manifest
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.ParcelUuid
import androidx.annotation.RequiresPermission
import java.util.UUID

abstract class BluetoothService() : Service() {
    protected lateinit var blManager: BluetoothManager
    protected lateinit var blAdapter: BluetoothAdapter

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

        val isLe2MPhySupported: Boolean = blAdapter.isLe2MPhySupported
        val isLeCodedPhySupported: Boolean = blAdapter.isLeCodedPhySupported
        val isLeExtendedAdvertisingSupported: Boolean = blAdapter.isLeExtendedAdvertisingSupported
        val isLePeriodicAdvertisingSupported: Boolean = blAdapter.isLePeriodicAdvertisingSupported
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }
}