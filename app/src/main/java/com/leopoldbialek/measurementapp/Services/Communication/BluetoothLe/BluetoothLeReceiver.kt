package com.leopoldbialek.measurementapp.Services.Communication.BluetoothLe

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.bluetooth.le.ScanSettings.PHY_LE_ALL_SUPPORTED
import android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY
import android.content.Intent
import android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresPermission
import com.leopoldbialek.measurementapp.DataClasses.Measurement
import com.leopoldbialek.measurementapp.DataClasses.TransmissionPaket
import com.leopoldbialek.measurementapp.Enums.BluetoothLeReceiverPhy
import com.leopoldbialek.measurementapp.Enums.Role
import com.leopoldbialek.measurementapp.Enums.Technology
import com.leopoldbialek.measurementapp.Enums.TestType
import com.leopoldbialek.measurementapp.Services.Location.LocationService
import com.leopoldbialek.measurementapp.Utils.LogController
import com.leopoldbialek.measurementapp.Utils.PersistenceController
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDateTime
import java.util.TimeZone
import java.util.UUID
import kotlin.math.pow
import kotlin.math.roundToLong

class BluetoothLeReceiver : BluetoothLeService() {
    private lateinit var bleScanner: BluetoothLeScanner

    private val measurements: MutableList<Measurement> =
        mutableListOf<Measurement>()

    private lateinit var persistenceController: PersistenceController

    private val bleScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            if (result != null) {
                if (result.isLegacy() == false && result.scanRecord?.serviceUuids?.contains(
                        serviceUUID
                    ) == true
                ) {
                    LogController.log(
                        Log.INFO,
                        "Bluetooth-LE-Receiver",
                        "Received Data from Device-Name: ${result.scanRecord?.deviceName}"
                    )
                    LogController.log(
                        Log.INFO,
                        "Bluetooth-LE-Receiver",
                        "Received Data with RSSI: ${result.rssi}"
                    )

                    val receivedTransmissionPacket: TransmissionPaket =
                        Json.decodeFromString<TransmissionPaket>(
                            result.scanRecord?.serviceData?.get(serviceUUID)?.decodeToString()
                                .toString()
                        )

                    val timestampMillisTransmissionPacketReceived: Long =
                        System.currentTimeMillis() - SystemClock.elapsedRealtime() + (result.timestampNanos * 10.0.pow(
                            -6
                        )).roundToLong()

                    //TODO: Really necessary to get primary and secondary phy? The calls .primaryPhy
                    // and .secondaryPhy only return the phys determined for scanning and not the
                    // exact phy of bot which received the data packet!
                    /*
                    val phyReceivedData: String =
                        if (result.primaryPhy == BluetoothDevice.PHY_LE_1M) {
                            "Data received by 1M PHY"
                        } else {
                            "Data received by LE Coded PHY"
                        }
                     */

                    //TODO: Why is timestamp for sending after the one for receiving?
                    val measurement: Measurement = Measurement(
                        "Bluetooth LE",
                        receivedTransmissionPacket.uuidSender,
                        receivedTransmissionPacket.coordinatesSender,
                        receivedTransmissionPacket.batteryLevelSender,
                        receivedTransmissionPacket.timestampSending,
                        UUID.randomUUID().toString(),
                        coordinatesReceiver = if (testType == TestType.DISTANCE) {
                            LocationService.currentCoordinates.toString()
                        } else {
                            ""
                        },
                        batteryManager.getIntProperty(BATTERY_PROPERTY_CAPACITY).toString(),
                        LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(timestampMillisTransmissionPacketReceived),
                            TimeZone.getDefault().toZoneId()
                        ).toString(),
                        result.rssi.toString()
                    )

                    measurements.add(measurement)

                    LogController.log(
                        Log.INFO,
                        "Bluetooth-LE-Receiver",
                        "Received Data: ${measurement}"
                    )
                }
            }

            super.onScanResult(callbackType, result)
        }
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onCreate() {
        super.onCreate()

        bleScanner = blAdapter.bluetoothLeScanner
    }

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        persistenceController = PersistenceController(
            Technology.BLUETOOTH_LE.name,
            intent!!.getStringExtra("TestType")!!,
            Role.RECEIVER.name
        )

        val phy: Int =
            if (intent?.getStringExtra("ReceiverPhy") == BluetoothLeReceiverPhy.PHY_1M.name) {
                BluetoothDevice.PHY_LE_1M
            } else if (intent?.getStringExtra("ReceiverPhy") == BluetoothLeReceiverPhy.PHY_LE_CODED.name) {
                BluetoothDevice.PHY_LE_CODED
            } else {
                PHY_LE_ALL_SUPPORTED
            }

        val scanSettings: ScanSettings = ScanSettings.Builder()
            .setLegacy(false)
            .setPhy(phy)
            .setScanMode(SCAN_MODE_LOW_LATENCY)
            .build()

        if (intent != null) {
            bleScanner.startScan(null, scanSettings, bleScanCallback)
            LogController.log(Log.INFO, "Bluetooth-LE-Receiver", "Started scanning")
        }

        return super.onStartCommand(intent, flags, startId)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun onDestroy() {
        bleScanner.stopScan(bleScanCallback)

        persistenceController.persistData(measurements.toString())

        LogController.log(Log.INFO, "Bluetooth-LE-Receiver", "Stopped scanning")

        super.onDestroy()
    }
}