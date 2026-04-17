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
     * Determines the next upcoming stop for the bus using segment projection.
     *
     * Strategy: For each segment (stop[i] → stop[i+1]), project the bus position
     * onto the segment. The bus is considered to be on the segment where the projection
     * falls between 0 and 1 (or closest to it). The next stop is stop[i+1] of that segment.
     *
     * Fallback: If projection fails, use distance-along-route to determine progress.
     */
    fun findNextStop(
        busLat: Double,
        busLon: Double,
        stops: List<Stop>
    ): Stop? {
        if (stops.isEmpty()) return null

        val sortedStops = stops.sortedBy { it.order }
        if (sortedStops.size == 1) return sortedStops[0]

        // Calculate cumulative distance along the route for each stop
        val cumulativeDistances = mutableListOf(0.0)
        for (i in 1 until sortedStops.size) {
            val prev = sortedStops[i - 1]
            val curr = sortedStops[i]
            cumulativeDistances.add(
                cumulativeDistances.last() + haversineKm(prev.latitude, prev.longitude, curr.latitude, curr.longitude)
            )
        }
        val totalRouteDistance = cumulativeDistances.last()

        // Project bus onto each segment; find the segment it's closest to
        var bestSegmentIndex = 0
        var bestProjectionDist = Double.MAX_VALUE

        for (i in 0 until sortedStops.size - 1) {
            val ax = sortedStops[i].latitude
            val ay = sortedStops[i].longitude
            val bx = sortedStops[i + 1].latitude
            val by = sortedStops[i + 1].longitude

            // Parameter t of projection onto segment [A, B]
            val dx = bx - ax
            val dy = by - ay
            val len2 = dx * dx + dy * dy
            val t = if (len2 < 1e-10) 0.0
                    else ((busLat - ax) * dx + (busLon - ay) * dy) / len2

            val tClamped = t.coerceIn(0.0, 1.0)

            // Closest point on segment
            val projLat = ax + tClamped * dx
            val projLon = ay + tClamped * dy

            val dist = haversineKm(busLat, busLon, projLat, projLon)

            if (dist < bestProjectionDist) {
                bestProjectionDist = dist
                bestSegmentIndex = i
            }
        }

        // Determine progress along the best segment
        val segStart = sortedStops[bestSegmentIndex]
        val segEnd = sortedStops[bestSegmentIndex + 1]
        val distToSegEnd = haversineKm(busLat, busLon, segEnd.latitude, segEnd.longitude)

        // If bus is within 200m of the segment end, the next stop is the one AFTER
        return if (distToSegEnd < 0.2 && bestSegmentIndex + 2 < sortedStops.size) {
            sortedStops[bestSegmentIndex + 2]
        } else {
            sortedStops[bestSegmentIndex + 1]
        }
    }

    /**
     * Returns the set of stop IDs that the bus has already passed.
     * Uses segment projection to determine which segment the bus is on.
     * All stops before the current segment's end stop are considered passed.
     */
    fun getPassedStopIds(
        busLat: Double,
        busLon: Double,
        stops: List<Stop>
    ): Set<String> {
        if (stops.isEmpty()) return emptySet()

        val sortedStops = stops.sortedBy { it.order }
        val nextStop = findNextStop(busLat, busLon, stops) ?: return emptySet()

        // Everything with order < nextStop.order is passed
        return sortedStops
            .filter { it.order < nextStop.order }
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
