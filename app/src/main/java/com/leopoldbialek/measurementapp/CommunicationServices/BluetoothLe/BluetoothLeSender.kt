package com.leopoldbialek.measurementapp.CommunicationServices.BluetoothLe

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
import com.leopoldbialek.measurementapp.Location.LocationService
import com.leopoldbialek.measurementapp.TransmissionData.TransmissionPaket
import com.leopoldbialek.measurementapp.MainActivity
import com.leopoldbialek.measurementapp.Misc.LogController
import com.leopoldbialek.measurementapp.Misc.TestType
import java.time.LocalDateTime
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.TimeZone
import java.util.concurrent.atomic.AtomicBoolean

class BluetoothLeSender() : BluetoothLeService() {
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
                            coordinatesSender = if (testType == TestType.DistanceTest) {
                                LocationService.currentCoordinates.toString()
                            } else {
                                ""
                            },
                            batteryManager.getIntProperty(BATTERY_PROPERTY_CAPACITY)
                                .toString(),
                            LocalDateTime.ofInstant(
                                Instant.ofEpochMilli(System.currentTimeMillis()),
                                TimeZone.getDefault().toZoneId()
                            ).toString()
                        )
                    ).encodeToByteArray()
                )
                .build()

            currentAdvertisingSet.setAdvertisingData(advertisingData)

            LogController.log(Log.DEBUG,"Bluetooth-Sender", "Updated advertising data!")
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

                advertiseDataUpdater.start()
            }

            if (status == ADVERTISE_SUCCESS) {
                LogController.log(Log.INFO,"Bluetooth-LE-Sender", "Advertising started successfully!")
            } else {
                LogController.log(Log.INFO,"Bluetooth-LE-Sender", "Error when starting advertising!")
            }

            super.onAdvertisingSetStarted(advertisingSet, txPower, status)
        }

        override fun onAdvertisingSetStopped(advertisingSet: AdvertisingSet?) {
            LogController.log(Log.INFO,"Bluetooth-LE-Sender", "Advertising stopped!")

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
        if (intent != null && intent.getStringExtra("TestType") == "Distance Test") {
            applicationContext.startService(Intent(applicationContext, LocationService::class.java))

            testType = TestType.DistanceTest
        }
        else{
            testType = TestType.BatteryTest
        }

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
                        coordinatesSender = if (testType == TestType.DistanceTest) {
                            LocationService.currentCoordinates.toString()
                        } else {
                            ""
                        },
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

        if (testType == TestType.DistanceTest) {
            applicationContext.stopService(Intent(applicationContext, LocationService::class.java))
        }
    }
}