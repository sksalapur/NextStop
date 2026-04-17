package com.yourteam.nextstop.ui.driver

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourteam.nextstop.data.repository.DriverRepository
import com.yourteam.nextstop.models.Route
import com.yourteam.nextstop.models.TripState
import com.yourteam.nextstop.service.LocationForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * State for the driver home UI.
 */
data class DriverUiState(
    val isLoading: Boolean = true,
    val assignedRoutes: List<Route> = emptyList(),
    val tripState: TripState = TripState.Stopped,
    val activeRouteId: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class DriverViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val driverRepository: DriverRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DriverUiState())
    val uiState: StateFlow<DriverUiState> = _uiState.asStateFlow()

    // Service binding
    private var service: LocationForegroundService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as LocationForegroundService.LocalBinder
            service = localBinder.getService()
            isBound = true

            // Observe service trip state and mirror it to our UI state
            viewModelScope.launch {
                localBinder.getService().tripState.collectLatest { tripState ->
                    _uiState.value = _uiState.value.copy(tripState = tripState)
                }
            }
            viewModelScope.launch {
                localBinder.getService().activeRouteId.collectLatest { routeId ->
                    _uiState.value = _uiState.value.copy(activeRouteId = routeId)
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            isBound = false
        }
    }

    init {
        observeAssignedRoutes()
        bindToServiceIfRunning()
    }

    /**
     * Observe the driver's assigned routes in real-time.
     */
    private fun observeAssignedRoutes() {
        viewModelScope.launch {
            val uid = driverRepository.getCurrentUid()
            if (uid == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Not signed in"
                )
                return@launch
            }

            driverRepository.observeAssignedRoutes(uid).collectLatest { routes ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    assignedRoutes = routes
                )
            }
        }
    }

    /**
     * Try binding to an already-running service instance
     */
    private fun bindToServiceIfRunning() {
        val intent = Intent(appContext, LocationForegroundService::class.java)
        appContext.bindService(intent, serviceConnection, 0)
    }

    /**
     * Start GPS tracking for a specific assigned route (which targets its associated bus).
     */
    fun startTrip(route: Route) {
        val intent = Intent(appContext, LocationForegroundService::class.java).apply {
            action = LocationForegroundService.ACTION_START
            putExtra(LocationForegroundService.EXTRA_ROUTE_ID, route.routeId)
            putExtra(LocationForegroundService.EXTRA_BUS_ID, route.busId)
        }
        appContext.startForegroundService(intent)

        _uiState.value = _uiState.value.copy(activeRouteId = route.routeId)

        // Bind to get live state updates
        appContext.bindService(
            Intent(appContext, LocationForegroundService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    /**
     * Stop GPS tracking.
     */
    fun stopTrip() {
        val intent = Intent(appContext, LocationForegroundService::class.java).apply {
            action = LocationForegroundService.ACTION_STOP
        }
        appContext.startService(intent)

        _uiState.value = _uiState.value.copy(
            tripState = TripState.Stopped,
            activeRouteId = null
        )
    }

    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            try {
                appContext.unbindService(serviceConnection)
            } catch (_: Exception) { /* already unbound */ }
            isBound = false
        }
    }
}
