package com.leopoldbialek.measurementapp.Services.Communication.BluetoothLe

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
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.leopoldbialek.measurementapp.DataClasses.TransmissionPaket
import com.leopoldbialek.measurementapp.Enums.BluetoothLeSenderPhy
import com.leopoldbialek.measurementapp.Enums.TestType
import com.leopoldbialek.measurementapp.Services.Location.LocationService
import com.leopoldbialek.measurementapp.Utils.LogController
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDateTime
import java.util.TimeZone
import java.util.UUID

class BluetoothLeSenderService() : BluetoothLeService() {
    private lateinit var bleAdvertiser: BluetoothLeAdvertiser
    private lateinit var advertisingData: AdvertiseData
    private lateinit var currentAdvertisingSet: AdvertisingSet
    private lateinit var advertisingSetParameters: AdvertisingSetParameters

    @SuppressLint("MissingPermission")
    private val advertiseDataUpdater: Thread = Thread {
        while (!Thread.currentThread().isInterrupted) {
            advertisingData = AdvertiseData
                .Builder()
                .addServiceUuid(serviceUUID)
                .setIncludeDeviceName(true)
                .addServiceData(
                    serviceUUID,
                    Json.encodeToString(
                        TransmissionPaket(
                            deviceUuid.toString(),
                            coordinatesSender = if (testType == TestType.DISTANCE) {
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

            sendBroadcast(Intent().setAction("MEASUREMENT_BROADCAST").putExtra("activeDataTransmission", true))
            currentAdvertisingSet.setAdvertisingData(advertisingData)


            LogController.log(Log.INFO, "Bluetooth-Sender", "Updated Advertising Data!")
        }
    }

    @SuppressLint("MissingPermission")
    private val advertisingThread: Thread = Thread {
        bleAdvertiser.startAdvertisingSet(
            advertisingSetParameters,
            advertisingData,
            null,
            null,
            null,
            advertisingSetCallback
        )
        while (!Thread.currentThread().isInterrupted) {
        }

        bleAdvertiser.stopAdvertisingSet(advertisingSetCallback)
    }

    private val advertisingSetCallback: AdvertisingSetCallback = object : AdvertisingSetCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
        override fun onAdvertisingSetStarted(
            advertisingSet: AdvertisingSet?,
            txPower: Int,
            status: Int
        ) {
            super.onAdvertisingSetStarted(advertisingSet, txPower, status)

            if (advertisingSet != null) {
                currentAdvertisingSet = advertisingSet

                advertiseDataUpdater.start()
            }

            if (status == ADVERTISE_SUCCESS) {
                LogController.log(
                    Log.INFO,
                    "Bluetooth-LE-Sender",
                    "Advertising Started Successfully!"
                )
            } else {
                LogController.log(
                    Log.ERROR,
                    "Bluetooth-LE-Sender",
                    "Error when Starting Advertising!"
                )
            }
        }

        override fun onAdvertisingSetStopped(advertisingSet: AdvertisingSet?) {
            super.onAdvertisingSetStopped(advertisingSet)

            LogController.log(Log.INFO, "Bluetooth-LE-Sender", "Advertising Stopped!")
        }
    }

    override fun onCreate() {
        super.onCreate()

        deviceUuid = ParcelUuid(UUID.fromString("23814546-50c0-4270-8963-427410ce44fb"))
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val statusSuperOnStartCommand: Int = super.onStartCommand(intent, flags, startId)

        if (intent != null && intent.getStringExtra("TestType") == "Distance Test") {
            applicationContext.startService(Intent(applicationContext, LocationService::class.java))

            testType = TestType.DISTANCE
        } else {
            testType = TestType.BATTERY
        }

        bleAdvertiser = blAdapter.bluetoothLeAdvertiser

        val primaryPhy: Int =
            if (intent?.getStringExtra("PrimarySenderPhy") == BluetoothLeSenderPhy.PHY_1M.name) {
                BluetoothDevice.PHY_LE_1M
            } else {
                BluetoothDevice.PHY_LE_CODED
            }

        val secondaryPhy: Int =
            if (intent?.getStringExtra("SecondarySenderPhy") == BluetoothLeSenderPhy.PHY_1M.name) {
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
            .addServiceData(
                serviceUUID,
                Json.encodeToString(
                    TransmissionPaket(
                        deviceUuid.toString(),
                        coordinatesSender = if (testType == TestType.DISTANCE) {
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
        //val maxAdvertisingDataLength: Int = blAdapter.leMaximumAdvertisingDataLength
        advertisingThread.start()

        return statusSuperOnStartCommand
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    override fun onDestroy() {
        super.onDestroy()

        advertiseDataUpdater.interrupt()
        advertisingThread.interrupt()

        if (testType == TestType.DISTANCE) {
            applicationContext.stopService(Intent(applicationContext, LocationService::class.java))
        }
    }
}