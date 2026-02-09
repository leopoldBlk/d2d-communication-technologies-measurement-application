package com.leopoldbialek.measurementapp.CommunicationServices.Bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.AdvertisingSetParameters
import android.bluetooth.le.AdvertisingSetParameters.TX_POWER_MEDIUM
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Intent
import android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresPermission
import com.leopoldbialek.measurementapp.CommunicationServices.LocationService
import com.leopoldbialek.measurementapp.CommunicationServices.TransmissionPaket
import com.leopoldbialek.measurementapp.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.time.LocalDateTime
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

class BluetoothSenderService() : BluetoothService() {
    private lateinit var bleAdvertiser: BluetoothLeAdvertiser
    private lateinit var advertisingData: AdvertiseData
    private lateinit var currentAdvertisingSet: AdvertisingSet
    private lateinit var advertisingSetParameters: AdvertisingSetParameters
    private val isAdvertising: AtomicBoolean = AtomicBoolean(false)

    @SuppressLint("MissingPermission")
    private val advertiseDataUpdater: Thread = Thread {
        while (isAdvertising.get()) {
            advertisingData = AdvertiseData
                .Builder()
                .addServiceUuid(serviceUUID)
                .setIncludeDeviceName(true)
                .addServiceData(
                    serviceUUID,
                    Json.encodeToString(
                        TransmissionPaket(
                            MainActivity.deviceUUID.toString(),
                            LocationService.currentCoordinates.toString(),
                            batteryManager.getIntProperty(BATTERY_PROPERTY_CAPACITY)
                                .toString(),
                            LocalDateTime.ofInstant(
                                Instant.ofEpochMilli(System.currentTimeMillis()),
                                java.util.TimeZone.getDefault().toZoneId()
                            ).toString()
                        )
                    ).encodeToByteArray()
                )
                .build()

            currentAdvertisingSet.setAdvertisingData(advertisingData)

            Log.d("Bluetooth-Sender", "Updated advertising data")
        }
    }

    private val advertisingSetCallback: AdvertisingSetCallback = object : AdvertisingSetCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
        override fun onAdvertisingSetStarted(
            advertisingSet: AdvertisingSet?,
            txPower: Int,
            status: Int
        ) {
            if (advertisingSet != null) {
                currentAdvertisingSet = advertisingSet

                isAdvertising.set(true)

                /*
                Previously used coroutine for updating timestamp -> now switched to threads

               coroutineScope.launch {
                    while (true) {
                        advertisingData = AdvertiseData
                            .Builder()
                            .addServiceUuid(serviceUUID)
                            .setIncludeDeviceName(true)
                            .addServiceData(
                                serviceUUID,
                                Json.encodeToString(
                                    TransmissionPaket(
                                        MainActivity.deviceUUID.toString(),
                                        LocationService.currentCoordinates.toString(),
                                        batteryManager.getIntProperty(BATTERY_PROPERTY_CAPACITY)
                                            .toString(),
                                        LocalDateTime.ofInstant(
                                            Instant.ofEpochMilli(System.currentTimeMillis()),
                                            java.util.TimeZone.getDefault().toZoneId()
                                        ).toString()
                                        //LocalDateTime.now().toString()
                                    )
                                ).encodeToByteArray()
                            )
                            .build()

                        currentAdvertisingSet.setAdvertisingData(advertisingData)

                        Log.d("Bluetooth-Sender", "Updated advertising data")

                        delay(1)
                    }
                }
                */
                advertiseDataUpdater.start()
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

        val primaryPhy: Int =
            if (intent?.getStringExtra("PrimaryPhyConfig") == "1M") {
                BluetoothDevice.PHY_LE_1M
            } else {
                BluetoothDevice.PHY_LE_CODED
            }

        val secondaryPhy: Int =
            if (intent?.getStringExtra("SecondaryPhyConfig") == "1M") {
                BluetoothDevice.PHY_LE_1M
            } else {
                BluetoothDevice.PHY_LE_CODED
            }

        advertisingSetParameters =
            AdvertisingSetParameters
                .Builder()
                .setLegacyMode(false)
                .setPrimaryPhy(primaryPhy)
                .setSecondaryPhy(secondaryPhy)
                .setInterval(AdvertisingSetParameters.INTERVAL_HIGH)
                .setTxPowerLevel(TX_POWER_MEDIUM)
                .build()

        advertisingData = AdvertiseData
            .Builder()
            .addServiceUuid(serviceUUID)
            .setIncludeDeviceName(true)
            .addServiceData(
                serviceUUID,
                Json.encodeToString(
                    TransmissionPaket(
                        MainActivity.deviceUUID.toString(),
                        LocationService.currentCoordinates.toString(),
                        batteryManager.getIntProperty(BATTERY_PROPERTY_CAPACITY).toString(),
                        LocalDateTime.now().toString()
                    )
                ).encodeToByteArray()
            )
            .build()

        //Max Advertising Data Length: 1650 Bytes
        val maxAdvertisingDataLength: Int = blAdapter.leMaximumAdvertisingDataLength

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

        isAdvertising.set(false)

        bleAdvertiser.stopAdvertisingSet(advertisingSetCallback)

        // Previously used coroutine for updating timestamp -> now switched to threads
        //coroutineScope.cancel()
    }
}