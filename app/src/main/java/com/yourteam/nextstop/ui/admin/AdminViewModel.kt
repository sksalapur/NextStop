package com.yourteam.nextstop.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourteam.nextstop.data.repository.AdminRepository
import com.yourteam.nextstop.models.Bus
import com.yourteam.nextstop.models.CollegeLocation
import com.yourteam.nextstop.models.LiveLocation
import com.yourteam.nextstop.models.Route
import com.yourteam.nextstop.models.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val repository: AdminRepository
) : ViewModel() {

    // ─── State Flows ──────────────────────────────────────────────────

    val collegeLocation: StateFlow<CollegeLocation?> = repository.observeCollegeLocation()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val buses: StateFlow<List<Bus>> = repository.observeBuses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val routes: StateFlow<List<Route>> = repository.observeRoutes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val drivers: StateFlow<List<User>> = repository.observeDrivers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val liveLocations: StateFlow<Map<String, LiveLocation>> = repository.observeAllLiveLocations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // ─── Operations ───────────────────────────────────────────────────

    fun updateCollegeLocation(location: CollegeLocation, onResult: (Boolean, String) -> Unit) {
        if (location.name.isBlank()) {
            onResult(false, "Location name cannot be blank")
            return
        }
        viewModelScope.launch {
            try {
                repository.setCollegeLocation(location)
                onResult(true, "College location updated")
            } catch (e: Exception) {
                onResult(false, e.message ?: "Failed to update location")
            }
        }
    }

    /**
     * Assigns a driver to a specific route. Enforces validation so a driver
     * cannot be assigned to two routes with the exact same departure time.
     */
    fun assignDriver(routeId: String, newDriverUid: String, onResult: (Boolean, String) -> Unit) {
        val targetRoute = routes.value.find { it.routeId == routeId }
        if (targetRoute == null) {
            onResult(false, "Route not found")
            return
        }

        // Validate time conflict: Is this driver already assigned to another route at the EXACT same time?
        val timeConflict = routes.value.any { 
            it.assignedDriverId == newDriverUid && 
            it.routeId != routeId && 
            it.departureTime == targetRoute.departureTime
        }

        if (timeConflict) {
            onResult(false, "Driver is already scheduled for another route at ${targetRoute.departureTime}")
            return
        }

        viewModelScope.launch {
            try {
                repository.assignDriverToRoute(routeId, newDriverUid)
                onResult(true, "Driver assigned successfully")
            } catch (e: Exception) {
                onResult(false, e.message ?: "Failed to assign driver")
            }
        }
    }

    fun unassignDriver(routeId: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.unassignDriverFromRoute(routeId)
                onResult(true, "Driver unassigned")
            } catch (e: Exception) {
                onResult(false, e.message ?: "Failed to unassign driver")
            }
        }
    }

    /**
     * Adds a new bus just by its number.
     */
    fun addBus(busNumber: String, onResult: (Boolean, String) -> Unit) {
        val normalizedNumber = busNumber.trim()
        
        if (normalizedNumber.isEmpty()) {
            onResult(false, "Bus number cannot be empty")
            return
        }
        if (normalizedNumber.length !in 3..15) {
            onResult(false, "Bus number must be between 3 and 15 characters")
            return
        }
        
        // Uniqueness check
        val isDuplicate = buses.value.any { it.busNumber.equals(normalizedNumber, ignoreCase = true) }
        if (isDuplicate) {
            onResult(false, "Bus number '$normalizedNumber' already exists")
            return
        }

        viewModelScope.launch {
            try {
                val newBus = Bus(busNumber = normalizedNumber)
                repository.addBus(newBus)
                onResult(true, "Bus added successfully")
            } catch (e: Exception) {
                onResult(false, e.message ?: "Failed to add bus")
            }
        }
    }

    fun updateBus(busId: String, busNumber: String, onResult: (Boolean, String) -> Unit) {
        val normalizedNumber = busNumber.trim()
        if (normalizedNumber.isEmpty() || normalizedNumber.length !in 3..15) {
            onResult(false, "Invalid bus number")
            return
        }
        viewModelScope.launch {
            try {
                repository.updateBus(busId, normalizedNumber)
                onResult(true, "Bus updated successfully")
            } catch (e: Exception) {
                onResult(false, e.message ?: "Failed to update bus")
            }
        }
    }

    fun deleteBus(busId: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteBus(busId)
                onResult(true, "Bus deleted successfully")
            } catch (e: Exception) {
                onResult(false, e.message ?: "Failed to delete bus")
            }
        }
    }

    /**
     * Adds a new route with its stops.
     */
    fun addRoute(route: Route, onResult: (Boolean, String) -> Unit) {
        if (route.name.isBlank()) {
            onResult(false, "Route name cannot be empty")
            return
        }
        if (route.stops.isEmpty()) {
            onResult(false, "Route must have at least one stop")
            return
        }
        if (route.busId.isBlank()) {
            onResult(false, "Please assign a bus to this route")
            return
        }

        viewModelScope.launch {
            try {
                repository.addRoute(route)
                onResult(true, "Route added successfully")
            } catch (e: Exception) {
                onResult(false, e.message ?: "Failed to add route")
            }
        }
    }

    fun updateRoute(route: Route, onResult: (Boolean, String) -> Unit) {
        if (route.name.isBlank() || route.stops.isEmpty()) {
            onResult(false, "Invalid route details")
            return
        }
        viewModelScope.launch {
            try {
                repository.updateRoute(route)
                onResult(true, "Route updated successfully")
            } catch (e: Exception) {
                onResult(false, e.message ?: "Failed to update route")
            }
        }
    }

    fun deleteRoute(routeId: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteRoute(routeId)
                onResult(true, "Route deleted successfully")
            } catch (e: Exception) {
                onResult(false, e.message ?: "Failed to delete route")
            }
        }
    }

    fun deleteDriver(driverUid: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteDriver(driverUid)
                onResult(true, "Driver deleted successfully")
            } catch (e: Exception) {
                onResult(false, e.message ?: "Failed to delete driver")
            }
        }
    }

    fun updateDriverName(driverUid: String, newName: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.updateDriverName(driverUid, newName)
                onResult(true, "Driver name updated successfully")
            } catch (e: Exception) {
                onResult(false, e.message ?: "Failed to update driver name")
            }
        }
    }

    fun inviteDriver(email: String, onResult: (Boolean, String) -> Unit) {
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            onResult(false, "Please enter a valid email address")
            return
        }
        viewModelScope.launch {
            try {
                repository.inviteDriver(email.trim())
                onResult(true, "Driver invited successfully")
            } catch (e: Exception) {
                onResult(false, e.message ?: "Failed to invite driver")
            }
        }
    }
}
