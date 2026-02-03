package com.leopoldbialek.measurementapp.CommunicationServices.Bluetooth

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.AdvertisingSetParameters
import android.bluetooth.le.AdvertisingSetParameters.TX_POWER_MEDIUM
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresPermission
import com.leopoldbialek.measurementapp.CommunicationServices.LocationService
import com.leopoldbialek.measurementapp.CommunicationServices.Measurement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.serialization.*
import kotlinx.serialization.json.Json

class BluetoothSenderService() : BluetoothService() {
    private lateinit var bleAdvertiser: BluetoothLeAdvertiser
    private lateinit var advertisingData: AdvertiseData
    private lateinit var currentAdvertisingSet: AdvertisingSet

    private val advertisingSetParameters: AdvertisingSetParameters =
        AdvertisingSetParameters
            .Builder()
            .setLegacyMode(false)
            .setPrimaryPhy(BluetoothDevice.PHY_LE_1M)
            .setInterval(AdvertisingSetParameters.INTERVAL_HIGH)
            .setTxPowerLevel(TX_POWER_MEDIUM)
            .build()
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)

    private val advertisingSetCallback: AdvertisingSetCallback = object : AdvertisingSetCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
        override fun onAdvertisingSetStarted(advertisingSet: AdvertisingSet?, txPower: Int, status: Int) {
            if (advertisingSet != null) {
                currentAdvertisingSet = advertisingSet

                coroutineScope.launch {
                    while (true) {
                        advertisingData = AdvertiseData
                            .Builder()
                            .addServiceUuid(serviceUUID)
                            .setIncludeDeviceName(true)
                            .addServiceData(
                                serviceUUID,
                                Json.encodeToString(
                                    Measurement(
                                        UUID.randomUUID().toString(),
                                        0F,
                                        LocationService.currentCoordinates.toString(),
                                        LocalDateTime.now().toString()
                                    )
                                ).encodeToByteArray()
                            )
                            .build()

                        currentAdvertisingSet.setAdvertisingData(advertisingData)

                        Log.d("Bluetooth-Sender", "Updated advertising data")

                        delay(100)
                    }
                }
            }

            if (status == ADVERTISE_SUCCESS) {
                Log.i("Bluetooth-Sender", "Advertising started successfully")
            } else {
                Log.e("Bluetooth-Sender", "Error when starting advertising")
            }

            super.onAdvertisingSetStarted(advertisingSet, txPower, status)
        }

        override fun onAdvertisingSetStopped(advertisingSet: AdvertisingSet?) {
            Log.i("Bluetooth-Sender", "Advertising stopped")

            super.onAdvertisingSetStopped(advertisingSet)
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
        bleAdvertiser = blAdapter.bluetoothLeAdvertiser

        advertisingData = AdvertiseData
            .Builder()
            .addServiceUuid(serviceUUID)
            .setIncludeDeviceName(true)
            .addServiceData(
                serviceUUID,
                Json.encodeToString(
                    Measurement(
                        UUID.randomUUID().toString(),
                        0F,
                        LocationService.currentCoordinates.toString(),
                        LocalDateTime.now().toString()
                    )
                ).encodeToByteArray()
            )
            .build()

        bleAdvertiser.startAdvertisingSet(
            advertisingSetParameters,
            advertisingData,
            null,
            null,
            null,
            advertisingSetCallback
        )

        return super.onStartCommand(intent, flags, startId)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    override fun onDestroy() {
        super.onDestroy()

        bleAdvertiser.stopAdvertisingSet(advertisingSetCallback)
        coroutineScope.cancel()
    }
}