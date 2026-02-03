package com.leopoldbialek.measurementapp.CommunicationServices

import android.Manifest
import android.app.Service
import android.content.Intent
import android.location.LocationManager
import android.location.LocationRequest
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

class LocationService : Service() {
    companion object {
        public lateinit var currentCoordinates: Coordinates
    }

    private lateinit var locationManager: LocationManager

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onCreate() {
        super.onCreate()

        locationManager = applicationContext.getSystemService(LOCATION_SERVICE) as LocationManager
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val status: Int = super.onStartCommand(intent, flags, startId)

        locationManager.getCurrentLocation(
            LocationManager.GPS_PROVIDER,
            null,
            Dispatchers.Main.asExecutor()
        )
        { location ->
            currentCoordinates = Coordinates(location.longitude, location.latitude)
        }

        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            LocationRequest.Builder(100).build(),
            Dispatchers.Main.asExecutor()
        )
        { location ->
            currentCoordinates = Coordinates(location.longitude, location.latitude)

            Log.d(
                "Location-Service",
                "Location changed to (lat/long): ${location.latitude}/${location.longitude}"
            )
        }

        return status
    }
}