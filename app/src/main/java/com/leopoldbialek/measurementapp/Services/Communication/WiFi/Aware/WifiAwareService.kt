package com.leopoldbialek.measurementapp.Services.Communication.WiFi.Aware

import android.app.Service
import android.net.wifi.aware.WifiAwareManager
import android.net.wifi.aware.WifiAwareSession
import android.os.BatteryManager
import android.os.ParcelUuid
import com.google.android.gms.time.TrustedTime
import com.google.android.gms.time.TrustedTimeClient
import com.leopoldbialek.measurementapp.Enums.TestType
import com.leopoldbialek.measurementapp.Utils.PersistenceController
import java.util.UUID

abstract class WifiAwareService : Service() {
    protected lateinit var wifiAwareManager: WifiAwareManager
    protected lateinit var wifiAwareSession: WifiAwareSession
    protected lateinit var batteryManager: BatteryManager
    protected lateinit var testType: TestType
    protected lateinit var persistenceController: PersistenceController
    protected var trustedTimeClient: TrustedTimeClient? = null

    protected val measurements: MutableList<Any> =
        mutableListOf<Any>()
    protected val serviceUUID: ParcelUuid =
        ParcelUuid(UUID.fromString("04c9e1ed-1ce1-4fde-b3af-60aa092978df"))

    override fun onCreate() {
        super.onCreate()

        TrustedTime.createClient(applicationContext).addOnSuccessListener { trustedTimeClient ->
            this.trustedTimeClient = trustedTimeClient
        }

        batteryManager = applicationContext.getSystemService(BATTERY_SERVICE) as BatteryManager
    }
}