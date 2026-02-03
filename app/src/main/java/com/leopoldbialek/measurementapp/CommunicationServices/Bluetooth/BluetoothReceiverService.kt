package com.leopoldbialek.measurementapp.CommunicationServices.Bluetooth

import android.Manifest
import android.bluetooth.BluetoothDevice.PHY_LE_1M
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresPermission
import com.leopoldbialek.measurementapp.CommunicationServices.Measurement
import kotlinx.serialization.json.Json

class BluetoothReceiverService : BluetoothService() {
    private lateinit var bleScanner: BluetoothLeScanner

    //TODO: Saving measurements to persistence with own location data form location service -> coordinatesOnStart
    private val bleScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            if (result != null) {
                if (result.isLegacy() == false && result.scanRecord?.serviceUuids?.contains(serviceUUID) == true) {
                    Log.i("Bluetooth-Receiver", "Received Data from Device-Name: ${result.scanRecord?.deviceName}")
                    Log.i("Bluetooth-Receiver", "Received Data with RSSI: ${result.rssi}")

                    val measurement: Measurement =
                        Json.decodeFromString<Measurement>(
                            result.scanRecord?.serviceData?.get(serviceUUID)?.decodeToString().toString()
                        )

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
        bleScanner = blAdapter.bluetoothLeScanner

        super.onCreate()
    }

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val scanSettings: ScanSettings = ScanSettings.Builder()
            .setLegacy(false)
            .setPhy(PHY_LE_1M)
            .setScanMode(SCAN_MODE_LOW_LATENCY)
            .build()

        if (intent != null) {
            bleScanner.startScan(null, scanSettings, bleScanCallback)
            Log.i("Bluetooth-Receiver", "Started scanning")
        }

        // TODO: Is this necessary here? Maybe improve later
        return super.onStartCommand(intent, flags, startId)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun onDestroy() {
        bleScanner.stopScan(bleScanCallback)
        Log.i("Bluetooth-Receiver", "Stopped scanning")

        super.onDestroy()
    }
}