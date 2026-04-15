package com.yourteam.nextstop.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.yourteam.nextstop.MainActivity
import com.yourteam.nextstop.R
import com.yourteam.nextstop.data.repository.DriverRepository
import com.yourteam.nextstop.models.LiveLocation
import com.yourteam.nextstop.models.TripState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LocationForegroundService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START_TRACKING"
        const val ACTION_STOP = "ACTION_STOP_TRACKING"
        const val EXTRA_ROUTE_ID = "EXTRA_ROUTE_ID"

        private const val NOTIFICATION_CHANNEL_ID = "nextstop_tracking_channel"
        private const val NOTIFICATION_ID = 1001
    }

    // Hilt injection
    @Inject lateinit var driverRepository: DriverRepository

    // Binder for ViewModel binding
    private val binder = LocalBinder()

    // Service-scoped coroutine scope
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // State exposed to bound clients
    private val _tripState = MutableStateFlow<TripState>(TripState.Stopped)
    val tripState: StateFlow<TripState> = _tripState.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    // Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var currentRouteId: String? = null

    // ─── Binder ──────────────────────────────────────────────────────

    inner class LocalBinder : Binder() {
        fun getService(): LocationForegroundService = this@LocationForegroundService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    // ─── Lifecycle ───────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
        initLocationCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val routeId = intent.getStringExtra(EXTRA_ROUTE_ID)
                if (routeId != null) {
                    startTracking(routeId)
                }
            }
            ACTION_STOP -> {
                stopTracking()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    // ─── Tracking ────────────────────────────────────────────────────

    private fun startTracking(routeId: String) {
        currentRouteId = routeId
        _isTracking.value = true

        // Start as foreground
        startForeground(NOTIFICATION_ID, buildNotification())

        // Mark bus as active in Firestore via RTDB (legacy Firestore status removed)
        // Active status is inferred from Realtime Database LiveLocation

        // Request location updates
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            4000L // interval
        ).setMinUpdateIntervalMillis(2000L) // fastest interval
            .build()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    private fun stopTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)

        val routeId = currentRouteId
        if (routeId != null) {
            serviceScope.launch {
                try {
                    driverRepository.clearLiveLocation(routeId)
                } catch (_: Exception) { /* best effort */ }
            }
        }

        _isTracking.value = false
        _tripState.value = TripState.Stopped
        currentRouteId = null

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ─── Location callback ───────────────────────────────────────────

    private fun initLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                val routeId = currentRouteId ?: return

                // Calculate valid speed, ignoring < 0.5f m/s (1.8 km/h) noise
                val validSpeed = if (location.hasSpeed() && location.speed > 0.5f) location.speed else 0f
                val speedKmh = validSpeed * 3.6f

                // Update local state for UI
                _tripState.value = TripState.Running(
                    speed = speedKmh,
                    latitude = location.latitude,
                    longitude = location.longitude
                )

                // Write to Firebase RTDB
                val liveLocation = LiveLocation(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    speed = validSpeed,
                    bearing = if (location.hasBearing()) location.bearing else 0f,
                    timestamp = System.currentTimeMillis(),
                    active = true
                )

                serviceScope.launch {
                    try {
                        driverRepository.updateLiveLocation(routeId, liveLocation)
                    } catch (_: Exception) { /* best effort, will retry on next update */ }
                }
            }
        }
    }

    // ─── Notification ────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Bus Tracking",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows when the bus location is being tracked"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("NextStop")
            .setContentText("Bus tracking is active")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
