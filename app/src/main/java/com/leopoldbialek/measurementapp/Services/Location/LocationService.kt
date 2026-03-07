package com.leopoldbialek.measurementapp.Services.Location

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.leopoldbialek.measurementapp.DataClasses.Coordinates
import com.leopoldbialek.measurementapp.Utils.LogController
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.Priority
import java.util.concurrent.Executors

class LocationService : Service() {
    companion object {
        public var currentCoordinates: Coordinates = Coordinates(0.0, 0.0)
    }

    private lateinit var locationUpdaterThreadLooper: Looper

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.S)
    private val locationUpdaterThread: Thread = Thread {
        Looper.prepare()
        val looper: Looper = Looper.myLooper()!!

        locationUpdaterThreadLooper = looper

        fusedLocationProviderClient.requestLocationUpdates(
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build(),
            Executors.newSingleThreadExecutor(),
            locationListener
        )

        Looper.loop()
    }

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    val locationListener: LocationListener = LocationListener { location ->
        currentCoordinates = Coordinates(location.latitude, location.longitude)

        LogController.log(
            Log.DEBUG,
            "Location-Service",
            "Location changed to (lat/long): ${location.latitude}/${location.longitude}"
        )
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onCreate() {
        super.onCreate()

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(applicationContext)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val status: Int = super.onStartCommand(intent, flags, startId)

        locationUpdaterThread.start()

        return status
    }

    override fun onDestroy() {
        super.onDestroy()

        locationUpdaterThreadLooper.quit()

        fusedLocationProviderClient.removeLocationUpdates(locationListener)
    }
}