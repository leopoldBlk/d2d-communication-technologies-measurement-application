package com.leopoldbialek.measurementapp.CommunicationServices.WiFi.Direct

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresPermission

class WifiDirectReceiverService : Service() {
    private lateinit var wifiP2pManager: WifiP2pManager
    lateinit var wifiP2pChannel: WifiP2pManager.Channel

    val wifiP2pServiceRequest = WifiP2pDnsSdServiceRequest.newInstance()

    @SuppressLint("MissingPermission")
    private val sdTxtListener =
        WifiP2pManager.DnsSdTxtRecordListener { fullDomainName, txtRecordMap, srcDevice ->
            Log.d("Wifi-P2P-Receiver", "TXT record received!")
        }

    private val sdServiveResponseListener =
        WifiP2pManager.DnsSdServiceResponseListener { instanceName, registrationType, srcDevice ->
            Log.d("Wifi-P2P-Receiver", "Service discovery response received!")
        }

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
                    Log.e("Wifi-P2P-Receiver", "Channel disconnected!")
                }
            })

        wifiP2pManager.setDnsSdResponseListeners(
            wifiP2pChannel,
            sdServiveResponseListener,
            sdTxtListener
        )

        wifiP2pManager.addServiceRequest(
            wifiP2pChannel,
            wifiP2pServiceRequest,
            object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.d("Wifi-P2P-Receiver", "Added service discovery request successfully!")
                }

                override fun onFailure(reason: Int) {
                    Log.e("Wifi-P2P-Receiver", "Error when adding service discovery request!")
                }
            }
        )

        wifiP2pManager.discoverServices(
            wifiP2pChannel,
            object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.d("Wifi-P2P-Receiver", "Service discovery started successfully!")
                }

                override fun onFailure(reason: Int) {
                    Log.e("Wifi-P2P-Receiver", "Error when starting service discovery!")
                }
            }
        )
    }
}