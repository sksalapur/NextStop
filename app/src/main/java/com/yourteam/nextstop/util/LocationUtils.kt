package com.yourteam.nextstop.util

import com.yourteam.nextstop.models.LiveLocation
import com.yourteam.nextstop.models.Stop
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Location utility functions for distance and ETA calculations.
 */
object LocationUtils {

    private const val EARTH_RADIUS_KM = 6371.0
    private const val MIN_SPEED_KMH = 20.0 // Floor to avoid divide-by-zero

    /**
     * Calculates the distance in kilometers between two coordinates
     * using the Haversine formula.
     */
    fun haversineKm(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) *
                cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_KM * c
    }

    /**
     * Calculates ETA in minutes from bus location to a stop.
     * Uses a minimum speed floor to handle stationary buses.
     *
     * @param busLat    current bus latitude
     * @param busLon    current bus longitude
     * @param stopLat   target stop latitude
     * @param stopLon   target stop longitude
     * @param speedKmh  current bus speed in km/h
     * @return estimated time in minutes (rounded up)
     */
    fun calculateEtaMinutes(
        busLat: Double, busLon: Double,
        stopLat: Double, stopLon: Double,
        speedKmh: Float
    ): Int {
        val distKm = haversineKm(busLat, busLon, stopLat, stopLon)
        val effectiveSpeed = maxOf(speedKmh.toDouble(), MIN_SPEED_KMH)
        val timeHours = distKm / effectiveSpeed
        return (timeHours * 60).toInt().coerceAtLeast(1)
    }

    /**
     * Determines the next upcoming stop for the bus.
     *
     * Strategy: Find the stop closest to the bus. All stops with a lower
     * or equal order index are considered "passed". The next stop is the
     * one immediately after the closest stop in order.
     *
     * @return the next [Stop], or null if the bus has passed all stops.
     */
    fun findNextStop(
        busLat: Double,
        busLon: Double,
        stops: List<Stop>
    ): Stop? {
        if (stops.isEmpty()) return null

        val sortedStops = stops.sortedBy { it.order }

        // Find the closest stop to the bus
        val closestStop = sortedStops.minByOrNull { stop ->
            haversineKm(busLat, busLon, stop.latitude, stop.longitude)
        } ?: return null

        val closestDistance = haversineKm(
            busLat, busLon,
            closestStop.latitude, closestStop.longitude
        )

        // If bus is very close to the closest stop (< 150m), consider it passed
        // and return the next one in order
        return if (closestDistance < 0.15) {
            sortedStops.firstOrNull { it.order > closestStop.order } ?: closestStop
        } else {
            // Bus is between stops — the closest one ahead
            closestStop
        }
    }

    /**
     * Returns the set of stop IDs that the bus has already passed.
     * A stop is "passed" if its order is less than or equal to the closest stop's order
     * when the bus is within 150m, or strictly less than the closest stop's order otherwise.
     */
    fun getPassedStopIds(
        busLat: Double,
        busLon: Double,
        stops: List<Stop>
    ): Set<String> {
        if (stops.isEmpty()) return emptySet()

        val sortedStops = stops.sortedBy { it.order }

        val closestStop = sortedStops.minByOrNull { stop ->
            haversineKm(busLat, busLon, stop.latitude, stop.longitude)
        } ?: return emptySet()

        val closestDistance = haversineKm(
            busLat, busLon,
            closestStop.latitude, closestStop.longitude
        )

        // If bus is very close to the closest stop (<150m), that stop and all before it are passed
        val passedOrder = if (closestDistance < 0.15) {
            closestStop.order
        } else {
            // Bus is between stops — everything before the closest stop is passed
            closestStop.order - 1
        }

        return sortedStops
            .filter { it.order <= passedOrder }
            .map { it.stopId }
            .toSet()
    }

    /**
     * Formats a connection status string based on the last location timestamp.
     */
    @Deprecated(
        message = "Use LocationFreshness sealed class instead for UI state management.",
        replaceWith = ReplaceWith("LocationFreshness")
    )
    fun connectionStatus(location: LiveLocation?): String {
        if (location == null) return "Connecting..."
        if (!location.active) return "Bus has not started"

        val ageMs = System.currentTimeMillis() - location.timestamp
        val ageMinutes = (ageMs / 60_000).toInt()

        return when {
            ageMinutes < 1 -> "Live"
            ageMinutes < 60 -> "Last seen $ageMinutes min ago"
            else -> "Offline"
        }
    }

    /**
     * Converts a Vector Drawable to a Google Maps BitmapDescriptor natively.
     */
    fun bitmapDescriptorFromVector(context: android.content.Context, vectorResId: Int): com.google.android.gms.maps.model.BitmapDescriptor? {
        val vectorDrawable = androidx.core.content.ContextCompat.getDrawable(context, vectorResId) ?: return null
        vectorDrawable.setBounds(0, 0, vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
        val bitmap = android.graphics.Bitmap.createBitmap(vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        vectorDrawable.draw(canvas)
        return com.google.android.gms.maps.model.BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}
