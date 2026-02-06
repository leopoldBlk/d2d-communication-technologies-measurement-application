package com.leopoldbialek.measurementapp.CommunicationServices.Bluetooth

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.PHY_LE_1M
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY
import android.content.Intent
import android.os.BatteryManager
import android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresPermission
import com.leopoldbialek.measurementapp.CommunicationServices.LocationService
import com.leopoldbialek.measurementapp.CommunicationServices.Measurement
import com.leopoldbialek.measurementapp.CommunicationServices.PersistenceService
import com.leopoldbialek.measurementapp.CommunicationServices.TransmissionPaket
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.Instant
import kotlin.math.pow
import kotlin.math.roundToLong

class BluetoothReceiverService : BluetoothService() {
    private lateinit var bleScanner: BluetoothLeScanner

    private val receivedTransmissionPackets: MutableList<Measurement> =
        mutableListOf<Measurement>()

    private val bleScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            if (result != null) {
                if (result.isLegacy() == false && result.scanRecord?.serviceUuids?.contains(
                        serviceUUID
                    ) == true
                ) {
                    Log.i(
                        "Bluetooth-Receiver",
                        "Received Data from Device-Name: ${result.scanRecord?.deviceName}"
                    )
                    Log.i("Bluetooth-Receiver", "Received Data with RSSI: ${result.rssi}")

                    val receivedTransmissionPacket: TransmissionPaket =
                        Json.decodeFromString<TransmissionPaket>(
                            result.scanRecord?.serviceData?.get(serviceUUID)?.decodeToString()
                                .toString()
                        )

                    val timestampMillisTransmissionPacketReceived: Long =
                        System.currentTimeMillis() - SystemClock.elapsedRealtime() + (result.timestampNanos * 10.0.pow(-6)).roundToLong()

                    //TODO: Why is timestamp for sending after the one for receiving?
                    val measurement: Measurement = Measurement(
                        receivedTransmissionPacket.coordinatesSender,
                        receivedTransmissionPacket.batteryLevelSender,
                        receivedTransmissionPacket.timestampSending,
                        LocationService.currentCoordinates.toString(),
                        batteryManager.getIntProperty(BATTERY_PROPERTY_CAPACITY).toString(),
                        LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(timestampMillisTransmissionPacketReceived),
                            java.util.TimeZone.getDefault().toZoneId()
                        ).toString(),
                        result.rssi.toString()
                    )

                    receivedTransmissionPackets.add(measurement)

                    Log.i("Bluetooth-Receiver", "Received Data: ${measurement}")
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

        applicationContext.startService(Intent(applicationContext, PersistenceService::class.java))
    }

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val phy: Int = if (intent?.getStringExtra("Technology") == "Bluetooth LE (1M PHY)") {
            BluetoothDevice.PHY_LE_1M
        }
        else if (intent?.getStringExtra("Technology") == "Bluetooth LE (2M PHY)"){
            BluetoothDevice.PHY_LE_2M
        }
        else{
            BluetoothDevice.PHY_LE_CODED
        }

        val scanSettings: ScanSettings = ScanSettings.Builder()
            .setLegacy(false)
            .setPhy(phy)
            .setScanMode(SCAN_MODE_LOW_LATENCY)
            .build()

        if (intent != null) {
            bleScanner.startScan(null, scanSettings, bleScanCallback)
            Log.i("Bluetooth-Receiver", "Started scanning")
        }

        return super.onStartCommand(intent, flags, startId)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun onDestroy() {
        bleScanner.stopScan(bleScanCallback)

        PersistenceService.Companion.persistData(receivedTransmissionPackets.toString())

        Log.i("Bluetooth-Receiver", "Stopped scanning")

        applicationContext.stopService(Intent(applicationContext, PersistenceService::class.java))

        super.onDestroy()
    }
}