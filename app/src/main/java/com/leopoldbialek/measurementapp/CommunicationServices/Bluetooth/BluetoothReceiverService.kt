package com.leopoldbialek.measurementapp.CommunicationServices.Bluetooth

import android.Manifest
import android.bluetooth.BluetoothDevice.PHY_LE_1M
import android.bluetooth.BluetoothDevice.PHY_LE_CODED
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.bluetooth.le.ScanSettings.PHY_LE_ALL_SUPPORTED
import android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresPermission

class BluetoothReceiverService : BluetoothService() {
    private lateinit var bleScanner: BluetoothLeScanner

    private val bleScanCallback: ScanCallback = object : ScanCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            if (result != null) {
                if (result.isLegacy() == false && result.scanRecord?.serviceUuids?.contains(serviceUUID) == true) {
                    Log.i("Bluetooth-Receiver", "Received Data from Device-Name: ${result.scanRecord?.deviceName}")
                    Log.i("Bluetooth-Receiver", "Received Data with RSSI: ${result.rssi}")
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
        val status: Int = super.onStartCommand(intent, flags, startId)

        val scanSettings: ScanSettings = ScanSettings.Builder()
            .setLegacy(false)
            .setPhy(PHY_LE_1M)
            .setScanMode(SCAN_MODE_LOW_LATENCY)
            .build()
        //val scanFilter: ScanFilter = ScanFilter.Builder().setDeviceAddress("5A:E2:87:39:D5:45").build()

        if (intent != null) {
            bleScanner.startScan(null, scanSettings, bleScanCallback)
            Log.i("Bluetooth-Receiver", "Started scanning")
        }

        // TODO: Is this necessary here? Maybe improve later
        return status
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun onDestroy() {
        bleScanner.stopScan(bleScanCallback)
        Log.i("Bluetooth-Receiver", "Stopped scanning")
        super.onDestroy()
    }
}