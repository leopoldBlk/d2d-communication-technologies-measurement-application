package com.leopoldbialek.measurementapp.CommunicationServices.WiFi

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
import com.leopoldbialek.measurementapp.CommunicationServices.LocationService
import com.leopoldbialek.measurementapp.CommunicationServices.Measurement
import com.leopoldbialek.measurementapp.CommunicationServices.PersistenceService
import com.leopoldbialek.measurementapp.CommunicationServices.TransmissionPaket
import com.leopoldbialek.measurementapp.MainActivity
import kotlinx.serialization.json.Json
import java.time.LocalDateTime

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

        applicationContext.startService(Intent(applicationContext, PersistenceService::class.java))

        if (!applicationContext.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE)) {
            Log.e("Wifi-Aware-Receiver: ", "Device does not support Wi-Fi Aware!")
            stopSelf()
        } else {
            Log.d("Wifi-Aware-Receiver: ", "Started Service!")

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

                                    Log.d("Wifi-Aware-Receiver", "Started Discovery!")
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
                                        MainActivity.deviceUUID.toString(),
                                        LocationService.currentCoordinates.toString(),
                                        batteryManager.getIntProperty(BATTERY_PROPERTY_CAPACITY)
                                            .toString(),
                                        timestampReceiving.toString()
                                    )

                                    receivedTransmissionPackets.add(measurement)

                                    Log.i("Wifi-Aware-Receiver", "Received Data: ${measurement}")

                                    //val test = info.serviceSpecificInfo?.decodeToString()
                                }
                            }, null
                        )
                    }
                }
            }, null)
        }
    }

    //TODO: Super implementations at the beginning of method or at the end?
    override fun onDestroy() {
        super.onDestroy()

        wifiAwareSession.close()

        PersistenceService.Companion.persistData(receivedTransmissionPackets.toString())
        applicationContext.stopService(Intent(applicationContext, PersistenceService::class.java))

        Log.d("Wifi-Aware-Receiver: ", "Stopped Service!")
    }
}