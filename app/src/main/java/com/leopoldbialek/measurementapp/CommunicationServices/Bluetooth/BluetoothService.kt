package com.leopoldbialek.measurementapp.CommunicationServices.Bluetooth

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.ParcelUuid
import java.util.UUID

abstract class BluetoothService : Service() {
    protected lateinit var blManager: BluetoothManager
    protected lateinit var blAdapter: BluetoothAdapter

    protected val serviceUUID: ParcelUuid =
        ParcelUuid
            .fromString(
                UUID.fromString("11111111-2222-3333-4444-555555555555")
                    .toString()
            )

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        blManager = applicationContext.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        blAdapter = blManager.adapter

        return super.onStartCommand(intent, flags, startId)
    }
}