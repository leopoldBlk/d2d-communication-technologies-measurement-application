package com.leopoldbialek.measurementapp.Services.Communication.WiFi.Aware

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.aware.AttachCallback
import android.net.wifi.aware.DiscoverySessionCallback
import android.net.wifi.aware.PublishConfig
import android.net.wifi.aware.PublishDiscoverySession
import android.net.wifi.aware.WifiAwareManager
import android.net.wifi.aware.WifiAwareSession
import android.os.BatteryManager
import android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import com.leopoldbialek.measurementapp.DataClasses.BatteryTest.MeasurementBatteryTest
import com.leopoldbialek.measurementapp.Services.Location.LocationService
import com.leopoldbialek.measurementapp.DataClasses.RangeTest.TransmissionPaketRangeTest
import com.leopoldbialek.measurementapp.Enums.Role
import com.leopoldbialek.measurementapp.Enums.Technology
import com.leopoldbialek.measurementapp.Utils.LogController
import com.leopoldbialek.measurementapp.Enums.TestType
import com.leopoldbialek.measurementapp.Utils.PersistenceController
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.util.TimeZone
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random

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

        persistenceController = PersistenceController(
            Technology.BLUETOOTH_LE.name,
            intent!!.getStringExtra("TestType")!!,
            Role.SENDER.toString()
        )

        if (intent != null && intent.getStringExtra("TestType") == "Distance Test") {
            applicationContext.startService(Intent(applicationContext, LocationService::class.java))

            testType = TestType.RANGE
        } else {
            testType = TestType.BATTERY

            registerReceiver(
                object : BroadcastReceiver() {
                    override fun onReceive(
                        context: Context?,
                        intent: Intent?
                    ) {
                        val measurementBatteryTest: MeasurementBatteryTest = MeasurementBatteryTest(
                            intent!!.getIntExtra(BatteryManager.EXTRA_LEVEL, 101).toString(),
                            LocalDateTime.now().toString(),
                            ""
                        )

                        measurements.add(measurementBatteryTest)
                    }
                }, IntentFilter(Intent.ACTION_BATTERY_CHANGED), RECEIVER_EXPORTED)
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

        if (testType == TestType.RANGE) {
            applicationContext.stopService(Intent(applicationContext, LocationService::class.java))
        }
        else{
            persistenceController.persistData(measurements.toString())
        }

        LogController.log(Log.DEBUG, "Wifi-Aware-Sender", "Stopped Service!")
    }

    //TODO: Timestamp accuracy when sending?
    @SuppressLint("MissingPermission")
    fun startPublishing() {
        isPublishing.set(true)

        if (testType == TestType.RANGE) {
            publishServiceConfig =
                PublishConfig.Builder()
                    .setServiceName(serviceUUID.toString())
                    .setTtlSec(1)
                    .setServiceSpecificInfo(
                        Json.encodeToString(
                            TransmissionPaketRangeTest(
                                LocationService.currentCoordinates.toString(),
                                LocalDateTime.now().toString()
                            )
                        ).encodeToByteArray()
                    ).build()
        } else {
            publishServiceConfig =
                PublishConfig.Builder()
                    .setServiceName(serviceUUID.toString())
                    .setTtlSec(1)
                    .setServiceSpecificInfo(
                        Random.nextBytes(255)
                    ).build()
        }

        wifiAwareSession.publish(
            publishServiceConfig,
            object : DiscoverySessionCallback() {
                override fun onPublishStarted(session: PublishDiscoverySession) {
                    super.onPublishStarted(session)

                    sendBroadcast(
                        Intent().setAction("MEASUREMENT_BROADCAST")
                            .putExtra("activeDataTransmission", true)
                    )

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