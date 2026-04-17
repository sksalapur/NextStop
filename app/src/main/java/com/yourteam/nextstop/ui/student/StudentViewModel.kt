package com.yourteam.nextstop.ui.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourteam.nextstop.data.repository.StudentRepository
import com.yourteam.nextstop.models.Bus
import com.yourteam.nextstop.models.LiveLocation
import com.yourteam.nextstop.models.Route
import com.yourteam.nextstop.models.Stop
import com.yourteam.nextstop.util.LocationUtils
import com.yourteam.nextstop.service.ProximityNotificationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class Direction { TO_COLLEGE, TO_HOME, ALL_SCHEDULED }

data class BusWithEta(
    val bus: Bus,
    val route: Route,
    val etaMinutes: Int,
    val isLive: Boolean
)

sealed class LocationFreshness {
    object Fresh : LocationFreshness()
    data class Stale(val minutesAgo: Int) : LocationFreshness()
    data class Unavailable(val minutesAgo: Int) : LocationFreshness()
}

@HiltViewModel
class StudentViewModel @Inject constructor(
    private val repository: StudentRepository,
    private val notificationService: ProximityNotificationService
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val buses: StateFlow<List<Bus>> = repository.observeBuses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val routes: StateFlow<List<Route>> = repository.observeRoutes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val liveLocations: StateFlow<Map<String, LiveLocation>> = repository.observeAllLiveLocations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _selectedRoute = MutableStateFlow<Route?>(null)
    val selectedRoute: StateFlow<Route?> = _selectedRoute.asStateFlow()

    private val _assignedStopId = MutableStateFlow<String?>(null)
    val assignedStopId: StateFlow<String?> = _assignedStopId.asStateFlow()

    private val _alertEnabled = MutableStateFlow(false)
    val alertEnabled: StateFlow<Boolean> = _alertEnabled.asStateFlow()

    private var lastNotifiedAt: Long = 0L

    private val _routeStops = MutableStateFlow<List<Stop>>(emptyList())
    val routeStops: StateFlow<List<Stop>> = _routeStops.asStateFlow()

    private val _busLocation = MutableStateFlow<LiveLocation?>(null)
    val busLocation: StateFlow<LiveLocation?> = _busLocation.asStateFlow()

    private val _etaMinutes = MutableStateFlow(0)
    val etaMinutes: StateFlow<Int> = _etaMinutes.asStateFlow()

    private val _boardingEtaMinutes = MutableStateFlow<Int?>(null)
    val boardingEtaMinutes: StateFlow<Int?> = _boardingEtaMinutes.asStateFlow()

    private val _nextStopName = MutableStateFlow("")
    val nextStopName: StateFlow<String> = _nextStopName.asStateFlow()

    private val _isBusActive = MutableStateFlow(false)
    val isBusActive: StateFlow<Boolean> = _isBusActive.asStateFlow()

    private val _passedStopIds = MutableStateFlow<Set<String>>(emptySet())
    val passedStopIds: StateFlow<Set<String>> = _passedStopIds.asStateFlow()

    private val _timeTicker = MutableStateFlow(System.currentTimeMillis())

    val locationFreshness: StateFlow<LocationFreshness> = combine(
        _busLocation,
        _timeTicker
    ) { location, _ ->
        if (location == null) return@combine LocationFreshness.Unavailable(0)
        
        val ageMs = System.currentTimeMillis() - location.timestamp
        val minutesAgo = (ageMs / 60_000).toInt()

        when {
            !location.active || minutesAgo > 10 -> LocationFreshness.Unavailable(minutesAgo)
            minutesAgo in 2..10 -> LocationFreshness.Stale(minutesAgo)
            else -> LocationFreshness.Fresh
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LocationFreshness.Unavailable(0))

    // Onboarding gate — true means HomeStopSetupScreen should show
    private val _needsHomeStopSetup = MutableStateFlow<Boolean?>(null) // null = still checking
    val needsHomeStopSetup: StateFlow<Boolean?> = _needsHomeStopSetup.asStateFlow()

    private val _direction = MutableStateFlow(Direction.TO_COLLEGE)
    val direction: StateFlow<Direction> = _direction.asStateFlow()

    private val _homeStop = MutableStateFlow<Stop?>(null)
    val homeStop: StateFlow<Stop?> = _homeStop.asStateFlow()

    private val _homeStopId = MutableStateFlow<String?>(null)

    // Deduplicated list of all stops across all routes for the inline dropdown
    val availableStops: StateFlow<List<Stop>> = routes.map { routeList ->
        routeList.flatMap { it.stops }
            .distinctBy { it.stopName.trim().lowercase() }
            .sortedBy { it.stopName }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun changeHomeStop(stop: Stop) {
        viewModelScope.launch {
            val uid = repository.getCurrentUid() ?: return@launch
            repository.setHomeStopId(uid, stop.stopName)
            _homeStop.value = stop
            _needsHomeStopSetup.value = false
        }
    }

    val filteredBuses: StateFlow<List<BusWithEta>> = combine(
        _direction,
        _homeStop,
        buses,
        routes,
        liveLocations
    ) { dir, homeStopObj, busList, routeList, locs ->
        if (homeStopObj == null || busList.isEmpty() || routeList.isEmpty()) {
            return@combine emptyList()
        }

        // Target direction string based on enum
        val activeRoutes = if (dir == Direction.ALL_SCHEDULED) {
            routeList
        } else {
            val targetDirection = if (dir == Direction.TO_COLLEGE) "to_college" else "from_college"
            routeList.filter { route ->
                route.direction == targetDirection && route.stops.any { it.stopName == homeStopObj.stopName }
            }
        }

        activeRoutes.mapNotNull { route ->
            val bus = busList.find { it.busId == route.busId } ?: return@mapNotNull null
            val loc = locs[route.busId]
            
            // Re-apply the same direction match logic as updateTrackingState
            val directionMatch = loc?.direction.isNullOrEmpty() || 
                                 route.direction.isNullOrEmpty() || 
                                 loc?.direction == route.direction
            val isLive = loc?.active == true && directionMatch

            var eta = -1
            if (isLive && loc != null) {
                // Determine ETA to the homeStop if applicable
                val hStop = route.stops.find { it.stopName == homeStopObj.stopName }
                if (hStop != null) {
                    val passedIds = com.yourteam.nextstop.util.LocationUtils.getPassedStopIds(
                        loc.latitude, loc.longitude, route.stops
                    )
                    if (passedIds.contains(hStop.stopId)) {
                        eta = -2 // Let's use -2 to indicate "Arrived/Passed"
                    } else {
                        val speedKmh = loc.speed * 3.6f
                        eta = com.yourteam.nextstop.util.LocationUtils.calculateEtaMinutes(
                            busLat = loc.latitude, busLon = loc.longitude,
                            stopLat = hStop.latitude, stopLon = hStop.longitude,
                            speedKmh = speedKmh
                        )
                    }
                }
            }

            BusWithEta(bus, route, eta, isLive)
        }.sortedWith(compareBy {
            // Natural sort: extract trailing number for numeric comparison
            val match = Regex("(.*?)(\\d+)$").find(it.route.name)
            if (match != null) {
                val prefix = match.groupValues[1]
                val num = match.groupValues[2].padStart(10, '0')
                prefix + num
            } else {
                it.route.name
            }
        })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())



    init {
        loadStudentData()
        
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(30_000)
                _timeTicker.value = System.currentTimeMillis()
            }
        }

        viewModelScope.launch {
            routes.collectLatest { routeList ->
                val route = _selectedRoute.value
                if (route != null) {
                    loadRouteStops(route)
                    updateTrackingState(_busLocation.value)
                }
                // If homeStop no longer exists in any route, clear it
                val currentStop = _homeStop.value
                if (currentStop != null && routeList.isNotEmpty()) {
                    val stillExists = routeList.any { r -> r.stops.any { it.stopName == currentStop.stopName } }
                    if (!stillExists) {
                        _homeStop.value = null
                        _needsHomeStopSetup.value = true
                    }
                } else if (currentStop != null && routeList.isEmpty()) {
                    _homeStop.value = null
                    _needsHomeStopSetup.value = true
                }
            }
        }
        
        viewModelScope.launch {
            liveLocations.collectLatest { locations ->
                val route = _selectedRoute.value ?: return@collectLatest
                val location = locations[route.busId]
                updateTrackingState(location)
            }
        }
    }

    private fun loadStudentData() {
        viewModelScope.launch {
            val uid = repository.getCurrentUid()
            if (uid != null) {
                _assignedStopId.value = repository.getAssignedStopId(uid)
                _alertEnabled.value = repository.getAlertEnabled(uid)
                val homeId = repository.getHomeStopId(uid)
                _homeStopId.value = homeId
                _needsHomeStopSetup.value = homeId.isNullOrBlank()

                if (!homeId.isNullOrBlank()) {
                    // Fetch the actual stop object representing home Stop
                    // homeId is stored as the stop NAME (or legacy stopId), so match by name first, then id
                    val allRoutes = repository.getAllRoutes()
                    val allStops = allRoutes.flatMap { it.stops }
                    val stop = allStops.find { it.stopName == homeId }
                        ?: allStops.find { it.stopId == homeId }
                    _homeStop.value = stop
                }
            } else {
                _needsHomeStopSetup.value = false // not signed in, let auth handle it
            }
            _isLoading.value = false
        }
    }

    fun setDirection(dir: Direction) {
        _direction.value = dir
    }

    private fun loadRouteStops(route: Route) {
        val rawStops = route.stops.sortedBy { it.order }.toMutableList()
        // Synthesize an end Stop if the route has an end location
        if (route.endName.isNotBlank() && route.endLat != 0.0) {
            val maxOrder = rawStops.maxOfOrNull { it.order } ?: 0
            rawStops.add(Stop(
                stopId = "endpoint_${route.routeId}",
                stopName = route.endName,
                latitude = route.endLat,
                longitude = route.endLng,
                order = maxOrder + 1
            ))
        }

        // Deduplicate by stop name — keep the first occurrence (lower order) to prevent
        // the last stop appearing twice when the seeder already includes the destination
        val deduped = rawStops.distinctBy { it.stopName }

        _routeStops.value = deduped.map { stop ->
            if (stop.stopId.isEmpty()) stop.copy(stopId = "temp_${stop.stopName}") else stop
        }
    }

    fun selectRoute(route: Route?) {
        _selectedRoute.value = route
        if (route == null) {
            _routeStops.value = emptyList()
            _busLocation.value = null
            _isBusActive.value = false
            _etaMinutes.value = 0
            _boardingEtaMinutes.value = null
            _nextStopName.value = ""
            _passedStopIds.value = emptySet()
        } else {
            loadRouteStops(route)
            val location = liveLocations.value[route.busId]
            updateTrackingState(location)
        }
    }

    private fun updateTrackingState(location: LiveLocation?) {
        _busLocation.value = location
        
        // Check direction mismatch: if the bus is running the opposite direction,
        // treat it as inactive for the route being viewed
        val route = _selectedRoute.value
        val directionMatch = location?.direction.isNullOrEmpty() || 
                             route?.direction.isNullOrEmpty() ||
                             location?.direction == route?.direction
        
        _isBusActive.value = location?.active == true && directionMatch

        if (location != null && location.active && directionMatch) {
            val stops = _routeStops.value
            if (stops.isEmpty()) return

            val passed = LocationUtils.getPassedStopIds(
                location.latitude, location.longitude, stops
            )
            _passedStopIds.value = passed

            val nextStop = LocationUtils.findNextStop(
                location.latitude,
                location.longitude,
                stops
            )

            if (nextStop != null) {
                _nextStopName.value = nextStop.stopName
                val speedKmh = location.speed * 3.6f
                _etaMinutes.value = LocationUtils.calculateEtaMinutes(
                    busLat = location.latitude,
                    busLon = location.longitude,
                    stopLat = nextStop.latitude,
                    stopLon = nextStop.longitude,
                    speedKmh = speedKmh
                )
                
                val assignedStopId = _assignedStopId.value
                val assignedStop = stops.find { it.stopId == assignedStopId }
                if (assignedStop != null) {
                    if (passed.contains(assignedStopId)) {
                        _boardingEtaMinutes.value = -1
                    } else {
                        _boardingEtaMinutes.value = LocationUtils.calculateEtaMinutes(
                            busLat = location.latitude,
                            busLon = location.longitude,
                            stopLat = assignedStop.latitude,
                            stopLon = assignedStop.longitude,
                            speedKmh = speedKmh
                        )
                    }
                } else {
                    _boardingEtaMinutes.value = null
                }
                
                checkProximity(location, stops)
            }
        }
    }

    private fun checkProximity(location: LiveLocation, stops: List<Stop>) {
        val assignedStopId = _assignedStopId.value ?: return
        val assignedStop = stops.find { it.stopId == assignedStopId } ?: return

        if (_passedStopIds.value.contains(assignedStopId)) return

        val distanceMeters = LocationUtils.haversineKm(
            location.latitude, location.longitude,
            assignedStop.latitude, assignedStop.longitude
        ) * 1000.0 

        if (distanceMeters < 500.0) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastNotifiedAt > 5 * 60 * 1000) {
                val routeName = _selectedRoute.value?.name ?: ""
                notificationService.sendProximityNotification(
                    busNumber = routeName,
                    stopName = assignedStop.stopName
                )
                lastNotifiedAt = currentTime
            }
        }
    }

    fun updateAssignedStop(stopId: String) {
        viewModelScope.launch {
            try {
                _assignedStopId.value = stopId
                lastNotifiedAt = 0L 
                updateTrackingState(_busLocation.value)
                
                val uid = repository.getCurrentUid() ?: return@launch
                repository.updateAssignedStopId(uid, stopId)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to update stop"
            }
        }
    }

    fun toggleAlertEnabled() {
        viewModelScope.launch {
            val uid = repository.getCurrentUid() ?: return@launch
            val newState = !_alertEnabled.value
            repository.updateAlertEnabled(uid, newState)
            _alertEnabled.value = newState
        }
    }
}
