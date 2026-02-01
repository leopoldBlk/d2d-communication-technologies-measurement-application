package com.leopoldbialek.measurementapp.CommunicationServices.Bluetooth

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresPermission

class BluetoothReceiverService : BluetoothService() {
    private lateinit var bleScanner: BluetoothLeScanner

    private val bleScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            if (result != null) {
                if (result.scanRecord?.serviceUuids?.contains(serviceUUID) == true){
                    Log.i("Bluetooth-Receiver", "Received Data from Device-Address: ${result.device}")
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
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        bleScanner = blAdapter.bluetoothLeScanner

        if (intent != null) {
            bleScanner.startScan(bleScanCallback)
            Log.i("Bluetooth-Receiver", "Started scanning")
        }

        // TODO: Is this necessary here? Maybe improve later
        return START_STICKY
    }

    override fun onDestroy() {
        bleScanner.stopScan(bleScanCallback)
        Log.i("Bluetooth-Receiver", "Stopped scanning")
        super.onDestroy()
    }
}