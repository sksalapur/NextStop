package com.yourteam.nextstop.utils

import com.google.firebase.database.FirebaseDatabase
import com.yourteam.nextstop.models.LiveLocation
import com.yourteam.nextstop.models.Route
import kotlinx.coroutines.*
import kotlin.coroutines.coroutineContext
import kotlin.math.*

object MockLocationSimulator {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var toCollegeJob: Job? = null
    private var fromCollegeJob: Job? = null
    
    // Flags exposed so the UI knows which simulations are running
    val isToCollegeRunning = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isFromCollegeRunning = kotlinx.coroutines.flow.MutableStateFlow(false)
    
    // Legacy combined flag for backward compat
    val isRunning = kotlinx.coroutines.flow.MutableStateFlow(false)

    /**
     * Starts the simulation for routes of a specific direction.
     * @param direction "to_college" or "from_college"
     */
    fun startSimulation(routes: List<Route>, direction: String) {
        val database = FirebaseDatabase.getInstance().reference.child("live_location")
        
        // Filter routes to only the requested direction, one per bus
        val filtered = routes
            .filter { it.direction == direction }
            .distinctBy { it.busId }
        
        if (filtered.isEmpty()) return
        
        // Auto-stop the opposite direction (same buses, can't run both)
        val opposite = if (direction == "to_college") "from_college" else "to_college"
        stopSimulation(routes, opposite)
        
        when (direction) {
            "to_college" -> {
                if (toCollegeJob?.isActive == true) return
                isToCollegeRunning.value = true
                isRunning.value = true
                toCollegeJob = scope.launch {
                    filtered.forEach { route ->
                        if (route.busId.isNotEmpty() && route.stops.isNotEmpty()) {
                            launch { simulateRoute(route.busId, route, database) }
                        }
                    }
                }
            }
            "from_college" -> {
                if (fromCollegeJob?.isActive == true) return
                isFromCollegeRunning.value = true
                isRunning.value = true
                fromCollegeJob = scope.launch {
                    filtered.forEach { route ->
                        if (route.busId.isNotEmpty() && route.stops.isNotEmpty()) {
                            launch { simulateRoute(route.busId, route, database) }
                        }
                    }
                }
            }
        }
    }

    /**
     * Stops the simulation for routes of a specific direction.
     */
    fun stopSimulation(routes: List<Route>, direction: String) {
        val database = FirebaseDatabase.getInstance().reference.child("live_location")
        
        when (direction) {
            "to_college" -> {
                toCollegeJob?.cancel()
                toCollegeJob = null
                isToCollegeRunning.value = false
            }
            "from_college" -> {
                fromCollegeJob?.cancel()
                fromCollegeJob = null
                isFromCollegeRunning.value = false
            }
        }
        
        isRunning.value = isToCollegeRunning.value || isFromCollegeRunning.value
        
        // Mark buses for this direction as inactive
        scope.launch {
            routes.filter { it.direction == direction }.distinctBy { it.busId }.forEach { route ->
                if (route.busId.isNotEmpty()) {
                    database.child(route.busId).child("active").setValue(false)
                }
            }
        }
    }
    
    /**
     * Stops ALL simulations.
     */
    fun stopAll(routes: List<Route>) {
        stopSimulation(routes, "to_college")
        stopSimulation(routes, "from_college")
    }

    private suspend fun simulateRoute(busId: String, route: Route, dbRef: com.google.firebase.database.DatabaseReference) {
        val stops = route.stops.sortedBy { it.order }
        if (stops.size < 2) return

        var currentStopIndex = 0
        var nextStopIndex = 1
        
        // Settings for realistic simulation
        val speedKmh = 45.0 // Average speed in km/h
        val updateIntervalSeconds = 3
        val distancePerTickKm = (speedKmh / 3600.0) * updateIntervalSeconds
        
        var currentLat = stops[currentStopIndex].latitude
        var currentLng = stops[currentStopIndex].longitude

        while (coroutineContext.isActive) {
            val targetStop = stops[nextStopIndex]
            val distTotalKm = calculateDistanceKm(currentLat, currentLng, targetStop.latitude, targetStop.longitude)
            
            if (distTotalKm <= distancePerTickKm) {
                currentLat = targetStop.latitude
                currentLng = targetStop.longitude
                
                currentStopIndex = nextStopIndex
                nextStopIndex = (nextStopIndex + 1) % stops.size
                
                if (nextStopIndex == 0) {
                    currentStopIndex = 0
                    nextStopIndex = 1
                    currentLat = stops[currentStopIndex].latitude
                    currentLng = stops[currentStopIndex].longitude
                    delay(3000)
                    continue
                }
            } else {
                val fraction = distancePerTickKm / distTotalKm
                currentLat += (targetStop.latitude - currentLat) * fraction
                currentLng += (targetStop.longitude - currentLng) * fraction
            }
            
            val bearing = calculateBearing(currentLat, currentLng, targetStop.latitude, targetStop.longitude)
            
            val liveLocation = LiveLocation(
                latitude = currentLat,
                longitude = currentLng,
                speed = (speedKmh / 3.6).toFloat(),
                bearing = bearing.toFloat(),
                timestamp = System.currentTimeMillis(),
                active = true,
                direction = route.direction
            )
            
            dbRef.child(busId).setValue(liveLocation)
            delay(updateIntervalSeconds * 1000L)
        }
    }

    private fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }

    private fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLon = Math.toRadians(lon2 - lon1)
        val l1 = Math.toRadians(lat1)
        val l2 = Math.toRadians(lat2)
        
        val y = sin(dLon) * cos(l2)
        val x = cos(l1) * sin(l2) - sin(l1) * cos(l2) * cos(dLon)
        val bearing = atan2(y, x)
        return (Math.toDegrees(bearing) + 360) % 360
    }
}
