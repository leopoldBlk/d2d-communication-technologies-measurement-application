package com.leopoldbialek.measurementapp.Services.Communication.WiFi.Aware

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.aware.AttachCallback
import android.net.wifi.aware.DiscoverySessionCallback
import android.net.wifi.aware.ServiceDiscoveryInfo
import android.net.wifi.aware.SubscribeConfig
import android.net.wifi.aware.SubscribeDiscoverySession
import android.net.wifi.aware.WifiAwareManager
import android.net.wifi.aware.WifiAwareSession
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import com.leopoldbialek.measurementapp.DataClasses.BatteryTest.MeasurementBatteryTest
import com.leopoldbialek.measurementapp.DataClasses.RangeTest.MeasurementRangeTest
import com.leopoldbialek.measurementapp.DataClasses.RangeTest.TransmissionPaketRangeTest
import com.leopoldbialek.measurementapp.Enums.Role
import com.leopoldbialek.measurementapp.Enums.Technology
import com.leopoldbialek.measurementapp.Enums.TestType
import com.leopoldbialek.measurementapp.Services.Location.LocationService
import com.leopoldbialek.measurementapp.Utils.LogController
import com.leopoldbialek.measurementapp.Utils.PersistenceController
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.ZoneOffset

class WifiAwareReceiverService : WifiAwareService() {
    private lateinit var callbackThreadLooper: Looper
    private lateinit var discoveryThreadLooper: Looper

    private var callbackHandler: Handler? = null
    private val subscribeServiceConfig: SubscribeConfig =
        SubscribeConfig.Builder().setServiceName(serviceUUID.toString()).build()

    private val callbackThread: Thread = Thread {
        Looper.prepare()
        val looper: Looper = Looper.myLooper()!!

        callbackThreadLooper = looper
        callbackHandler = Handler(looper)

        Looper.loop()
    }

    @SuppressLint("MissingPermission", "NewApi")
    private val discoveryThread: Thread = Thread {
        Looper.prepare()
        val looper: Looper = Looper.myLooper()!!

        discoveryThreadLooper = looper

        while (callbackHandler == null) {
            Thread.sleep(10)
        }

        var sessionAttached: Boolean = false

        wifiAwareManager.attach(object : AttachCallback() {

            override fun onAttached(session: WifiAwareSession?) {
                super.onAttached(session)

                if (session != null) {
                    wifiAwareSession = session
                    sessionAttached = true

                    wifiAwareSession.subscribe(
                        subscribeServiceConfig, object : DiscoverySessionCallback() {
                            override fun onSubscribeStarted(session: SubscribeDiscoverySession) {
                                super.onSubscribeStarted(session)

                                LogController.log(
                                    Log.INFO,
                                    "Wifi-Aware-Receiver",
                                    "Started Discovery!"
                                )
                            }

                            override fun onServiceDiscovered(info: ServiceDiscoveryInfo) {
                                super.onServiceDiscovered(info)

                                val timestamp: LocalDateTime = if (trustedTimeClient == null) {
                                    LocalDateTime.now()
                                } else {
                                    LocalDateTime.ofInstant(
                                        trustedTimeClient!!.computeCurrentInstant(),
                                        ZoneOffset.systemDefault()
                                    )
                                }

                                if (testType == TestType.RANGE) {
                                    val receivedTransmissionPacket: TransmissionPaketRangeTest =
                                        Json.decodeFromString<TransmissionPaketRangeTest>(
                                            info.serviceSpecificInfo?.decodeToString()
                                                .toString()
                                        )

                                    val measurementRangeTest: MeasurementRangeTest =
                                        MeasurementRangeTest(
                                            receivedTransmissionPacket.coordinatesSender,
                                            receivedTransmissionPacket.timestampSending,
                                            receivedTransmissionPacket.additionalInfoSender,
                                            LocationService.currentCoordinates.toString(),
                                            timestamp.toString()
                                        )

                                    measurements.add(measurementRangeTest)

                                    LogController.log(
                                        Log.INFO,
                                        "Wifi-Aware-Receiver",
                                        "New Measurement: ${measurementRangeTest}"
                                    )
                                }

                                sendBroadcast(
                                    Intent().setAction("MEASUREMENT_BROADCAST")
                                        .putExtra("activeDataTransmission", true)
                                )
                            }
                        }, callbackHandler
                    )
                }
            }
        }, callbackHandler)

        Looper.loop()

        while (!sessionAttached) {
            Thread.sleep(10)
        }

        wifiAwareSession.close()
    }

    @SuppressLint("MissingPermission", "NewApi")
    override fun onCreate() {
        super.onCreate()

        if (!applicationContext.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE)) {
            LogController.log(
                Log.ERROR,
                "Wifi-Aware-Receiver",
                "No Support for Wi-Fi Aware!"
            )
            stopSelf()
        } else {
            wifiAwareManager =
                applicationContext.getSystemService(WIFI_AWARE_SERVICE) as WifiAwareManager

            callbackThread.start()
            discoveryThread.start()
        }
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val statusSuperOnStartCommand: Int = super.onStartCommand(intent, flags, startId)

        persistenceController = PersistenceController(
            Technology.WIFI_AWARE.name,
            intent!!.getStringExtra("TestType")!!,
            Role.RECEIVER.name
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

        return statusSuperOnStartCommand
    }

    override fun onDestroy() {
        super.onDestroy()

        discoveryThreadLooper.quit()
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

        LogController.log(Log.DEBUG, "Wifi-Aware-Receiver", "Stopped Service!")
    }
}