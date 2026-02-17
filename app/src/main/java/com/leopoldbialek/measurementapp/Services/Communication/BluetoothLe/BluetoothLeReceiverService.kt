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
import android.os.ParcelUuid
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresPermission
import com.leopoldbialek.measurementapp.DataClasses.Measurement
import com.leopoldbialek.measurementapp.DataClasses.TransmissionPaket
import com.leopoldbialek.measurementapp.Enums.BluetoothLeReceiverPhy
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

class BluetoothLeReceiverService : BluetoothLeService() {
    private lateinit var persistenceController: PersistenceController
    private lateinit var bleScanner: BluetoothLeScanner
    private lateinit var bleReceiverPhy: BluetoothLeReceiverPhy

    private val measurements: MutableList<Measurement> =
        mutableListOf<Measurement>()

    private val bleScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)

            if (result != null) {
                if (!result.isLegacy && result.scanRecord?.serviceUuids?.contains(serviceUUID) == true) {
                    val timestampMillisTransmissionPacketReceived: Long =
                        System.currentTimeMillis() - SystemClock.elapsedRealtime() + (result.timestampNanos * 10.0.pow(
                            -6
                        )).roundToLong()

                    val receivedTransmissionPacket: TransmissionPaket =
                        Json.decodeFromString<TransmissionPaket>(
                            result.scanRecord?.serviceData?.get(serviceUUID)?.decodeToString()
                                .toString()
                        )

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
                        receivedTransmissionPacket.uuidSender,
                        receivedTransmissionPacket.coordinatesSender,
                        receivedTransmissionPacket.batteryLevelSender,
                        receivedTransmissionPacket.timestampSending,
                        deviceUuid.toString(),
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
                        "PHY-Config-Receiver: ${bleReceiverPhy.name}"
                    )

                    measurements.add(measurement)

                    LogController.log(
                        Log.INFO,
                        "Bluetooth-LE-Receiver",
                        "Received Data from Device-Name: ${result.scanRecord?.deviceName}"
                    )

                    LogController.log(
                        Log.INFO,
                        "Bluetooth-LE-Receiver",
                        "New Measurement: ${measurement}"
                    )
                }
            }
        }
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onCreate() {
        super.onCreate()

        deviceUuid = ParcelUuid(UUID.fromString("fd07f220-6dba-419f-8d12-1035aed4652e"))

        bleScanner = blAdapter.bluetoothLeScanner
    }

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val statusSuperOnStartCommand: Int = super.onStartCommand(intent, flags, startId)

        persistenceController = PersistenceController(
            Technology.BLUETOOTH_LE.name,
            intent!!.getStringExtra("TestType")!!
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

        val scanSettings: ScanSettings = ScanSettings.Builder()
            .setLegacy(false)
            .setPhy(phy)
            .setScanMode(SCAN_MODE_LOW_LATENCY)
            .build()

        bleScanner.startScan(null, scanSettings, bleScanCallback)
        LogController.log(Log.INFO, "Bluetooth-LE-Receiver", "Started Scanning!")

        return statusSuperOnStartCommand
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun onDestroy() {
        super.onDestroy()

        bleScanner.stopScan(bleScanCallback)

        persistenceController.persistData(measurements.toString())

        LogController.log(Log.INFO, "Bluetooth-LE-Receiver", "Stopped Scanning!")
    }
}