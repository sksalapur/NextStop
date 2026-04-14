package com.yourteam.nextstop.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourteam.nextstop.data.repository.AdminRepository
import com.yourteam.nextstop.models.Bus
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

    val buses: StateFlow<List<Bus>> = repository.observeBuses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val routes: StateFlow<List<Route>> = repository.observeRoutes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val drivers: StateFlow<List<User>> = repository.observeDrivers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val liveLocations: StateFlow<Map<String, LiveLocation>> = repository.observeAllLiveLocations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // ─── Operations ───────────────────────────────────────────────────

    /**
     * Batch assigns a driver to a bus, clearing the old driver if applicable.
     */
    fun assignDriver(busId: String, newDriverUid: String, oldDriverUid: String?, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.assignDriverToBus(busId, newDriverUid, oldDriverUid)
                onResult(true, "Driver assigned successfully")
            } catch (e: Exception) {
                onResult(false, e.message ?: "Failed to assign driver")
            }
        }
    }

    /**
     * Tries to add a new bus. Enforces validation rules:
     * 1. Not empty
     * 2. 3-15 chars
     * 3. Unique bus number
     */
    fun addBus(busNumber: String, routeId: String, onResult: (Boolean, String) -> Unit) {
        val normalizedNumber = busNumber.trim()
        
        if (normalizedNumber.isEmpty()) {
            onResult(false, "Bus number cannot be empty")
            return
        }
        if (normalizedNumber.length !in 3..15) {
            onResult(false, "Bus number must be between 3 and 15 characters")
            return
        }
        
        // Uniqueness check using current snapshot flow state
        val isDuplicate = buses.value.any { it.busNumber.equals(normalizedNumber, ignoreCase = true) }
        if (isDuplicate) {
            onResult(false, "Bus number '$normalizedNumber' already exists")
            return
        }

        viewModelScope.launch {
            try {
                val newBus = Bus(
                    busNumber = normalizedNumber,
                    routeId = routeId,
                    status = "inactive"
                )
                repository.addBus(newBus)
                onResult(true, "Bus added successfully")
            } catch (e: Exception) {
                onResult(false, e.message ?: "Failed to add bus")
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

        viewModelScope.launch {
            try {
                repository.addRoute(route)
                onResult(true, "Route added successfully")
            } catch (e: Exception) {
                onResult(false, e.message ?: "Failed to add route")
            }
        }
    }

    fun updateBus(busId: String, busNumber: String, routeId: String, onResult: (Boolean, String) -> Unit) {
        val normalizedNumber = busNumber.trim()
        if (normalizedNumber.isEmpty() || normalizedNumber.length !in 3..15) {
            onResult(false, "Invalid bus number")
            return
        }
        viewModelScope.launch {
            try {
                repository.updateBus(busId, normalizedNumber, routeId)
                onResult(true, "Bus updated successfully")
            } catch (e: Exception) {
                onResult(false, e.message ?: "Failed to update bus")
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

    /**
     * Invite a new driver by email via the whitelisting system.
     */
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
