package com.leopoldbialek.measurementapp.CommunicationServices.WiFi.Aware

import android.app.Service
import android.net.wifi.aware.WifiAwareManager
import android.net.wifi.aware.WifiAwareSession
import android.os.BatteryManager
import android.os.ParcelUuid
import java.util.UUID

abstract class WifiAwareService : Service() {
    protected lateinit var wifiAwareManager: WifiAwareManager
    protected lateinit var wifiAwareSession: WifiAwareSession

    protected lateinit var batteryManager: BatteryManager

    protected val serviceUUID: ParcelUuid =
        ParcelUuid
            .fromString(
                UUID.fromString("11111111-2222-3333-4444-555555555555")
                    .toString()
            )

    override fun onCreate() {
        super.onCreate()

        batteryManager = applicationContext.getSystemService(BATTERY_SERVICE) as BatteryManager
    }
}