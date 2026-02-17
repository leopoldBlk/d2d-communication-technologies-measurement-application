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
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import com.leopoldbialek.measurementapp.Services.Location.LocationService
import com.leopoldbialek.measurementapp.DataClasses.TransmissionPaket
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
    private lateinit var publishServiceConfig: PublishConfig

    private var callbackHandler: Handler? = null
    private lateinit var callbackThreadLooper: Looper

    private val callbackThread: Thread = Thread {
        Looper.prepare()
        val looper: Looper = Looper.myLooper()!!

        callbackThreadLooper = looper
        callbackHandler = Handler(looper)

        Looper.loop()
    }

    private val publishingThread: Thread = Thread {
        while (callbackHandler == null) {
            Thread.sleep(10)
        }

        var sessionAttached: Boolean = false

        // Max Service Specific Info Length: 255 Bytes
        //val maxServiceSpecificInfoLength: Int? = wifiAwareManager.characteristics?.maxServiceSpecificInfoLength

        wifiAwareManager.attach(object : AttachCallback() {

            override fun onAttached(session: WifiAwareSession?) {
                super.onAttached(session)

                if (session != null) {
                    wifiAwareSession = session
                    sessionAttached = true
                }
            }
        }, callbackHandler)

        while (!Thread.currentThread().isInterrupted) {
            if (!isPublishing.get() && sessionAttached) {
                isPublishing.set(true)
                startPublishing()
            }
        }

        while (!sessionAttached) {
            Thread.sleep(10)
        }

        wifiAwareSession.close()
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    @SuppressLint("MissingPermission")
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    override fun onCreate() {
        super.onCreate()

        deviceUuid = ParcelUuid(UUID.fromString("1369479e-f28b-4e8f-8609-315a10aaaa72"))

        if (!applicationContext.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE)) {
            LogController.log(Log.ERROR, "Wifi-Aware-Sender", "No Support for Wi-Fi Aware!")
            stopSelf()
        } else {
            wifiAwareManager =
                applicationContext.getSystemService(WIFI_AWARE_SERVICE) as WifiAwareManager
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val statusSuperOnStartCommand: Int = super.onStartCommand(intent, flags, startId)

        if (intent != null && intent.getStringExtra("TestType") == "Distance Test") {
            applicationContext.startService(Intent(applicationContext, LocationService::class.java))

            testType = TestType.DISTANCE
        } else {
            testType = TestType.BATTERY
        }

        callbackThread.start()
        publishingThread.start()

        return statusSuperOnStartCommand
    }

    override fun onDestroy() {
        super.onDestroy()

        isPublishing.set(false)

        publishingThread.interrupt()
        callbackThreadLooper.quit()

        if (testType == TestType.DISTANCE) {
            applicationContext.stopService(Intent(applicationContext, LocationService::class.java))
        }

        LogController.log(Log.DEBUG, "Wifi-Aware-Sender", "Stopped Service!")
    }

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
                            deviceUuid.toString(),
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

                    LogController.log(
                        Log.INFO,
                        "Wifi-Aware-Sender",
                        "Publishing Started!"
                    )
                }

                override fun onSessionTerminated() {
                    super.onSessionTerminated()

                    LogController.log(Log.INFO, "Wifi-Aware-Sender", "Publishing Stopped!")

                    isPublishing.set(false)
                }
            },
            callbackHandler
        )
    }
}