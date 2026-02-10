package com.leopoldbialek.measurementapp.Services.Communication.WiFi.Aware

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.aware.WifiAwareSession
import android.os.IBinder
import android.util.Log
import android.net.wifi.aware.WifiAwareManager
import android.net.wifi.aware.AttachCallback
import android.net.wifi.aware.DiscoverySessionCallback
import android.net.wifi.aware.ServiceDiscoveryInfo
import android.net.wifi.aware.SubscribeConfig
import android.net.wifi.aware.SubscribeDiscoverySession
import android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY
import com.leopoldbialek.measurementapp.Services.Location.LocationService
import com.leopoldbialek.measurementapp.DataClasses.Measurement
import com.leopoldbialek.measurementapp.Services.Persistence.PersistenceService
import com.leopoldbialek.measurementapp.DataClasses.TransmissionPaket
import com.leopoldbialek.measurementapp.MainActivity
import com.leopoldbialek.measurementapp.Utils.LogController
import com.leopoldbialek.measurementapp.Enums.TestType
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.util.UUID

class WifiAwareReceiverService : WifiAwareService() {
    private val receivedTransmissionPackets: MutableList<Measurement> =
        mutableListOf<Measurement>()
    private val subscribeServiceConfig: SubscribeConfig =
        SubscribeConfig.Builder().setServiceName(serviceUUID.toString()).build()

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    @SuppressLint("MissingPermission", "NewApi")
    override fun onCreate() {
        super.onCreate()

        if (!applicationContext.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE)) {
            LogController.log(Log.ERROR,"Wifi-Aware-Receiver", "Device does not support Wi-Fi Aware!")
            stopSelf()
        } else {
            LogController.log(Log.INFO,"Wifi-Aware-Receiver", "Started Service!")

            wifiAwareManager =
                applicationContext.getSystemService(WIFI_AWARE_SERVICE) as WifiAwareManager

            wifiAwareManager.attach(object : AttachCallback() {

                override fun onAttached(session: WifiAwareSession?) {
                    super.onAttached(session)

                    if (session != null) {
                        wifiAwareSession = session

                        wifiAwareSession.subscribe(
                            subscribeServiceConfig, object : DiscoverySessionCallback() {
                                override fun onSubscribeStarted(session: SubscribeDiscoverySession) {
                                    super.onSubscribeStarted(session)

                                    LogController.log(Log.DEBUG,"Wifi-Aware-Receiver", "Started Discovery!")
                                }

                                override fun onServiceDiscovered(info: ServiceDiscoveryInfo) {
                                    super.onServiceDiscovered(info)

                                    //TODO: Timestamp calculation accuracy? (Compared to the one with bluetooth)
                                    val timestampReceiving: LocalDateTime = LocalDateTime.now()

                                    val receivedTransmissionPacket: TransmissionPaket =
                                        Json.decodeFromString<TransmissionPaket>(
                                            info.serviceSpecificInfo?.decodeToString().toString()
                                        )

                                    //TODO: RSSI value cannot be obtained from the ServiceDiscoveryInfo object
                                    val measurement: Measurement = Measurement(
                                        "Wi-Fi Aware",
                                        receivedTransmissionPacket.uuidSender,
                                        receivedTransmissionPacket.coordinatesSender,
                                        receivedTransmissionPacket.batteryLevelSender,
                                        receivedTransmissionPacket.timestampSending,
                                        UUID.randomUUID().toString(),
                                        coordinatesReceiver = if (testType == TestType.DISTANCE) {
                                            LocationService.currentCoordinates.toString()
                                        } else {
                                            ""
                                        },
                                        batteryManager.getIntProperty(BATTERY_PROPERTY_CAPACITY)
                                            .toString(),
                                        timestampReceiving.toString()
                                    )

                                    receivedTransmissionPackets.add(measurement)

                                    LogController.log(Log.INFO,"Wifi-Aware-Receiver", "Received Data: ${measurement}")
                                }
                            }, null
                        )
                    }
                }
            }, null)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && intent.getStringExtra("TestType") == "Distance Test") {
            applicationContext.startService(Intent(applicationContext, LocationService::class.java))

            testType = TestType.DISTANCE
        }
        else{
            testType = TestType.BATTERY
        }

        applicationContext.startService(
            Intent(applicationContext, PersistenceService::class.java)
                .putExtra("Technology", "WifiAware")
                .putExtra("TestType", testType.name)
                .putExtra("Role", "Receiver")
        )

        return super.onStartCommand(intent, flags, startId)
    }

    //TODO: Super implementations at the beginning of method or at the end?
    override fun onDestroy() {
        wifiAwareSession.close()

        PersistenceService.Companion.persistData(receivedTransmissionPackets.toString())
        applicationContext.stopService(Intent(applicationContext, PersistenceService::class.java))

        LogController.log(Log.DEBUG,"Wifi-Aware-Receiver", "Stopped Service!")

        super.onDestroy()
    }
}