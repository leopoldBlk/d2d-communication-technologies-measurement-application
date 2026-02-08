package com.leopoldbialek.measurementapp.CommunicationServices.WiFi

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
import com.leopoldbialek.measurementapp.CommunicationServices.LocationService
import com.leopoldbialek.measurementapp.CommunicationServices.TransmissionPaket
import com.leopoldbialek.measurementapp.MainActivity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.util.TimeZone
import java.time.Instant

class WifiAwareSenderService : WifiAwareService() {

    private lateinit var publishServiceConfig: PublishConfig

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    @SuppressLint("MissingPermission")
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    override fun onCreate() {
        super.onCreate()

        if (!applicationContext.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE)) {
            Log.e("Wifi-Aware-Sender: ", "Device does not support Wi-Fi Aware!")
            stopSelf()
        } else {
            Log.d("Wifi-Aware-Sender: ", "Started Service!")

            wifiAwareManager =
                applicationContext.getSystemService(WIFI_AWARE_SERVICE) as WifiAwareManager

            wifiAwareManager.attach(object : AttachCallback() {

                override fun onAttached(session: WifiAwareSession?) {
                    super.onAttached(session)
                    if (session != null) {
                        wifiAwareSession = session
                        startPublishing()
                    }
                }
            }, null)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        wifiAwareSession.close()
        Log.d("Wifi-Aware-Sender: ", "Stopped Service!")
    }

    //TODO: Find another solution, not sure about recursive invocation
    //TODO: Timestamp accuracy when sending?
    @SuppressLint("MissingPermission")
    fun startPublishing() {
        publishServiceConfig =
            PublishConfig.Builder()
                .setServiceName(serviceUUID.toString())
                .setTtlSec(1)
                .setServiceSpecificInfo(
                    Json.encodeToString(
                        TransmissionPaket(
                            MainActivity.deviceUUID.toString(),
                            LocationService.currentCoordinates.toString(),
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

                    Log.d("Wifi-Aware-Sender", "Publishing Started!")
                }

                override fun onSessionTerminated() {
                    super.onSessionTerminated()
                    startPublishing()
                }
            },
            null
        )
    }
}