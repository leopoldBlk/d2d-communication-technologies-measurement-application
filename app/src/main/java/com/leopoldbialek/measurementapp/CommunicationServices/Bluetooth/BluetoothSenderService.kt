package com.leopoldbialek.measurementapp.CommunicationServices.Bluetooth

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.AdvertisingSetParameters
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Intent
import android.os.IBinder
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import java.util.UUID

class BluetoothSenderService : BluetoothService() {
    private lateinit var bleAdvertiser: BluetoothLeAdvertiser

    private val advertisingSetParameters: AdvertisingSetParameters =
        AdvertisingSetParameters
            .Builder()
            .setPrimaryPhy(BluetoothDevice.PHY_LE_CODED)
            .setSecondaryPhy(BluetoothDevice.PHY_LE_CODED)
            .setInterval(AdvertisingSetParameters.INTERVAL_HIGH)
            .build()

    private val advertisingData: AdvertiseData =
        AdvertiseData
            .Builder()
            .addServiceUuid(serviceUUID)
            .build()

    private val advertisingSetCallback: AdvertisingSetCallback = object : AdvertisingSetCallback() {
        override fun onAdvertisingSetStarted(advertisingSet: AdvertisingSet?, txPower: Int, status: Int) {
            super.onAdvertisingSetStarted(advertisingSet, txPower, status)

            if (status == ADVERTISE_SUCCESS) {
                Log.i("Bluetooth-Sender", "Advertising started successfully")
            } else {
                Log.e("Bluetooth-Sender", "Error when starting advertising")
            }
        }

        override fun onAdvertisingSetStopped(advertisingSet: AdvertisingSet?) {
            super.onAdvertisingSetStopped(advertisingSet)

            Log.i("Bluetooth-Sender", "Advertising stopped")
        }
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onCreate() {
        super.onCreate()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        bleAdvertiser = blAdapter.bluetoothLeAdvertiser

        if (intent != null) {
            bleAdvertiser.startAdvertisingSet(
                advertisingSetParameters,
                advertisingData,
                null,
                null,
                null,
                advertisingSetCallback
            )
        }

        // TODO: Is this necessary here? Maybe improve later
        return START_STICKY
    }

    override fun onDestroy() {
        bleAdvertiser.stopAdvertisingSet(advertisingSetCallback)
        super.onDestroy()
    }
}