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
import com.leopoldbialek.measurementapp.DataClasses.Coordinates
import com.leopoldbialek.measurementapp.DataClasses.RangeTest.MeasurementRangeTestSender
import com.leopoldbialek.measurementapp.Enums.BluetoothLeSenderPrimaryPhy
import com.leopoldbialek.measurementapp.Enums.BluetoothLeSenderSecondaryPhy
import com.leopoldbialek.measurementapp.Enums.Role
import com.leopoldbialek.measurementapp.Enums.Technology
import com.leopoldbialek.measurementapp.Enums.TestType
import com.leopoldbialek.measurementapp.Services.Location.LocationService
import com.leopoldbialek.measurementapp.Utils.LogController
import com.leopoldbialek.measurementapp.Utils.PersistenceController
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.random.Random

class BluetoothLeSenderService() : BluetoothLeService() {
    private lateinit var bleAdvertiser: BluetoothLeAdvertiser
    private lateinit var advertisingData: AdvertiseData
    private lateinit var advertisingSetParameters: AdvertisingSetParameters
    private lateinit var currentAdvertisingSet: AdvertisingSet
    private lateinit var currentAdvertisedCoordinates: Coordinates
    private lateinit var primaryPhyEnum: BluetoothLeSenderPrimaryPhy
    private lateinit var secondaryPhyEnum: BluetoothLeSenderSecondaryPhy

    @SuppressLint("MissingPermission")
    private val advertiseDataUpdater: Thread = Thread {
        while (!Thread.currentThread().isInterrupted) {
            if(currentAdvertisedCoordinates != LocationService.currentCoordinates){
                setupAdvertisingData()

                currentAdvertisingSet.setAdvertisingData(advertisingData)

                sendBroadcast(
                    Intent().setAction("MEASUREMENT_BROADCAST").putExtra("activeDataTransmission", true)
                )

                LogController.log(Log.INFO, "Bluetooth-LE-Sender", "Updated Advertising Data!")

            }

            // Adapt to restriction of Wifi Aware where the Pub-Data is only updated every second!
            try {
                Thread.sleep(1000)
            }catch (e: InterruptedException ){
                Thread.currentThread().interrupt()
            }
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

                if(testType == TestType.RANGE){
                    advertiseDataUpdater.start()
                }
            }

            if (status == ADVERTISE_SUCCESS) {
                sendBroadcast(
                    Intent().setAction("MEASUREMENT_BROADCAST").putExtra("activeDataTransmission", true)
                )

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

            sendBroadcast(
                Intent().setAction("MEASUREMENT_BROADCAST").putExtra("activeDataTransmission", false)
            )

            LogController.log(Log.INFO, "Bluetooth-LE-Sender", "Advertising Stopped!")
        }
    }

    private fun setupAdvertisingData() {
        if (testType == TestType.RANGE) {

            val packetUuid: UUID = UUID.randomUUID()

            advertisingData = AdvertiseData
                .Builder()
                .addServiceData(
                    serviceUUID,
                    packetUuid.toString().encodeToByteArray()
                ).build()

            currentAdvertisedCoordinates = LocationService.currentCoordinates

            val timestamp: LocalDateTime = if (trustedTimeClient == null) {
                LocalDateTime.now()
            } else {
                LocalDateTime.ofInstant(
                    trustedTimeClient!!.computeCurrentInstant(),
                    ZoneOffset.systemDefault()
                )
            }

            val measurementRangeTestSender: MeasurementRangeTestSender =
                MeasurementRangeTestSender(
                packetUuid.toString(),
                currentAdvertisedCoordinates.latitude.toString(),
                    currentAdvertisedCoordinates.longitude.toString(),
                timestamp.toString(),
                "1_PHY: ${primaryPhyEnum}; 2_PHY: ${secondaryPhyEnum}"
            )

            measurements.add(Json.encodeToString(measurementRangeTestSender))

            if(measurements.size >= 100){
                persistenceController.persistData(measurements.toString())
                measurements.clear()
            }

        } else {
            advertisingData = AdvertiseData
                .Builder()
                .addServiceData(
                    serviceUUID,
                    Random.nextBytes(237)
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
            Role.SENDER.name
        )

        if (intent != null && intent.getStringExtra("TestType") == TestType.RANGE.name) {
            applicationContext.startService(Intent(applicationContext, LocationService::class.java))

            testType = TestType.RANGE
        } else {
            testType = TestType.BATTERY

            batteryChangeBroadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context?,
                    intent: Intent?
                ) {
                    val timestamp: LocalDateTime = if (trustedTimeClient == null) {
                        LocalDateTime.now()
                    } else {
                        LocalDateTime.ofInstant(
                            trustedTimeClient!!.computeCurrentInstant(),
                            ZoneOffset.systemDefault()
                        )
                    }

                    val primaryPhy: BluetoothLeSenderPrimaryPhy =
                        if (advertisingSetParameters.primaryPhy == BluetoothDevice.PHY_LE_1M) {
                            BluetoothLeSenderPrimaryPhy.PHY_1M
                        } else {
                            BluetoothLeSenderPrimaryPhy.PHY_LE_CODED
                        }

                    val secondaryPhy: BluetoothLeSenderSecondaryPhy =
                        if (advertisingSetParameters.secondaryPhy == BluetoothDevice.PHY_LE_1M) {
                            BluetoothLeSenderSecondaryPhy.PHY_1M
                        } else if (advertisingSetParameters.secondaryPhy == BluetoothDevice.PHY_LE_2M) {
                            BluetoothLeSenderSecondaryPhy.PHY_2M
                        } else {
                            BluetoothLeSenderSecondaryPhy.PHY_LE_CODED
                        }

                    val measurementBatteryTest: MeasurementBatteryTest = MeasurementBatteryTest(
                        intent!!.getIntExtra(BatteryManager.EXTRA_LEVEL, 101).toString(),
                        timestamp.toString(),
                        "Prim PHY Config: ${primaryPhy}; Sec PHY Config: ${secondaryPhy}"
                    )

                    measurements.add(Json.encodeToString(measurementBatteryTest))

                    if(measurements.size >= 100){
                        persistenceController.persistData(measurements.toString())
                        measurements.clear()
                    }
                }
            }

            registerReceiver(
                batteryChangeBroadcastReceiver,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                RECEIVER_EXPORTED
            )
        }

        bleAdvertiser = blAdapter.bluetoothLeAdvertiser

        val primaryPhy: Int =
            if (intent?.getStringExtra("PrimarySenderPhy") == BluetoothLeSenderPrimaryPhy.PHY_1M.name) {
                primaryPhyEnum = BluetoothLeSenderPrimaryPhy.PHY_1M
                BluetoothDevice.PHY_LE_1M
            } else {
                primaryPhyEnum = BluetoothLeSenderPrimaryPhy.PHY_LE_CODED
                BluetoothDevice.PHY_LE_CODED
            }

        val secondaryPhy: Int =
            if (intent?.getStringExtra("SecondarySenderPhy") == BluetoothLeSenderSecondaryPhy.PHY_1M.name) {
                secondaryPhyEnum = BluetoothLeSenderSecondaryPhy.PHY_1M
                BluetoothDevice.PHY_LE_1M
            } else if (intent?.getStringExtra("SecondarySenderPhy") == BluetoothLeSenderSecondaryPhy.PHY_2M.name) {
                secondaryPhyEnum = BluetoothLeSenderSecondaryPhy.PHY_2M
                BluetoothDevice.PHY_LE_2M
            } else {
                secondaryPhyEnum = BluetoothLeSenderSecondaryPhy.PHY_LE_CODED
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

        setupAdvertisingData()

        //Max Advertising Data Length: 1650 Bytes
        //val maxAdvertisingDataLength: Int = blAdapter.leMaximumAdvertisingDataLength
        advertisingThread.start()

        return statusSuperOnStartCommand
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    override fun onDestroy() {
        super.onDestroy()

        if(testType == TestType.RANGE){
            advertiseDataUpdater.interrupt()
        }

        advertisingThread.interrupt()

        if (testType == TestType.RANGE) {
            applicationContext.stopService(Intent(applicationContext, LocationService::class.java))
        } else {
            unregisterReceiver(batteryChangeBroadcastReceiver)
        }

        persistenceController.persistData(measurements.toString())

        sendBroadcast(
            Intent().setAction("MEASUREMENT_BROADCAST").putExtra("activeDataTransmission", false)
        )
    }
}