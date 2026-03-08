package com.leopoldbialek.measurementapp.Services.Communication.BluetoothLe

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.bluetooth.le.ScanSettings
import android.bluetooth.le.ScanSettings.PHY_LE_ALL_SUPPORTED
import android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresPermission
import com.leopoldbialek.measurementapp.DataClasses.BatteryTest.MeasurementBatteryTest
import com.leopoldbialek.measurementapp.DataClasses.RangeTest.MeasurementRangeTest
import com.leopoldbialek.measurementapp.DataClasses.RangeTest.TransmissionPacketRangeTest
import com.leopoldbialek.measurementapp.Enums.BluetoothLeReceiverPhy
import com.leopoldbialek.measurementapp.Enums.Role
import com.leopoldbialek.measurementapp.Enums.Technology
import com.leopoldbialek.measurementapp.Enums.TestType
import com.leopoldbialek.measurementapp.Services.Location.LocationService
import com.leopoldbialek.measurementapp.Utils.LogController
import com.leopoldbialek.measurementapp.Utils.PersistenceController
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.uuid.ExperimentalUuidApi

class BluetoothLeReceiverService : BluetoothLeService() {
    private lateinit var bleScanner: BluetoothLeScanner
    private lateinit var bleReceiverPhy: BluetoothLeReceiverPhy
    private lateinit var scanSettings: ScanSettings

    @SuppressLint("MissingPermission")
    private val scanningThread: Thread = Thread {
        bleScanner.startScan(null, scanSettings, bleScanCallback)
        LogController.log(Log.INFO, "Bluetooth-LE-Receiver", "Started Scanning!")

        while (!Thread.currentThread().isInterrupted) {
        }

        bleScanner.stopScan(bleScanCallback)
        LogController.log(Log.INFO, "Bluetooth-LE-Receiver", "Stopped Scanning!")
    }

    @OptIn(ExperimentalUuidApi::class)
    private val bleScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)

            if (result != null) {
                if (!result.isLegacy && result.scanRecord?.serviceData?.containsKey(serviceUUID) == true) {
//                    val timestampMillisTransmissionPacketReceived: Long =
//                        System.currentTimeMillis() - SystemClock.elapsedRealtime() + (result.timestampNanos * 10.0.pow(
//                            -6
//                        )).roundToLong()

                    if (testType == TestType.RANGE) {
                        val timestamp: LocalDateTime = if (trustedTimeClient == null) {
                            LocalDateTime.now()
                        } else {
                            LocalDateTime.ofInstant(
                                trustedTimeClient!!.computeCurrentInstant(),
                                ZoneOffset.systemDefault()
                            )
                        }

                        val receivedTransmissionPacket: TransmissionPacketRangeTest =
                            Json.decodeFromString<TransmissionPacketRangeTest>(
                                result.scanRecord?.serviceData?.get(serviceUUID)?.decodeToString()
                                    .toString()
                            )

                        val measurementRangeTest: MeasurementRangeTest = MeasurementRangeTest(
                            receivedTransmissionPacket.packetUuid,
                            receivedTransmissionPacket.coordinatesSender,
                            receivedTransmissionPacket.timestampSending,
                            receivedTransmissionPacket.additionalInfoSender,
                            LocationService.currentCoordinates.toString(),
                            timestamp.toString(),
                            "PHY: ${bleReceiverPhy.name}; RSSI: ${result.rssi}"
                        )

                        measurements.add(measurementRangeTest)

                        LogController.log(
                            Log.INFO,
                            "Bluetooth-LE-Receiver",
                            "New Measurement: ${measurements.last()}"
                        )
                    }

                    sendBroadcast(
                        Intent().setAction("MEASUREMENT_BROADCAST")
                            .putExtra("activeDataTransmission", true)
                    )
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        bleScanner = blAdapter.bluetoothLeScanner
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val statusSuperOnStartCommand: Int = super.onStartCommand(intent, flags, startId)

        persistenceController = PersistenceController(
            Technology.BLUETOOTH_LE.name,
            intent!!.getStringExtra("TestType")!!,
            Role.RECEIVER.name
        )

        val phy: Int =
            if (intent.getStringExtra("ReceiverPhy") == BluetoothLeReceiverPhy.PHY_1M.name) {
                bleReceiverPhy = BluetoothLeReceiverPhy.PHY_1M
                BluetoothDevice.PHY_LE_1M
            } else if (intent.getStringExtra("ReceiverPhy") == BluetoothLeReceiverPhy.PHY_LE_CODED.name) {
                bleReceiverPhy = BluetoothLeReceiverPhy.PHY_LE_CODED
                BluetoothDevice.PHY_LE_CODED
            } else {
                bleReceiverPhy = BluetoothLeReceiverPhy.PHY_ALL_SUPPORTED
                PHY_LE_ALL_SUPPORTED
            }

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

                    val measurementBatteryTest: MeasurementBatteryTest = MeasurementBatteryTest(
                        intent!!.getIntExtra(BatteryManager.EXTRA_LEVEL, 101).toString(),
                        timestamp.toString(),
                        "PHY Config: ${bleReceiverPhy.name}"
                    )

                    measurements.add(measurementBatteryTest)
                }
            }

            registerReceiver(
                batteryChangeBroadcastReceiver,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                RECEIVER_EXPORTED
            )
        }

        scanSettings = ScanSettings.Builder()
            .setLegacy(false)
            .setPhy(phy)
            .setScanMode(SCAN_MODE_LOW_LATENCY)
            .build()

        scanningThread.start()

        return statusSuperOnStartCommand
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun onDestroy() {
        super.onDestroy()

        scanningThread.interrupt()

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