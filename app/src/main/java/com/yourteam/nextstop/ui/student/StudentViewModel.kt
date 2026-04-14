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

    private val _selectedTrackingBus = MutableStateFlow<Bus?>(null)
    val selectedTrackingBus: StateFlow<Bus?> = _selectedTrackingBus.asStateFlow()

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

    init {
        loadStudentData()
        
        viewModelScope.launch {
            routes.collectLatest {
                val bus = _selectedTrackingBus.value
                if (bus != null) {
                    val route = it.find { r -> r.routeId == bus.routeId }
                    val rawStops = route?.stops?.sortedBy { s -> s.order } ?: emptyList()
                    // Dynamically heal corrupted stopIds so Selection UI successfully Maps ETA coordinates
                    _routeStops.value = rawStops.map { stop -> if (stop.stopId.isEmpty()) stop.copy(stopId = "temp_${stop.stopName}") else stop }
                    updateTrackingState(_busLocation.value)
                }
            }
        }
        
        viewModelScope.launch {
            liveLocations.collectLatest { locations ->
                val bus = _selectedTrackingBus.value ?: return@collectLatest
                val location = locations[bus.busId]
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

    fun selectBus(bus: Bus?) {
        _selectedTrackingBus.value = bus
        if (bus == null) {
            _routeStops.value = emptyList()
            _busLocation.value = null
            _isBusActive.value = false
            _connectionStatus.value = "Connecting..."
            _etaMinutes.value = 0
            _boardingEtaMinutes.value = null
            _nextStopName.value = ""
        } else {
            val route = routes.value.find { it.routeId == bus.routeId }
            val rawStops = route?.stops?.sortedBy { it.order } ?: emptyList()
            _routeStops.value = rawStops.map { stop -> if (stop.stopId.isEmpty()) stop.copy(stopId = "temp_${stop.stopName}") else stop }
            val location = liveLocations.value[bus.busId]
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
                    _boardingEtaMinutes.value = LocationUtils.calculateEtaMinutes(
                        busLat = location.latitude,
                        busLon = location.longitude,
                        stopLat = assignedStop.latitude,
                        stopLon = assignedStop.longitude,
                        speedKmh = speedKmh
                    )
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

        val distanceMeters = LocationUtils.haversineKm(
            location.latitude, location.longitude,
            assignedStop.latitude, assignedStop.longitude
        ) * 1000.0 

        if (distanceMeters < 500.0) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastNotifiedAt > 5 * 60 * 1000) {
                val busNum = _selectedTrackingBus.value?.busNumber ?: ""
                notificationService.sendProximityNotification(
                    busNumber = busNum,
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
