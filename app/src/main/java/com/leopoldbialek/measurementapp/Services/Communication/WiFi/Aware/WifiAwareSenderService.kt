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
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import com.leopoldbialek.measurementapp.DataClasses.BatteryTest.MeasurementBatteryTest
import com.leopoldbialek.measurementapp.Services.Location.LocationService
import com.leopoldbialek.measurementapp.DataClasses.RangeTest.TransmissionPacketRangeTest
import com.leopoldbialek.measurementapp.Enums.Role
import com.leopoldbialek.measurementapp.Enums.Technology
import com.leopoldbialek.measurementapp.Utils.LogController
import com.leopoldbialek.measurementapp.Enums.TestType
import com.leopoldbialek.measurementapp.Utils.PersistenceController
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class WifiAwareSenderService : WifiAwareService() {
    private lateinit var publishServiceConfig: PublishConfig
    private lateinit var callbackThreadLooper: Looper

    private var isPublishing: AtomicBoolean = AtomicBoolean(false)
    private var callbackHandler: Handler? = null

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

    @OptIn(ExperimentalUuidApi::class)
    @SuppressLint("MissingPermission")
    fun startPublishing() {
        if (testType == TestType.RANGE) {
            val timestamp: LocalDateTime = if (trustedTimeClient == null) {
                LocalDateTime.now()
            } else {
                LocalDateTime.ofInstant(
                    trustedTimeClient!!.computeCurrentInstant(),
                    ZoneOffset.systemDefault()
                )
            }

            val transmissionPacketRangeTest: TransmissionPacketRangeTest = TransmissionPacketRangeTest(
                UUID.randomUUID().toString(),
                LocationService.currentCoordinates.toString(),
                timestamp.toString(),
                // Length: 28 Chars
                "<----PLACEHOLDER-STRING---->"
            )

            publishServiceConfig =
                PublishConfig.Builder()
                    .setServiceName(serviceUUID.toString())
                    .setTtlSec(1)
                    .setServiceSpecificInfo(
                        Json.encodeToString(transmissionPacketRangeTest).encodeToByteArray()
                    ).build()

            measurements.add(transmissionPacketRangeTest)

        } else {
            publishServiceConfig =
                PublishConfig.Builder()
                    .setServiceName(serviceUUID.toString())
                    .setTtlSec(1)
                    .setServiceSpecificInfo(
                        Random.nextBytes(237)
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

    @SuppressLint("MissingPermission")
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    override fun onCreate() {
        super.onCreate()

        if (!applicationContext.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE)) {
            LogController.log(Log.ERROR, "Wifi-Aware-Sender", "No Support for Wifi Aware!")
            stopSelf()
        } else {
            wifiAwareManager =
                applicationContext.getSystemService(WIFI_AWARE_SERVICE) as WifiAwareManager
        }
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val statusSuperOnStartCommand: Int = super.onStartCommand(intent, flags, startId)

        persistenceController = PersistenceController(
            Technology.BLUETOOTH_LE.name,
            intent!!.getStringExtra("TestType")!!,
            Role.SENDER.name
        )

        if (intent != null && intent.getStringExtra("TestType") == TestType.RANGE.name) {
            applicationContext.startService(Intent(applicationContext, LocationService::class.java))

            testType = TestType.RANGE
        } else {
            testType = TestType.BATTERY

            batteryChangeBroadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context?,
                    intent: Intent?
                ) {
                    val timestamp: LocalDateTime = if (trustedTimeClient == null) {
                        LocalDateTime.now()
                    } else {
                        LocalDateTime.ofInstant(
                            trustedTimeClient!!.computeCurrentInstant(),
                            ZoneOffset.systemDefault()
                        )
                    }

                    val measurementBatteryTest: MeasurementBatteryTest = MeasurementBatteryTest(
                        intent!!.getIntExtra(BatteryManager.EXTRA_LEVEL, 101).toString(),
                        timestamp.toString(),
                        ""
                    )

                    measurements.add(measurementBatteryTest)
                }
            }

            registerReceiver(
                batteryChangeBroadcastReceiver,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                RECEIVER_EXPORTED
            )
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
        } else {
            unregisterReceiver(batteryChangeBroadcastReceiver)
        }

        persistenceController.persistData(measurements.toString())

        sendBroadcast(
            Intent().setAction("MEASUREMENT_BROADCAST").putExtra("activeDataTransmission", false)
        )

        LogController.log(Log.DEBUG, "Wifi-Aware-Sender", "Stopped Service!")
    }
}