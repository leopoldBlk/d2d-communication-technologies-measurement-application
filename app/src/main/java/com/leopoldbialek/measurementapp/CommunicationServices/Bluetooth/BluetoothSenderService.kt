package com.leopoldbialek.measurementapp.CommunicationServices.Bluetooth

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.AdvertisingSetParameters
import android.bluetooth.le.AdvertisingSetParameters.TX_POWER_MEDIUM
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Intent
import android.os.IBinder
import android.os.ParcelUuid
import android.util.JsonWriter
import android.util.Log
import androidx.annotation.RequiresPermission
import com.leopoldbialek.measurementapp.CommunicationServices.Coordinates
import com.leopoldbialek.measurementapp.CommunicationServices.Measurement
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.json.Json
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.timer
import kotlin.coroutines.CoroutineContext

class BluetoothSenderService() : BluetoothService() {
    private lateinit var bleAdvertiser: BluetoothLeAdvertiser
    private lateinit var currentAdvertisingSet: AdvertisingSet
    private lateinit var advertisingData: AdvertiseData

    private val coroutineScope: CoroutineScope =
        CoroutineScope(Job() + Dispatchers.Default + CoroutineName("Update Advertisement Coroutine"))
    private val advertisingSetParameters: AdvertisingSetParameters =
        AdvertisingSetParameters
            .Builder()
            .setLegacyMode(false)
            .setPrimaryPhy(BluetoothDevice.PHY_LE_1M)
            .setInterval(AdvertisingSetParameters.INTERVAL_HIGH)
            .setTxPowerLevel(TX_POWER_MEDIUM)
            .build()

    private val advertisingSetCallback: AdvertisingSetCallback = object : AdvertisingSetCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
        override fun onAdvertisingSetStarted(advertisingSet: AdvertisingSet?, txPower: Int, status: Int) {
            super.onAdvertisingSetStarted(advertisingSet, txPower, status)

            if (advertisingSet != null) {
                currentAdvertisingSet = advertisingSet

                coroutineScope.launch {
                    while (true) {
                        advertisingData = AdvertiseData
                            .Builder()
                            .addServiceUuid(serviceUUID)
                            .setIncludeDeviceName(true)
                            .addServiceData(
                                serviceUUID,
                                Json.encodeToString(
                                    Measurement(
                                        UUID.randomUUID().toString(),
                                        0F,
                                        Coordinates(10.0, 15.0).toString(),
                                        LocalDateTime.now().toString()
                                    )
                                ).encodeToByteArray()
                            )
                            .build()

                        currentAdvertisingSet.setAdvertisingData(advertisingData)

                        Log.d("Bluetooth-Sender", "Updated advertising data}")

                        delay(100)
                    }
                }
            }

            if (status == ADVERTISE_SUCCESS) {
                Log.i("Bluetooth-Sender", "Advertising started successfully")
            } else {
                Log.e("Bluetooth-Sender", "Error when starting advertising")
            }
        }

        override fun onAdvertisingSetStopped(advertisingSet: AdvertisingSet?) {
            super.onAdvertisingSetStopped(advertisingSet)

            Log.i("Bluetooth-Sender", "Advertising stopped")
        }
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onCreate() {
        super.onCreate()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val status: Int = super.onStartCommand(intent, flags, startId)

        bleAdvertiser = blAdapter.bluetoothLeAdvertiser

        advertisingData = AdvertiseData
            .Builder()
            .addServiceUuid(serviceUUID)
            .setIncludeDeviceName(true)
            .addServiceData(
                serviceUUID,
                Json.encodeToString(
                    Measurement(
                        UUID.randomUUID().toString(),
                        0F,
                        Coordinates(10.0, 15.0).toString(),
                        LocalDateTime.now().toString()
                    )
                ).encodeToByteArray()
            )
            .build()

        bleAdvertiser.startAdvertisingSet(
            advertisingSetParameters,
            advertisingData,
            null,
            null,
            null,
            advertisingSetCallback
        )

        return status
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    override fun onDestroy() {
        super.onDestroy()

        bleAdvertiser.stopAdvertisingSet(advertisingSetCallback)
        coroutineScope.cancel()
    }
}