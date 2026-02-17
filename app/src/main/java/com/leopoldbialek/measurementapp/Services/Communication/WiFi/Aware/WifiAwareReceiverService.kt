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
import android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY
import android.os.IBinder
import android.os.ParcelUuid
import android.util.Log
import com.leopoldbialek.measurementapp.DataClasses.Measurement
import com.leopoldbialek.measurementapp.DataClasses.TransmissionPaket
import com.leopoldbialek.measurementapp.Enums.Technology
import com.leopoldbialek.measurementapp.Enums.TestType
import com.leopoldbialek.measurementapp.Services.Location.LocationService
import com.leopoldbialek.measurementapp.Utils.LogController
import com.leopoldbialek.measurementapp.Utils.PersistenceController
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.util.UUID

class WifiAwareReceiverService : WifiAwareService() {
    private lateinit var persistenceController: PersistenceController

    private val measurements: MutableList<Measurement> =
        mutableListOf<Measurement>()
    private val subscribeServiceConfig: SubscribeConfig =
        SubscribeConfig.Builder().setServiceName(serviceUUID.toString()).build()

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    @SuppressLint("MissingPermission", "NewApi")
    override fun onCreate() {
        super.onCreate()

        deviceUuid = ParcelUuid(UUID.fromString("b059d0aa-f6c8-41a8-893a-b17635f6bc72"))

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

            wifiAwareManager.attach(object : AttachCallback() {

                override fun onAttached(session: WifiAwareSession?) {
                    super.onAttached(session)

                    if (session != null) {
                        wifiAwareSession = session

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

                                    //TODO: Timestamp calculation accuracy?
                                    val timestampReceiving: LocalDateTime = LocalDateTime.now()

                                    val receivedTransmissionPacket: TransmissionPaket =
                                        Json.decodeFromString<TransmissionPaket>(
                                            info.serviceSpecificInfo?.decodeToString().toString()
                                        )

                                    val measurement: Measurement = Measurement(
                                        receivedTransmissionPacket.uuidSender,
                                        receivedTransmissionPacket.coordinatesSender,
                                        receivedTransmissionPacket.batteryLevelSender,
                                        receivedTransmissionPacket.timestampSending,
                                        deviceUuid.toString(),
                                        coordinatesReceiver = if (testType == TestType.DISTANCE) {
                                            LocationService.currentCoordinates.toString()
                                        } else {
                                            ""
                                        },
                                        batteryManager.getIntProperty(BATTERY_PROPERTY_CAPACITY)
                                            .toString(),
                                        timestampReceiving.toString()
                                    )

                                    measurements.add(measurement)

                                    LogController.log(
                                        Log.INFO,
                                        "Wifi-Aware-Receiver",
                                        "New Measurement: ${measurement}"
                                    )
                                }
                            }, null
                        )
                    }
                }
            }, null)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val statusSuperOnStartCommand: Int = super.onStartCommand(intent, flags, startId)

        persistenceController = PersistenceController(
            Technology.WIFI_AWARE.name,
            intent!!.getStringExtra("TestType")!!
        )

        return statusSuperOnStartCommand
    }

    override fun onDestroy() {
        super.onDestroy()

        wifiAwareSession.close()

        persistenceController.persistData(measurements.toString())

        LogController.log(Log.DEBUG, "Wifi-Aware-Receiver", "Stopped Service!")
    }
}