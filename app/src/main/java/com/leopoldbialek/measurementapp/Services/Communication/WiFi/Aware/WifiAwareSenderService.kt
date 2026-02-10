package com.leopoldbialek.measurementapp.Services.Communication.WiFi.Aware

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.aware.AttachCallback
import android.net.wifi.aware.DiscoverySessionCallback
import android.net.wifi.aware.PublishConfig
import android.net.wifi.aware.PublishDiscoverySession
import android.net.wifi.aware.WifiAwareManager
import android.net.wifi.aware.WifiAwareSession
import android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresPermission
import com.leopoldbialek.measurementapp.Services.Location.LocationService
import com.leopoldbialek.measurementapp.DataClasses.TransmissionPaket
import com.leopoldbialek.measurementapp.MainActivity
import com.leopoldbialek.measurementapp.Utils.LogController
import com.leopoldbialek.measurementapp.Enums.TestType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.util.TimeZone
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class WifiAwareSenderService : WifiAwareService() {
    private var isPublishing: AtomicBoolean = AtomicBoolean(false)

    //TODO: Add delay to request?
    private val publishingStateMonitor: Thread = Thread {
        while (true) {
            if (!isPublishing.get()) {
                startPublishing()
            }
        }
    }

    private lateinit var publishServiceConfig: PublishConfig

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    @SuppressLint("MissingPermission")
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    override fun onCreate() {
        super.onCreate()

        if (!applicationContext.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE)) {
            LogController.log(Log.ERROR,"Wifi-Aware-Sender", "Device does not support Wi-Fi Aware!")
            stopSelf()
        } else {
            LogController.log(Log.INFO,"Wifi-Aware-Sender", "Started Service!")

            wifiAwareManager =
                applicationContext.getSystemService(WIFI_AWARE_SERVICE) as WifiAwareManager

            // Max Service Specific Info Length: 255 Bytes
            val maxServiceSpecificInfoLength: Int? =
                wifiAwareManager.characteristics?.maxServiceSpecificInfoLength

            wifiAwareManager.attach(object : AttachCallback() {

                override fun onAttached(session: WifiAwareSession?) {
                    super.onAttached(session)
                    if (session != null) {
                        wifiAwareSession = session

                        startPublishing()

                        publishingStateMonitor.start()
                    }
                }
            }, null)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && intent.getStringExtra("TestType") == "Distance Test") {
            applicationContext.startService(Intent(applicationContext, LocationService::class.java))

            testType = TestType.DISTANCE
        } else {
            testType = TestType.BATTERY
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()

        isPublishing.set(false)

        wifiAwareSession.close()

        if (testType == TestType.DISTANCE) {
            applicationContext.stopService(Intent(applicationContext, LocationService::class.java))
        }

        LogController.log(Log.DEBUG,"Wifi-Aware-Sender", "Stopped Service!")
    }

    //TODO: Find another solution, not sure about recursive invocation
    //TODO: Timestamp accuracy when sending?
    @SuppressLint("MissingPermission")
    fun startPublishing() {
        isPublishing.set(true)

        publishServiceConfig =
            PublishConfig.Builder()
                .setServiceName(serviceUUID.toString())
                .setTtlSec(1)
                .setServiceSpecificInfo(
                    Json.encodeToString(
                        TransmissionPaket(
                            UUID.randomUUID().toString(),
                            coordinatesSender = if (testType == TestType.DISTANCE) {
                                LocationService.currentCoordinates.toString()
                            } else {
                                ""
                            },
                            batteryManager.getIntProperty(BATTERY_PROPERTY_CAPACITY)
                                .toString(),
                            LocalDateTime.ofInstant(
                                Instant.ofEpochMilli(System.currentTimeMillis()),
                                TimeZone.getDefault().toZoneId()
                            ).toString()
                        )
                    ).encodeToByteArray()
                ).build()

        wifiAwareSession.publish(
            publishServiceConfig,
            object : DiscoverySessionCallback() {
                override fun onPublishStarted(session: PublishDiscoverySession) {
                    super.onPublishStarted(session)

                    LogController.log(Log.DEBUG,"Wifi-Aware-Sender", "Publishing Started!")
                }

                override fun onSessionTerminated() {
                    super.onSessionTerminated()

                    isPublishing.set(false)
                }
            },
            null
        )
    }
}