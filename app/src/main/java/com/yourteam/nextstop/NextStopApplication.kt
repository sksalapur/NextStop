package com.yourteam.nextstop

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * NextStop Application class.
 *
 * Annotated with @HiltAndroidApp to trigger Hilt's code generation,
 * including a base class for the application that serves as the
 * application-level dependency container.
 */
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

@HiltAndroidApp
class NextStopApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize the SDK
        com.google.android.libraries.places.api.Places.initialize(applicationContext, BuildConfig.MAPS_API_KEY)
        
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Bus Proximity Alerts"
            val descriptionText = "Notifications for when your bus is approaching your stop"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("bus_proximity", name, importance).apply {
                description = descriptionText
            }
            
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
