package com.yourteam.nextstop.service

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.yourteam.nextstop.MainActivity
import com.yourteam.nextstop.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProximityNotificationService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun sendProximityNotification(busNumber: String, stopName: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, "bus_proximity")
            .setSmallIcon(R.mipmap.ic_launcher) // Use default launcher icon as placeholder
            .setContentTitle("Your bus is nearby!")
            .setContentText("Bus $busNumber is less than 500m from $stopName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Return safely if permission isn't granted.
                return
            }
            // notificationId is a unique int for each notification that you must define, 
            // but we can use a hardcoded value 1001 so it updates the same alert.
            notify(1001, builder.build())
        }
    }
}
