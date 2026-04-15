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
import kotlinx.coroutines.launch
import javax.inject.Inject

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

    private val _connectionStatus = MutableStateFlow("Connecting...")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private val _isBusActive = MutableStateFlow(false)
    val isBusActive: StateFlow<Boolean> = _isBusActive.asStateFlow()

    private val _passedStopIds = MutableStateFlow<Set<String>>(emptySet())
    val passedStopIds: StateFlow<Set<String>> = _passedStopIds.asStateFlow()

    init {
        loadStudentData()
        
        viewModelScope.launch {
            routes.collectLatest {
                val route = _selectedRoute.value
                if (route != null) {
                    loadRouteStops(route)
                    updateTrackingState(_busLocation.value)
                }
            }
        }
        
        viewModelScope.launch {
            liveLocations.collectLatest { locations ->
                val route = _selectedRoute.value ?: return@collectLatest
                val location = locations[route.routeId]
                updateTrackingState(location)
            }
        }
    }

    private fun loadStudentData() {
        viewModelScope.launch {
            val uid = repository.getCurrentUid()
            if (uid != null) {
                _assignedStopId.value = repository.getAssignedStopId(uid)
            }
            _isLoading.value = false
        }
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

        _routeStops.value = rawStops.map { stop ->
            if (stop.stopId.isEmpty()) stop.copy(stopId = "temp_${stop.stopName}") else stop
        }
    }

    fun selectRoute(route: Route?) {
        _selectedRoute.value = route
        if (route == null) {
            _routeStops.value = emptyList()
            _busLocation.value = null
            _isBusActive.value = false
            _connectionStatus.value = "Connecting..."
            _etaMinutes.value = 0
            _boardingEtaMinutes.value = null
            _nextStopName.value = ""
            _passedStopIds.value = emptySet()
        } else {
            loadRouteStops(route)
            val location = liveLocations.value[route.routeId]
            updateTrackingState(location)
        }
    }

    private fun updateTrackingState(location: LiveLocation?) {
        _busLocation.value = location
        _connectionStatus.value = LocationUtils.connectionStatus(location)
        _isBusActive.value = location?.active == true

        if (location != null && location.active) {
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
}
