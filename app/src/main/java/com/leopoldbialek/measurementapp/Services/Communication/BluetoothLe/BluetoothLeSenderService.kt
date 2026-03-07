package com.leopoldbialek.measurementapp.Services.Communication.BluetoothLe

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.AdvertisingSetParameters
import android.bluetooth.le.AdvertisingSetParameters.TX_POWER_MEDIUM
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.leopoldbialek.measurementapp.DataClasses.BatteryTest.MeasurementBatteryTest
import com.leopoldbialek.measurementapp.DataClasses.RangeTest.TransmissionPaketRangeTest
import com.leopoldbialek.measurementapp.Enums.BluetoothLeSenderPhy
import com.leopoldbialek.measurementapp.Enums.Role
import com.leopoldbialek.measurementapp.Enums.Technology
import com.leopoldbialek.measurementapp.Enums.TestType
import com.leopoldbialek.measurementapp.Services.Location.LocationService
import com.leopoldbialek.measurementapp.Utils.LogController
import com.leopoldbialek.measurementapp.Utils.PersistenceController
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import kotlin.random.Random

class BluetoothLeSenderService() : BluetoothLeService() {
    private lateinit var bleAdvertiser: BluetoothLeAdvertiser
    private lateinit var advertisingData: AdvertiseData
    private lateinit var currentAdvertisingSet: AdvertisingSet
    private lateinit var advertisingSetParameters: AdvertisingSetParameters

    @SuppressLint("MissingPermission")
    private val advertiseDataUpdater: Thread = Thread {
        while (!Thread.currentThread().isInterrupted) {
            setAdvertisingData()

            currentAdvertisingSet.setAdvertisingData(advertisingData)

            sendBroadcast(
                Intent().setAction("MEASUREMENT_BROADCAST").putExtra("activeDataTransmission", true)
            )

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

    private fun setAdvertisingData(){
        if (testType == TestType.RANGE) {
            advertisingData = AdvertiseData
                .Builder()
                .addServiceUuid(serviceUUID)
                .setIncludeDeviceName(true)
                .addServiceData(
                    serviceUUID,
                    Json.encodeToString(
                        TransmissionPaketRangeTest(
                            LocationService.currentCoordinates.toString(),
                            LocalDateTime.now().toString(),
                            "Prim PHY Config: ${advertisingSetParameters.primaryPhy}; Sec PHY Config: ${advertisingSetParameters.secondaryPhy}"
                        )
                    ).encodeToByteArray()
                ).build()
        } else {
            advertisingData = AdvertiseData
                .Builder()
                .addServiceUuid(serviceUUID)
                .setIncludeDeviceName(true)
                .addServiceData(
                    serviceUUID,
                    Random.nextBytes(255)
                ).build()
        }
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val statusSuperOnStartCommand: Int = super.onStartCommand(intent, flags, startId)

        persistenceController = PersistenceController(
            Technology.BLUETOOTH_LE.name,
            intent!!.getStringExtra("TestType")!!,
            Role.SENDER.toString()
        )

        if (intent != null && intent.getStringExtra("TestType") == "Distance Test") {
            applicationContext.startService(Intent(applicationContext, LocationService::class.java))

            testType = TestType.RANGE
        } else {
            testType = TestType.BATTERY

            registerReceiver(
                object : BroadcastReceiver() {
                    override fun onReceive(
                        context: Context?,
                        intent: Intent?
                    ) {
                        val measurementBatteryTest: MeasurementBatteryTest = MeasurementBatteryTest(
                            intent!!.getIntExtra(BatteryManager.EXTRA_LEVEL, 101).toString(),
                            LocalDateTime.now().toString(),
                            "Prim PHY Config: ${advertisingSetParameters.primaryPhy}; Sec PHY Config: ${advertisingSetParameters.secondaryPhy}"
                        )

                        measurements.add(measurementBatteryTest)
                    }
                }, IntentFilter(Intent.ACTION_BATTERY_CHANGED), RECEIVER_EXPORTED)
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

        setAdvertisingData()

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

        if (testType == TestType.RANGE) {
            applicationContext.stopService(Intent(applicationContext, LocationService::class.java))
        }
        else{
            persistenceController.persistData(measurements.toString())
        }
    }
}