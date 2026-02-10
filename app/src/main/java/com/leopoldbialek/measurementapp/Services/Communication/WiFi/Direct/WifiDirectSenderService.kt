package com.leopoldbialek.measurementapp.Services.Communication.WiFi.Direct

import android.Manifest
import android.app.Service
import android.content.Intent
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresPermission

class WifiDirectSenderService : Service() {
    private lateinit var wifiP2pManager: WifiP2pManager
    private lateinit var wifiP2pChannel: WifiP2pManager.Channel

    private val txtRecord: MutableMap<String, String> = mutableMapOf<String, String>(
        "TestKey" to "TestValue"
    )
    private val wifiP2pServiceInfo: WifiP2pDnsSdServiceInfo = WifiP2pDnsSdServiceInfo.newInstance(
        "TestInstanceName",
        "TestServiceName",
        txtRecord
    )

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    override fun onCreate() {
        super.onCreate()

        wifiP2pManager =
            applicationContext.getSystemService(WIFI_P2P_SERVICE) as WifiP2pManager

        wifiP2pChannel = wifiP2pManager.initialize(
            applicationContext,
            applicationContext.mainLooper,
            object : WifiP2pManager.ChannelListener {
                override fun onChannelDisconnected() {
                    Log.e("Wifi-P2P-Sender", "Channel disconnected!")
                }
            })

        wifiP2pManager.addLocalService(
            wifiP2pChannel,
            wifiP2pServiceInfo,
            object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.d("Wifi-P2P-Sender", "Local service successfully registered for service discovery!")
                }

                override fun onFailure(reason: Int) {
                    Log.e("Wifi-P2P-Sender", "Error when registering local service for service discovery!")
                }
            })
    }
}