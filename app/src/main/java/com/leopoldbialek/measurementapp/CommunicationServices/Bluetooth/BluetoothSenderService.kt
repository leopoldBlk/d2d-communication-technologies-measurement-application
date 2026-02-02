package com.leopoldbialek.measurementapp.CommunicationServices.Bluetooth

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.AdvertisingSetParameters
import android.bluetooth.le.AdvertisingSetParameters.TX_POWER_MEDIUM
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Intent
import android.os.IBinder
import android.os.ParcelUuid
import android.util.JsonWriter
import android.util.Log
import androidx.annotation.RequiresPermission
import com.leopoldbialek.measurementapp.CommunicationServices.Coordinates
import com.leopoldbialek.measurementapp.CommunicationServices.Measurement
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.serialization.*

class BluetoothSenderService : BluetoothService() {
    private lateinit var bleAdvertiser: BluetoothLeAdvertiser

    private val advertisingSetParameters: AdvertisingSetParameters =
        AdvertisingSetParameters
            .Builder()
            .setLegacyMode(false)
            .setPrimaryPhy(BluetoothDevice.PHY_LE_1M)
            .setInterval(AdvertisingSetParameters.INTERVAL_HIGH)
            .setTxPowerLevel(TX_POWER_MEDIUM)
            .build()

    private val advertisingData: AdvertiseData =
        AdvertiseData
            .Builder()
            .addServiceUuid(serviceUUID)
            .setIncludeDeviceName(true)
            .build()

    private val advertisingSetCallback: AdvertisingSetCallback = object : AdvertisingSetCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
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

        override fun onAdvertisingDataSet(advertisingSet: AdvertisingSet?, status: Int) {
            super.onAdvertisingDataSet(advertisingSet, status)
        }
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onCreate() {
        super.onCreate()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val status: Int = super.onStartCommand(intent, flags, startId)

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
        return status
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    override fun onDestroy() {
        bleAdvertiser.stopAdvertisingSet(advertisingSetCallback)
        super.onDestroy()
    }
}